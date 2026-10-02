package com.alkimor.regnum.survival.kit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;

public class WildfireFlaskEntity extends ThrowableItemProjectile {
    public static final String BURN_KEY = "regnum_wildfire";
    public static final int BURN_TICKS = 160;

    public WildfireFlaskEntity(EntityType<? extends WildfireFlaskEntity> type, Level level) {
        super(type, level);
    }

    public WildfireFlaskEntity(Level level, LivingEntity owner) {
        super(KitModule.WILDFIRE_FLASK_ENTITY.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return KitModule.WILDFIRE_FLASK.get();
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!(level() instanceof ServerLevel sl)) return;
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.6f);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.3, getZ(), 70, 1.8, 0.5, 1.8, 0.06);
        sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 12, 1.2, 0.3, 1.2, 0.0);
        long until = sl.getGameTime() + BURN_TICKS;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.0))) {
            if (e == getOwner()) continue;
            e.getPersistentData().putLong(BURN_KEY, until);
            e.hurt(sl.damageSources().inFire(), 8f);
            e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), 200));
        }
        // языки пламени на земле, но не в городах — там только живые цели
        BlockPos c = blockPosition();
        if (com.alkimor.regnum.kingdom.Territory.cityAt(sl, c) == null) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx * dx + dz * dz > 5 || sl.random.nextInt(3) == 0) continue;
                    BlockPos p = c.offset(dx, 0, dz);
                    if (sl.getBlockState(p).isAir() && sl.getBlockState(p.below()).isSolidRender(sl, p.below())) {
                        sl.setBlock(p, Blocks.SOUL_FIRE.defaultBlockState(), 3);
                    }
                }
            }
        }
        discard();
    }
}
