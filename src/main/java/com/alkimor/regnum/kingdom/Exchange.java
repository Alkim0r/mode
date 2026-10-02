package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Обмен городского рынка (I046-I048): за монеты казны можно закупить запасы или продать излишки. Цена зависит от ресурса
 * и сезона (зимой провиант и травы дороже), покупка идёт по полной цене, продажа — за 60%. Нужен Рынок в городе.
 */
public final class Exchange {
    private Exchange() {}

    public static final int MAX_LOT = 200;

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
        int p = price(r, season);
        if (buy) {
            long cost = (long) p * n;
            if (c.treasury < cost) return "Не хватает монет: нужно " + cost + ", в казне " + c.treasury + ".";
            int put = c.deposit(r, n);
            if (put < n) { // не поместилось — возвращаем плату за непоместившееся
                cost = (long) p * put;
                if (put == 0) return "Склад переполнен.";
            }
            c.treasury -= (int) cost;
        } else {
            if (c.stock(r) < n) return "На складе только " + c.stock(r) + ".";
            c.take(r, n);
            c.treasury += (int) Math.floor(p * n * 0.6);
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
                    for (Resource r : Resource.values()) Text.info(p, r.title + " (" + r.name().toLowerCase() + "): купить за " + price(r, s) + ", продать за " + (int) Math.floor(price(r, s) * 0.6));
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
}
