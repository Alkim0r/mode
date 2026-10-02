package com.alkimor.regnum.kingdom.ai;

import com.alkimor.regnum.kingdom.SoldierEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/** Защита короля: атакуем того, кто ударил короля, и того, кого бьёт король. */
public class SoldierAssistKingGoal extends TargetGoal {
    private final SoldierEntity soldier;
    private LivingEntity candidate;
    private int lastHurtBy, lastHurt;

    public SoldierAssistKingGoal(SoldierEntity soldier) {
        super(soldier, false);
        this.soldier = soldier;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        Player king = soldier.getOwnerPlayer();
        if (king == null || soldier.distanceToSqr(king) > 32 * 32) return false;
        LivingEntity a = king.getLastHurtByMob();
        int ta = king.getLastHurtByMobTimestamp();
        if (a != null && ta != lastHurtBy && valid(a)) {
            candidate = a;
            lastHurtBy = ta;
            return true;
        }
        LivingEntity b = king.getLastHurtMob();
        int tb = king.getLastHurtMobTimestamp();
        if (b != null && tb != lastHurt && valid(b)) {
            candidate = b;
            lastHurt = tb;
            return true;
        }
        return false;
    }

    private boolean valid(LivingEntity e) {
        return e.isAlive() && !soldier.isAlliedTo(e) && canAttack(e, TargetingConditions.DEFAULT);
    }

    @Override
    public void start() {
        mob.setTarget(candidate);
        super.start();
    }
}
