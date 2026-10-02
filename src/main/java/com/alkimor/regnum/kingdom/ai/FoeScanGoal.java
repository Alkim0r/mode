package com.alkimor.regnum.kingdom.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

import java.util.function.Predicate;

/**
 * Поиск цели для больших армий: не сканирует округу, пока у бойца есть живая цель, и делает это реже обычного.
 * Профиль 240 бойцов показал, что обычный NearestAttackableTargetGoal съедал ~80% тика сервера.
 */
public class FoeScanGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
    private java.util.function.DoubleSupplier range;
    private int backoff;

    /** Ограничить радиус поиска (например, радиусом боевой готовности приказа). */
    public FoeScanGoal<T> range(java.util.function.DoubleSupplier r) {
        this.range = r;
        return this;
    }

    @Override
    protected double getFollowDistance() {
        double base = super.getFollowDistance();
        return range == null ? base : Math.min(base, range.getAsDouble());
    }

    public FoeScanGoal(Mob mob, Class<T> cls, int interval, boolean mustSee, boolean mustReach, Predicate<LivingEntity> filter) {
        super(mob, cls, interval, mustSee, mustReach, filter);
    }

    public FoeScanGoal(Mob mob, Class<T> cls, boolean mustSee) {
        super(mob, cls, 10, mustSee, false, null);
    }

    @Override
    public boolean canUse() {
        LivingEntity t = mob.getTarget();
        if (t != null && t.isAlive()) return false;
        if (backoff > 0) {
            backoff--;
            return false;
        }
        boolean found = super.canUse();
        if (!found) backoff = 12 + mob.getRandom().nextInt(14); // ничего не нашли — следующий поиск не сразу
        return found;
    }
}
