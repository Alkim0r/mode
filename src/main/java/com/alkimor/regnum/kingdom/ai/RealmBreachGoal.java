package com.alkimor.regnum.kingdom.ai;

import com.alkimor.regnum.kingdom.Order;
import com.alkimor.regnum.kingdom.Realms;
import com.alkimor.regnum.kingdom.SoldierEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Осаждающие бойцы ломают стены чужого города, если путь к цели закрыт; сапёры делают это втрое быстрее. */
public class RealmBreachGoal extends Goal {
    private final SoldierEntity soldier;
    private int stuck;

    public RealmBreachGoal(SoldierEntity soldier) {
        this.soldier = soldier;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (soldier.realmId() == null || soldier.getTarget() != null || soldier.getOrder() != Order.CHARGE) return false;
        if (soldier.getNavigation().isInProgress() && soldier.getDeltaMovement().horizontalDistanceSqr() > 0.0004) {
            stuck = 0;
            return false;
        }
        Vec3 d = soldier.desiredPosition();
        if (soldier.position().distanceToSqr(d) < 9) return false;
        return ++stuck > 20;
    }

    @Override
    public boolean canContinueToUse() {
        return soldier.getTarget() == null && stuck > 0;
    }

    @Override
    public void tick() {
        if (soldier.tickCount % 8 != 0) return;
        Realms.breach(soldier, soldier.desiredPosition());
    }

    @Override
    public void stop() {
        stuck = 0;
    }
}
