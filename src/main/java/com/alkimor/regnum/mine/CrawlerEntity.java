package com.alkimor.regnum.mine;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;

/**
 * Шахтный ползун: тёмный многоногий хищник глубоких пещер. Лазает по стенам, прыгает, живёт стаей:
 * ранение одного поднимает всех вокруг. Яд кислоты замедляет. Боится света — при ярком освещении теряет цель.
 */
public class CrawlerEntity extends Spider {
    public CrawlerEntity(EntityType<? extends Spider> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Spider.createAttributes()
                .add(Attributes.MAX_HEALTH, 22.0)
                .add(Attributes.ATTACK_DAMAGE, 4.5)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ARMOR, 3.0)
                .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        targetSelector.addGoal(1, new HurtByTargetGoal(this, CrawlerEntity.class).setAlertOthers());
        // Выводок Королевы: сам ищет бойцов и героев при любом свете (ванильный паук теряет цель на свету и ищет только в темноте).
        targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, false, false,
                e -> isMinion() && (e instanceof com.alkimor.regnum.kingdom.SoldierEntity
                        || e instanceof net.minecraft.world.entity.player.Player p && !p.isCreative() && !p.isSpectator())));
    }

    private net.minecraft.world.phys.Vec3 stallPos = net.minecraft.world.phys.Vec3.ZERO;
    private int stallTicks;

    /** Индекс ближайшей из дистанций (-1 — список пуст). */
    public static int nearestIndex(double[] dists) {
        int best = -1;
        for (int i = 0; i < dists.length; i++) if (best < 0 || dists[i] < dists[best]) best = i;
        return best;
    }

    /** Выводок застрял на недостижимой цели: берём ближайшую другую досягаемую; иначе сбрасываем цель, и поиск идёт заново. */
    public boolean retarget() {
        LivingEntity cur = getTarget();
        java.util.List<LivingEntity> cands = new java.util.ArrayList<>();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(28),
                e -> e != cur && e.isAlive() && (e instanceof com.alkimor.regnum.kingdom.SoldierEntity
                        || e instanceof net.minecraft.world.entity.player.Player p && !p.isCreative() && !p.isSpectator()))) {
            if (getNavigation().createPath(e, 0) != null) cands.add(e);
        }
        double[] d = new double[cands.size()];
        for (int i = 0; i < d.length; i++) d[i] = distanceToSqr(cands.get(i));
        int idx = nearestIndex(d);
        if (idx >= 0) { super.setTarget(cands.get(idx)); return true; }
        super.setTarget(null);
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isMinion()) return;
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive() || distanceToSqr(t) < 9.0) { stallTicks = 0; stallPos = position(); return; }
        if (tickCount % 10 == 0) {
            if (position().distanceToSqr(stallPos) < 0.04) stallTicks += 10; else stallTicks = 0;
            stallPos = position();
            if (stallTicks >= 60) { stallTicks = 0; retarget(); }
        }
    }
    public boolean isMinion() {
        return getTags().contains("regnum_minion");
    }

    /** Выводок не бросает живую цель из-за света; цель, которая умерла или исчезла, сбрасывается как обычно. */
    @Override
    public void setTarget(@org.jetbrains.annotations.Nullable LivingEntity target) {
        if (target == null && isMinion()) {
            LivingEntity cur = getTarget();
            boolean invalid = cur == null || !cur.isAlive() || cur.isRemoved()
                    || cur instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator())
                    || distanceToSqr(cur) > 60.0 * 60.0;
            if (!invalid) return;
        }
        super.setTarget(target);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !swinging) swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        if (hit && target instanceof LivingEntity le && random.nextInt(3) == 0) {
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0), this);
            le.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0), this);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean r = super.hurt(source, amount);
        if (r && source.getEntity() instanceof LivingEntity attacker && !level().isClientSide && tickCount % 7 == 0) {
            for (CrawlerEntity c : level().getEntitiesOfClass(CrawlerEntity.class, getBoundingBox().inflate(16), e -> e != this && e.getTarget() == null)) c.setTarget(attacker);
        }
        return r;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return true;
    }
}
