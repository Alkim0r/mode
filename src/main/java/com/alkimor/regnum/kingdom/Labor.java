package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Заказ производства: правитель (или тот, у кого есть право «склад») направляет часть жителей на промысел.
 * Каждые сутки они добывают выбранный ресурс на склад, но город теряет 15% налогов — люди заняты не торговлей.
 */
public final class Labor {
    private Labor() {}

    public static final String[] KEYS = {"нет", "дерево", "камень", "железо", "травы", "провиант"};
    private static final Resource[] RES = {null, Resource.WOOD, Resource.STONE, Resource.IRON, Resource.HERBS, Resource.FOOD};
    private static final int[] PER_WORKER = {0, 2, 2, 1, 1, 2};

    /** Сколько единиц даст город за сутки при данном заказе (0 — нет заказа или некому работать). */
    public static int output(City c) {
        if (c.labor <= 0 || c.labor >= RES.length) return 0;
        int workers = Math.max(0, c.population) / 5;
        int base = workers * PER_WORKER[c.labor];
        if (c.labor == 3 && c.count(BuildingType.SMITHY) == 0) base /= 2; // без кузницы железо идёт вдвое хуже
        return base;
    }

    /** Суточный приход; возвращает строку для отчёта (пусто, если заказа нет). */
    public static String daily(City c) {
        int n = output(c);
        if (c.labor <= 0 || n <= 0) return "";
        Resource r = RES[c.labor];
        int added = c.deposit(r, n);
        return "промысел: " + r.title + " +" + added;
    }

    public static int byKey(String s) {
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equalsIgnoreCase(s)) return i;
        return -1;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("labor")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null) { Text.bad(p, "Рядом нет вашего города."); return 0; }
                    Text.gold(p, "Заказ производства «" + c.name + "»: " + KEYS[Math.max(0, Math.min(c.labor, KEYS.length - 1))] + (c.labor > 0 ? " (≈" + output(c) + "/сутки, налоги −15%)" : ""));
                    Text.info(p, "Выбрать: /regnum labor <дерево|камень|железо|травы|провиант|нет>. Работает пятая часть жителей.");
                    return 1;
                })
                .then(Commands.argument("что", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null) { Text.bad(p, "Рядом нет вашего города."); return 0; }
                    if (!Council.can(c, p.getUUID(), Council.Role.STOCK)) { Text.bad(p, "Нужно право «склад»."); return 0; }
                    int k = byKey(StringArgumentType.getString(ctx, "что"));
                    if (k < 0) { Text.bad(p, "Есть: дерево, камень, железо, травы, провиант, нет."); return 0; }
                    c.labor = k;
                    KingdomData.get(p.server).setDirty();
                    Text.good(p, k == 0 ? "Промысел отменён." : "Жители заняты промыслом: " + KEYS[k] + " (≈" + output(c) + "/сутки).");
                    return 1;
                }))));
    }
}
