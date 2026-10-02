package com.alkimor.regnum.combat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Парирование щитом (идея Epic Fight/Better Combat, переписано под Regnum): удар ближнего боя, пришедший в первые (щит считается поднятым с 5-го тика)
 * {@value #WINDOW} тика после поднятия щита, полностью отбивается, а нападавший отшатывается и ненадолго замедлен.
 * Стрелы и магию парировать нельзя. Следующее парирование — не раньше чем через секунду.
 */
public final class PlayerParry {
    private PlayerParry() {}

    public static final int WINDOW = 12, COOLDOWN = 20;
    private static final String CD = "regnum_parry_cd";

    /** Можно ли парировать: щит поднят «только что» и перезарядка прошла. */
    public static boolean window(ServerPlayer p) {
        return p.isBlocking() && p.getTicksUsingItem() <= WINDOW && p.tickCount >= p.getPersistentData().getInt(CD);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel sl)) return;
        var src = event.getSource();
        if (src.is(DamageTypeTags.IS_PROJECTILE) || src.is(DamageTypeTags.IS_EXPLOSION) || src.is(DamageTypeTags.IS_FIRE)
                || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || src.is(DamageTypeTags.BYPASSES_SHIELD)) return;
        if (!(src.getDirectEntity() instanceof LivingEntity attacker) || !window(p)) return;
        event.setCanceled(true);
        p.getPersistentData().putInt(CD, p.tickCount + COOLDOWN);
        attacker.knockback(0.9, p.getX() - attacker.getX(), p.getZ() - attacker.getZ());
        attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2, false, false));
        sl.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2f, 1.6f);
        sl.sendParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 1.2, p.getZ(), 10, 0.4, 0.3, 0.4, 0.2);
        p.displayClientMessage(Component.literal("Парирование!").withStyle(ChatFormatting.AQUA), true);
        com.alkimor.regnum.survival.Skills.addXp(p, com.alkimor.regnum.survival.Skill.TACTICS, 1.5f);
    }
}
