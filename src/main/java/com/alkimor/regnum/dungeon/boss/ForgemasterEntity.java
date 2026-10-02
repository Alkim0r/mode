package com.alkimor.regnum.dungeon.boss;

import com.alkimor.regnum.dungeon.RegionsModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Горновой — огненный голем-кузнец Гномьей крепости.
 * Пока на арене горят Угольные сердца, он восстанавливает здоровье — их нужно разбить.
 */
public class ForgemasterEntity extends IronGolem implements net.minecraft.world.entity.monster.Enemy, VisualActor {
    private static final com.alkimor.regnum.dungeon.boss.VisualAction VA = com.alkimor.regnum.dungeon.boss.VisualAction.of(ForgemasterEntity.class);

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        VA.define(builder);
    }

    @Override public void visualStart(int action, int impactTick, int totalTicks) { VA.start(this, action, impactTick, totalTicks); }
    @Override public int getVisualAction() { return VA.current(this); }
    @Override public int getVisualActionElapsed() { return VA.elapsed(this); }
    public int getVisualActionTicks() { return VA.elapsed(this); }
    @Override public int getVisualActionDuration() { return VA.duration(this); }
    @Override public int getVisualActionImpact() { return VA.impact(this); }
    @Override public int getVisualPhase() { return VA.phase(this); }

    private final BossBase.Controller ctrl = new BossBase.Controller(this, "Горновой", BossEvent.BossBarColor.YELLOW,
            "Теперь кузнецам доступно Звёздное железо.");
    private final List<BlockPos> cores = new ArrayList<>();
    private boolean scanned = false;
    private final Telegraph tele = new Telegraph(this);

    public ForgemasterEntity(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
        this.xpReward = 260;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return IronGolem.createAttributes()
                .add(Attributes.MAX_HEALTH, 380.0).add(Attributes.ATTACK_DAMAGE, 13.0).add(Attributes.ARMOR, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        BossBase.standardGoals(this, goalSelector, targetSelector, new MeleeAttackGoal(this, 1.0, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        VA.setPhase(this, ctrl.phase);
        int newPhase = ctrl.tick();
        if (newPhase > 0) visualStart(7, 0, 40);
        if (newPhase == 2) ctrl.say("Горн раздувается! Пламя, ко мне!");
        if (newPhase == 3) ctrl.say("Земля расплавится под твоими ногами!");
        if (!(level() instanceof ServerLevel sl)) return;

        if (!scanned) scanCores(sl);
        if (tickCount % 40 == 0) feedFromCores(sl);

        tele.tick(sl);
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive()) return;
        if (tickCount % 20 == 0) tele.antiCheese(sl, t, () -> ctrl.say("Камень не спрячет тебя от горна!"));
        if (tele.busy()) return;
        int phase = ctrl.phase;
        if (ctrl.ready(4, 260) && BossRules.reapArmy(sl, this, tele, ParticleTypes.FLAME, ParticleTypes.LAVA, 15f,
                "Горновой обрушивает пламя на ваше войско!")) {
            ctrl.say("Сталь солдат — лишь руда для моего горна!");
            return;
        }
        double d2 = distanceToSqr(t);
        if (d2 < 14 * 14 && ctrl.ready(3, phase >= 2 ? 90 : 130)) {
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            tele.cast(sl, Telegraph.line(position(), Telegraph.toward(this, t), 13, 1.6, 22, 16f).root()
                    .warn(ParticleTypes.FLAME).burst(ParticleTypes.LAVA).sound(SoundEvents.ANVIL_LAND)
                    .text("Горновой заносит молот!")
                    .onHit((l, e) -> { e.push(0, 0.9, 0); e.hurtMarked = true; e.igniteForSeconds(3); }));
            return;
        }
        if (d2 < 36 && ctrl.ready(0, phase >= 2 ? 120 : 160)) {
            tele.cast(sl, Telegraph.circle(position(), 5.5, 22, 10f).root().warn(ParticleTypes.FLAME).burst(ParticleTypes.LAVA)
                    .sound(SoundEvents.BLAZE_SHOOT)
                    .onHit((l, e) -> { e.igniteForSeconds(5); e.push(0, 0.8, 0); e.hurtMarked = true; }));
            return;
        }
        if (phase >= 2 && hasLineOfSight(t) && ctrl.ready(1, 80)) fireballs(sl, t);
        if (phase >= 3 && ctrl.ready(2, 200)) {
            ctrl.say("Извергайся!");
            for (LivingEntity e : ctrl.foesAround(16)) {
                tele.cast(sl, Telegraph.circle(Telegraph.lead(e, 10), 2.6, 24, 9f).warn(ParticleTypes.LAVA).burst(ParticleTypes.FLAME)
                        .sound(SoundEvents.LAVA_POP).onHit((l, x) -> x.igniteForSeconds(4)));
            }
        }
        if (phase >= 3 && ctrl.ready(5, 320)) {
            tele.cast(sl, Telegraph.ring(position(), 5.0, 15.0, 36, 12f).root().warn(ParticleTypes.FLAME).burst(ParticleTypes.LAVA)
                    .sound(SoundEvents.GENERIC_EXPLODE.value()).text("Кольцо горна! Ближе к Горновому!")
                    .onHit((l, e) -> e.igniteForSeconds(6)));
        }
    }

    private void scanCores(ServerLevel sl) {
        scanned = true;
        BlockPos c = hasRestriction() ? getRestrictCenter() : blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-16, -4, -16), c.offset(16, 6, 16))) {
            if (sl.getBlockState(p).is(RegionsModule.EMBER_CORE.get())) cores.add(p.immutable());
        }
        if (!cores.isEmpty()) ctrl.say("Пока горят мои горны — я вечен! Тебе не погасить пламя кузни!");
    }

    private void feedFromCores(ServerLevel sl) {
        int alive = 0;
        for (BlockPos p : cores) {
            if (sl.getBlockState(p).is(RegionsModule.EMBER_CORE.get())) {
                alive++;
                Vec3 from = Vec3.atCenterOf(p), to = position().add(0, 1.5, 0);
                for (int i = 0; i <= 6; i++) {
                    Vec3 q = from.lerp(to, i / 6.0);
                    sl.sendParticles(ParticleTypes.FLAME, q.x, q.y, q.z, 1, 0, 0, 0, 0);
                }
            }
        }
        if (alive > 0 && getHealth() < getMaxHealth()) heal(1.5f * alive);
        if (alive == 0 && !cores.isEmpty()) {
            cores.clear();
            ctrl.say("Нет! Мои горны остыли!..");
        }
    }

    private void fireballs(ServerLevel sl, LivingEntity t) {
        visualStart(5, 6, 18);
        Vec3 base = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(getX(), getEyeY(), getZ());
        for (int i = -1; i <= 1; i++) {
            Vec3 dir = base.yRot(i * 0.15f).normalize();
            SmallFireball fb = new SmallFireball(sl, this, dir);
            fb.setPos(getX(), getEyeY(), getZ());
            sl.addFreshEntity(fb);
        }
        sl.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1f, 0.8f);
    }


    /** Пламя не рисуем: горн и раскалённые трещины светятся в самой модели. */
    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (ctrl.blocksDamage()) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        ctrl.onDeath(source, "Пламя... гаснет...");
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        ctrl.bar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        ctrl.bar.removePlayer(player);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ctrl.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ctrl.load(tag);
    }
}
