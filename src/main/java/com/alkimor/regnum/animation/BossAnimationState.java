package com.alkimor.regnum.animation;

/** Common-side state only; the server owns the action and actual impact timing. */
public interface BossAnimationState {
    // 0 idle, 1 sweep, 2 slam, 3 summon, 4 ring/curse, 5 projectile, 6 plates, 7 phase.
    int getVisualAction();
    int getVisualActionTicks();
    int getVisualActionImpact();
    int getVisualActionDuration();
    int getVisualPhase();
}
