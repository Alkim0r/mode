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
        // фокус огня: если соратники рядом уже бьют цель — берём её (так отряд давит одного врага, а не растекается)
        if (mob instanceof com.alkimor.regnum.kingdom.SoldierEntity self) {
            LivingEntity focus = focusTarget(self);
            if (focus != null && withinEngagementOf(self, focus)) {
                self.setTarget(focus);
                return false; // цель уже назначена, обычный поиск не нужен
            }
        }
        boolean found = super.canUse();
        if (!found) backoff = 12 + mob.getRandom().nextInt(14); // ничего не нашли — следующий поиск не сразу
        return found;
    }

    private static boolean withinEngagementOf(com.alkimor.regnum.kingdom.SoldierEntity s, LivingEntity e) {
        return s.withinEngagement(e);
    }

    /** Самый частый элемент списка (null — пусто). При равенстве берётся тот, что встретился раньше. */
    public static <E> E mostCommon(java.util.List<E> items) {
        E best = null;
        int bc = 0;
        java.util.Map<E, Integer> count = new java.util.LinkedHashMap<>();
        for (E e : items) count.merge(e, 1, Integer::sum);
        for (var en : count.entrySet()) if (en.getValue() > bc) { bc = en.getValue(); best = en.getKey(); }
        return best;
    }

    /** Цель, на которую сейчас нацелено больше всего соратников в 10 блоках. */
    private static LivingEntity focusTarget(com.alkimor.regnum.kingdom.SoldierEntity self) {
        java.util.List<LivingEntity> targets = new java.util.ArrayList<>();
        for (com.alkimor.regnum.kingdom.SoldierEntity m : self.level().getEntitiesOfClass(com.alkimor.regnum.kingdom.SoldierEntity.class,
                self.getBoundingBox().inflate(10, 4, 10), o -> o != self && o.isAlive() && self.isAlliedTo(o))) {
            LivingEntity t = m.getTarget();
            if (t != null && t.isAlive() && !self.isAlliedTo(t)) targets.add(t);
        }
        return mostCommon(targets);
    }
}
