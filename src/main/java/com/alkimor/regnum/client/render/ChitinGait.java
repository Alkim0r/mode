package com.alkimor.regnum.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Eight alternating legs with stance and return strokes, solved against the model ground plane. */
public final class ChitinGait {
    private final ModelPart[] hips, knees, feet;
    private final float[] upper = new float[8], lower = new float[8];
    private final float[] restX = new float[8], restZ = new float[8];

    public ChitinGait(ModelPart[] hips, ModelPart[] knees) {
        this.hips = hips;
        this.knees = knees;
        feet = new ModelPart[8];
        for (int i = 0; i < 8; i++) {
            String side = i < 4 ? "l" : "r";
            String row = switch (i % 4) {
                case 0 -> "front";
                case 1 -> "mid_front";
                case 2 -> "mid_back";
                default -> "rear";
            };
            feet[i] = knees[i].getChild("leg_" + row + "_" + side + "_foot");
            upper[i] = Math.abs(knees[i].x);
            lower[i] = Math.abs(feet[i].x);
            float sx = Math.signum(knees[i].x);
            float a = hips[i].zRot, b = knees[i].zRot, yaw = hips[i].yRot;
            float along = sx * (upper[i] + lower[i] * Mth.cos(b));
            float down = sx * lower[i] * Mth.sin(b);
            restX[i] = Mth.cos(a) * Mth.cos(yaw) * along - Mth.sin(a) * down;
            restZ[i] = -Mth.sin(yaw) * along;
        }
    }

    /** Allocation-free; called after resetting the pose. Decorative bones are unaffected. */
    public void apply(float swing, float amount, float frequency, float reach, float lift) {
        float weight = Mth.clamp(amount, 0, 1);
        if (weight < 0.0001F) return;
        for (int i = 0; i < 8; i++) {
            float phase = swing * frequency / (2 * Mth.PI) + ((i + i / 4) % 2) * 0.5F;
            phase -= Mth.floor(phase);
            float travel, height;
            if (phase < 0.66F) {
                // The foot remains on the floor while the body moves past it.
                travel = 1 - 2 * phase / 0.66F;
                height = 0;
            } else {
                float t = (phase - 0.66F) / 0.34F;
                float smooth = t * t * (3 - 2 * t);
                travel = -1 + 2 * smooth;
                height = Mth.sin(t * Mth.PI) * lift * weight;
            }
            float x = restX[i];
            float y = 24 - hips[i].y - height;
            float z = restZ[i] + travel * reach * weight;
            float u = upper[i], l = lower[i], side = Math.signum(knees[i].x);
            float cosine = Mth.clamp((x * x + y * y + z * z - u * u - l * l) / (2 * u * l), -0.98F, 0.98F);
            float bend = side * (float) Math.acos(cosine);
            float along = side * (u + l * Mth.cos(bend));
            float down = side * l * Mth.sin(bend);
            float yaw = (float) Math.asin(Mth.clamp(-z / along, -0.98F, 0.98F));
            float roll = (float) (Math.atan2(y, x) - Math.atan2(down, along * Mth.cos(yaw)));
            hips[i].yRot = yaw;
            hips[i].zRot = roll;
            knees[i].zRot = bend;
            feet[i].zRot -= roll - hips[i].getInitialPose().zRot + bend - knees[i].getInitialPose().zRot;
        }
    }
}
