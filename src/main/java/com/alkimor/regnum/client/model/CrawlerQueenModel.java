package com.alkimor.regnum.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import com.alkimor.regnum.mine.CrawlerQueenEntity;

/** Фазы и боевые позы привязаны к синхронизированным состояниям Королевы. */
public final class CrawlerQueenModel extends EntityModel<CrawlerQueenEntity> {
    public static final net.minecraft.client.model.geom.ModelLayerLocation LAYER =
            CrawlerQueenGeometry.LAYER;

    private final ModelPart root;
    private final java.util.List<ModelPart> poseParts;
    private final ModelPart head;
    private final ModelPart thorax;
    private final ModelPart abdomen;
    private final ModelPart fissures;
    private final ModelPart core;
    private final ModelPart flapLeft;
    private final ModelPart flapRight;
    private final ModelPart mandibleLeft;
    private final ModelPart mandibleRight;
    private final ModelPart sacLeft;
    private final ModelPart sacRight;
    private final java.util.List<ModelPart> decor;
    private final ModelPart[] upperLegs = new ModelPart[8];
    private final ModelPart[] lowerLegs = new ModelPart[8];
    private final com.alkimor.regnum.client.render.ChitinGait gait;
    private final com.alkimor.regnum.client.render.AdaptedModelAnimator keyframes;

