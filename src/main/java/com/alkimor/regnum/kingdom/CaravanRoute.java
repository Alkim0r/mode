package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Выбор пути каравана (хранится в памяти): обычный, быстрый (доход x1.35, но 25% что разбойники перехватят),
 * безопасный (x0.85, без риска), платный проход (x1.00, пошлина 4 монеты, без риска).
 */
public final class CaravanRoute {
    private CaravanRoute() {}

    public static final String[] KEYS = {"обычный", "быстрый", "безопасный", "платный"};
    private static final Map<UUID, Integer> MODE = new HashMap<>();

    public static int mode(UUID city) { return MODE.getOrDefault(city, 0); }

    /** Множитель выплаты: roll 0..1 — бросок на перехват. 0 — караван потерян. */
    public static double payMult(int mode, double roll) {
        return switch (mode) {
            case 1 -> roll < 0.25 ? 0.0 : 1.35;
            case 2 -> 0.85;
            default -> 1.0;
        };
    }

    public static int toll(int mode) { return mode == 3 ? 4 : 0; }

    public static int byKey(String s) {
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equalsIgnoreCase(s)) return i;
        return -1;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("караван")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null) { Text.bad(p, "Рядом нет вашего города."); return 0; }
                    Text.gold(p, "Путь караванов «" + c.name + "»: " + KEYS[mode(c.id)]);
                    Text.info(p, "быстрый: x1.35, но 25% перехватят. безопасный: x0.85. платный: пошлина 4 монеты, без риска.");
                    return 1;
                })
                .then(Commands.argument("путь", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null) { Text.bad(p, "Рядом нет вашего города."); return 0; }
                    if (!Council.can(c, p.getUUID(), Council.Role.TREASURY)) { Text.bad(p, "Нужно право «казна»."); return 0; }
                    int k = byKey(StringArgumentType.getString(ctx, "путь"));
                    if (k < 0) { Text.bad(p, "Есть: обычный, быстрый, безопасный, платный."); return 0; }
                    MODE.put(c.id, k);
                    Text.good(p, "Путь караванов: " + KEYS[k] + ".");
                    return 1;
                }))));
    }
}
