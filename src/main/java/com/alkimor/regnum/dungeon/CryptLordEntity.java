package com.alkimor.regnum.dungeon;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.kingdom.SoldierEntity;
import com.alkimor.regnum.survival.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerBossEvent;
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
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Морграт, Костяной Владыка — босс склепа. Три фазы:
 * <ol>
 *   <li>&gt;66% — ближний бой и сокрушительный удар по площади;</li>
 *   <li>33–66% — призывает скелетов-стражей, ускоряется;</li>
 *   <li>&lt;33% — черепа иссушения и волна проклятия.</li>
 * </ol>
 */
public class CryptLordEntity extends Monster implements com.alkimor.regnum.dungeon.boss.VisualActor {
    private static final com.alkimor.regnum.dungeon.boss.VisualAction VA = com.alkimor.regnum.dungeon.boss.VisualAction.of(CryptLordEntity.class);

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

    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private int phase = 1;
    private int slamCd = 60, summonCd = 100, skullCd = 60, curseCd = 160, invulTicks = 0;
    private int coneCd = 40, ringCd = 200, platesCd = 120, reapCd = 160;
    private final com.alkimor.regnum.dungeon.boss.Telegraph tele = new com.alkimor.regnum.dungeon.boss.Telegraph(this);

    public CryptLordEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 250;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 320.0)
                .add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, AbstractSkeleton.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, SoldierEntity.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, difficulty, reason, data);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(DungeonModule.MORGRATH_BLADE.get()));
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        for (EquipmentSlot s : EquipmentSlot.values()) setDropChance(s, 0f);
        return d;
    }

    // ------------------------------------------------------------------ фазы и способности

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        VA.setPhase(this, phase);
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (invulTicks > 0) invulTicks--;
        if (slamCd > 0) slamCd--;
        if (summonCd > 0) summonCd--;
        if (skullCd > 0) skullCd--;
        if (curseCd > 0) curseCd--;

        float hp = getHealth() / getMaxHealth();
        int newPhase = hp > 0.66f ? 1 : hp > 0.33f ? 2 : 3;
        if (newPhase > phase) {
            phase = newPhase;
            visualStart(7, 0, 40);
            onPhaseChange();
        }

        if (!(level() instanceof ServerLevel sl)) return;
        tele.tick(sl);
        if (coneCd > 0) coneCd--;
        if (ringCd > 0) ringCd--;
        if (platesCd > 0) platesCd--;
        if (reapCd > 0) reapCd--;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        if (tickCount % 20 == 0) tele.antiCheese(sl, target, () -> say(sl, "От меня не спрятаться — склеп везде!"));
        if (tele.busy()) return;

        double d2 = distanceToSqr(target);
        if (reapCd == 0 && com.alkimor.regnum.dungeon.boss.BossRules.reapArmy(sl, this, tele, ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.SOUL,
                14f, "Морграт обращает свой взор на ваше войско!")) {
            reapCd = 240;
            say(sl, "Ваше войско станет моим!");
            return;
        }
        if (coneCd == 0 && d2 < 8 * 8) {
            coneCd = phase >= 2 ? 70 : 100;
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            tele.cast(sl, com.alkimor.regnum.dungeon.boss.Telegraph.cone(position(), com.alkimor.regnum.dungeon.boss.Telegraph.toward(this, target), 7.5, 100, 18, 14f)
                    .root().warn(ParticleTypes.SOUL_FIRE_FLAME).burst(ParticleTypes.SWEEP_ATTACK).sound(SoundEvents.PLAYER_ATTACK_SWEEP)
                    .onHit((l, e) -> e.knockback(1.0, getX() - e.getX(), getZ() - e.getZ())));
            return;
        }
        if (slamCd == 0 && d2 < 30) {
            slamCd = phase >= 2 ? 120 : 170;
            tele.cast(sl, com.alkimor.regnum.dungeon.boss.Telegraph.circle(position(), 5.0, 26, 12f).root()
                    .warn(ParticleTypes.SOUL_FIRE_FLAME).burst(ParticleTypes.EXPLOSION).text("Морграт заносит клинок над землёй — отходите!")
                    .onHit((l, e) -> { e.knockback(1.4, getX() - e.getX(), getZ() - e.getZ()); e.push(0, 0.5, 0); }));
            return;
        }
        if (phase >= 2 && summonCd == 0) summon(sl, target);
        if (phase >= 2 && ringCd == 0) {
            ringCd = 280;
            say(sl, "Души склепа, сомкнитесь!");
            tele.cast(sl, com.alkimor.regnum.dungeon.boss.Telegraph.ring(position(), 3.0, 11.0, 34, 11f).root()
                    .warn(ParticleTypes.SCULK_SOUL).burst(ParticleTypes.SOUL).sound(SoundEvents.WARDEN_SONIC_BOOM)
                    .text("Кольцо душ! Спасение — вплотную к Морграту.")
                    .onHit((l, e) -> e.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1), this)));
            return;
        }
        if (phase >= 3 && skullCd == 0 && hasLineOfSight(target)) shootSkull(sl, target);
        if (phase >= 3 && platesCd == 0) {
            platesCd = 150;
            for (LivingEntity foe : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(18), com.alkimor.regnum.dungeon.boss.Telegraph::foe)) {
                tele.cast(sl, com.alkimor.regnum.dungeon.boss.Telegraph.circle(com.alkimor.regnum.dungeon.boss.Telegraph.lead(foe, 10), 2.4, 26, 11f)
                        .warn(ParticleTypes.WITCH).burst(ParticleTypes.SCULK_SOUL).sound(SoundEvents.WITHER_BREAK_BLOCK)
                        .onHit((l, e) -> e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1), this)));
            }
        }
        if (phase >= 3 && curseCd == 0) curse(sl);
    }

    private void onPhaseChange() {
        if (!(level() instanceof ServerLevel sl)) return;
        invulTicks = 40;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt != null) {
            bolt.moveTo(getX(), getY(), getZ());
            bolt.setVisualOnly(true);
            sl.addFreshEntity(bolt);
        }
        if (phase == 2) {
            say(sl, "Восстаньте, мои верные стражи!");
            bossEvent.setColor(BossEvent.BossBarColor.RED);
            var speed = getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(0.31);
        } else if (phase == 3) {
            say(sl, "Довольно! Познай проклятие склепа!");
            bossEvent.setColor(BossEvent.BossBarColor.WHITE);
            bossEvent.setDarkenScreen(true);
        }
    }

    private void summon(ServerLevel sl, LivingEntity target) {
        visualStart(3, 0, 28);
        summonCd = 300;
        long alive = sl.getEntitiesOfClass(AbstractSkeleton.class, getBoundingBox().inflate(24), e -> e.getTags().contains("regnum_minion")).size();
        if (alive >= 6) return;
        sl.playSound(null, blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.0f, 0.7f);
        for (int i = 0; i < 3; i++) {
            AbstractSkeleton sk = (phase >= 3 ? EntityType.WITHER_SKELETON : EntityType.SKELETON).create(sl);
            if (sk == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            BlockPos p = BlockPos.containing(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3);
            sk.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, random.nextFloat() * 360f, 0f);
            sk.finalizeSpawn(sl, sl.getCurrentDifficultyAt(p), MobSpawnType.MOB_SUMMONED, null);
            sk.addTag("regnum_minion");
            sk.setTarget(target);
            sl.addFreshEntity(sk);
            sl.sendParticles(ParticleTypes.SOUL, sk.getX(), sk.getY() + 1, sk.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
        }
    }

    private void shootSkull(ServerLevel sl, LivingEntity target) {
        skullCd = 50;
        double dx = target.getX() - getX();
        double dy = target.getY(0.5) - getEyeY();
        double dz = target.getZ() - getZ();
        WitherSkull skull = new WitherSkull(sl, this, new Vec3(dx, dy, dz).normalize());
        skull.setOwner(this);
        skull.setPos(getX(), getEyeY(), getZ());
        sl.addFreshEntity(skull);
        sl.playSound(null, blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.0f, 0.8f);
    }

    private void curse(ServerLevel sl) {
        curseCd = 200;
        sl.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 1, getZ(), 40, 4, 1, 4, 0.02);
        for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(12))) {
            p.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1), this);
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0), this);
        }
    }

    private void say(ServerLevel sl, String line) {
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) < 40 * 40) {
                p.sendSystemMessage(Text.of("Морграт: ", ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD).append(Text.of(line, ChatFormatting.LIGHT_PURPLE)));
            }
        }
    }

    // ------------------------------------------------------------------ служебное

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (invulTicks > 0) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel sl) {
            say(sl, "Нет... склеп... падёт...");
            if (source.getEntity() instanceof ServerPlayer killer) {
                Skills.addHonor(killer, 20, "повержен Костяной Владыка");
                Skills.chronicle(killer, "Повержен Морграт, Костяной Владыка");
                sl.getServer().getPlayerList().broadcastSystemMessage(
                        Text.of("⚔ " + killer.getName().getString() + " одолел Морграта, Костяного Владыку!", ChatFormatting.GOLD), false);
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", phase);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        phase = Math.max(1, tag.getInt("Phase"));
        if (hasCustomName()) bossEvent.setName(getDisplayName());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.WITHER) && !effect.is(MobEffects.POISON) && super.canBeAffected(effect);
    }
}
