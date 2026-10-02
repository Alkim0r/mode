package com.alkimor.regnum.kingdom.ai;

import com.alkimor.regnum.kingdom.BanditEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Налётчики идут к ратуше, если не заняты боем. */
public class BanditMarchGoal extends Goal {
    private final BanditEntity bandit;
    private int recalc;

    public BanditMarchGoal(BanditEntity bandit) {
        this.bandit = bandit;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        BlockPos hall = bandit.getRaidHall();
        return hall != null && bandit.getTarget() == null && !bandit.blockPosition().closerThan(hall, 3.0);
    }

    @Override
    public void tick() {
        if (--recalc > 0) return;
        recalc = 20;
        BlockPos hall = bandit.getRaidHall();
        if (hall != null) bandit.getNavigation().moveTo(hall.getX() + 0.5, hall.getY(), hall.getZ() + 0.5, 1.0);
    }
}
