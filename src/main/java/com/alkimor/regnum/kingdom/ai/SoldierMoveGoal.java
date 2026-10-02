package com.alkimor.regnum.kingdom.ai;

import com.alkimor.regnum.kingdom.Order;
import com.alkimor.regnum.kingdom.SoldierEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Движение к своему месту в строю, когда нет врага. */
public class SoldierMoveGoal extends Goal {
    private final SoldierEntity soldier;
    private int recalc;

    public SoldierMoveGoal(SoldierEntity soldier) {
        this.soldier = soldier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (soldier.getTarget() != null || soldier.getOrder() == Order.ATTACK_TARGET) return false;
        return soldier.position().distanceToSqr(soldier.desiredPosition()) > 2.25;
    }

    @Override
    public boolean canContinueToUse() {
        return soldier.getTarget() == null && soldier.position().distanceToSqr(soldier.desiredPosition()) > 1.0;
    }

    @Override
    public void start() {
        recalc = 0;
    }

    @Override
    public void tick() {
        if (--recalc > 0) return;
        recalc = 10;
        Vec3 d = soldier.desiredPosition();
        double dist = soldier.position().distanceTo(d);
        double speed = dist > 12 ? 1.35 : soldier.getOrder() == Order.FOLLOW ? 1.15 : 1.0;
        soldier.getNavigation().moveTo(d.x, d.y, d.z, speed);
    }

    @Override
    public void stop() {
        soldier.getNavigation().stop();
        if (soldier.getOrder() != Order.FOLLOW) {
            soldier.setYRot(soldier.getFacing());
            soldier.setYHeadRot(soldier.getFacing());
            soldier.yBodyRot = soldier.getFacing();
        }
    }
}
