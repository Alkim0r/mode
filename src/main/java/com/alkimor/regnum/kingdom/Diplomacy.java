package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.UUID;

/**
 * Живая дипломатия: послы приходят с предложениями (торговля, союз, брак) и ультиматумами (дань), перемирия после
 * мира, поводы к войне (casus belli), вероломство — нападение без повода портит отношения со всеми соседями.
 */
public final class Diplomacy {
    private Diplomacy() {}

    public static final int OFFER_NONE = 0, OFFER_TRADE = 1, OFFER_ALLIANCE = 2, OFFER_TRIBUTE = 3, OFFER_MARRIAGE = 4;
    public static final long DAY = 24000L;

    public static String offerTitle(int o) {
        return switch (o) {
            case OFFER_TRADE -> "торговый договор";
            case OFFER_ALLIANCE -> "союз";
            case OFFER_TRIBUTE -> "ультиматум: дань";
            case OFFER_MARRIAGE -> "династический брак";
            default -> "—";
        };
    }

    public static boolean truce(ServerLevel ow, Realm r) {
        return ow.getGameTime() < r.truceUntil;
    }

    public static void setTruce(ServerLevel ow, Realm r, int days) {
        r.truceUntil = ow.getGameTime() + days * DAY;
    }

    /** Есть ли у игрока законный повод к войне с королевством. */
    public static boolean hasCb(Realm r) {
        return r.playerCb || r.relation <= -40;
    }

    /** Объявить войну с учётом повода и перемирия. Без повода — вероломство: все соседи охладевают. */
    public static void declare(ServerLevel ow, KingdomData data, Realm r, ServerPlayer p) {
        boolean cb = hasCb(r);
        boolean broke = truce(ow, r);
        if (!cb || broke) {
            for (Realm o : data.realms()) {
                if (o == r) continue;
                int loss = broke ? 14 : 8;
                o.relation = Math.max(-100, o.relation - loss);
                if (o.ally && o.relation < 40) o.ally = false;
            }
            for (City c : data.ownedBy(p.getUUID())) {
                c.glory = Math.max(0, c.glory - 15);
                break;
            }
            p.sendSystemMessage(Text.of(broke ? "⚠ Вы нарушили перемирие — это вероломство! Соседние дворы отвернулись от вас."
                    : "⚠ Война без повода: соседние дворы осуждают вас, слава города падает.", ChatFormatting.RED));
        }
        r.playerCb = false;
        r.offer = OFFER_NONE;
        Realms.declareWar(ow, data, r, p);
    }

