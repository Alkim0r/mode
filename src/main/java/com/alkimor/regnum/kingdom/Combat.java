package com.alkimor.regnum.kingdom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Тактические правила боя: роли родов войск.
 * — щитник гасит удары и стрелы спереди, но уязвим с тыла и фланга;
 * — копейщик (и алебарда) бьёт конницу вдвое сильнее и сбивает разгон;
 * — двуручник рубит широко (задевает соседей цели);
 * — удар конницы тем сильнее, чем быстрее скачет конь; тяжёлая сбивает пехоту с ног;
 * — удар в спину/фланг сильнее.
 */
public final class Combat {
    private Combat() {}

    /** Угол между направлением взгляда жертвы и направлением на источник: 0 — прямо спереди, 180 — сзади. */
    public static double angleTo(LivingEntity victim, Vec3 from) {
        Vec3 look = Vec3.directionFromRotation(0, victim.yBodyRot);
        Vec3 to = from.subtract(victim.position());
        double len = Math.sqrt(to.x * to.x + to.z * to.z);
        if (len < 0.001) return 0;
        double dot = (look.x * to.x + look.z * to.z) / len;
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));
    }

    public static boolean mountedTarget(LivingEntity v) {
        if (v instanceof AbstractHorse) return true;
        if (v instanceof SoldierEntity s && s.isMountedCavalry()) return true;
        return v.getVehicle() instanceof AbstractHorse;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (e.isCanceled()) return;
        LivingEntity victim = e.getEntity();
        DamageSource src = e.getSource();
        Entity attacker = src.getEntity();
        Entity direct = src.getDirectEntity();
        float amount = e.getAmount();

        // 1. защита
        if (victim instanceof SoldierEntity s) {
            Vec3 from = direct != null ? direct.position() : attacker != null ? attacker.position() : null;
            if (from != null && !src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)) {
                double ang = angleTo(s, from);
                SoldierType.Role r = s.getSoldierType().role;
                boolean shield = s.getOffhandItem().is(net.minecraft.world.item.Items.SHIELD);
                boolean proj = direct instanceof Projectile;
                if (shield && ang <= (r == SoldierType.Role.SHIELD ? 75 : 55)) {
                    float f = r == SoldierType.Role.SHIELD ? (proj ? 0.2f : 0.4f) : (proj ? 0.45f : 0.75f);
                    amount *= f;
                    if (s.tickCount % 3 == 0) s.level().playSound(null, s.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.NEUTRAL, 0.8f, 1f);
                    if (r == SoldierType.Role.SHIELD && attacker instanceof LivingEntity la && !proj) {
                        la.knockback(0.3, s.getX() - from.x, s.getZ() - from.z);
                    }
                } else if (ang >= 110 && attacker != null && !proj) {
                    amount *= 1.25f; // удар в спину
                }
            }
        }

        // 2. копьё против конницы
        if (attacker instanceof SoldierEntity a && direct == a && mountedTarget(victim)
                && (a.getSoldierType().role == SoldierType.Role.SPEAR || a.getSoldierType() == SoldierType.MILITIA)) {
            amount *= a.getSoldierType().role == SoldierType.Role.SPEAR ? 2.4f : 1.5f;
            victim.knockback(0.6, a.getX() - victim.getX(), a.getZ() - victim.getZ());
            if (victim.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY(0.7), victim.getZ(), 8, 0.3, 0.3, 0.3, 0.2);
        }

        // 3. двуручник и конница: множители атакующего
        if (attacker instanceof SoldierEntity a && direct == a) {
            SoldierType t = a.getSoldierType();
            if (t.role == SoldierType.Role.HEAVY) amount *= 1.7f;
            if (t.role == SoldierType.Role.CAVALRY && a.isPassenger() && a.getVehicle() != null) {
                Vec3 v = a.getVehicle().getDeltaMovement();
                double speed = Math.sqrt(v.x * v.x + v.z * v.z);
                // 0.0 скорости -> ×1.0, полный галоп (~0.28 блок/тик) -> ×1.7 (лёгкая) / ×2.0 (тяжёлая)
                double k = Math.min(1.0, speed / 0.28);
                amount *= 1.0f + (float) (k * (t == SoldierType.HEAVY_CAV ? 1.0 : 0.7));
                if (k > 0.55) {
                    double kb = t == SoldierType.HEAVY_CAV ? 1.0 : 0.6;
                    victim.knockback(kb, a.getX() - victim.getX(), a.getZ() - victim.getZ());
                }
            }
        }
        if (amount != e.getAmount()) e.setAmount(amount);
    }

    private static boolean SWEEPING = false;

    /** Конь солдата не оставляет седло и броню (иначе бесплатные сёдла). */
    @SubscribeEvent
    public static void onMountDrops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent e) {
        if (e.getEntity().getTags().contains(SoldierEntity.MOUNT_TAG)) e.getDrops().clear();
    }

    /** Размах двуручника: часть урона получают соседи цели. */
    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post e) {
        DamageSource src = e.getSource();
        if (!SWEEPING && src.getEntity() instanceof SoldierEntity a && src.getDirectEntity() == a && a.getSoldierType().role == SoldierType.Role.HEAVY
                && e.getEntity().level() instanceof ServerLevel sl) {
            SWEEPING = true;
            try {
            LivingEntity v = e.getEntity();
            float part = e.getNewDamage() * 0.5f;
            for (LivingEntity o : sl.getEntitiesOfClass(LivingEntity.class, v.getBoundingBox().inflate(1.6, 0.5, 1.6),
                    o -> o != v && o != a && o.isAlive() && a.isFoePublic(o))) {
                o.hurt(sl.damageSources().mobAttack(a), part);
            }
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, v.getX(), v.getY(0.6), v.getZ(), 1, 0, 0, 0, 0);
            } finally {
                SWEEPING = false;
            }
        }
    }
}
