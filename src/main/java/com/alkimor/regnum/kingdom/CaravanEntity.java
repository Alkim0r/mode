package com.alkimor.regnum.kingdom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/** Торговый караван королевства-партнёра: идёт к городу игрока и привозит монеты в казну. */
public class CaravanEntity extends PathfinderMob {
    @Nullable private UUID realmId;
    @Nullable private UUID cityId;
    private int walked = 0;
    /** Были ли рядом бойцы города в пути: охраняемый караван платит на 50% больше. */
    private boolean escorted = false;

    public CaravanEntity(EntityType<? extends CaravanEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 64);
    }

    public void setRoute(UUID realm, UUID city) {
        realmId = realm;
        cityId = city;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE)); }
            @Override public boolean canUse() { return cityId != null; }
            @Override public void tick() {
                if (!(level() instanceof ServerLevel sl)) return;
                City c = KingdomData.get(sl.getServer()).byId(cityId);
                if (c == null) { discard(); return; }
                BlockPos h = c.hall;
                if (tickCount % 20 == 0 && getNavigation().isDone()) getNavigation().moveTo(h.getX() + 0.5, h.getY(), h.getZ() + 0.5, 1.0);
            }
        });
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl) || tickCount % 20 != 0) return;
        walked++;
        if (cityId == null) return;
        KingdomData data = KingdomData.get(sl.getServer());
        City c = data.byId(cityId);
        if (c == null || walked > 600) { discard(); return; }
        if (!escorted && walked % 3 == 0 && !sl.getEntitiesOfClass(SoldierEntity.class, getBoundingBox().inflate(12), so -> so.isAlive() && cityId.equals(so.getCityId())).isEmpty()) escorted = true;
        if (blockPosition().closerThan(c.hall, 6)) arrive(sl, data, c);
    }

    /** Приход в город: монеты в казну, отношения растут. */
    public void arrive(ServerLevel sl, KingdomData data, City c) {
        Realm r = realmId == null ? null : data.realm(realmId);
        int pay = 12 + (r == null ? 0 : Math.max(0, r.relation) / 5) + (r != null && r.ally ? 10 : 0);
        if (escorted) pay += pay / 2;
        Exchange.markAllSeen(com.alkimor.regnum.survival.Seasons.index(sl), sl.getDayTime() / 24000L);
        int rmode = CaravanRoute.mode(c.id);
        double rm = CaravanRoute.payMult(rmode, sl.random.nextDouble());
        if (rm <= 0) { ServerPlayer_msg(sl, c, "Быстрый караван перехватили разбойники: груз потерян."); discard(); return; }
        pay = Math.max(1, (int) Math.round(pay * rm) - CaravanRoute.toll(rmode));
        c.treasury += pay;
        if (r != null) r.relation = Math.min(100, r.relation + 1);
        data.setDirty();
        ServerPlayer_msg(sl, c, "Караван «" + (r == null ? "торговцев" : r.name) + "» привёз в казну " + pay + (escorted ? " (охрана доплатила: путь был безопасен)." : "."));
        discard();
    }

    private static void ServerPlayer_msg(ServerLevel sl, City c, String msg) {
        var p = sl.getServer().getPlayerList().getPlayer(c.owner);
        if (p != null) com.alkimor.regnum.core.Text.good(p, msg);
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (!(level() instanceof ServerLevel sl)) return;
        KingdomData data = KingdomData.get(sl.getServer());
        Realm r = realmId == null ? null : data.realm(realmId);
        if (r != null && src.getEntity() instanceof Player p) {
            r.relation = Math.max(-100, r.relation - 25);
            r.note("вы убили их торговцев");
            data.setDirty();
            com.alkimor.regnum.core.Text.bad(p, "Вы убили торговцев «" + r.name + "». Отношения упали: " + r.relation);
        }
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (realmId != null) tag.putUUID("Realm", realmId);
        if (cityId != null) tag.putUUID("City", cityId);
        tag.putBoolean("Escorted", escorted);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Realm")) realmId = tag.getUUID("Realm");
        if (tag.hasUUID("City")) cityId = tag.getUUID("City");
        escorted = tag.getBoolean("Escorted");
    }
}
