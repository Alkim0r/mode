package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * Обмен городского рынка (I046-I048): за монеты казны можно закупить запасы или продать излишки. Цена зависит от ресурса
 * и сезона (зимой провиант и травы дороже), покупка идёт по полной цене, продажа — за 60%. Нужен Рынок в городе.
 */
public final class Exchange {
    private Exchange() {}

    public static final int MAX_LOT = 200;
    public static final int MAX_PRESSURE = 200;
    /** Давление спроса по ресурсам: покупки поднимают цену, продажи опускают; остывает на 10% в сутки (не сохраняется). */
    private static final Map<Resource, Integer> pressure = new EnumMap<>(Resource.class);

    public static int pressure(Resource r) { return pressure.getOrDefault(r, 0); }

    public static void addPressure(Resource r, int delta) {
        pressure.put(r, Math.max(-MAX_PRESSURE, Math.min(MAX_PRESSURE, pressure(r) + delta)));
    }

    public static void coolDown() {
        for (Resource r : Resource.values()) pressure.put(r, (int) (pressure(r) * 0.9));
    }

    public static void resetPressure() { pressure.clear(); }

    /** Торговая разведка: последняя известная цена и день, когда её узнали (в памяти). */
    private static final Map<Resource, int[]> SEEN = new EnumMap<>(Resource.class);
    public static final int FRESH_DAYS = 2;

    /** Сколько дней сведения старше свежих (0 — свежие). */
    public static int staleDays(long today, long seenDay) { return (int) Math.max(0, today - seenDay - FRESH_DAYS); }

    /** Какую цену показать: свежие данные — живую, старые — запомненную. */
    public static int shownPrice(int live, int remembered, int stale) { return stale > 0 ? remembered : live; }

    public static void markSeen(Resource r, int price, long day) { SEEN.put(r, new int[]{price, (int) day}); }
    public static void markAllSeen(int season, long day) { for (Resource r : Resource.values()) markSeen(r, priceNow(r, season), day); }
    public static int[] seen(Resource r) { return SEEN.get(r); }


