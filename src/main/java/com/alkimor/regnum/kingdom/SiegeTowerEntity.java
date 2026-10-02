package com.alkimor.regnum.kingdom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Осадная башня: медленно едет к воюющему городу, упирается в стену и опускает мост — воины, спрятанные внутри,
 * выходят на гребень стены (как в Mount &amp; Blade).
 */
public class SiegeTowerEntity extends Mob {
    public static final double SPEED = 0.045;

    @Nullable private UUID realmId;
    private int carried = 4;
    private boolean docked = false;
    private int dockedTicks = 0;
    private int stuck = 0;
    /** Вышедшие из башни воины (для проверок). */
    public final java.util.List<SoldierEntity> released = new java.util.ArrayList<>();
    /** Клиент: мост опущен. */
    public boolean bridgeDown = false;

    public SiegeTowerEntity(EntityType<? extends SiegeTowerEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 180).add(Attributes.ARMOR, 8)
                .add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.FOLLOW_RANGE, 8)
                .add(Attributes.STEP_HEIGHT, 1.1);
    }

    @Nullable
    public UUID realmId() {
        return realmId;
    }

    public void setRealm(@Nullable UUID id) {
        realmId = id;
    }

    public void setCarried(int n) {
        carried = n;
    }

    public boolean docked() {
        return docked;
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
        if (id == 5) bridgeDown = true;
        else super.handleEntityEvent(id);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        KingdomData data = KingdomData.get(sl.getServer());
        City target = null;
        double best = 140.0 * 140.0;
        for (City c : data.all()) {
            if (!c.war) continue;
            double d = c.hall.distToCenterSqr(getX(), getY(), getZ());
            if (d < best) {
                best = d;
                target = c;
            }
        }
        if (target == null) {
            if (tickCount % 100 == 0 && !docked) discard();
            return;
        }
        if (docked) {
            if (++dockedTicks > 1200) discard();
            return;
        }
        Vec3 hall = target.hall.getCenter();
        double dx = hall.x - getX(), dz = hall.z - getZ();
        double dist = Math.hypot(dx, dz);
        if (dist < 4) {
            dock(sl, target, Direction.getNearest(dx, 0, dz));
            return;
        }
        Direction face = Direction.getNearest(dx, 0, dz);
        setYRot(face.toYRot());
        yBodyRot = getYRot();
        // едем по оси (чтобы упираться в стену ровно лицом)
        Vec3 step = new Vec3(face.getStepX() * SPEED, 0, face.getStepZ() * SPEED);
        double before = getX() + getZ();
        move(MoverType.SELF, new Vec3(step.x, -0.2, step.z));
        if (horizontalCollision) {
            if (++stuck > 12) dock(sl, target, face);
        } else {
            stuck = 0;
            if (tickCount % 30 == 0) sl.playSound(null, blockPosition(), SoundEvents.WOOD_STEP, SoundSource.HOSTILE, 1.2f, 0.5f);
        }
        // слегка выправляем ход по второй оси, чтобы не застрять на углу
        if (tickCount % 40 == 0) {
            if (face.getAxis() == Direction.Axis.X && Math.abs(dz) > 6) move(MoverType.SELF, new Vec3(0, 0, Math.signum(dz) * 0.4));
            if (face.getAxis() == Direction.Axis.Z && Math.abs(dx) > 6) move(MoverType.SELF, new Vec3(Math.signum(dx) * 0.4, 0, 0));
        }
    }

    /** Высота гребня стены перед башней или Integer.MIN_VALUE, если стены нет. */
    private int wallTop(ServerLevel sl, BlockPos from, Direction d, int steps) {
        int top = Integer.MIN_VALUE;
        for (int k = 1; k <= steps; k++) {
            BlockPos col = from.relative(d, k);
            for (int y = from.getY() + 14; y >= from.getY(); y--) {
                if (!sl.getBlockState(new BlockPos(col.getX(), y, col.getZ())).isAir()) {
                    top = Math.max(top, y);
                    break;
                }
            }
        }
        return top;
    }

    private void dock(ServerLevel sl, City target, Direction d) {
        docked = true;
        sl.broadcastEntityEvent(this, (byte) 5);
        sl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.4f, 0.6f);
        BlockPos base = blockPosition().relative(d, 2);
        int top = wallTop(sl, base, d, 5);
        int y = top == Integer.MIN_VALUE ? blockPosition().getY() : top + 1;
        // первая колонка, где стена достигает гребня: сюда ложится мост
        int kWall = 3;
        if (top != Integer.MIN_VALUE) {
            for (int k = 1; k <= 8; k++) {
                BlockPos col = blockPosition().relative(d, k);
                if (!sl.getBlockState(new BlockPos(col.getX(), top, col.getZ())).isAir()) {
                    kWall = k;
                    break;
                }
            }
        }
        // мост лежит на гребне: воины сходят на стену и идут к ратуше
        UUID rid = realmId;
        Realm realm = rid == null ? null : KingdomData.get(sl.getServer()).realm(rid);
        int spawned = 0;
        if (realm != null) {
            for (int i = 0; i < carried; i++) {
                int k = kWall + i / 2;
                int side = (i % 2 == 0 ? -1 : 1) * (1 + i / 2);
                BlockPos p = blockPosition().relative(d, k).relative(d.getClockWise(), side);
                int py = top == Integer.MIN_VALUE ? p.getY() : y;
                SoldierType t = i % 3 == 0 ? SoldierType.KNIGHT : SoldierType.SWORDSMAN;
                SoldierEntity s = Realms.spawn(sl, realm, t, new BlockPos(p.getX(), py, p.getZ()), 2);
                if (s != null) {
                    s.command(Order.CHARGE, target.hall.getCenter(), s.getYRot(), Formation.LOOSE, i, carried, null);
                    spawned++;
                    released.add(s);
                }
            }
        }
        carried = 0;
        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 8, getZ(), 20, 1.2, 0.5, 1.2, 0.02);
        com.alkimor.regnum.Regnum.LOGGER.info("[SiegeTower] мост опущен, вышло воинов: {} (гребень {})", spawned, top);
    }

    /** Для проверок: принудительная стыковка. */
    public void forceDock(ServerLevel sl, City target, Direction d) {
        dock(sl, target, d);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (realmId != null) tag.putUUID("Realm", realmId);
        tag.putInt("Carried", carried);
        tag.putBoolean("Docked", docked);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Realm")) realmId = tag.getUUID("Realm");
        carried = tag.getInt("Carried");
        docked = tag.getBoolean("Docked");
        bridgeDown = docked;
    }
}
