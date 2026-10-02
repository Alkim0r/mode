package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Разведка и диверсии. Агенты (до 3 на королевство) выполняют операции: чем больше агентов, навык «Плутовство»
 * и знаний о цели — и чем меньше настороженность врага — тем выше шанс. Успех остаётся тайной (репутация не страдает);
 * провал может раскрыть агента: тогда дипломатический скандал.
 */
public final class Espionage {
    private Espionage() {}

    public enum Op {
        SCOUT("Разведка", "Узнать состав гарнизона, силу и сроки армии", 10, 70, true),
        STEAL("Кража казны", "Умыкнуть из их казны 30–80 монет", 20, 40, false),
        SABOTAGE("Диверсия на складах", "Гарнизон слабеет на 2 бойца: следующая армия меньше", 40, 40, false),
        POISON("Порча колодцев", "Следующий поход отложен на ~5 минут", 35, 45, false),
        BRIBE("Подкуп командиров", "Следующая армия выйдет с подорванным духом (−30)", 60, 35, false),
        SIEGE("Порча осадных машин", "Следующая армия придёт без башни и катапульт", 55, 35, false);

        public final String title, desc;
        public final int cost, baseChance;
        public final boolean safe;

        Op(String title, String desc, int cost, int baseChance, boolean safe) {
            this.title = title;
            this.desc = desc;
            this.cost = cost;
            this.baseChance = baseChance;
            this.safe = safe;
        }

        public static Op byName(String n) {
            for (Op o : values()) if (o.name().equalsIgnoreCase(n)) return o;
            return null;
        }
    }

    public static final int MAX_AGENTS = 3;
    private static final Map<Op, Boolean> UNUSED = new EnumMap<>(Op.class);
    private static final Random RNG = new Random();

    /** Шанс успеха операции (0..95). */
    public static int chance(Realm r, Op op, int roguery) {
        int c = op.baseChance + 9 * r.agents + roguery / 3 + r.intel / 8 - r.suspicion / 3;
        return Math.max(5, Math.min(95, c));
    }

    public static String send(ServerPlayer p, KingdomData data, Realm r, City c) {
        if (!Science.has(p.server, p.getUUID(), Science.Tech.ESPIONAGE)) return "Для тайной службы нужна технология «Шпионаж» (/regnum science).";
        if (r.agents >= MAX_AGENTS) return "В «" + r.name + "» уже работают " + MAX_AGENTS + " агента — больше не спрятать.";
        if (r.state == Realm.WAR && r.suspicion > 50) return "Во время войны границы закрыты: агента схватят.";
        if (c.treasury < 30) return "Для переброски агента нужно 30 монет в казне.";
        c.treasury -= 30;
        r.agents++;
        data.setDirty();
        return null;
    }

    /**
     * Выполнить операцию. Возвращает человекочитаемый итог (и пишет игроку). roll — 0..99 для детерминированных проверок.
     */
    public static String perform(ServerPlayer p, KingdomData data, Realm r, City c, Op op, int roll) {
        if (r.agents <= 0) return "В «" + r.name + "» нет ваших агентов. /regnum spy send <номер>";
        if (c.treasury < op.cost) return "Для операции нужно " + op.cost + " монет в казне.";
        long day = p.serverLevel().getDayTime() / 24000L;
        if (r.spyDay != day) {
            r.spyDay = day;
            r.spyOps = 0;
        }
        if (r.spyOps >= 4) return "Агенты в «" + r.name + "» затаились: больше четырёх операций в сутки — это верный провал. Ждите до завтра.";
        r.spyOps++;
        c.treasury -= op.cost;
        int rog = Skills.level(p, Skill.ROGUERY);
        int ch = chance(r, op, rog);
        boolean ok = roll < ch;
        Skills.addXp(p, Skill.ROGUERY, ok ? 5 : 2);
        String res;
        if (ok) {
            r.suspicion = Math.min(100, r.suspicion + (op.safe ? 2 : 6));
            r.intel = Math.min(100, r.intel + (op == Op.SCOUT ? 20 : 4));
            res = apply(p, r, op, day);
            data.setDirty();
            return "Успех (шанс был " + ch + "%): " + res + " Агенты остались незамеченными.";
        }
        // провал: чаще всего никто ничего не заметил
        boolean exposed = !op.safe && (roll >= 100 - (30 + r.suspicion / 2));
        r.suspicion = Math.min(100, r.suspicion + (exposed ? 25 : 8));
        if (exposed) {
            r.agents--;
            r.relation = Math.max(-100, r.relation - 25);
            r.note("раскрыт ваш шпион");
            data.setDirty();
            if (r.state == Realm.PEACE && r.relation <= -60) Realms.declareWar(p.serverLevel(), data, r, p);
            return "ПРОВАЛ (шанс " + ch + "%): агент схвачен, «" + r.name + "» знает, чьих рук дело! Отношения: " + r.relation + ".";
        }
        data.setDirty();
        return "Неудача (шанс " + ch + "%): ничего не вышло, но подозрений нет — агент затаился.";
    }

