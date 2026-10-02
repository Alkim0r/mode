package com.alkimor.regnum.kingdom.ai;

import com.alkimor.regnum.dungeon.boss.Telegraph;
import com.alkimor.regnum.kingdom.SoldierEntity;
import com.alkimor.regnum.kingdom.SoldierType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Боец видит предупреждающую зону удара босса и уходит из неё, если успевает: ближайшая безопасная точка по проходимому
 * пути. Реакция не мгновенная (зависит от рода войск), тяжёлые бойцы медлительнее. Когда зон нет, цель стоит один вызов
 * {@link Telegraph#live()} на тик — то есть почти ничего.
 */
public class SoldierDodgeGoal extends Goal {
    /** Счётчики для логов проверки: попытки, успехи, невозможность успеть. */
    public static int attempts, escaped, noRoute;
    /** Бюджет построений пути: не больше 10 на тик на весь мир, чтобы вспышка зон не била по TPS. */
    private static long budgetTick = -1;
    private static int budgetUsed;

    private boolean pathBudget() {
        long t = s.level().getGameTime();
        if (t != budgetTick) { budgetTick = t; budgetUsed = 0; }
        return ++budgetUsed <= 10;
    }

    private final SoldierEntity s;
    private Telegraph.Strike threat;
    private Vec3 exit;
    private int giveUp;
    private int backoff;
    private int recheck;

    public SoldierDodgeGoal(SoldierEntity s) {
        this.s = s;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private int reaction() {
        return switch (s.getSoldierType().role) {
            case HEAVY, SHIELD -> 10 + s.getRandom().nextInt(5);
            case ARCHER, GUNNER, HORSE_ARCHER -> 4 + s.getRandom().nextInt(4);
            default -> 6 + s.getRandom().nextInt(4);
        };
    }

    private double blocksPerTick() {
        return s.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2.2;
    }

    @Override
    public boolean canUse() {
        if (s.isPassenger() || !s.onGround() || s.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)) return false;
        if (backoff > 0) { backoff--; return false; }
        List<Telegraph.Strike> live = Telegraph.live();
        if (live.isEmpty()) return false;
        Vec3 pos = s.position();
        List<Telegraph.Strike> near = new ArrayList<>(4);
        for (Telegraph.Strike t : live) {
            if (t.owner().level() != s.level()) continue;
            if (!Telegraph.isInside(t, pos)) continue;
            if (t.owner().distanceToSqr(s) > 48 * 48) continue; // предупреждение видно только вблизи
            if (t.elapsed() < reaction() + (s.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS) ? 6 : 0) || t.remaining() < 5) continue;
            near.add(t);
            if (near.size() >= 4) break;
        }
        if (near.isEmpty()) return false;
        threat = near.get(0);
        exit = findExit(live, near.get(0));
        if (exit == null) {
            noRoute++;
            backoff = 20 + s.getRandom().nextInt(10);
            return false;
        }
        attempts++;
        return true;
    }

    private boolean clear(List<Telegraph.Strike> live, Vec3 p) {
        for (Telegraph.Strike t : live) if (t.owner().level() == s.level() && Telegraph.isInside(t, p)) return false;
        return true;
    }

    /** Точка безопасна, если вне всех зон вместе с запасом ~1.3 блока по четырём сторонам (границы кругов и линий не «задевают»). */
    private boolean safeWithMargin(List<Telegraph.Strike> live, Vec3 p) {
        return clear(live, p) && clear(live, p.add(1.3, 0, 0)) && clear(live, p.add(-1.3, 0, 0)) && clear(live, p.add(0, 0, 1.3)) && clear(live, p.add(0, 0, -1.3));
    }

    /** Ближайшая точка вне всех активных зон, до которой есть путь и которую успеть достичь. */
    private Vec3 findExit(List<Telegraph.Strike> live, Telegraph.Strike main) {
        Vec3 pos = s.position();
        double reach = blocksPerTick() * 1.5 * (main.remaining() - 2);
        record Cand(Vec3 p, double d) {}
        List<Cand> cands = new ArrayList<>();
        for (int ring = 2; ring <= 12; ring += 2) {
            for (int a = 0; a < 16; a++) {
                double ang = a * Math.PI / 8 + (ring % 4) * 0.2;
                Vec3 c = new Vec3(pos.x + Math.cos(ang) * ring, pos.y, pos.z + Math.sin(ang) * ring);
                if (ring > reach) continue;
                boolean safe = safeWithMargin(live, c);
                if (safe) cands.add(new Cand(c, ring));
            }
            if (cands.size() >= 6) break;
        }
        cands.sort(java.util.Comparator.comparingDouble(Cand::d));
        int tried = 0;
        for (Cand c : cands) {
            if (tried++ >= 3) break;
            if (!pathBudget()) return null;
            BlockPos bp = BlockPos.containing(c.p.x, pos.y, c.p.z);
            Path path = s.getNavigation().createPath(bp, 0);
            if (path == null || !path.canReach() || path.getNodeCount() > reach + 3) continue;
            Vec3 end = Vec3.atBottomCenterOf(path.getEndNode() != null ? path.getEndNode().asBlockPos() : bp);
            if (safeWithMargin(live, end)) return end;
        }
        return null;
    }

    @Override
    public void start() {
        giveUp = 120;
        recheck = 0;
        s.getNavigation().moveTo(exit.x, exit.y, exit.z, 1.5);
        Vec3 d = exit.subtract(s.position());
        s.startDodge(d, (int) Math.ceil(Math.sqrt(d.x * d.x + d.z * d.z) / Math.max(0.05, blocksPerTick())));
    }

    /** Держимся, пока зона не сработала: вышли — стоим на месте, не даём бою вернуть нас под удар; не вышли — ищем новый выход. */
    @Override
    public boolean canContinueToUse() {
        return threat != null && threat.alive() && threat.remaining() > 0 && --giveUp > 0;
    }

    @Override
    public void tick() {
        if (threat == null) return;
        List<Telegraph.Strike> live = Telegraph.live();
        if (!clear(live, s.position())) {
            if (s.getNavigation().isDone() && --recheck <= 0) {
                recheck = 6;
                Telegraph.Strike worst = threat;
                for (Telegraph.Strike t : live) if (t.owner().level() == s.level() && Telegraph.isInside(t, s.position())) { worst = t; break; }
                Vec3 e = findExit(live, worst);
                if (e != null) {
                    exit = e;
                    s.getNavigation().moveTo(e.x, e.y, e.z, 1.5);
                    Vec3 d2 = e.subtract(s.position());
                    s.startDodge(d2, (int) Math.ceil(Math.sqrt(d2.x * d2.x + d2.z * d2.z) / Math.max(0.05, blocksPerTick())));
                }
            }
        } else {
            s.getNavigation().stop(); // в безопасности: ждём удара на месте
        }
    }

    @Override
    public void stop() {
        if (threat != null && clear(Telegraph.live(), s.position())) escaped++;
        threat = null;
        exit = null;
        s.endDodge();
        s.getNavigation().stop();
    }
}