    /** Цена с учётом давления: +-50% при предельном давлении. */
    public static int priceNow(Resource r, int season) {
        double p = price(r, season) * (1.0 + pressure(r) / (double) (MAX_PRESSURE * 2));
        return (int) Math.max(1, Math.round(p));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 100 == 57 && DayClock.newDay(e.getServer(), "exchange")) coolDown();
    }

    public static int price(Resource r, int season) {
        double base = switch (r) {
            case FOOD -> 2; case WOOD -> 2; case STONE -> 2; case IRON -> 6; case HERBS -> 5; case GUNPOWDER -> 10;
        };
        if (season == 3 && (r == Resource.FOOD || r == Resource.HERBS)) base *= 1.5;
        if (season == 2 && r == Resource.FOOD) base *= 0.8; // осенью урожай дешевле
        return (int) Math.max(1, Math.round(base));
    }

    /** null — успех, иначе причина отказа. */
    public static String trade(City c, Resource r, int n, boolean buy, int season) {
        if (c.count(BuildingType.MARKET) == 0) return "Для обмена нужен Рынок в городе.";
        if (n < 1 || n > MAX_LOT) return "Партия — от 1 до " + MAX_LOT + ".";
        int p = priceNow(r, season);
        if (buy) {
            long cost = (long) p * n;
            if (c.treasury < cost) return "Не хватает монет: нужно " + cost + ", в казне " + c.treasury + ".";
            int put = c.deposit(r, n);
            if (put < n) { // не поместилось — возвращаем плату за непоместившееся
                cost = (long) p * put;
                if (put == 0) return "Склад переполнен.";
            }
            c.treasury -= (int) cost;
            addPressure(r, put);
        } else {
            if (c.stock(r) < n) return "На складе только " + c.stock(r) + ".";
            c.take(r, n);
            c.treasury += (int) Math.floor(p * n * 0.6);
            addPressure(r, -n);
        }
        return null;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        var res = Commands.argument("res", StringArgumentType.word()).then(Commands.argument("n", IntegerArgumentType.integer(1, MAX_LOT)).executes(ctx -> run(ctx, true)));
        var res2 = Commands.argument("res", StringArgumentType.word()).then(Commands.argument("n", IntegerArgumentType.integer(1, MAX_LOT)).executes(ctx -> run(ctx, false)));
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("exchange")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    int s = com.alkimor.regnum.survival.Seasons.index(p.level());
                    Text.gold(p, "══ Обмен рынка (" + com.alkimor.regnum.survival.Seasons.name(p.level()) + ") ══");
                    long today = p.level().getDayTime() / 24000L;
                    for (Resource r : Resource.values()) {
                        int live = priceNow(r, s);
                        int[] sn = seen(r);
                        int stale = sn == null ? 0 : staleDays(today, sn[1]);
                        int shown = sn == null ? live : shownPrice(live, sn[0], stale);
                        if (stale == 0) markSeen(r, live, today);
                        Text.info(p, r.title + " (" + r.name().toLowerCase() + "): купить за " + shown + ", продать за " + (int) Math.floor(shown * 0.6)
                                + (stale > 0 ? " [сведения " + stale + " дн. назад — цена могла измениться; караван обновит]" : pressure(r) > 20 ? " (дорожает из-за спроса)" : pressure(r) < -20 ? " (дешевеет из-за предложения)" : ""));
                    }
                    Text.info(p, "/regnum exchange buy <ресурс> <число> · /regnum exchange sell <ресурс> <число>");
                    return 1;
                })
                .then(Commands.literal("buy").then(res))
                .then(Commands.literal("sell").then(res2))));
    }

    private static int run(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> ctx, boolean buy) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        City c = KingdomData.get(p.server).at(p.blockPosition());
        if (c == null || !c.owner.equals(p.getUUID()) && !Council.can(c, p.getUUID(), Council.Role.TREASURY)) {
            Text.bad(p, "Встаньте на земле своего города."); return 0;
        }
        Resource r = null;
        String name = StringArgumentType.getString(ctx, "res");
        for (Resource x : Resource.values()) if (x.name().equalsIgnoreCase(name)) r = x;
        if (r == null) { Text.bad(p, "Нет такого ресурса. Список: /regnum exchange"); return 0; }
        int n = IntegerArgumentType.getInteger(ctx, "n");
        String err = trade(c, r, n, buy, com.alkimor.regnum.survival.Seasons.index(p.level()));
        if (err != null) { Text.bad(p, err); return 0; }
        KingdomData.get(p.server).setDirty();
        Text.good(p, (buy ? "Куплено " : "Продано ") + n + " × " + r.title.toLowerCase() + ". Казна: " + c.treasury + ", на складе: " + c.stock(r) + ".");
        return 1;
    }


    public static net.minecraft.nbt.CompoundTag toTag() {
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        net.minecraft.nbt.CompoundTag pr = new net.minecraft.nbt.CompoundTag();
        pressure.forEach((r, v) -> pr.putInt(r.name(), v));
        t.put("pressure", pr);
        net.minecraft.nbt.CompoundTag sn = new net.minecraft.nbt.CompoundTag();
        SEEN.forEach((r, v) -> sn.putIntArray(r.name(), v));
        t.put("seen", sn);
        return t;
    }

    public static void fromTag(net.minecraft.nbt.CompoundTag t) {
        pressure.clear();
        SEEN.clear();
        net.minecraft.nbt.CompoundTag pr = t.getCompound("pressure");
        for (String k : pr.getAllKeys()) {
            try { pressure.put(Resource.valueOf(k), pr.getInt(k)); } catch (IllegalArgumentException ignored) {}
        }
        net.minecraft.nbt.CompoundTag sn = t.getCompound("seen");
        for (String k : sn.getAllKeys()) {
            try {
                int[] a = sn.getIntArray(k);
                if (a.length == 2) SEEN.put(Resource.valueOf(k), a);
            } catch (IllegalArgumentException ignored) {}
        }
    }
}
