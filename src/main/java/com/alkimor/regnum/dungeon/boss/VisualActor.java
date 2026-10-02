package com.alkimor.regnum.dungeon.boss;

/** Босс с синхронизированными визуальными действиями; Telegraph сам запускает действие при касте. */
public interface VisualActor extends com.alkimor.regnum.animation.BossAnimationState {
    void visualStart(int action, int impactTick, int totalTicks);

    int getVisualAction();
    int getVisualActionElapsed();
    int getVisualActionDuration();
    int getVisualActionImpact();
    int getVisualPhase();
}
