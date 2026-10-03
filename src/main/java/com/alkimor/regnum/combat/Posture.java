package com.alkimor.regnum.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Равновесие врага («стойка»): каждый ваш удар по умному врагу копит шкалу, она остывает, если не бить.
 * Когда шкала заполнена, враг сбит с ног: на 3 секунды замедлен, а ближайший добивающий удар наносит ×1.5 урона.
 * Старшие ранги держатся устойчивее. Идея из Sekiro/Epic Fight, реализация своя.
 */
public final class Posture {
    private Posture() {}

    public static final int STAGGER_TICKS = 60, IDLE_BEFORE_DECAY = 60;
    public static final float DECAY_PER_TICK = 0.075f, RIPOSTE = 1.5f;

    private static final class State { float value; long last; long staggerUntil; boolean riposteReady; }
    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();

    /** Порог срыва равновесия по рангу врага (0 — обычный, 1–3 — старшие). */
    public static float threshold(int rank) { return 30f + 10f * Math.max(0, Math.min(3, rank)); }

    /** Накопленное значение после паузы без ударов. */
    public static float decayed(float value, long idleTicks) {
        long over = idleTicks - IDLE_BEFORE_DECAY;
        return over <= 0 ? value : Math.max(0f, value - DECAY_PER_TICK * over);
    }

    /** Прирост шкалы от удара: чем сильнее удар, тем больше. */
    public static float gain(float damage) { return Math.max(0f, damage) * 2f; }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHit(LivingIncomingDamageEvent event) {
        LivingEntity v = event.getEntity();
        if (v.level().isClientSide() || !(v instanceof Mob m) || !Tactics.tactical(m)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getSource().getDirectEntity() != p) return;
        ServerLevel sl = (ServerLevel) v.level();
        long now = sl.getGameTime();
        State s = STATES.computeIfAbsent(v, k -> new State());
        if (s.riposteReady && now <= s.staggerUntil) {
            s.riposteReady = false;
            event.setAmount(event.getAmount() * RIPOSTE);
            sl.sendParticles(ParticleTypes.CRIT, v.getX(), v.getY(0.6), v.getZ(), 14, 0.3, 0.3, 0.3, 0.2);
            p.displayClientMessage(Component.literal("Добивающий удар!").withStyle(net.minecraft.ChatFormatting.RED), true);
            return;
        }
        if (now < s.staggerUntil) return; // уже сбит, шкала не копится
        s.value = decayed(s.value, now - s.last) + gain(event.getAmount());
        s.last = now;
        if (s.value >= threshold(Tactics.rank(m))) {
            s.value = 0;
            s.staggerUntil = now + STAGGER_TICKS;
            s.riposteReady = true;
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STAGGER_TICKS, 3, false, false));
            m.setTarget(null);
            sl.playSound(null, m.blockPosition(), SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 0.9f, 1.3f);
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT, m.getX(), m.getY(0.9), m.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
            p.displayClientMessage(Component.literal("Враг потерял равновесие — бейте!").withStyle(net.minecraft.ChatFormatting.GOLD), true);
        }
    }
}
