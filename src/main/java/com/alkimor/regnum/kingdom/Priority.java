package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Приоритет развития города: один курс на выбор. Курс даёт плюс в одном месте и небольшой минус в другом,
 * поэтому город нельзя «улучшить везде сразу».
 *  казна — налоги +10%, провиант −5%; провиант — урожай +15%, налоги −5%; оборона — жалованье армии −10%, налоги −5%.
 */
public final class Priority {
    private Priority() {}

    public static final String[] KEYS = {"нет", "казна", "провиант", "оборона"};

    public static double incomeMult(City c) { return c.priority == 1 ? 1.10 : (c.priority == 2 || c.priority == 3) ? 0.95 : 1.0; }
    public static double foodMult(City c) { return c.priority == 2 ? 1.15 : c.priority == 1 ? 0.95 : 1.0; }
    public static double upkeepMult(City c) { return c.priority == 3 ? 0.90 : 1.0; }

    public static int byKey(String s) {
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equalsIgnoreCase(s)) return i;
        return -1;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("priority")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null) { Text.bad(p, "Рядом нет вашего города."); return 0; }
                    Text.gold(p, "Курс «" + c.name + "»: " + KEYS[Math.max(0, Math.min(c.priority, KEYS.length - 1))]);
                    Text.info(p, "казна: налоги +10%, провиант −5%. провиант: урожай +15%, налоги −5%. оборона: жалованье −10%, налоги −5%.");
                    return 1;
                })
                .then(Commands.argument("курс", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null) { Text.bad(p, "Рядом нет вашего города."); return 0; }
                    if (!Council.can(c, p.getUUID(), Council.Role.TREASURY)) { Text.bad(p, "Нужно право «казна»."); return 0; }
                    int k = byKey(StringArgumentType.getString(ctx, "курс"));
                    if (k < 0) { Text.bad(p, "Есть: казна, провиант, оборона, нет."); return 0; }
                    c.priority = k;
                    KingdomData.get(p.server).setDirty();
                    Text.good(p, k == 0 ? "Курс отменён." : "Курс города: " + KEYS[k] + ".");
                    return 1;
                }))));
    }
}
