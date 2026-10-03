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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Управляющий (I039): раз в сутки докупает на рынке ресурсы до заданного складского минимума.
 * Лимиты: дневной бюджет, неприкосновенный запас казны 50, нужен Рынок. Политики хранятся в памяти.
 */
public final class Steward {
    private Steward() {}

    public static final int RESERVE = 50;

    static final class Policy {
        final Map<Resource, Integer> min = new EnumMap<>(Resource.class);
        int budget = 0;
        boolean on = false;
    }

    private static final Map<UUID, Policy> POLICIES = new HashMap<>();

    /** Сколько единиц докупить: до минимума, в пределах остатка бюджета и казны сверх резерва. */
    public static int buyAmount(int stock, int min, int price, int budgetLeft, int treasury) {
        if (price <= 0 || stock >= min) return 0;
        int byBudget = budgetLeft / price;
        int byTreasury = Math.max(0, treasury - RESERVE) / price;
        return Math.max(0, Math.min(Math.min(min - stock, byBudget), Math.min(byTreasury, Exchange.MAX_LOT)));
    }

    /** Один суточный проход по городу; возвращает потраченные монеты. */
    public static int run(City c, int season) {
        Policy p = POLICIES.get(c.id);
        if (p == null || !p.on || p.budget <= 0) return 0;
        int left = p.budget, spent = 0;
        for (Resource r : Resource.values()) {
            Integer m = p.min.get(r);
            if (m == null) continue;
            int price = Exchange.priceNow(r, season);
            int n = buyAmount(c.stock(r), m, price, left, c.treasury);
            if (n <= 0) continue;
            int before = c.treasury;
            if (Exchange.trade(c, r, n, true, season) == null) {
                int cost = before - c.treasury;
                left -= cost;
                spent += cost;
            }
        }
        return spent;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 100 != 71 || !DayClock.newDay(e.getServer(), "steward")) return;
        KingdomData data = KingdomData.get(e.getServer());
        int season = com.alkimor.regnum.survival.Seasons.index(e.getServer().overworld());
        boolean any = false;
        for (City c : data.all()) {
            int spent = run(c, season);
            if (spent > 0) {
                any = true;
                ServerPlayer o = e.getServer().getPlayerList().getPlayer(c.owner);
                if (o != null) Text.info(o, "Управляющий «" + c.name + "» докупил запасы за " + spent + " монет.");
            }
        }
        if (any) data.setDirty();
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("управляющий")
                .executes(ctx -> {
                    ServerPlayer pl = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(pl.server).nearestOwned(pl.getUUID(), pl.blockPosition());
                    if (c == null) { Text.bad(pl, "Рядом нет вашего города."); return 0; }
                    Policy p = POLICIES.get(c.id);
                    if (p == null || !p.on) { Text.info(pl, "Управляющий выключен. /regnum управляющий бюджет <монет>, затем минимум <ресурс> <число>, затем вкл."); return 1; }
                    Text.gold(pl, "Управляющий «" + c.name + "»: бюджет " + p.budget + "/сутки, резерв казны " + RESERVE);
                    for (var en : p.min.entrySet()) Text.info(pl, en.getKey().title + ": держать от " + en.getValue() + " (сейчас " + c.stock(en.getKey()) + ")");
                    return 1;
                })
                .then(Commands.literal("бюджет").then(Commands.argument("монет", IntegerArgumentType.integer(0, 500)).executes(ctx -> {
                    ServerPlayer pl = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(pl.server).nearestOwned(pl.getUUID(), pl.blockPosition());
                    if (c == null || !Council.can(c, pl.getUUID(), Council.Role.TREASURY)) { Text.bad(pl, "Нужен ваш город и право «казна»."); return 0; }
                    POLICIES.computeIfAbsent(c.id, k -> new Policy()).budget = IntegerArgumentType.getInteger(ctx, "монет");
                    Text.good(pl, "Дневной бюджет управляющего: " + IntegerArgumentType.getInteger(ctx, "монет") + ".");
                    return 1;
                })))
                .then(Commands.literal("минимум").then(Commands.argument("ресурс", StringArgumentType.word()).then(Commands.argument("число", IntegerArgumentType.integer(0, 400)).executes(ctx -> {
                    ServerPlayer pl = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(pl.server).nearestOwned(pl.getUUID(), pl.blockPosition());
                    if (c == null || !Council.can(c, pl.getUUID(), Council.Role.TREASURY)) { Text.bad(pl, "Нужен ваш город и право «казна»."); return 0; }
                    Resource r = null;
                    String key = StringArgumentType.getString(ctx, "ресурс");
                    for (Resource x : Resource.values()) if (x.name().equalsIgnoreCase(key) || x.title.equalsIgnoreCase(key)) r = x;
                    if (r == null) { Text.bad(pl, "Ресурс: food, wood, stone, iron, herbs, gunpowder."); return 0; }
                    POLICIES.computeIfAbsent(c.id, k -> new Policy()).min.put(r, IntegerArgumentType.getInteger(ctx, "число"));
                    Text.good(pl, "Минимум " + r.title + ": " + IntegerArgumentType.getInteger(ctx, "число") + ".");
                    return 1;
                }))))
                .then(Commands.literal("вкл").executes(ctx -> switchOn(ctx.getSource().getPlayerOrException(), true)))
                .then(Commands.literal("выкл").executes(ctx -> switchOn(ctx.getSource().getPlayerOrException(), false)))));
    }

    private static int switchOn(ServerPlayer pl, boolean on) {
        City c = KingdomData.get(pl.server).nearestOwned(pl.getUUID(), pl.blockPosition());
        if (c == null || !Council.can(c, pl.getUUID(), Council.Role.TREASURY)) { Text.bad(pl, "Нужен ваш город и право «казна»."); return 0; }
        POLICIES.computeIfAbsent(c.id, k -> new Policy()).on = on;
        Text.good(pl, on ? "Управляющий включён." : "Управляющий выключен.");
        return 1;
    }


    public static net.minecraft.nbt.CompoundTag toTag() {
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        POLICIES.forEach((id, p) -> {
            net.minecraft.nbt.CompoundTag c = new net.minecraft.nbt.CompoundTag();
            c.putInt("budget", p.budget);
            c.putBoolean("on", p.on);
            net.minecraft.nbt.CompoundTag m = new net.minecraft.nbt.CompoundTag();
            p.min.forEach((r, v) -> m.putInt(r.name(), v));
            c.put("min", m);
            t.put(id.toString(), c);
        });
        return t;
    }

    public static void fromTag(net.minecraft.nbt.CompoundTag t) {
        POLICIES.clear();
        for (String k : t.getAllKeys()) {
            net.minecraft.nbt.CompoundTag c = t.getCompound(k);
            Policy p = new Policy();
            p.budget = c.getInt("budget");
            p.on = c.getBoolean("on");
            net.minecraft.nbt.CompoundTag m = c.getCompound("min");
            for (String rk : m.getAllKeys()) {
                try { p.min.put(Resource.valueOf(rk), m.getInt(rk)); } catch (IllegalArgumentException ignored) {}
            }
            POLICIES.put(UUID.fromString(k), p);
        }
    }
}
