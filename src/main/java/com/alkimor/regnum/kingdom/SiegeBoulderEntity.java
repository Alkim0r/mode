package com.alkimor.regnum.kingdom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

/** Каменное ядро осадной катапульты: ломает блоки стен воюющего города и калечит защитников. */
public class SiegeBoulderEntity extends ThrowableItemProjectile {
    public static final float GRAVITY = 0.03f;
    public static final float DRAG = 0.99f;

    public SiegeBoulderEntity(EntityType<? extends SiegeBoulderEntity> type, Level level) {
        super(type, level);
    }

    public SiegeBoulderEntity(Level level, LivingEntity owner) {
        super(KingdomModule.SIEGE_BOULDER.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBBLESTONE;
    }

    @Override
    protected double getDefaultGravity() {
        return GRAVITY;
    }

    /** Скорость запуска под углом 50°, чтобы ядро упало на расстоянии range (с учётом сопротивления воздуха) при перепаде высоты dy. */
    public static double speedFor(double range, double dy) {
        double ang = Math.toRadians(50);
        double lo = 0.3, hi = 6.0;
        for (int it = 0; it < 40; it++) {
            double v = (lo + hi) / 2;
            double x = 0, y = 0, vx = Math.cos(ang) * v, vy = Math.sin(ang) * v;
            for (int t = 0; t < 400; t++) {
                x += vx;
                y += vy;
                vx *= DRAG;
                vy *= DRAG;
                vy -= GRAVITY;
                if (vy < 0 && y <= dy) break;
            }
            if (x < range) lo = v;
            else hi = v;
        }
        return (lo + hi) / 2;
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!(level() instanceof ServerLevel sl)) return;
        explode(sl, BlockPos.containing(hit.getLocation()));
        discard();
    }

    /** Разрушительный взрыв ядра в точке c (для проверок вызывается напрямую). */
    public void explode(ServerLevel sl, BlockPos c) {
        sl.playSound(null, c, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4f, 0.7f);
        sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 3, 1.0, 0.6, 1.0, 0.0);
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.5, getZ(), 12, 1.2, 0.8, 1.2, 0.02);
        int r = 2;
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -r; dy <= r; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (d > r + 0.5) continue;
                    BlockPos p = c.offset(dx, dy, dz);
                    BlockState st = sl.getBlockState(p);
                    if (st.isAir() || !st.getFluidState().isEmpty() || st.getDestroySpeed(sl, p) < 0) continue;
                    City city = Territory.cityAt(sl, p);
                    if (city == null || !city.war) continue;
                    float dmg = (float) (10 * (1.0 - d / (r + 1.0)));
                    if (Territory.damage(sl, p, dmg)) {
                        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.05);
                        sl.destroyBlock(p, false);
                    }
                }
        java.util.UUID realm = getOwner() instanceof CatapultEntity cat ? cat.realmId() : null;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.0))) {
            if (e instanceof CatapultEntity) continue;
            if (e instanceof SoldierEntity s && realm != null && realm.equals(s.realmId())) continue;
            double d = e.distanceTo(this);
            e.hurt(damageSources().thrown(this, getOwner()), (float) Math.max(2, 14 - 3 * d));
        }
    }
}
