package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Древо знаний королевства (как в «Цивилизации»): от земледелия и бронзы до пороха, бомбард и полевой артиллерии.
 * Очки знаний копят библиотеки и университеты, управляющий ускоряет. Технологии открывают войска, здания и стены.
 */
public class Science extends SavedData {
    private static final String FILE = "regnum_science";

    public enum Tech {
        // --- Эра I: Заря (30)
        AGRICULTURE("Земледелие", 1, 30, "Зернохранилища и склады; +5% дохода городов", new Tech[]{}),
        BRONZE("Обработка бронзы", 1, 30, "Копейщики и щитники", new Tech[]{}),
        WRITING("Письменность", 1, 30, "Библиотека: даёт очки знаний", new Tech[]{}),
        MASONRY("Каменная кладка", 1, 30, "Каменные стены и сторожевые башни", new Tech[]{}),
        HUSBANDRY("Коневодство", 1, 30, "Конюшня: можно держать коней", new Tech[]{}),
        // --- Эра II: Античность (70)
        MATHEMATICS("Математика", 2, 70, "+10% очков знаний; осадные машины в армиях", new Tech[]{WRITING}),
        IRON("Обработка железа", 2, 70, "Кузница и рыцари", new Tech[]{BRONZE}),
        RIDING("Верховая езда", 2, 70, "Лёгкая конница", new Tech[]{HUSBANDRY}),
        COMPOSITE("Составной лук", 2, 70, "Конные лучники", new Tech[]{RIDING, BRONZE}),
        CONSTRUCTION("Строительство", 2, 70, "Крепостные стены; мастерская строит быстрее", new Tech[]{MASONRY, MATHEMATICS}),
        MEDICINE("Медицина", 2, 70, "Лазарет; лучше лечение, защита от эпидемий", new Tech[]{WRITING, AGRICULTURE}),
        TRADE("Торговля", 2, 70, "Караваны и торговые договоры дают больше", new Tech[]{WRITING, HUSBANDRY}),
        ESPIONAGE("Шпионаж", 2, 70, "Агенты и диверсии", new Tech[]{WRITING, BRONZE}),
        // --- Эра III: Средневековье (140)
        STEEL("Сталь", 3, 140, "Двуручники; ковка закалённых клинков", new Tech[]{IRON}),
        CHIVALRY("Рыцарство", 3, 140, "Тяжёлая конница", new Tech[]{RIDING, STEEL}),
        MACHINERY("Механика", 3, 140, "Арбалетчики", new Tech[]{MATHEMATICS, IRON}),
        EDUCATION("Образование", 3, 140, "Университет: много очков знаний", new Tech[]{WRITING, MATHEMATICS}),
        ENGINEERING("Инженерия", 3, 140, "Крепости культуры, осадные башни", new Tech[]{CONSTRUCTION, MACHINERY}),
        BANKING("Банковское дело", 3, 140, "Облигации выгоднее, казна больше", new Tech[]{TRADE, MATHEMATICS}),
        // --- Эра IV: Порох (260)
        CHEMISTRY("Химия", 4, 260, "Селитра, сера и чёрный порох", new Tech[]{EDUCATION, MEDICINE}),
        GUNPOWDER("Порох", 4, 260, "Гранаты и подрывные заряды", new Tech[]{CHEMISTRY, STEEL}),
        MUSKETRY("Мушкетёры", 4, 260, "Мушкетёр: дальний выстрел, пробивает броню", new Tech[]{GUNPOWDER, MACHINERY}),
        METALLURGY("Металлургия", 4, 260, "Бомбарда: тяжёлая осадная пушка", new Tech[]{GUNPOWDER, ENGINEERING}),
        BASTION("Бастионы", 4, 260, "Бастионные стены держат пушечный огонь", new Tech[]{GUNPOWDER, CONSTRUCTION}),
        ARTILLERY("Артиллерия", 4, 260, "Полевая пушка — вершина развития", new Tech[]{METALLURGY, MUSKETRY});

