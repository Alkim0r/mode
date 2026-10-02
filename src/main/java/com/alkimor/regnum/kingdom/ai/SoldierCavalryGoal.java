package com.alkimor.regnum.kingdom.ai;

import com.alkimor.regnum.kingdom.SoldierEntity;
import com.alkimor.regnum.kingdom.SoldierType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Атаки конницы заходами. Лёгкая: разгон → удар → отход на 12–14 блоков → новый заход (флангом).
 * Тяжёлая: короткий разгон → таранный удар → разворот на 6 блоков. Сила удара растёт со скоростью коня (см. Combat).
 */
public class SoldierCavalryGoal extends Goal {
    private final SoldierEntity s;
    private enum Phase { CHARGE, DISENGAGE }
    private Phase phase = Phase.CHARGE;
    private int timer;
    private int hitCooldown;

    public SoldierCavalryGoal(SoldierEntity s) {
        this.s = s;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = s.getTarget();
        return s.isMountedCavalry() && t != null && t.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        phase = Phase.CHARGE;
        timer = 0;
    }

    @Override
    public void stop() {
        s.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity t = s.getTarget();
        if (t == null) return;
        timer++;
        hitCooldown = Math.max(0, hitCooldown - 1);
        double d2 = s.distanceToSqr(t);
        boolean heavy = s.getSoldierType() == SoldierType.HEAVY_CAV;
        if (phase == Phase.CHARGE) {
            s.getLookControl().setLookAt(t, 30f, 30f);
            if (timer % 4 == 0 || s.getNavigation().isDone()) s.getNavigation().moveTo(t, 1.45);
            if (d2 <= 7.5 && hitCooldown == 0 && s.getSensing().hasLineOfSight(t)) {
                s.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                s.doHurtTarget(t);
                hitCooldown = heavy ? 24 : 16;
                phase = Phase.DISENGAGE;
                timer = 0;
            }
        } else {
            // отход: точка за спиной врага относительно него самого, со смещением на фланг
            if (timer == 1 || s.getNavigation().isDone()) {
                Vec3 away = s.position().subtract(t.position());
                double len = Math.max(0.01, Math.sqrt(away.x * away.x + away.z * away.z));
                double dist = heavy ? 7 : 13;
                double side = (s.getUUID().hashCode() & 1) == 0 ? 0.5 : -0.5;
                double ax = away.x / len, az = away.z / len;
                double px = s.getX() + (ax - az * side) * dist, pz = s.getZ() + (az + ax * side) * dist;
                s.getNavigation().moveTo(px, s.getY(), pz, 1.5);
            }
            if (timer > (heavy ? 30 : 45) || (d2 > (heavy ? 49 : 100) && timer > 12)) {
                phase = Phase.CHARGE;
                timer = 0;
            }
        }
    }
}
