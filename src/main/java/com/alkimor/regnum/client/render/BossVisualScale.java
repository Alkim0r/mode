package com.alkimor.regnum.client.render;

/** Shared enlargement relative to the authored scale of every Regnum boss. */
public final class BossVisualScale {
    public static final float MULTIPLIER = 1.5F;

    private BossVisualScale() {}

    public static float enlarged(float original) {
        return original * MULTIPLIER;
    }
}
