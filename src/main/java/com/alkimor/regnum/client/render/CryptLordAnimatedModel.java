package com.alkimor.regnum.client.render;

import com.alkimor.regnum.animation.BossAnimationState;
import com.alkimor.regnum.client.model.CryptLordModel;
import com.alkimor.regnum.dungeon.CryptLordEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Morrgrath has a heavy stalking gait and full-body casts, not vanilla player animation. */
public final class CryptLordAnimatedModel extends RegnumHumanoidModel<CryptLordEntity> {
    private final AdaptedModelAnimator clips;
    private final ModelPart mantle;

    public CryptLordAnimatedModel(ModelPart root) {
        super(root, CryptLordModel.LAYER);
        clips = new AdaptedModelAnimator(poseParts);
        mantle = body.getChild("cape");
    }

    @Override
    public void setupAnim(CryptLordEntity entity, float limbSwing, float amount, float age,
                          float yaw, float pitch) {
        for (ModelPart part : poseParts) part.resetPose();
        float partial = Mth.clamp(age - entity.tickCount, 0, 1);
        float weight = Mth.clamp(amount * 1.5F, 0, 1);
        float step = limbSwing * 0.52F;
        head.yRot = Mth.clamp(yaw, -45, 45) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(pitch, -25, 25) * Mth.DEG_TO_RAD - 0.10F;
        body.xRot = 0.10F;
        body.zRot = Mth.sin(step) * 0.07F * weight;
        rightLeg.xRot = Mth.cos(step) * 0.65F * weight;
        leftLeg.xRot = Mth.cos(step + Mth.PI) * 0.65F * weight;
        rightLeg.zRot = 0.06F;
        leftLeg.zRot = -0.06F;
        rightArm.xRot = -0.38F - Mth.cos(step) * 0.12F * weight;
        rightArm.zRot = -0.20F;
        leftArm.xRot = -0.18F + Mth.cos(step) * 0.15F * weight;
        leftArm.zRot = 0.23F;
        mantle.xRot = 0.20F + weight * 0.24F + Mth.sin(age * 0.07F - 0.8F) * 0.07F;
        mantle.yRot = -body.zRot * 0.8F;

        if ((Object) entity instanceof BossAnimationState state && state.getVisualAction() != 0) {
            animateCast(state.getVisualAction(), state.getVisualActionTicks() + partial,
                    state.getVisualActionImpact(), state.getVisualActionDuration());
        } else {
            // Only the real server melee swing; no fabricated cast or windup.
            float swing = entity.getAttackAnim(partial);
            if (swing > 0) {
                clips.update(swing * 6);
                clips.startKeyframe(1);
                clips.rotate(rightArm, -1.9F, -0.6F, -0.35F);
                clips.rotate(body, -0.18F, -0.45F, 0);
                clips.endKeyframe();
                clips.startKeyframe(2);
                clips.rotate(rightArm, 0.55F, 0.8F, 0.3F);
                clips.rotate(body, 0.42F, 0.55F, 0);
                clips.rotate(leftArm, -0.45F, 0, 0.4F);
                clips.endKeyframe();
                clips.resetKeyframe(3);
            }
        }
        // Arms are root siblings in the generated skeleton: rotate their pivots
        // with the torso so the weapon does not detach during a full-body sweep.
        rightArm.x = -5 * Mth.cos(body.yRot);
        rightArm.z = 5 * Mth.sin(body.yRot) + rightArm.z;
        leftArm.x = 5 * Mth.cos(body.yRot);
        leftArm.z = -5 * Mth.sin(body.yRot) + leftArm.z;
        rightArm.yRot += body.yRot;
        leftArm.yRot += body.yRot;
        if (entity.deathTime > 0) {
            float collapse = Mth.clamp(entity.deathTime / 20F, 0, 1);
            head.xRot += 0.7F * collapse;
            body.xRot += 0.45F * collapse;
            rightArm.xRot = -0.1F;
            leftArm.zRot += collapse * 0.65F;
        }
    }

    private void animateCast(int action, float tick, int impact, int duration) {
        int windup = Math.max(1, impact);
        clips.update(tick);
        clips.startKeyframe(windup);
        if (action == 1) {
            clips.rotate(rightArm, -1.5F, -1.1F, -0.75F);
            clips.rotate(body, -0.12F, -0.65F, -0.10F);
            clips.rotate(leftArm, -0.6F, 0, 0.55F);
        } else if (action == 2) {
            clips.rotate(rightArm, -2.5F, -0.2F, -0.1F);
            clips.rotate(leftArm, -1.95F, 0.2F, 0.12F);
            clips.rotate(body, -0.28F, 0, 0);
            clips.rotate(head, -0.28F, 0, 0);
        } else {
            clips.rotate(rightArm, -1.65F, -0.25F, -0.9F);
            clips.rotate(leftArm, -1.65F, 0.25F, 0.9F);
            clips.rotate(head, -0.35F, 0, 0);
            clips.rotate(body, -0.15F, 0, 0);
        }
        clips.endKeyframe();
        clips.startKeyframe(2);
        if (action == 1) {
            clips.rotate(rightArm, 0.6F, 0.8F, 0.45F);
            clips.rotate(body, 0.20F, 0.7F, 0.12F);
        } else if (action == 2) {
            clips.rotate(rightArm, -0.25F, 0, 0);
            clips.rotate(leftArm, -0.3F, 0, 0);
            clips.rotate(body, 0.75F, 0, 0);
            clips.rotate(head, 0.35F, 0, 0);
        } else {
            clips.rotate(rightArm, -0.8F, 0, -1.1F);
            clips.rotate(leftArm, -0.8F, 0, 1.1F);
            clips.rotate(body, 0.15F, 0, 0);
        }
        clips.endKeyframe();
        clips.resetKeyframe(Math.max(1, duration - windup - 2));
    }
}