    // ------------------------------------------------------------------ послы

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 100 != 41 || !DayClock.newDay(server, "diplomacy")) return;
        ServerLevel ow = server.overworld();
        KingdomData data = KingdomData.get(server);
        for (Realm r : data.realms()) daily(ow, data, r);
        data.setDirty();
    }

    /**
     * Суточное остывание отношений в мире: дружба без поддержки выветривается к «привычному» уровню
     * (союзник 60, торговый партнёр 30, остальные 0), а мелкая вражда затихает; глубокая вражда (−40 и ниже) не проходит сама.
     */
    public static int driftStep(int relation, boolean trade, boolean ally) {
        int base = ally ? 60 : trade ? 30 : 0;
        if (relation > base) return relation - 1;
        if (relation < base && relation > -40) return relation + 1;
        return relation;
    }

    public static void daily(ServerLevel ow, KingdomData data, Realm r) {
        if (!r.built) return;
        if (r.state == Realm.PEACE) r.relation = driftStep(r.relation, r.trade, r.ally);
        Treaties.daily(r, ow.getGameTime(), ow.getServer().getPlayerList().getPlayers().isEmpty() ? null : ow.getServer().getPlayerList().getPlayers().get(0));
        long now = ow.getGameTime();
        UUID target = null;
        City tc = null;
        for (City c : data.all()) {
            tc = c;
            target = c.owner;
            break;
        }
        ServerPlayer p = target == null ? null : ow.getServer().getPlayerList().getPlayer(target);
        // просроченное предложение
        if (r.offer != OFFER_NONE && now > r.offerExpires) {
            if (r.offer == OFFER_TRIBUTE && r.state == Realm.PEACE && target != null) {
                r.offer = OFFER_NONE;
                r.playerCb = true;
                r.warOwner = target;
                if (p != null) Text.bad(p, "«" + r.name + "» не дождались дани и объявили войну!");
                r.state = Realm.WAR;
                r.relation = -100;
                r.armyTimer = now + 3600;
                if (tc != null) tc.war = true;
                return;
            }
            String old = offerTitle(r.offer);
            r.offer = OFFER_NONE;
            if (p != null) Text.info(p, "Послы «" + r.name + "» уехали: предложение (" + old + ") истекло.");
        }
        if (r.state != Realm.PEACE || r.known == 0 || r.offer != OFFER_NONE || tc == null) return;
        int power = Realms.playerPower(data, target);
        float roll = ow.random.nextFloat();
        if (r.relation >= 70 && r.trade && !r.ally && roll < 0.5f) post(ow, r, p, OFFER_ALLIANCE, 0);
        else if (r.relation >= (r.aggression() < 38 ? 30 : r.aggression() >= 55 ? 50 : 40) && !r.trade && roll < 0.5f) post(ow, r, p, OFFER_TRADE, 0); // осторожные сами тянутся к торговле, воинственные — неохотно
        else if (r.relation >= 55 && r.ally && roll < 0.25f) post(ow, r, p, OFFER_MARRIAGE, 150);
        else if (r.aggression() >= 55 && r.relation < 15 && !truce(ow, r) && power < r.strength * 0.9f && roll < 0.35f) {
            post(ow, r, p, OFFER_TRIBUTE, 25 + r.strength * 3);
        }
    }

    private static void post(ServerLevel ow, Realm r, ServerPlayer p, int offer, int amount) {
        r.offer = offer;
        r.offerAmount = amount;
        r.offerExpires = ow.getGameTime() + 2 * DAY;
        r.offerTo = p == null ? null : p.getUUID();
        if (p == null) return;
        var line = net.minecraft.network.chat.Component.empty();
        String what = switch (offer) {
            case OFFER_TRADE -> "Послы «" + r.name + "» предлагают торговый договор (бесплатно).";
            case OFFER_ALLIANCE -> "Послы «" + r.name + "» предлагают союз против общих врагов (бесплатно).";
            case OFFER_MARRIAGE -> "«" + r.name + "» предлагает скрепить дружбу династическим браком (" + amount + " на свадьбу).";
            default -> "☠ «" + r.name + "» требует дань " + amount + " изумр. за мир (2 дня), иначе — война.";
        };
        line.append(Text.of(what + " ", offer == OFFER_TRIBUTE ? ChatFormatting.RED : ChatFormatting.GOLD));
        int idx = Realms.indexOf(KingdomData.get(ow.getServer()), r);
        line.append(Text.button("[принять]", "/regnum realm accept " + idx, "Принять предложение"));
        line.append(net.minecraft.network.chat.Component.literal(" "));
        line.append(Text.button("[отказать]", "/regnum realm refuse " + idx, offer == OFFER_TRIBUTE ? "Отказ даёт повод к войне, но они нападут" : "Отказаться"));
        p.sendSystemMessage(line);
    }

    public static void accept(ServerPlayer p, Realm r) {
        KingdomData data = KingdomData.get(p.server);
        ServerLevel ow = p.serverLevel();
        if (r.offer == OFFER_NONE) {
            Text.info(p, "Предложений нет.");
            return;
        }
        if (r.offerTo != null && !r.offerTo.equals(p.getUUID())) {
            Text.bad(p, "Это предложение адресовано другому правителю.");
            return;
        }
        if (r.state != Realm.PEACE || ow.getGameTime() > r.offerExpires || (r.offer == OFFER_TRIBUTE && r.playerCb)) {
            r.offer = OFFER_NONE;
            data.setDirty();
            Text.bad(p, "Предложение уже недействительно.");
            return;
        }
        City c = Realms.cityOf(p, data);
        if (c == null) return;
        switch (r.offer) {
            case OFFER_TRADE -> {
                r.trade = true;
                Text.good(p, "Торговый договор с «" + r.name + "» заключён.");
            }
            case OFFER_ALLIANCE -> {
                r.ally = true;
                r.trade = true;
                Text.gold(p, "«" + r.name + "» теперь ваш союзник.");
            }
            case OFFER_MARRIAGE -> {
                if (c.treasury < r.offerAmount) {
                    Text.bad(p, "На свадьбу не хватает " + r.offerAmount + " в казне.");
                    return;
                }
                c.treasury -= r.offerAmount;
                r.relation = Math.min(100, r.relation + 30);
                r.note("свадьба сыграна");
                r.strength += 2;
                c.glory += 25;
                Text.gold(p, "Свадьба сыграна! Отношения с «" + r.name + "» крепки, слава города растёт.");
            }
            case OFFER_TRIBUTE -> {
                if (c.treasury < r.offerAmount) {
                    Text.bad(p, "В казне нет " + r.offerAmount + ": заплатить нечем.");
                    return;
                }
                c.treasury -= r.offerAmount;
                r.relation = Math.min(100, r.relation + 5);
                r.note("вы откупились данью");
                setTruce(ow, r, 6);
                Text.info(p, "Вы откупились от «" + r.name + "» на 6 дней. Это унизительно — копите армию.");
            }
            default -> {
                Text.info(p, "Предложений нет.");
                return;
            }
        }
        r.offer = OFFER_NONE;
        data.setDirty();
    }

    public static void refuse(ServerPlayer p, Realm r) {
        KingdomData data = KingdomData.get(p.server);
        if (r.offer == OFFER_NONE) return;
        if (r.offerTo != null && !r.offerTo.equals(p.getUUID())) return;
        if (r.offer == OFFER_TRIBUTE) {
            if (r.playerCb) return; // уже отказали
            r.playerCb = true;
            r.relation = Math.max(-100, r.relation - 25);
            r.note("вы отказали в требовании дани");
            r.offerExpires = p.serverLevel().getGameTime() + 1; // следующий день — война
            Text.bad(p, "Вы отказали «" + r.name + "». Ждите войска; зато у вас теперь законный повод к войне.");
        } else {
            r.relation = Math.max(-100, r.relation - 3);
            r.note("вы отклонили предложение");
            Text.info(p, "Вы отклонили предложение «" + r.name + "».");
            r.offer = OFFER_NONE;
        }
        data.setDirty();
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("realm")
                .then(Commands.literal("offers").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    boolean any = false;
                    int i = 1;
                    for (Realm r : data.realms()) {
                        if (r.offer != OFFER_NONE) {
                            any = true;
                            Text.info(p, "  " + i + ". «" + r.name + "»: " + offerTitle(r.offer) + (r.offerAmount > 0 ? " (" + r.offerAmount + ")" : "")
                                    + ", осталось дней: " + Math.max(0, (r.offerExpires - p.serverLevel().getGameTime()) / DAY + 1)
                                    + "  /regnum realm accept " + i + " | refuse " + i);
                        }
                        i++;
                    }
                    if (!any) Text.info(p, "Послы ничего не предлагают.");
                    return 1;
                }))
                .then(Commands.literal("accept").then(Commands.argument("id", IntegerArgumentType.integer(1, 16)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Realm r = Realms.byIndex(KingdomData.get(p.server), IntegerArgumentType.getInteger(ctx, "id"));
                    if (r == null) return 0;
                    accept(p, r);
                    return 1;
                })))
                .then(Commands.literal("refuse").then(Commands.argument("id", IntegerArgumentType.integer(1, 16)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Realm r = Realms.byIndex(KingdomData.get(p.server), IntegerArgumentType.getInteger(ctx, "id"));
                    if (r == null) return 0;
                    refuse(p, r);
                    return 1;
                })))));
    }
}
