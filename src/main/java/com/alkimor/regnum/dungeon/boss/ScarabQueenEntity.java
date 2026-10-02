package com.alkimor.regnum.dungeon.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Сехмет-ра, царица-скарабей — владычица Песчаной гробницы.
 * Рои скарабеев, песчаная буря, уход в песок с ударом из-за спины, солнечное проклятие.
 */
public class ScarabQueenEntity extends Monster implements VisualActor {
    private static final com.alkimor.regnum.dungeon.boss.VisualAction VA = com.alkimor.regnum.dungeon.boss.VisualAction.of(ScarabQueenEntity.class);

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

    private final BossBase.Controller ctrl = new BossBase.Controller(this, "Сехмет-ра", BossEvent.BossBarColor.YELLOW,
            "В песке блестит Солнечный янтарь.");
    private int burrow = 0;
    private Vec3 emergeAt = null;
    private final Telegraph tele = new Telegraph(this);

    public ScarabQueenEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 240;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0).add(Attributes.ATTACK_DAMAGE, 9.0).add(Attributes.ARMOR, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.7);
    }

    @Override
    protected void registerGoals() {
        BossBase.standardGoals(this, goalSelector, targetSelector, new MeleeAttackGoal(this, 1.1, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, difficulty, reason, data);
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        for (EquipmentSlot s : EquipmentSlot.values()) setDropChance(s, 0f);
        return d;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        VA.setPhase(this, ctrl.phase);
        int newPhase = ctrl.tick();
        if (newPhase > 0) visualStart(7, 0, 40);
        if (newPhase == 2) ctrl.say("Пески, поглотите их!");
        if (newPhase == 3) ctrl.say("Солнце пустыни — моё око. Сгори в его взгляде!");
        if (!(level() instanceof ServerLevel sl)) return;

        tele.tick(sl);
        LivingEntity t = getTarget();
        if (burrow > 0) {
            burrow--;
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), getX(), getY(), getZ(), 8, 0.4, 0.1, 0.4, 0);
            if (burrow == 0 && t != null) emerge(sl, t);
            return;
        }
        if (t == null || !t.isAlive()) return;
        if (tickCount % 20 == 0) tele.antiCheese(sl, t, () -> ctrl.say("Пески найдут тебя!"));
        if (tele.busy()) return;
        int phase = ctrl.phase;
        if (ctrl.ready(4, 260) && BossRules.reapArmy(sl, this, tele, ParticleTypes.END_ROD, ParticleTypes.FLASH, 13f,
                "Сехмет-ра призывает солнце на ваше войско!")) {
            ctrl.say("Солнце сожжёт ваших рабов!");
            return;
        }
        if (distanceToSqr(t) < 7 * 7 && ctrl.ready(5, phase >= 2 ? 70 : 100)) {
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            tele.cast(sl, Telegraph.cone(position(), Telegraph.toward(this, t), 6.5, 75, 14, 11f).root()
                    .warn(ParticleTypes.CRIT).burst(ParticleTypes.SWEEP_ATTACK).sound(SoundEvents.PLAYER_ATTACK_SWEEP)
                    .onHit((l, e) -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1), this)));
            return;
        }
        if (ctrl.ready(0, phase >= 3 ? 180 : 240)) swarm(sl, t);
        if (phase >= 2 && ctrl.ready(1, 300)) sandstorm(sl);
        if (phase >= 2 && ctrl.ready(2, 220)) startBurrow(sl, t);
        if (phase >= 3 && ctrl.ready(3, 260)) {
            ctrl.say("Взгляни на солнце!");
            double base = random.nextDouble() * Math.PI;
            for (int i = 0; i < 4; i++) {
                double a = base + i * Math.PI / 2;
                tele.cast(sl, Telegraph.line(position(), new Vec3(Math.cos(a), 0, Math.sin(a)), 18, 1.3, 30, 13f).root()
                        .warn(ParticleTypes.END_ROD).burst(ParticleTypes.FLASH).sound(SoundEvents.BEACON_ACTIVATE)
                        .text(i == 0 ? "Лучи солнца! Встаньте между лучами!" : null)
                        .onHit((l, e) -> e.igniteForSeconds(4)));
            }
        }
    }

    private void swarm(ServerLevel sl, LivingEntity t) {
        visualStart(3, 0, 28);
        int alive = sl.getEntitiesOfClass(Silverfish.class, getBoundingBox().inflate(24), e -> e.getTags().contains("regnum_minion")).size();
        if (alive >= 10) return;
        for (int i = 0; i < 4; i++) {
            Silverfish s = EntityType.SILVERFISH.create(sl);
            if (s == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            s.moveTo(getX() + Math.cos(a) * 2, getY(), getZ() + Math.sin(a) * 2, 0, 0);
            s.addTag("regnum_minion");
            s.setCustomName(Component.literal("Скарабей"));
            s.setTarget(t);
            sl.addFreshEntity(s);
        }
        sl.playSound(null, blockPosition(), SoundEvents.SILVERFISH_AMBIENT, SoundSource.HOSTILE, 1.5f, 0.5f);
    }

    private void sandstorm(ServerLevel sl) {
        visualStart(4, 8, 30);
        ctrl.say("Буря!");
        for (LivingEntity e : ctrl.foesAround(16)) {
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), this);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), this);
        }
        sl.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                getX(), getY() + 3, getZ(), 300, 10, 3, 10, 0);
    }

    private void startBurrow(ServerLevel sl, LivingEntity t) {
        visualStart(2, 4, 24);
        burrow = 30;
        emergeAt = Telegraph.lead(t, 18);
        tele.cast(sl, Telegraph.circle(emergeAt, 3.0, 30, 12f).warn(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()))
                .burst(ParticleTypes.EXPLOSION).sound(SoundEvents.SAND_BREAK).text("Песок под ногами шевелится!")
                .onHit((l, e) -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1), this)));
        ctrl.invul = Math.max(ctrl.invul, 32);
        addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 32, 0, false, false));
        getNavigation().stop();
        sl.playSound(null, blockPosition(), SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 1.5f, 0.6f);
    }

    private void emerge(ServerLevel sl, LivingEntity t) {
        Vec3 behind = emergeAt != null ? emergeAt : t.position().subtract(t.getLookAngle().multiply(1, 0, 1).normalize().scale(2.0));
        emergeAt = null;
        BlockPos p = BlockPos.containing(behind);
        if (sl.getBlockState(p).isAir() && sl.getBlockState(p.above()).isAir() && sl.getBlockState(p.below()).isSolid()) {
            teleportTo(behind.x, behind.y, behind.z);
        }
        removeEffect(MobEffects.INVISIBILITY);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), getX(), getY() + 1, getZ(), 60, 0.6, 1, 0.6, 0.1);
        if (distanceToSqr(t) < 9) doHurtTarget(t);
    }

    private void solarCurse(ServerLevel sl) {
        for (LivingEntity e : ctrl.foesAround(14)) {
            e.igniteForSeconds(3);
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0), this);
            sl.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 2.5, e.getZ(), 15, 0.2, 0.5, 0.2, 0.02);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (ctrl.blocksDamage()) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        ctrl.onDeath(source, "Солнце... зашло...");
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
        return SoundEvents.HUSK_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.HUSK_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HUSK_DEATH;
    }
}
