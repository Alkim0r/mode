/*
 * Adapted from iLexiconn's LLibrary ModelAnimator (LGPL-2.1).
 * Upstream: https://github.com/iLexiconn/LLibrary/blob/1.11.2/src/main/java/net/ilexiconn/llibrary/client/model/ModelAnimator.java
 * Original source and license: _refs/llibrary-source/. See THIRD_PARTY_NOTICES.md.
 * Regnum changes: ModelPart, server-synchronized floating tick, fixed reusable buffers
 * in place of allocating HashMaps/Transforms every frame. No LLibrary dependency.
 */
package com.alkimor.regnum.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** LLibrary keyframe transitions adapted to Minecraft 1.21 ModelPart. */
public final class AdaptedModelAnimator {
    private final ModelPart[] parts;
    private final java.util.IdentityHashMap<ModelPart, Integer> indices = new java.util.IdentityHashMap<>();
    private float[][] current, previous;
    private float animationTick;
    private int tempTick, prevTempTick;

    public AdaptedModelAnimator(java.util.List<ModelPart> parts) {
        this.parts = parts.toArray(ModelPart[]::new);
        current = new float[this.parts.length][6];
        previous = new float[this.parts.length][6];
        for (int i = 0; i < this.parts.length; i++) indices.put(this.parts[i], i);
    }

    public void update(float tick) {
        animationTick = Math.max(0, tick);
        tempTick = prevTempTick = 0;
        clear(current);
        clear(previous);
    }

    public void startKeyframe(int duration) {
        prevTempTick = tempTick;
        tempTick += Math.max(1, duration);
    }

    public void rotate(ModelPart part, float x, float y, float z) {
        float[] t = current[indices.get(part)];
        t[0] += x; t[1] += y; t[2] += z;
    }

    public void move(ModelPart part, float x, float y, float z) {
        float[] t = current[indices.get(part)];
        t[3] += x; t[4] += y; t[5] += z;
    }

    public void setStaticKeyframe(int duration) {
        startKeyframe(duration);
        endKeyframe(true);
    }

    public void resetKeyframe(int duration) {
        startKeyframe(duration);
        endKeyframe();
    }

    public void endKeyframe() { endKeyframe(false); }

    private void endKeyframe(boolean stationary) {
        if (animationTick >= prevTempTick && animationTick < tempTick) {
            float inc = stationary ? 0 : Mth.sin((animationTick - prevTempTick) / (tempTick - prevTempTick) * Mth.HALF_PI);
            float dec = 1 - inc;
            for (int i = 0; i < parts.length; i++) {
                ModelPart part = parts[i];
                float[] a = previous[i], b = current[i];
                part.xRot += dec * a[0] + inc * b[0];
                part.yRot += dec * a[1] + inc * b[1];
                part.zRot += dec * a[2] + inc * b[2];
                part.x += dec * a[3] + inc * b[3];
                part.y += dec * a[4] + inc * b[4];
                part.z += dec * a[5] + inc * b[5];
            }
        }
        if (!stationary) {
            float[][] swap = previous;
            previous = current;
            current = swap;
            clear(current);
        }
    }

    private static void clear(float[][] buffer) {
        for (float[] transform : buffer) java.util.Arrays.fill(transform, 0);
    }
}
