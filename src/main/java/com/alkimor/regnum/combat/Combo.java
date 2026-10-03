package com.alkimor.regnum.combat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Серия ударов: каждый следующий удар игрока в ближнем бою не позже чем через 2 секунды после предыдущего даёт +3% урона
 * (не более +15% на шестом ударе). Любой полученный урон обрывает серию, так что бездумная драка не награждается.
 */
public final class Combo {
    private Combo() {}

    public static final int WINDOW = 40, MAX_BONUS_STEPS = 5;
    public static final float STEP = 0.03f;
    private static final class Chain { int count; long last; }
    private static final Map<UUID, Chain> CHAINS = new HashMap<>();

    public static float multiplier(int count) {
        return 1f + STEP * Math.max(0, Math.min(MAX_BONUS_STEPS, count - 1));
    }

    /** Номер удара в серии при ударе в тик now (прошлый — в last, счётчик — count). */
    public static int next(int count, long last, long now) {
        return (count > 0 && now - last <= WINDOW) ? count + 1 : 1;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHit(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getEntity() instanceof ServerPlayer hurt) { CHAINS.remove(hurt.getUUID()); return; }
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getSource().getDirectEntity() != p) return;
        Chain c = CHAINS.computeIfAbsent(p.getUUID(), k -> new Chain());
        long now = p.level().getGameTime();
        c.count = next(c.count, c.last, now);
        c.last = now;
        float m = multiplier(c.count);
        if (m > 1f) event.setAmount(event.getAmount() * m);
        if (c.count >= 3) p.displayClientMessage(Component.literal("Серия ×" + c.count + " (+" + Math.round((m - 1f) * 100) + "%)").withStyle(net.minecraft.ChatFormatting.YELLOW), true);
    }
}
