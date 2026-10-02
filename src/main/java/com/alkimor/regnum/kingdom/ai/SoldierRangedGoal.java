package com.alkimor.regnum.kingdom.ai;

import com.alkimor.regnum.kingdom.SoldierEntity;
import com.alkimor.regnum.kingdom.SoldierType;
import com.alkimor.regnum.kingdom.Technique;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Стрельба. Лучник: дистанция 8–16, залпы. Арбалетчик: стоит на месте и бьёт до 28 блоков, но заряжает 3,5 с.
 * Конный лучник: на скаку держит 10–15, при сближении врага уходит и стреляет с разворота.
 */
public class SoldierRangedGoal extends Goal {
    private final SoldierEntity soldier;
    private int cooldown;
    private int seeTime;

    public SoldierRangedGoal(SoldierEntity soldier) {
        this.soldier = soldier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = soldier.getTarget();
        return soldier.isArcher() && t != null && t.isAlive();
    }

    @Override
    public void stop() {
        seeTime = 0;
        soldier.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity t = soldier.getTarget();
        if (t == null) return;
        SoldierType type = soldier.getSoldierType();
        int range = type.range();
        double d2 = soldier.distanceToSqr(t);
        boolean see = soldier.getSensing().hasLineOfSight(t);
        seeTime = see ? seeTime + 1 : 0;
        int kite = soldier.tech(Technique.KITING);
        boolean horse = type == SoldierType.HORSE_ARCHER || type == SoldierType.SCOUT;
        boolean stand = type == SoldierType.CROSSBOW || type.role == SoldierType.Role.GUNNER;
        double minDist = horse ? 10 + kite : stand ? 5 : 6 + kite;
        double speed = horse ? 1.25 : 1.1;

        if (d2 > (double) (range - 2) * (range - 2) || !see) {
            soldier.getNavigation().moveTo(t, speed);
        } else if (d2 < minDist * minDist && !stand) {
            // отступить от врага (всадник уходит дальше и быстрее)
            double dx = soldier.getX() - t.getX(), dz = soldier.getZ() - t.getZ();
            double len = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
            double step = horse ? 9 : 4;
            soldier.getNavigation().moveTo(soldier.getX() + dx / len * step, soldier.getY(), soldier.getZ() + dz / len * step, horse ? 1.5 : 1.2);
        } else if (horse && soldier.tickCount % 40 < 15) {
            // конник не стоит: описывает дугу вокруг цели
            double ang = Math.atan2(soldier.getZ() - t.getZ(), soldier.getX() - t.getX()) + 0.6;
            double r = Math.max(minDist + 1, Math.sqrt(d2));
            soldier.getNavigation().moveTo(t.getX() + Math.cos(ang) * r, soldier.getY(), t.getZ() + Math.sin(ang) * r, 1.3);
        } else {
            soldier.getNavigation().stop();
        }
        soldier.getLookControl().setLookAt(t, 30f, 30f);

        if (--cooldown <= 0 && see && seeTime > 5 && d2 <= (double) (range + 2) * (range + 2) && com.alkimor.regnum.kingdom.FireControl.canFire(soldier, t)) {
            soldier.performRangedAttack(t, 1.0f);
            soldier.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            cooldown = type.reload() - 3 * kite + soldier.getRandom().nextInt(10);
        }
    }
}
