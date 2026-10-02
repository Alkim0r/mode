package com.alkimor.regnum.client.render;

import com.alkimor.regnum.dungeon.boss.ForgemasterEntity;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Heavy hammer preparation, downward impact and recovery from server cast state. */
public final class ForgemasterAnimatedModel extends IronGolemModel<ForgemasterEntity> {
    private final java.util.List<ModelPart> parts;
    private final ModelPart head, torso, rightArm, leftArm, rightLeg, leftLeg;
    private final AdaptedModelAnimator clips;

    public ForgemasterAnimatedModel(ModelPart root) {
        super(root);
        parts = root.getAllParts().toList();
        clips = new AdaptedModelAnimator(parts);
        head = root.getChild("head"); torso = root.getChild("body");
        rightArm = root.getChild("right_arm"); leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg"); leftLeg = root.getChild("left_leg");
    }

    @Override public void setupAnim(ForgemasterEntity entity, float swing, float amount, float age, float yaw, float pitch) {
        for (ModelPart part : parts) part.resetPose();
        float weight = Mth.clamp(amount * 1.3F, 0, 1);
        float step = Mth.triangleWave(swing, 16);
        head.yRot = Mth.clamp(yaw, -35, 35) * Mth.DEG_TO_RAD;
        head.xRot += pitch * Mth.DEG_TO_RAD;
        torso.zRot += step * 0.09F * weight;
        rightLeg.xRot += step * 0.48F * weight;
        leftLeg.xRot -= step * 0.48F * weight;
        rightArm.xRot = -0.28F;
        leftArm.xRot = -0.18F;
        if (entity.getVisualAction() == 0) return;
        int impact = Math.max(1, entity.getVisualActionImpact());
        clips.update(entity.getVisualActionTicks() + Mth.clamp(age - entity.tickCount, 0, 1));
        clips.startKeyframe(impact);
        clips.rotate(rightArm, -2.5F, -0.20F, -0.25F);
        clips.rotate(leftArm, -1.75F, 0.20F, 0.25F);
        clips.rotate(torso, -0.25F, 0, 0);
        clips.rotate(head, -0.25F, 0, 0);
        clips.endKeyframe();
        clips.startKeyframe(2);
        clips.rotate(rightArm, -0.10F, 0, 0);
        clips.rotate(leftArm, -0.25F, 0, 0);
        clips.rotate(torso, 0.60F, 0, 0);
        clips.rotate(head, 0.30F, 0, 0);
        clips.endKeyframe();
        clips.resetKeyframe(Math.max(1, entity.getVisualActionDuration() - impact - 2));
    }
}