    /** Достоверность донесения (I061): чем меньше накоплено знаний о королевстве, тем сильнее агент ошибается (до ±25%). */
    public static int reported(int truth, int intel, Random rng) {
        int maxErr = Math.max(0, (100 - Math.min(100, intel)) / 4);
        if (maxErr == 0 || truth <= 0) return truth;
        double f = (rng.nextInt(2 * maxErr + 1) - maxErr) / 100.0;
        return Math.max(1, (int) Math.round(truth * (1 + f)));
    }

    /** Как лучше брать их укрепления, по составу гарнизона (I029): без угадывания, только по числам. */
    public static String assaultAdvice(Map<SoldierType, Integer> m) {
        int total = 0, shooters = 0, heavy = 0, cav = 0, gunners = 0;
        for (var e : m.entrySet()) {
            int n = e.getValue();
            total += n;
            var role = e.getKey().role;
            if (role == SoldierType.Role.ARCHER || role == SoldierType.Role.HORSE_ARCHER) shooters += n;
            else if (role == SoldierType.Role.GUNNER) gunners += n;
            else if (role == SoldierType.Role.HEAVY || role == SoldierType.Role.SHIELD) heavy += n;
            else if (role == SoldierType.Role.CAVALRY) cav += n;
        }
        if (total == 0) return "гарнизона нет — штурм не нужен, хватит небольшого отряда";
        if (gunners * 4 >= total) return "много стрелков с порохом: идите щитами и осадной башней, не стойте под залпом";
        if (shooters * 3 >= total) return "много лучников: берите щитников и башню, давите ночью или в дождь";
        if (heavy * 2 >= total) return "тяжёлая пехота: катапульты и стрелки издали, в ближний бой не лезьте";
        if (cav * 3 >= total) return "конница: стройте копейщиков в плотный ряд, коней остановят пики";
        return "смешанный гарнизон: берите всех родов войск поровну";
    }

    private static String apply(ServerPlayer p, Realm r, Op op, long day) {
        switch (op) {
            case SCOUT -> {
                r.known = 2;
                r.lastSpyDay = day;
                StringBuilder sb = new StringBuilder();
                Map<SoldierType, Integer> m = new EnumMap<>(SoldierType.class);
                int seen = reported(r.strength, r.intel, RNG);
                for (int i = 0; i < seen; i++) m.merge(Doctrine.garrison(r.culture, i), 1, Integer::sum);
                m.forEach((t, n) -> sb.append(t.title.toLowerCase()).append(' ').append(n).append(", "));
                String comp = sb.length() > 2 ? sb.substring(0, sb.length() - 2) : "пусто";
                String when = r.state == Realm.WAR ? "следующая армия — через ~" + Math.max(0, (r.armyTimer - p.serverLevel().getGameTime()) / 1200) + " мин"
                        : "армии пока не собирают";
                return "гарнизон «" + r.name + "»: " + (seen == r.strength ? "" : "по слухам ≈") + seen + " (" + comp + "), " + when + ". Совет разведчика: " + assaultAdvice(m) + ".";
            }
            case STEAL -> {
                int gold = Math.min(30 + RNG.nextInt(51), 10 + r.strength * 3);
                City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
                if (c != null) c.treasury += gold;
                return "в вашу казну доставлено " + gold + " монет.";
            }
            case SABOTAGE -> {
                r.strength = Math.max(4, r.strength - 2);
                return "склады горят, гарнизон «" + r.name + "» ослаблен до " + r.strength + ".";
            }
            case POISON -> {
                r.armyTimer = Math.min(r.armyTimer + 6000, p.serverLevel().getGameTime() + 18000);
                return "колодцы отравлены, поход отложен.";
            }
            case BRIBE -> {
                r.bribed = true;
                return "командиры подкуплены: армия выйдет с подорванным духом.";
            }
            case SIEGE -> {
                r.siegeSabotaged = true;
                return "осадные машины испорчены ещё в обозе.";
            }
        }
        return "";
    }

