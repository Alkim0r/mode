package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.survival.SurvivalModule;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Особенности оружия культур: кровотечение, пробитие брони, широкий размах. */
public final class WeaponPerks {
    private WeaponPerks() {}

    private static boolean cleaving = false;

    private static CultureWeaponItem weapon(LivingEntity e) {
        ItemStack s = e.getMainHandItem();
        return s.getItem() instanceof CultureWeaponItem w ? w : null;
    }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof LivingEntity attacker)) return;
        CultureWeaponItem w = weapon(attacker);
        if (w == null) return;
        if (w.kind.perk == CultureWeaponItem.Perk.ARMOR_PIERCE) {
            float bonus = Math.min(4f, event.getEntity().getArmorValue() * 0.15f);
            event.setAmount(event.getAmount() + bonus);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !(event.getSource().getDirectEntity() instanceof LivingEntity attacker)) return;
        CultureWeaponItem w = weapon(attacker);
        if (w == null) return;
        switch (w.kind.perk) {
            case BLEED -> {
                if (attacker.getRandom().nextFloat() < 0.25f && !victim.hasEffect(SurvivalModule.BLEEDING)) {
                    victim.addEffect(new MobEffectInstance(SurvivalModule.BLEEDING, 20 * 8, 0), attacker);
                    ((ServerLevel) victim.level()).sendParticles(ParticleTypes.DAMAGE_INDICATOR, victim.getX(), victim.getY(0.6), victim.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
                }
            }
            case CLEAVE -> {
                if (cleaving) return;
                cleaving = true;
                try {
                // широкий размах: часть урона по всем врагам рядом с целью
                AABB box = victim.getBoundingBox().inflate(1.6, 0.4, 1.6);
                for (LivingEntity e : victim.level().getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != victim && e != attacker && e.isAlive() && !e.isAlliedTo(attacker) && !(e instanceof SoldierEntity && attacker instanceof net.minecraft.world.entity.player.Player))) {
                    if (e instanceof net.minecraft.world.entity.player.Player && attacker instanceof net.minecraft.world.entity.player.Player) continue;
                    e.hurt(attacker.damageSources().mobAttack(attacker), event.getNewDamage() * 0.4f);
                }
                } finally {
                    cleaving = false;
                }
            }
            default -> {}
        }
    }
}
