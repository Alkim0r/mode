package com.alkimor.regnum.dungeon.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Матушка Топь — хозяйка Затопленного капища.
 * Ядовитые облака, хватка трясины (подтягивает к себе), призыв утопцев, зелья.
 * В воде восстанавливает здоровье — её нужно выманить на сушу.
 */
public class MireMotherEntity extends Monster implements VisualActor {
    private static final com.alkimor.regnum.dungeon.boss.VisualAction VA = com.alkimor.regnum.dungeon.boss.VisualAction.of(MireMotherEntity.class);

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

    private final BossBase.Controller ctrl = new BossBase.Controller(this, "Матушка Топь", BossEvent.BossBarColor.GREEN,
            "Из её логова можно унести Гнилой корень.");
    private final Telegraph tele = new Telegraph(this);

    public MireMotherEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 220;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 280.0).add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.ARMOR, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
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
        if (newPhase == 2) ctrl.say("Дети болот, ко мне! Утащите их на дно!");
        if (newPhase == 3) ctrl.say("Трясина помнит каждого, кто в ней утонул... И ты станешь одним из них!");
        if (!(level() instanceof ServerLevel sl)) return;

        if (isInWaterOrBubble() && tickCount % 20 == 0 && getHealth() < getMaxHealth()) {
            heal(3f);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 2, getZ(), 4, 0.5, 0.5, 0.5, 0);
            if (tickCount % 200 == 0) ctrl.say("Вода болот — моя кровь... Ха-ха-ха!");
        }

        tele.tick(sl);
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive()) return;
        if (tickCount % 20 == 0) tele.antiCheese(sl, t, () -> ctrl.say("Трясина достанет тебя где угодно!"));
        if (tele.busy()) return;
        int phase = ctrl.phase;

        if (ctrl.ready(4, 260) && BossRules.reapArmy(sl, this, tele, ParticleTypes.SQUID_INK, ParticleTypes.SPLASH, 12f,
                "Матушка Топь тянет ваших солдат в трясину!")) {
            ctrl.say("Болоту нужны новые утопленники!");
            return;
        }
        if (ctrl.ready(0, phase >= 3 ? 110 : 160)) {
            Vec3 at = Telegraph.lead(t, 14);
            tele.cast(sl, Telegraph.circle(at, 3.2, 22, 6f).warn(ParticleTypes.ITEM_SLIME).burst(ParticleTypes.SNEEZE)
                    .sound(SoundEvents.WITCH_THROW).onFire(l -> poisonCloud(l, at)));
        }
        if (distanceToSqr(t) < 18 * 18 && ctrl.ready(1, 220)) {
            Vec3 dir = Telegraph.toward(this, t);
            tele.cast(sl, Telegraph.line(position(), dir, 16, 1.4, 20, 7f).root().warn(ParticleTypes.SQUID_INK).burst(ParticleTypes.SPLASH)
                    .sound(SoundEvents.PLAYER_SPLASH_HIGH_SPEED).text("Плеть трясины! Уходите с линии!")
                    .onHit((l, e) -> bogGrip(l, e)));
            return;
        }
        if (phase >= 2 && ctrl.ready(2, 360)) summon(sl, t);
        if (phase >= 2 && hasLineOfSight(t) && ctrl.ready(3, 70)) throwPotion(t);
        if (phase >= 2 && ctrl.ready(5, 240)) {
            for (int i = 0; i < 4; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = 1.5 + random.nextDouble() * 4;
                Vec3 at = t.position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                tele.cast(sl, Telegraph.circle(at, 2.8, 28, 9f).warn(ParticleTypes.SQUID_INK).burst(ParticleTypes.BUBBLE_POP)
                        .sound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE)
                        .onHit((l, e) -> { e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), this); e.push(0, -0.6, 0); }));
            }
        }
        if (phase >= 3 && ctrl.ready(6, 300)) {
            ctrl.say("Корни болот, держите их!");
            tele.cast(sl, Telegraph.ring(position(), 4.0, 13.0, 32, 8f).root().warn(ParticleTypes.MYCELIUM).burst(ParticleTypes.SPORE_BLOSSOM_AIR)
                    .sound(SoundEvents.ROOTED_DIRT_BREAK).text("Корни поднимаются! Ближе к ведьме — или прочь из круга!")
                    .onHit((l, e) -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4), this)));
        }
    }

    private void poisonCloud(ServerLevel sl, Vec3 at) {
        AreaEffectCloud cloud = new AreaEffectCloud(sl, at.x, at.y, at.z);
        cloud.setOwner(this);
        cloud.setRadius(3.2f);
        cloud.setDuration(140);
        cloud.setWaitTime(10);
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        sl.addFreshEntity(cloud);
        sl.playSound(null, BlockPos.containing(at), SoundEvents.WITCH_THROW, SoundSource.HOSTILE, 1f, 0.6f);
    }

    private void bogGrip(ServerLevel sl, LivingEntity t) {
        Vec3 d = position().subtract(t.position()).normalize().scale(1.3);
        t.push(d.x, 0.35, d.z);
        t.hurtMarked = true;
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 3), this);
        sl.sendParticles(ParticleTypes.SQUID_INK, t.getX(), t.getY() + 0.5, t.getZ(), 20, 0.4, 0.4, 0.4, 0.02);
        if (t instanceof ServerPlayer p) p.displayClientMessage(net.minecraft.network.chat.Component.literal("Трясина затягивает вас!")
                .withStyle(net.minecraft.ChatFormatting.DARK_GREEN), true);
    }

    private void summon(ServerLevel sl, LivingEntity t) {
        visualStart(3, 0, 28);
        int alive = sl.getEntitiesOfClass(Drowned.class, getBoundingBox().inflate(24), e -> e.getTags().contains("regnum_minion")).size();
        if (alive >= 6) return;
        for (int i = 0; i < 3; i++) {
            Drowned d = EntityType.DROWNED.create(sl);
            if (d == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            BlockPos p = BlockPos.containing(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3);
            d.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, random.nextFloat() * 360f, 0f);
            d.finalizeSpawn(sl, sl.getCurrentDifficultyAt(p), MobSpawnType.MOB_SUMMONED, null);
            d.addTag("regnum_minion");
            d.setTarget(t);
            sl.addFreshEntity(d);
            sl.sendParticles(ParticleTypes.SPLASH, d.getX(), d.getY() + 1, d.getZ(), 20, 0.4, 0.6, 0.4, 0.1);
        }
    }

    private void throwPotion(LivingEntity t) {
        visualStart(5, 6, 16);
        var potion = switch (random.nextInt(3)) {
            case 0 -> Potions.HARMING;
            case 1 -> Potions.SLOWNESS;
            default -> Potions.WEAKNESS;
        };
        ThrownPotion tp = new ThrownPotion(level(), this);
        tp.setItem(PotionContents.createItemStack(Items.SPLASH_POTION, potion));
        tp.setXRot(tp.getXRot() + 20f);
        double dx = t.getX() + t.getDeltaMovement().x - getX();
        double dy = t.getEyeY() - 1.1 - getY();
        double dz = t.getZ() + t.getDeltaMovement().z - getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        tp.shoot(dx, dy + dist * 0.2, dz, 0.75f, 8f);
        level().addFreshEntity(tp);
        playSound(SoundEvents.WITCH_THROW, 1f, 0.8f);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.POISON) && super.canBeAffected(effect);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (ctrl.blocksDamage()) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        ctrl.onDeath(source, "Болота... заберут... тебя...");
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
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

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITCH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.WITCH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITCH_DEATH;
    }
}