    // ------------------------------------------------------------------ контрразведка: нас тоже пытаются разведать

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        int tc = e.getServer().getTickCount();
        if (tc % 100 != 47 || !DayClock.newDay(e.getServer(), "counterspy")) return;
        ServerLevel ow = e.getServer().overworld();
        KingdomData data = KingdomData.get(e.getServer());
        for (Realm r : data.realms()) {
            r.suspicion = Math.max(0, r.suspicion - 4);
            // вражеские королевства засылают лазутчиков: башни и стража ловят их
            if (r.state != Realm.WAR || r.warOwner == null) continue;
            City c = data.nearestOwned(r.warOwner, r.capital());
            if (c == null) continue;
            ServerPlayer owner = e.getServer().getPlayerList().getPlayer(r.warOwner);
            if (owner == null || ow.random.nextInt(100) >= 35) continue;
            int towers = c.count(BuildingType.WATCHTOWER);
            int guard = c.soldiers.size();
            if (ow.random.nextInt(10) < Math.min(8, towers * 3 + guard / 6)) {
                c.glory += 2;
                Text.good(owner, "Стража «" + c.name + "» поймала вражеского лазутчика из «" + r.name + "»! +2 славы.");
            } else {
                int loss = Math.min(c.treasury, Math.max(1, c.treasury / 12 / (1 + c.count(BuildingType.WAREHOUSE))));
                c.treasury -= loss;
                Text.bad(owner, "Лазутчики «" + r.name + "» подожгли склады «" + c.name + "»: казна −" + loss + ". Башни и стража помогут ловить шпионов.");
            }
            data.setDirty();
        }
    }

    // ------------------------------------------------------------------ команды

    private static MutableComponent opLine(Realm r, int idx, int roguery) {
        MutableComponent all = Component.empty();
        for (Op op : Op.values()) {
            all.append(Text.button(op.title + " (" + op.cost + ", " + chance(r, op, roguery) + "%)",
                    "/regnum spy op " + idx + " " + op.name().toLowerCase(), op.desc + (op.safe ? " — без риска" : " — риск раскрытия")));
            all.append(Component.literal("  "));
        }
        return all;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("spy")
                .then(Commands.literal("list").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    Text.gold(p, "— Тайная служба —");
                    int rog = Skills.level(p, Skill.ROGUERY);
                    for (Realm r : data.realms()) {
                        if (r.known == 0) continue;
                        int idx = Realms.indexOf(data, r);
                        Text.info(p, "  " + idx + ". " + r.name + ": агентов " + r.agents + "/" + MAX_AGENTS + ", знания " + r.intel + "%, настороженность " + r.suspicion + "%");
                        if (r.agents > 0) p.sendSystemMessage(opLine(r, idx, rog));
                    }
                    Text.info(p, "Новый агент: /regnum spy send <номер> (30 монет). Операции: /regnum spy op <номер> <операция>");
                    return 1;
                }))
                .then(Commands.literal("send").then(Commands.argument("id", IntegerArgumentType.integer(1, 16)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    Realm r = Realms.byIndex(data, IntegerArgumentType.getInteger(ctx, "id"));
                    City c = Realms.cityOf(p, data);
                    if (r == null || c == null || r.known == 0) return 0;
                    String err = send(p, data, r, c);
                    if (err != null) Text.bad(p, err);
                    else {
                        Text.good(p, "Агент заброшен в «" + r.name + "». Теперь доступны операции: /regnum spy list");
                        p.sendSystemMessage(opLine(r, IntegerArgumentType.getInteger(ctx, "id"), Skills.level(p, Skill.ROGUERY)));
                    }
                    return err == null ? 1 : 0;
                })))
                .then(Commands.literal("op").then(Commands.argument("id", IntegerArgumentType.integer(1, 16))
                        .then(Commands.argument("op", StringArgumentType.word()).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            KingdomData data = KingdomData.get(p.server);
                            Realm r = Realms.byIndex(data, IntegerArgumentType.getInteger(ctx, "id"));
                            City c = Realms.cityOf(p, data);
                            Op op = Op.byName(StringArgumentType.getString(ctx, "op"));
                            if (r == null || c == null || op == null) {
                                Text.bad(p, "Неверная операция.");
                                return 0;
                            }
                            String res = perform(p, data, r, c, op, p.getRandom().nextInt(100));
                            if (res.startsWith("Успех")) Text.good(p, res); else Text.bad(p, res);
                            return 1;
                        })))))
        );
    }
}
