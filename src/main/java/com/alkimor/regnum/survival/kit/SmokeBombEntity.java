package com.alkimor.regnum.survival.kit;

import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class SmokeBombEntity extends ThrowableItemProjectile {
    public SmokeBombEntity(EntityType<? extends SmokeBombEntity> type, Level level) {
        super(type, level);
    }

    public SmokeBombEntity(Level level, LivingEntity owner) {
        super(KitModule.SMOKE_BOMB_ENTITY.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return KitModule.SMOKE_BOMB.get();
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!(level() instanceof ServerLevel sl)) return;
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.2f, 0.6f);
        AreaEffectCloud cloud = new AreaEffectCloud(sl, getX(), getY(), getZ());
        cloud.setRadius(4.5f);
        cloud.setDuration(140);
        cloud.setRadiusPerTick(0f);
        cloud.setWaitTime(0);
        cloud.setParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE);
        cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        if (getOwner() instanceof LivingEntity o) cloud.setOwner(o);
        sl.addFreshEntity(cloud);
        sl.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, getX(), getY() + 0.5, getZ(), 40, 1.8, 0.8, 1.8, 0.02);
        int n = 0;
        for (Mob m : sl.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(5.5))) {
            m.setTarget(null);
            m.getNavigation().stop();
            m.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            n++;
        }
        if (getOwner() instanceof Player p && p.distanceToSqr(this) < 6 * 6) {
            p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false));
        }
        if (getOwner() instanceof Player p && n > 0) Skills.addXp(p, Skill.ROGUERY, 3 + n);
        discard();
    }
}