        public final String title, desc;
        public final int era, cost;
        public final Tech[] prereq;

        Tech(String title, int era, int cost, String desc, Tech[] prereq) {
            this.title = title;
            this.era = era;
            this.cost = cost;
            this.desc = desc;
            this.prereq = prereq;
        }

        public static Tech byName(String n) {
            for (Tech t : values()) if (t.name().equalsIgnoreCase(n)) return t;
            return null;
        }
    }

    public static final String[] ERAS = {"", "Заря", "Античность", "Средневековье", "Эра пороха"};

    public static class Kingdom {
        public final Set<Tech> done = EnumSet.noneOf(Tech.class);
        public Tech current;
        public int progress;
        public int perDay;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            ListTag l = new ListTag();
            for (Tech x : done) l.add(StringTag.valueOf(x.name()));
            t.put("done", l);
            if (current != null) t.putString("current", current.name());
            t.putInt("progress", progress);
            return t;
        }

        static Kingdom load(CompoundTag t) {
            Kingdom k = new Kingdom();
            for (Tag x : t.getList("done", Tag.TAG_STRING)) {
                Tech te = Tech.byName(x.getAsString());
                if (te != null) k.done.add(te);
            }
            if (t.contains("current")) k.current = Tech.byName(t.getString("current"));
            k.progress = t.getInt("progress");
            return k;
        }
    }

    private final Map<UUID, Kingdom> map = new HashMap<>();

    public static Science get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Science::new, Science::load), FILE);
    }

    private static Science load(CompoundTag tag, HolderLookup.Provider p) {
        Science s = new Science();
        CompoundTag m = tag.getCompound("map");
        for (String key : m.getAllKeys()) s.map.put(UUID.fromString(key), Kingdom.load(m.getCompound(key)));
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider p) {
        CompoundTag m = new CompoundTag();
        map.forEach((k, v) -> m.put(k.toString(), v.save()));
        tag.put("map", m);
        return tag;
    }

    public Kingdom of(UUID owner) {
        return map.computeIfAbsent(owner, k -> new Kingdom());
    }

    // ------------------------------------------------------------------ правила

    /** Открыта ли технология у правителя; null-технология открыта всегда. */
    public static boolean has(MinecraftServer server, UUID owner, @Nullable Tech t) {
        return t == null || Science.get(server).of(owner).done.contains(t);
    }

    public static boolean canResearch(Kingdom k, Tech t) {
        if (k.done.contains(t)) return false;
        for (Tech p : t.prereq) if (!k.done.contains(p)) return false;
        return true;
    }

    /** Очки знаний в сутки от городов правителя. */
    public static int dailyPoints(MinecraftServer server, UUID owner) {
        KingdomData data = KingdomData.get(server);
        Kingdom k = Science.get(server).of(owner);
        double sum = 0;
        for (City c : data.ownedBy(owner)) {
            sum += 1 + c.level;
            sum += 4 * c.count(BuildingType.LIBRARY);
            sum += 10 * c.count(BuildingType.UNIVERSITY);
            if (c.spec == 3) sum += 4 + c.level;
        }
        if (k.done.contains(Tech.MATHEMATICS)) sum *= 1.1;
        int mech = 0;
        for (City c : data.ownedBy(owner)) mech = Math.max(mech, c.mech);
        sum *= 1 + 0.05 * mech;
        ServerPlayer p = server.getPlayerList().getPlayer(owner);
        if (p != null) sum *= 1 + Skills.level(p, Skill.STEWARD) * 0.005;
        return (int) Math.round(sum);
    }

    /** Добавить очки и завершить исследования. Возвращает завершённую технологию или null. */
    @Nullable
    public Tech addPoints(UUID owner, int pts) {
        Kingdom k = of(owner);
        if (k.current == null) return null;
        k.progress += pts;
        setDirty();
        if (k.progress >= k.current.cost) {
            Tech done = k.current;
            k.done.add(done);
            k.progress -= done.cost;
            k.current = null;
            return done;
        }
        return null;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 100 != 37 || !DayClock.newDay(e.getServer(), "science")) return;
        MinecraftServer server = e.getServer();
        Science sc = Science.get(server);
        for (UUID owner : KingdomData.get(server).all().stream().map(c -> c.owner).distinct().toList()) {
            int pts = dailyPoints(server, owner);
            Kingdom k = sc.of(owner);
            k.perDay = pts;
            Tech done = sc.addPoints(owner, pts);
            ServerPlayer p = server.getPlayerList().getPlayer(owner);
            if (p == null) continue;
            if (done != null) {
                Text.gold(p, "✦ Открыта технология «" + done.title + "»! " + done.desc + ". Выберите следующую: /regnum science");
                Skills.chronicle(p, "Изучено: " + done.title);
            } else if (k.current == null) {
                Text.info(p, "Учёные скучают: выберите исследование (/regnum science). Очки знаний: +" + pts + "/день.");
            }
        }
    }

    // ------------------------------------------------------------------ команды

    private static MutableComponent techLine(Kingdom k, Tech t) {
        boolean done = k.done.contains(t);
        boolean can = canResearch(k, t);
        MutableComponent c = Component.literal("  " + (done ? "✔ " : can ? "▸ " : "· ") + t.title + " (" + t.cost + ") — " + t.desc);
        c.withStyle(done ? ChatFormatting.GREEN : can ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY);
        if (can && k.current != t) {
            c.append(Text.button(" [изучать]", "/regnum science research " + t.name().toLowerCase(), "Начать исследование «" + t.title + "»"));
        }
        if (!done && !can) {
            StringBuilder need = new StringBuilder();
            for (Tech p : t.prereq) if (!k.done.contains(p)) need.append(p.title).append(", ");
            if (need.length() > 2) c.append(Component.literal(" (нужно: " + need.substring(0, need.length() - 2) + ")").withStyle(ChatFormatting.DARK_GRAY));
        }
        return c;
    }

    public static boolean research(ServerPlayer p, @Nullable Tech t) {
        Science sc = Science.get(p.server);
        Kingdom k = sc.of(p.getUUID());
        if (t == null || !canResearch(k, t)) {
            Text.bad(p, "Эту технологию сейчас изучить нельзя.");
            return false;
        }
        if (k.current != null && k.current != t) k.progress = k.progress / 2; // смена курса: теряется половина
        k.current = t;
        sc.setDirty();
        Text.good(p, "Учёные взялись за «" + t.title + "» (" + t.cost + " очков, +" + dailyPoints(p.server, p.getUUID()) + "/день).");
        return true;
    }

    public static void sendInfo(ServerPlayer p) {
        Kingdom k = Science.get(p.server).of(p.getUUID());
        k.perDay = dailyPoints(p.server, p.getUUID());
        int[] done = k.done.stream().mapToInt(Enum::ordinal).toArray();
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,
                new com.alkimor.regnum.core.network.ScienceInfoPayload(done, k.current == null ? -1 : k.current.ordinal(), k.progress, k.perDay));
    }

    public static void handleAction(ServerPlayer p, com.alkimor.regnum.core.network.ScienceActionPayload a) {
        Tech[] all = Tech.values();
        if (a.tech() >= 0 && a.tech() < all.length) research(p, all[a.tech()]);
        sendInfo(p);
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("science")
                .executes(ctx -> show(ctx.getSource().getPlayerOrException(), false))
                .then(Commands.literal("tree").executes(ctx -> show(ctx.getSource().getPlayerOrException(), true)))
                .then(Commands.literal("gui").executes(ctx -> {
                    sendInfo(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("research").then(Commands.argument("tech", StringArgumentType.word()).executes(ctx ->
                        research(ctx.getSource().getPlayerOrException(), Tech.byName(StringArgumentType.getString(ctx, "tech"))) ? 1 : 0)))
                .then(Commands.literal("exchange").then(Commands.argument("id", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 16)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    Realm r = Realms.byIndex(data, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "id"));
                    City c = Realms.cityOf(p, data);
                    if (r == null || c == null) { Text.bad(p, "Нужны свой город и известное королевство (номер из /regnum realm)."); return 0; }
                    String err = exchange(p.server, p.getUUID(), data, r, c, p.serverLevel().getDayTime() / 24000L);
                    if (err != null) Text.bad(p, err);
                    return err == null ? 1 : 0;
                })))
                .then(Commands.literal("grant").requires(s -> s.hasPermission(2)).then(Commands.argument("tech", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    String n = StringArgumentType.getString(ctx, "tech");
                    Science sc = Science.get(p.server);
                    if (n.equalsIgnoreCase("all")) for (Tech t : Tech.values()) sc.of(p.getUUID()).done.add(t);
                    else {
                        Tech t = Tech.byName(n);
                        if (t == null) return 0;
                        sc.of(p.getUUID()).done.add(t);
                    }
                    sc.setDirty();
                    Text.good(p, "Знания выданы.");
                    return 1;
                })))));
    }

    /** Обмен знаниями (I068): учёные союзного или торгового королевства делятся опытом. Раз в 3 суток на королевство, 30 монет. */
    public static String exchange(MinecraftServer server, UUID owner, KingdomData data, Realm r, City c, long day) {
        if (r.state != Realm.PEACE || !(r.ally || r.trade)) return "Обмениваться знаниями можно только с союзным или торговым королевством в мире.";
        Science sc = Science.get(server);
        Kingdom k = sc.of(owner);
        if (k.current == null) return "Сначала выберите исследование: /regnum science";
        if (day - r.sciDay < 3) return "Учёные «" + r.name + "» уже делились знаниями; ждите ещё " + (3 - (day - r.sciDay)) + " сут.";
        if (c.treasury < 30) return "Для обмена нужно 30 монет в казне.";
        c.treasury -= 30;
        r.sciDay = day;
        int pts = 10 + 2 * c.level + (r.ally ? 10 : 0);
        Tech done = sc.addPoints(owner, pts);
        data.setDirty();
        ServerPlayer p = server.getPlayerList().getPlayer(owner);
        if (p != null) Text.gold(p, "Учёные «" + r.name + "» поделились знаниями: +" + pts + " очков" + (done != null ? " — изучено «" + done.title + "»!" : "."));
        return null;
    }

    private static int show(ServerPlayer p, boolean all) {
        Science sc = Science.get(p.server);
        Kingdom k = sc.of(p.getUUID());
        int pts = dailyPoints(p.server, p.getUUID());
        Text.gold(p, "— Знания королевства: +" + pts + " очков/день, изучено " + k.done.size() + "/" + Tech.values().length + " —");
        if (k.current != null) Text.info(p, "  Исследуется: " + k.current.title + " — " + k.progress + "/" + k.current.cost);
        else Text.info(p, "  Исследование не выбрано.");
        for (int era = 1; era <= 4; era++) {
            boolean any = false;
            for (Tech t : Tech.values()) {
                if (t.era != era) continue;
                if (!all && !canResearch(k, t) && !k.done.contains(t)) continue;
                if (!any) { Text.gold(p, " Эра " + era + ": " + ERAS[era]); any = true; }
                p.sendSystemMessage(techLine(k, t));
            }
        }
        if (!all) Text.info(p, "  Всё дерево: /regnum science tree");
        return 1;
    }

    public static List<Tech> available(Kingdom k) {
        List<Tech> l = new ArrayList<>();
        for (Tech t : Tech.values()) if (canResearch(k, t)) l.add(t);
        return l;
    }
}
