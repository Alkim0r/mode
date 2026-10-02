package com.alkimor.regnum.client;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.dungeon.boss.Telegraph;
import com.alkimor.regnum.kingdom.SoldierEntity;
import com.alkimor.regnum.kingdom.ai.SoldierDodgeGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Only called by the opt-in battle demo: records movement rather than treating moveTo as success. */
@net.neoforged.fml.common.EventBusSubscriber(modid = Regnum.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class BattleDodgeTelemetry {
    private record Trace(SoldierEntity soldier, Vec3 start, Telegraph.Strike strike, UUID target, Object order) {}
    private static final Map<UUID, Trace> active = new HashMap<>();
    private static Field goalsField, threatField;
    private static boolean failed;
    private static final StackWalker damageStack = StackWalker.getInstance();
    private static int battleAge;

    private BattleDodgeTelemetry() {}

    static void reset() { active.clear(); battleAge = 0; }

    /** Both melee and telegraphed strikes use mobAttack; the firing stack distinguishes them. */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (!Boolean.getBoolean("regnum.battleShowcase") || event.getEntity().level().isClientSide()) return;
        Trace trace = active.get(event.getEntity().getUUID());
        if (trace == null) return;
        boolean telegraph = damageStack.walk(frames -> frames.anyMatch(frame ->
                frame.getClassName().equals(Telegraph.class.getName()) && frame.getMethodName().equals("fire")));
        int overlapping = 0;
        for (Telegraph.Strike strike : Telegraph.live())
            if (strike.owner().level() == trace.soldier().level()
                    && Telegraph.isInside(strike, trace.soldier().position())) overlapping++;
        var source = event.getSource();
        Regnum.LOGGER.info("[REGNUM-DODGE] DAMAGE t={} id={} origin={} source={} loss={} hp={} mainRemaining={} mainInside={} overlapping={}",
                battleAge, trace.soldier().getUUID(), telegraph ? "telegraph" : "other",
                source.getMsgId(), event.getNewDamage(), trace.soldier().getHealth(),
                trace.strike() == null ? -1 : trace.strike().remaining(),
                trace.strike() != null && Telegraph.isInside(trace.strike(), trace.soldier().position()), overlapping);
    }

    static void tick(List<SoldierEntity> army, int age) {
        battleAge = age;
        if (failed) return;
        try {
            if (goalsField == null) {
                goalsField = Mob.class.getDeclaredField("goalSelector");
                goalsField.setAccessible(true);
                threatField = SoldierDodgeGoal.class.getDeclaredField("threat");
                threatField.setAccessible(true);
            }
            for (SoldierEntity soldier : army) {
                SoldierDodgeGoal running = null;
                if (soldier.isAlive()) {
                    var selector = (GoalSelector) goalsField.get(soldier);
                    for (var goal : selector.getAvailableGoals()) {
                        if (goal.isRunning() && goal.getGoal() instanceof SoldierDodgeGoal dodge) {
                            running = dodge; break;
                        }
                    }
                }
                UUID id = soldier.getUUID();
                if (running != null && !active.containsKey(id)) {
                    var strike = (Telegraph.Strike) threatField.get(running);
                    UUID target = soldier.getTarget() == null ? null : soldier.getTarget().getUUID();
                    active.put(id, new Trace(soldier, soldier.position(), strike, target, soldier.getOrder()));
                    Regnum.LOGGER.info("[REGNUM-DODGE] START t={} id={} type={} pos={} remaining={} order={}",
                            age, id, soldier.getSoldierType(), soldier.position(), strike == null ? -1 : strike.remaining(), soldier.getOrder());
                } else if (running == null && active.containsKey(id)) {
                    Trace trace = active.remove(id);
                    UUID target = soldier.getTarget() == null ? null : soldier.getTarget().getUUID();
                    boolean safe = trace.strike() != null && !Telegraph.isInside(trace.strike(), soldier.position());
                    Regnum.LOGGER.info("[REGNUM-DODGE] STOP t={} id={} moved={} safe={} remaining={} alive={} orderKept={} targetKept={} pos={}",
                            age, id, String.format(java.util.Locale.ROOT, "%.2f", soldier.position().distanceTo(trace.start())), safe,
                            trace.strike() == null ? -1 : trace.strike().remaining(), soldier.isAlive(),
                            java.util.Objects.equals(trace.order(), soldier.getOrder()), java.util.Objects.equals(trace.target(), target), soldier.position());
                }
            }
            if (age % 100 == 0) Regnum.LOGGER.info("[REGNUM-DODGE] COUNTERS t={} attempts={} escaped={} noRoute={} running={}",
                    age, SoldierDodgeGoal.attempts, SoldierDodgeGoal.escaped, SoldierDodgeGoal.noRoute, active.size());
        } catch (ReflectiveOperationException e) {
            failed = true;
            Regnum.LOGGER.error("[REGNUM-DODGE] Telemetry unavailable; battle continues", e);
        }
    }
}
