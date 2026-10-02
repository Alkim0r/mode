package com.alkimor.regnum.kingdom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Осадная катапульта армии королевства: стоит у города в состоянии войны и обстреливает его стены. */
public class CatapultEntity extends Mob {
    public static final int RELOAD = 360;
    public static final double MAX_RANGE = 78, MIN_RANGE = 22;

    @Nullable private UUID realmId;
    private int cooldown = 200;
    private int idle = 0;
    /** Клиентская анимация выстрела (тики с момента выстрела). */
    public int fireAnim = 99;

    public CatapultEntity(EntityType<? extends CatapultEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 70).add(Attributes.ARMOR, 6)
                .add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.FOLLOW_RANGE, 8);
    }

    @Nullable
    public UUID realmId() {
        return realmId;
    }

    public void setRealm(@Nullable UUID id) {
        realmId = id;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) fireAnim = 0;
        else super.handleEntityEvent(id);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (fireAnim < 99) fireAnim++;
            return;
        }
        if (!(level() instanceof ServerLevel sl) || tickCount % 5 != 0) return;
        KingdomData data = KingdomData.get(sl.getServer());
        City target = null;
        double best = MAX_RANGE * MAX_RANGE;
        for (City c : data.all()) {
            if (!c.war) continue;
            double d = c.hall.distToCenterSqr(getX(), getY(), getZ());
            if (d < best) {
                best = d;
                target = c;
            }
        }
        if (target == null) {
            // нет города на войне — через 10 секунд орудие бросают
            if ((idle += 5) > 200) discard();
            return;
        }
        idle = 0;
        cooldown -= 5;
        if (cooldown > 0) return;
        fire(sl, target);
        cooldown = RELOAD;
    }

    private void fire(ServerLevel sl, City target) {
        Vec3 hall = target.hall.getCenter();
        Vec3 me = position();
        double dx = hall.x - me.x, dz = hall.z - me.z;
        double dist = Math.max(1, Math.hypot(dx, dz));
        // целимся в границу города на своей стороне (там стена); слишком близко — бьём по центру
        double aimDist = Math.max(MIN_RANGE * 0.6, dist - target.radius() + 3 + sl.random.nextInt(5));
        double ax = me.x + dx / dist * aimDist + (sl.random.nextDouble() - 0.5) * 16;
        double az = me.z + dz / dist * aimDist + (sl.random.nextDouble() - 0.5) * 16;
        int ay = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) ax, (int) az);
        double range = Math.hypot(ax - me.x, az - me.z);
        double v = SiegeBoulderEntity.speedFor(range, ay + 1 - (me.y + 2.2));
        double ang = Math.toRadians(50);
        float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        setYRot(yaw);
        setYHeadRot(yaw);
        yBodyRot = yaw;
        SiegeBoulderEntity b = new SiegeBoulderEntity(sl, this);
        b.setPos(me.x, me.y + 2.2, me.z);
        double hx = dx / dist, hz = dz / dist;
        // направление к точке прицеливания (а не к центру), чтобы разброс работал
        double ux = (ax - me.x) / range, uz = (az - me.z) / range;
        b.setDeltaMovement(ux * Math.cos(ang) * v, Math.sin(ang) * v, uz * Math.cos(ang) * v);
        sl.addFreshEntity(b);
        sl.playSound(null, blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.HOSTILE, 1.6f, 0.6f);
        sl.playSound(null, blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.2f, 0.5f);
        sl.broadcastEntityEvent(this, (byte) 4);
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        return super.hurt(src, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (realmId != null) tag.putUUID("Realm", realmId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Realm")) realmId = tag.getUUID("Realm");
    }
}
