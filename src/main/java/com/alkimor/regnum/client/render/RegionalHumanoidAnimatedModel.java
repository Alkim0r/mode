package com.alkimor.regnum.client.render;

import com.alkimor.regnum.animation.BossAnimationState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/** Separate shuffling witch and regal scarab poses, driven by server cast timing. */
public final class RegionalHumanoidAnimatedModel<T extends Mob> extends RegnumHumanoidModel<T> {
    private final boolean scarab;
    private final AdaptedModelAnimator clips;
    private final ModelPart leftWing, rightWing;

    public RegionalHumanoidAnimatedModel(ModelPart root, ModelLayerLocation layer, boolean scarab) {
        super(root, layer);
        this.scarab = scarab;
        clips = new AdaptedModelAnimator(poseParts);
        leftWing = scarab ? body.getChild("wing_l") : null;
        rightWing = scarab ? body.getChild("wing_r") : null;
    }

    @Override public void setupAnim(T entity, float swing, float amount, float age, float yaw, float pitch) {
        for (ModelPart part : poseParts) part.resetPose();
        float weight = Mth.clamp(amount * 1.4F, 0, 1);
        float step = swing * (scarab ? 0.48F : 0.58F);
        head.yRot = Mth.clamp(yaw, -45, 45) * Mth.DEG_TO_RAD;
        head.xRot += pitch * Mth.DEG_TO_RAD + (scarab ? -0.14F : 0.20F);
        body.xRot += scarab ? -0.06F : 0.25F;
        body.zRot += Mth.sin(step) * weight * (scarab ? 0.03F : 0.10F);
        rightLeg.xRot = Mth.cos(step) * weight * (scarab ? 0.28F : 0.42F);
        leftLeg.xRot = -Mth.cos(step) * weight * (scarab ? 0.28F : 0.22F);
        rightArm.xRot = scarab ? -0.55F : -0.75F;
        leftArm.xRot = scarab ? -0.55F : -0.35F;
        rightArm.zRot = scarab ? -0.45F : -0.15F;
        leftArm.zRot = scarab ? 0.45F : 0.30F;
        if (scarab) {
            float flutter = Mth.sin(age * 0.16F) * 0.12F;
            leftWing.yRot -= flutter;
            rightWing.yRot += flutter;
        }
        if (entity instanceof BossAnimationState state && state.getVisualAction() != 0) {
            int action = state.getVisualAction();
            int impact = Math.max(1, state.getVisualActionImpact());
            float partial = Mth.clamp(age - entity.tickCount, 0, 1);
            clips.update(state.getVisualActionTicks() + partial);
            clips.startKeyframe(impact);
            boolean overhead = action == 2 || action == 7;
            clips.rotate(rightArm, overhead ? -2.0F : -1.1F, -0.30F, scarab ? -0.55F : -0.45F);
            clips.rotate(leftArm, overhead ? -2.0F : -1.1F, 0.30F, scarab ? 0.55F : 0.45F);
            clips.rotate(head, -0.30F, 0, 0);
            clips.rotate(body, -0.20F, action == 1 ? -0.45F : 0, 0);
            if (scarab) {
                clips.rotate(leftWing, 0, -1.0F, 0.45F);
                clips.rotate(rightWing, 0, 1.0F, -0.45F);
            }
            clips.endKeyframe();
            clips.startKeyframe(2);
            clips.rotate(rightArm, action == 5 ? -0.75F : 0.30F, 0, -0.30F);
            clips.rotate(leftArm, action == 5 ? -0.75F : 0.30F, 0, 0.30F);
            clips.rotate(body, action == 2 ? 0.55F : 0.15F, action == 1 ? 0.55F : 0, 0);
            if (scarab) {
                clips.rotate(leftWing, 0, 0.35F, -0.15F);
                clips.rotate(rightWing, 0, -0.35F, 0.15F);
            }
            clips.endKeyframe();
            clips.resetKeyframe(Math.max(1, state.getVisualActionDuration() - impact - 2));
        }
        rightArm.x = -5 * Mth.cos(body.yRot);
        rightArm.z += 5 * Mth.sin(body.yRot);
        leftArm.x = 5 * Mth.cos(body.yRot);
        leftArm.z -= 5 * Mth.sin(body.yRot);
        rightArm.yRot += body.yRot;
        leftArm.yRot += body.yRot;
    }
}
