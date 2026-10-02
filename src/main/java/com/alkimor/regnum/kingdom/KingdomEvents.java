package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class KingdomEvents {
    private KingdomEvents() {}

    /** Стрелы своих не ранят союзников. */
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit)) return;
        Entity shooter = event.getProjectile().getOwner();
        Entity victim = hit.getEntity();
        if (shooter instanceof SoldierEntity s && s.isAlliedTo(victim)) {
            event.setCanceled(true);
        } else if (shooter instanceof Player p && victim instanceof SoldierEntity s && s.isOwnedBy(p) && !s.isSparringWith(p)) {
            event.setCanceled(true);
        }
    }

    /**
     * Урон с участием солдат: защита своих, учебный поединок (обучение приёмам у короля)
     * и боевые приёмы — блок щитом, уклонение, удар в прыжке.
     */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource src = event.getSource();
        Entity attacker = src.getEntity();
        Entity direct = src.getDirectEntity();

        // король бьёт своего солдата
        if (victim instanceof SoldierEntity s && attacker instanceof Player p && s.isOwnedBy(p)) {
            if (s.isSparringWith(p)) {
                if (direct == p) {
                    if (p.isSprinting()) s.trainTech(Technique.W_TAP, 2);
                    if (p.tickCount - p.getLastHurtByMobTimestamp() > 60) s.trainTech(Technique.STRAFE, 1);
                } else if (direct instanceof Projectile && p.distanceTo(s) > 10) {
                    s.trainTech(Technique.KITING, 3);
                }
                Skills.addXp(p, Skill.LEADERSHIP, 1);
                Skills.addXp(p, Skill.TACTICS, 1);
                event.setAmount(event.getAmount() * 0.5f);
                if (s.getHealth() - event.getAmount() < 4) {
                    event.setCanceled(true);
                    s.heal(s.getMaxHealth() * 0.5f);
                    s.endSpar("боец признал поражение");
                }
                return;
            }
            if (!p.isShiftKeyDown()) event.setCanceled(true);
            return;
        }
        // солдат бьёт короля в поединке
        if (victim instanceof Player p && attacker instanceof SoldierEntity s && s.isSparringWith(p)) {
            event.setAmount(event.getAmount() * 0.3f);
            if (p.getHealth() - event.getAmount() < 4) {
                event.setCanceled(true);
                s.trainTech(Technique.values()[s.getRandom().nextInt(Technique.values().length)], 3);
                s.endSpar("боец одолел короля — достойный ученик!");
            }
            return;
        }
        // приёмы обороняющегося солдата
        if (victim instanceof SoldierEntity s && direct != null && direct == attacker && attacker instanceof LivingEntity la) {
            int block = s.tech(Technique.SHIELD_BLOCK);
            if (block > 0 && s.getOffhandItem().is(Items.SHIELD) && s.getRandom().nextFloat() < 0.08f * block) {
                event.setCanceled(true);
                s.level().playSound(null, s.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.NEUTRAL, 1f, 1f);
                if (block >= 3 && s.distanceToSqr(la) < 9) s.doHurtTarget(la);
                return;
            }
            int dodge = s.tech(Technique.STRAFE);
            if (dodge > 0 && s.getRandom().nextFloat() < 0.05f * dodge) {
                event.setCanceled(true);
                if (s.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CLOUD, s.getX(), s.getY() + 0.5, s.getZ(), 6, 0.3, 0.2, 0.3, 0.02);
                return;
            }
        }
        // фланг и тыл (I019): удар сзади по бойцу в строю на 20% сильнее; щитник и тяжёлая пехота прикрыты хуже всего спереди, но сзади уязвимы так же
        if (victim instanceof SoldierEntity fs && direct != null && direct == attacker && attacker instanceof LivingEntity fa && !(attacker instanceof SoldierEntity ff && ff.isAlliedTo(fs))) {
            if (isBehind(fs, fa)) {
                event.setAmount(event.getAmount() * 1.2f);
                if (fs.level() instanceof ServerLevel fl && fs.getRandom().nextInt(6) == 0)
                    fl.sendParticles(ParticleTypes.CRIT, fs.getX(), fs.getY(0.7), fs.getZ(), 4, 0.2, 0.2, 0.2, 0.1);
            }
        }
        // управляемое отступление (I024): при приказе «Отступить» арьергард (щитники, тяжёлая пехота) и ветераны держатся — потери меньше
        if (victim instanceof SoldierEntity rs && rs.getOrder() == Order.RETREAT) {
            event.setAmount(event.getAmount() * retreatFactor(rs.getSoldierType().role, rs.getKills()));
        }
        // приёмы атакующего солдата
        if (attacker instanceof SoldierEntity s && direct == s) {
            int crit = s.tech(Technique.JUMP_CRIT);
            float amount = event.getAmount() * (1f + 0.06f * crit);
            if (crit > 0 && s.getRandom().nextFloat() < 0.08f * crit) {
                amount *= 1.5f;
                if (s.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY(0.6), victim.getZ(), 10, 0.3, 0.3, 0.3, 0.2);
            }
            event.setAmount(amount);
        }
    }

    /** Множитель урона по отступающему бойцу: арьергард прикрывает отход, ветеран знает, как уходить. */
    public static float retreatFactor(SoldierType.Role role, int kills) {
        float f = (role == SoldierType.Role.SHIELD || role == SoldierType.Role.HEAVY) ? 0.6f : 1.0f;
        if (kills >= 3) f *= 0.8f;
        return f;
    }

    /** Атакующий находится в задней полусфере цели (угол от взгляда больше 120°). */
    public static boolean isBehind(LivingEntity victim, LivingEntity attacker) {
        double dx = attacker.getX() - victim.getX(), dz = attacker.getZ() - victim.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01) return false;
        float yaw = victim.yBodyRot * ((float) Math.PI / 180f);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        return (dx * fx + dz * fz) / len < -0.5;
    }

    /** Отбрасывание (w-tap). */
    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof SoldierEntity s && event.getSource().getDirectEntity() == s) {
            int w = s.tech(Technique.W_TAP);
            LivingEntity v = event.getEntity();
            if (w > 0) v.knockback(0.15 * w, s.getX() - v.getX(), s.getZ() - v.getZ());
        }
    }

    /** Король блокирует щитом удар ученика — тот учится блоку. */
    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (event.getEntity() instanceof Player p && event.getBlocked()
                && event.getDamageSource().getEntity() instanceof SoldierEntity s && s.isSparringWith(p)) {
            s.trainTech(Technique.SHIELD_BLOCK, 3);
        }
    }

    /** Король бьёт критом в прыжке — ученик перенимает. */
    @SubscribeEvent
    public static void onCrit(CriticalHitEvent event) {
        if (event.isVanillaCritical() && event.getTarget() instanceof SoldierEntity s && s.isSparringWith(event.getEntity())) {
            s.trainTech(Technique.JUMP_CRIT, 3);
        }
    }

    /** Солдаты набираются опыта за победы. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getSource().getEntity() instanceof SoldierEntity s && !s.isAlliedTo(event.getEntity())
                && event.getEntity() instanceof net.minecraft.world.entity.Mob) {
            s.addKill();
        }
    }

    /** Вербовка жителей жезлом: перехватываем до открытия торговли. */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getItemStack().getItem() instanceof CommanderBatonItem)) return;
        if (!(event.getTarget() instanceof Villager v)) return;
        if (event.getEntity() instanceof ServerPlayer sp) {
            ArmyCommands.recruitVillager(sp, v);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