    public CrawlerQueenModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        poseParts = root.getAllParts().toList();
        keyframes = new com.alkimor.regnum.client.render.AdaptedModelAnimator(poseParts);
        head = root.getChild("head");
        thorax = root.getChild("thorax");
        abdomen = root.getChild("abdomen");
        fissures = abdomen.getChild("phase_two_fissures");
        core = abdomen.getChild("phase_three_core");
        flapLeft = abdomen.getChild("shell_flap_l");
        flapRight = abdomen.getChild("shell_flap_r");
        mandibleLeft = head.getChild("mandible_l");
        mandibleRight = head.getChild("mandible_r");
        sacLeft = head.getChild("acid_sac_l");
        sacRight = head.getChild("acid_sac_r");
        decor = CrawlerQueenGeometry.decor(root);
        int i = 0;
        for (String side : new String[]{"l", "r"}) {
            for (String row : new String[]{"front", "mid_front", "mid_back", "rear"}) {
                String name = "leg_" + row + "_" + side;
                upperLegs[i] = root.getChild(name);
                lowerLegs[i] = upperLegs[i].getChild(name + "_shin");
                i++;
            }
        }
        gait = new com.alkimor.regnum.client.render.ChitinGait(upperLegs, lowerLegs);
    }

    @Override
    public void setupAnim(CrawlerQueenEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        for (ModelPart part : poseParts) part.resetPose();
        head.yRot += Mth.clamp(netHeadYaw, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
        head.xRot += Mth.clamp(headPitch, -25.0F, 30.0F) * Mth.DEG_TO_RAD;
        boolean detailed = !com.alkimor.regnum.core.RegnumClientConfig.decorHidden();
        for (ModelPart part : decor) part.visible = detailed;

        float ratio = entity.getHealth() / Math.max(1.0F, entity.getMaxHealth());
        int phaseId = Math.max(entity.getVisualPhase(), ratio <= 0.33F ? 3 : ratio <= 0.66F ? 2 : 1);
        boolean phaseTwo = phaseId >= 2;
        boolean phaseThree = phaseId >= 3;
        fissures.visible = phaseTwo;
        core.visible = phaseThree;

        float stride = Mth.clamp(limbSwingAmount * 1.8F, 0.0F, 1.0F);
        float idle = Mth.sin(ageInTicks * 0.075F);
        float partialTick = Mth.clamp(ageInTicks - entity.tickCount, 0.0F, 1.0F);
        float attack = Mth.sin(Mth.clamp(entity.getAttackAnim(partialTick), 0.0F, 1.0F) * Mth.PI);
        boolean lunging = entity.getDeltaMovement().y > 0.12;
        float spread = phaseThree ? 0.78F : phaseTwo ? 0.38F : 0.0F;
        flapLeft.zRot += spread + idle * 0.015F;
        flapRight.zRot -= spread + idle * 0.015F;
        abdomen.xRot += idle * 0.022F;
        head.y += idle * 0.1F;
        head.xRot += -0.24F * attack + (lunging ? -0.25F : idle * 0.018F);
        float gape = idle * 0.02F + attack * 0.42F;
        mandibleLeft.yRot += gape;
        mandibleRight.yRot -= gape;
        mandibleLeft.xRot -= attack * 0.18F;
        mandibleRight.xRot -= attack * 0.18F;
        if (entity.hurtTime > 0) head.zRot += Mth.sin(entity.hurtTime * 0.65F) * 0.06F;

        gait.apply(limbSwing, stride, 0.62F, 3.8F, 3.2F);
        thorax.zRot += Mth.sin(limbSwing * 0.62F) * stride * 0.018F;
        abdomen.yRot -= Mth.sin(limbSwing * 0.62F - 0.5F) * stride * 0.028F;
        for (int i = 0; i < 8; i++) if (i % 4 == 0)
            upperLegs[i].zRot -= (i < 4 ? -1.0F : 1.0F) * 0.18F * attack;
        if (lunging) {
            head.xRot -= 0.2F;
            for (int i = 0; i < upperLegs.length; i++) upperLegs[i].zRot -= (i < 4 ? -1.0F : 1.0F) * 0.12F;
        }
        animateAction(entity.getVisualAction(), entity.getVisualActionTicks() + partialTick,
                entity.getVisualActionDuration(), entity.getVisualActionImpact(), ageInTicks);
    }

    private void animateAction(int action, float elapsed, int duration, int impact, float age) {
        if (action == CrawlerQueenEntity.ACT_IDLE || duration <= 0) return;
        if (action == CrawlerQueenEntity.ACT_ACID) {
            int release = Math.min(2, Math.max(1, duration - impact));
            keyframes.update(elapsed);
            keyframes.startKeyframe(Math.max(1, impact));
            keyframes.rotate(head, -0.65F, 0, 0);
            keyframes.move(head, 0, -1.0F, 1.5F);
            keyframes.rotate(thorax, -0.12F, 0, 0);
            keyframes.rotate(abdomen, 0.12F, 0, 0);
            keyframes.rotate(mandibleLeft, -0.1F, 0.6F, 0);
            keyframes.rotate(mandibleRight, -0.1F, -0.6F, 0);
            keyframes.endKeyframe();
            keyframes.startKeyframe(release);
            keyframes.rotate(head, 0.4F, 0, 0);
            keyframes.move(head, 0, 0.6F, -2.5F);
            keyframes.rotate(thorax, 0.14F, 0, 0);
            keyframes.rotate(mandibleLeft, 0, 0.8F, 0);
            keyframes.rotate(mandibleRight, 0, -0.8F, 0);
            keyframes.endKeyframe();
            keyframes.resetKeyframe(Math.max(1, duration - impact - release));
            float swell = 1 + pulse(elapsed, Math.max(1, impact - 2), impact, duration) * 0.45F;
            sacLeft.xScale = sacLeft.yScale = sacRight.xScale = sacRight.yScale = swell;
            return;
        }
        if (action == CrawlerQueenEntity.ACT_BROOD) {
            keyframes.update(elapsed);
            keyframes.startKeyframe(4);
            keyframes.rotate(abdomen, -0.4F, 0, 0);
            keyframes.move(abdomen, 0, -1.5F, 0.6F);
            keyframes.rotate(head, -0.3F, 0, 0);
            keyframes.rotate(flapLeft, 0, 0, 0.5F);
            keyframes.rotate(flapRight, 0, 0, -0.5F);
            for (int i = 0; i < 8; i++) if (i % 4 >= 2)
                keyframes.rotate(upperLegs[i], 0, 0, (i < 4 ? 1 : -1) * 0.55F);
            keyframes.endKeyframe();
            keyframes.setStaticKeyframe(9);
            keyframes.resetKeyframe(Math.max(1, duration - 13));
            return;
        }
        if (action == CrawlerQueenEntity.ACT_CHARGE) {
            keyframes.update(elapsed);
            keyframes.startKeyframe(Math.max(1, impact));
            keyframes.rotate(head, 0.42F, 0, 0);
            keyframes.move(head, 0, 0.8F, 0.8F);
            keyframes.rotate(thorax, 0.1F, 0, 0);
            keyframes.rotate(abdomen, -0.22F, 0, 0);
            for (int i = 0; i < 8; i++) {
                float side = i < 4 ? -1 : 1;
                keyframes.rotate(upperLegs[i], 0, side * (i % 4 < 2 ? -0.2F : 0.18F),
                        -side * (i % 4 < 2 ? 0.28F : 0.14F));
            }
            keyframes.endKeyframe();
            keyframes.startKeyframe(2);
            keyframes.rotate(head, -0.4F, 0, 0);
            keyframes.move(head, 0, -0.3F, -2.0F);
            keyframes.rotate(thorax, -0.12F, 0, 0);
            keyframes.rotate(abdomen, 0.12F, 0, 0);
            keyframes.rotate(mandibleLeft, 0, 0.5F, 0);
            keyframes.rotate(mandibleRight, 0, -0.5F, 0);
            keyframes.endKeyframe();
            keyframes.setStaticKeyframe(Math.max(1, duration - impact - 2));
            return;
        }
        float progress = Mth.clamp(elapsed / duration, 0.0F, 1.0F);
        switch (action) {
            case CrawlerQueenEntity.ACT_STUNNED -> {
                float hold = smooth((duration - elapsed) / 8.0F);
                head.xRot += hold * 0.55F;
                head.y += hold * 1.0F;
                head.zRot += hold * Mth.sin(age * 0.55F) * 0.07F;
                mandibleLeft.yRot += hold * 0.12F;
                mandibleRight.yRot -= hold * 0.12F;
                abdomen.xRot += hold * 0.08F;
            }
            case CrawlerQueenEntity.ACT_CAVEIN -> {
                float pulse = pulse(elapsed, Math.max(1, impact) * 0.45F, impact, duration);
                head.xRot -= pulse * 0.48F;
                thorax.xRot -= pulse * 0.09F;
                for (int i = 0; i < 8; i++) if (i % 4 < 2) {
                    float side = i < 4 ? -1.0F : 1.0F;
                    upperLegs[i].zRot -= side * pulse * 0.35F;
                    lowerLegs[i].zRot += side * pulse * 0.12F;
                }
            }
            case CrawlerQueenEntity.ACT_DARKNESS -> {
                float pulse = pulse(elapsed, 6, duration * 0.65F, duration);
                flapLeft.zRot += pulse * 0.32F;
                flapRight.zRot -= pulse * 0.32F;
                abdomen.xRot -= pulse * 0.08F;
                head.zRot += pulse * Mth.sin(age * 0.4F) * 0.08F;
                mandibleLeft.yRot += pulse * 0.55F;
                mandibleRight.yRot -= pulse * 0.55F;
            }
            default -> { }
        }
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static float pulse(float elapsed, float peak, float hold, float end) {
        if (elapsed < peak) return smooth(elapsed / peak);
        if (elapsed <= hold) return 1;
        return 1 - smooth((elapsed - hold) / Math.max(1, end - hold));
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vertices, int light, int overlay,
                               int color) {
        root.render(pose, vertices, light, overlay, color);
    }
}
