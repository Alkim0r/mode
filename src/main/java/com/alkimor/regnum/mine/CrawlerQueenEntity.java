package com.alkimor.regnum.mine;

import com.alkimor.regnum.dungeon.boss.BossBase;
import com.alkimor.regnum.dungeon.boss.BossRules;
import com.alkimor.regnum.dungeon.boss.Telegraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Королева ползунов — хозяйка самой глубокой шахты.
 * Фаза 1: кислотные лужи и рывок. Фаза 2: выводок (призыв ползунов) и обвал потолка.
 * Фаза 3: Мрак — слепит и замедляет всех, из тьмы выходят новые стаи; кислота повсюду.
 * Слабое место — свет: у факела она теряет скорость, а при рывке остаётся оглушённой.
 */
public class CrawlerQueenEntity extends Monster {
    private final BossBase.Controller ctrl = new BossBase.Controller(this, "Королева ползунов", BossEvent.BossBarColor.RED,
            "Из её логова можно унести Хитиновые пластины и звёздное железо.");
    private final Telegraph tele = new Telegraph(this);
    private int stunned = 0;

    /** Визуальные действия для клиентских анимаций. */
    public static final int ACT_IDLE = 0, ACT_ACID = 1, ACT_CHARGE = 2, ACT_STUNNED = 3, ACT_BROOD = 4, ACT_CAVEIN = 5, ACT_DARKNESS = 6;
    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(CrawlerQueenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ACTION_START = SynchedEntityData.defineId(CrawlerQueenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ACTION_LEN = SynchedEntityData.defineId(CrawlerQueenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ACTION_IMPACT = SynchedEntityData.defineId(CrawlerQueenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(CrawlerQueenEntity.class, EntityDataSerializers.INT);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, 0).define(DATA_ACTION_START, 0).define(DATA_ACTION_LEN, 0).define(DATA_ACTION_IMPACT, 0).define(DATA_PHASE, 1);
    }

    /** Запускает визуальное действие: сервер хранит только начало и длительность, клиент считает прошедшее время сам. */
    private void act(int action, int ticks) {
        act(action, 0, ticks);
    }

    /** impact — тик удара от начала действия (учитывает сложность), ticks — полная длина с восстановлением. */
    private void act(int action, int impact, int ticks) {
        entityData.set(DATA_ACTION, action);
        entityData.set(DATA_ACTION_IMPACT, impact);
        entityData.set(DATA_ACTION_START, (int) level().getGameTime());
        entityData.set(DATA_ACTION_LEN, ticks);
    }

    private int elapsed() {
        return (int) level().getGameTime() - entityData.get(DATA_ACTION_START);
    }

    public int getVisualAction() {
        int a = entityData.get(DATA_ACTION);
        return a != ACT_IDLE && elapsed() <= entityData.get(DATA_ACTION_LEN) ? a : ACT_IDLE;
    }

    public int getVisualActionTicks() {
        return getVisualAction() == ACT_IDLE ? 0 : Math.max(0, elapsed());
    }

    public int getVisualActionDuration() {
        return getVisualAction() == ACT_IDLE ? 0 : entityData.get(DATA_ACTION_LEN);
    }

    public int getVisualActionImpact() {
        return getVisualAction() == ACT_IDLE ? 0 : entityData.get(DATA_ACTION_IMPACT);
    }

    public int getVisualPhase() {
        return entityData.get(DATA_PHASE);
    }

    public CrawlerQueenEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 300;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0).add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.ARMOR, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.7);
    }

    @Override
    protected void registerGoals() {
        BossBase.standardGoals(this, goalSelector, targetSelector, new MeleeAttackGoal(this, 1.0, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        int np = ctrl.tick();
        if (entityData.get(DATA_PHASE) != ctrl.phase) entityData.set(DATA_PHASE, ctrl.phase);
        if (np == 2) ctrl.say("Дети мои, проснитесь! В гнезде — чужие!");
        if (np == 3) ctrl.say("Тьма — мой дом. Вы в нём — лишь пища!");
        if (!(level() instanceof ServerLevel sl)) return;
        tele.tick(sl);
        if (dashLeft > 0) {
            // рывок идёт 6 тиков с проверкой столкновения каждый тик (не телепорт)
            getNavigation().stop();
            Vec3 before = position();
            move(net.minecraft.world.entity.MoverType.SELF, dashStep);
            if (position().distanceToSqr(before) < dashStep.lengthSqr() * 0.25) dashLeft = 0; else dashLeft--;
            if (tickCount % 2 == 0) sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 4, 0.6, 0.2, 0.6, 0.02);
            if (dashLeft <= 0) {
                stunned = 50;
                act(ACT_STUNNED, 0, 50);
                sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.5, getZ(), 20, 0.8, 0.3, 0.8, 0.05);
            }
            return;
        }
        if (stunned > 0) {
            stunned--;
            getNavigation().stop();
            if (stunned % 10 == 0) sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + 2, getZ(), 6, 0.6, 0.3, 0.6, 0.1);
            return;
        }
        // свет — слабость: рядом с ярким источником теряет скорость
        if (tickCount % 20 == 0) {
            boolean lit = sl.getMaxLocalRawBrightness(blockPosition()) >= 10;
            if (lit) addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
        }
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive()) return;
        if (tickCount % 20 == 0) tele.antiCheese(sl, t, () -> ctrl.say("Стены — мои глаза. Прятаться негде!"));
        if (tele.busy()) return;
        int phase = ctrl.phase;

        if (ctrl.ready(4, 260) && BossRules.reapArmy(sl, this, tele, ParticleTypes.SQUID_INK, ParticleTypes.POOF, 12f,
                "Королева ползунов обрушивает кислоту на ваше войско!")) {
            return;
        }
        if (ctrl.ready(0, phase >= 3 ? 90 : 140)) {
            Vec3 at = Telegraph.lead(t, 14);
            act(ACT_ACID, Telegraph.windupTicks(22), Telegraph.windupTicks(22) + 8);
            tele.cast(sl, Telegraph.circle(at, 3.4, 22, 8f).warn(ParticleTypes.ITEM_SLIME).burst(ParticleTypes.SNEEZE)
                    .sound(SoundEvents.SPIDER_HURT).text("Кислота! Уходите с зелёного круга!")
                    .onFire(l -> acid(l, at)));
        }
        double d = distanceToSqr(t);
        if (d > 5 * 5 && d < 16 * 16 && ctrl.ready(1, 200)) {
            Vec3 rawDir = Telegraph.toward(this, t);
            final Vec3 dir = rawDir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : rawDir.normalize();
            ctrl.say("Рывок!");
            act(ACT_CHARGE, Telegraph.windupTicks(26), Telegraph.windupTicks(26) + 6);
            tele.cast(sl, Telegraph.line(position(), dir, 15, 1.6, 26, 12f).root().warn(ParticleTypes.CRIT).burst(ParticleTypes.POOF)
                    .sound(SoundEvents.RAVAGER_ROAR).text("Королева рвётся вперёд! Отойдите в сторону — после рывка она оглушена!")
                    .onHit((l, e) -> { e.push(dir.x * 1.2, 0.4, dir.z * 1.2); e.hurtMarked = true; })
                    .onFire(l -> {
                        // рывок: сама перемещается по линии и на миг оглушена
                        Vec3 to = safeDashEnd(l, dir, 10);
                        Vec3 delta = to.subtract(position());
                        if (delta.lengthSqr() > 1.0) {
                            dashStep = delta.scale(1.0 / DASH_TICKS);
                            dashLeft = DASH_TICKS;
                        } else {
                            stunned = 50;
                            act(ACT_STUNNED, 0, 50);
                        }
                    }));
            return;
        }
        if (phase >= 2 && ctrl.ready(2, 340)) brood(sl, t, 4);
        if (phase >= 2 && ctrl.ready(3, 240)) cavein(sl, t);
        if (phase >= 3 && ctrl.ready(5, 420)) darkness(sl, t);
    }

    /** Рывок по прямой шагами по 0.5 блока: останавливается перед первой стеной и не прыгает дальше max блоков. */
    private static final int DASH_TICKS = 6;
    private int dashLeft;
    private Vec3 dashStep = Vec3.ZERO;

    private Vec3 safeDashEnd(ServerLevel sl, Vec3 dir, double max) {
        Vec3 best = position();
        for (double d = 0.5; d <= max; d += 0.5) {
            Vec3 p = position().add(dir.scale(d));
            if (!sl.noCollision(this, getBoundingBox().move(p.subtract(position())))) break;
            // нужна опора под ногами и не лава
            BlockPos below = BlockPos.containing(p.x, p.y - 0.6, p.z);
            if (!sl.getBlockState(below).isSolid() || sl.getFluidState(below.above()).is(net.minecraft.tags.FluidTags.LAVA)) continue;
            best = p;
        }
        return best;
    }

    private void acid(ServerLevel sl, Vec3 at) {
        AreaEffectCloud cloud = new AreaEffectCloud(sl, at.x, at.y, at.z);
        cloud.setOwner(this);
        cloud.setRadius(3.4f);
        cloud.setDuration(160);
        cloud.setWaitTime(10);
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
        cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        sl.addFreshEntity(cloud);
        sl.playSound(null, BlockPos.containing(at), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.2f, 0.5f);
    }

    private void brood(ServerLevel sl, LivingEntity t, int n) {
        int alive = sl.getEntitiesOfClass(CrawlerEntity.class, getBoundingBox().inflate(28), e -> e.getTags().contains("regnum_minion") && e.isAlive()).size();
        if (alive >= 10) return;
        n = Math.min(n, 10 - alive);
        ctrl.say("Выводок, к бою!");
        act(ACT_BROOD, 10, 24);
        for (int i = 0; i < n; i++) {
            CrawlerEntity c = MineModule.CRAWLER.get().create(sl);
            if (c == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            BlockPos p = BlockPos.containing(getX() + Math.cos(a) * 3.5, getY(), getZ() + Math.sin(a) * 3.5);
            c.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, random.nextFloat() * 360f, 0f);
            if (!sl.noCollision(c)) c.moveTo(getX(), getY(), getZ(), random.nextFloat() * 360f, 0f); // в тесном туннеле — рядом с королевой, не в стене
            c.finalizeSpawn(sl, sl.getCurrentDifficultyAt(p), MobSpawnType.MOB_SUMMONED, null);
            c.addTag("regnum_minion");
            c.setTarget(t);
            sl.addFreshEntity(c);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()), c.getX(), c.getY() + 0.3, c.getZ(), 15, 0.4, 0.2, 0.4, 0.05);
        }
        sl.playSound(null, blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 1.6f, 0.4f);
    }

    private void cavein(ServerLevel sl, LivingEntity t) {
        ctrl.say("Потолок падает!");
        act(ACT_CAVEIN, Telegraph.windupTicks(30), Telegraph.windupTicks(30) + 10);
        for (int i = 0; i < 4; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = i == 0 ? 0 : 2 + random.nextDouble() * 5;
            Vec3 at = t.position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
            tele.cast(sl, Telegraph.circle(at, 2.6, 30, 9f).warn(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.GRAVEL.defaultBlockState()))
                    .burst(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState())).sound(SoundEvents.GRAVEL_BREAK)
                    .text(i == 0 ? "С потолка сыплются камни! Выходите из кругов!" : null)
                    .onHit((l, e) -> e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), this)));
        }
    }

    private void darkness(ServerLevel sl, LivingEntity t) {
        ctrl.say("Мрак!");
        for (LivingEntity e : ctrl.foesAround(18)) {
            e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0), this);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0), this);
        }
        brood(sl, t, 5);
        act(ACT_DARKNESS, 10, 30); // после призыва, чтобы выводок не затёр визуальное действие Мрака
        sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.5, getZ(), 80, 4, 1.5, 4, 0.05);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.POISON) && super.canBeAffected(effect);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (ctrl.blocksDamage()) return false;
        return super.hurt(source, stunned > 0 ? amount * 1.5f : amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        ctrl.onDeath(source, "Гнездо... умирает... но тьма... вечна...");
        if (source.getEntity() instanceof ServerPlayer p) com.alkimor.regnum.story.Quests.setFlag(p, "queen_slain");
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
        return SoundEvents.SPIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }
}
