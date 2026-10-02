package com.alkimor.regnum.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import com.alkimor.regnum.mine.CrawlerEntity;

/** Лёгкая процедурная анимация шахтного ползуна: восемь ног, жвала и низкий бросок. */
public final class CrawlerModel extends EntityModel<CrawlerEntity> {
    public static final net.minecraft.client.model.geom.ModelLayerLocation LAYER =
            CrawlerGeometry.LAYER;

    private final ModelPart root;
    private final java.util.List<ModelPart> poseParts;
    private final ModelPart head;
    private final ModelPart mandibleLeft;
    private final ModelPart mandibleRight;
    private final ModelPart[] upperLegs = new ModelPart[8];
    private final ModelPart[] lowerLegs = new ModelPart[8];
    private final com.alkimor.regnum.client.render.ChitinGait gait;
    private final com.alkimor.regnum.client.render.AdaptedModelAnimator biteAnimator;
    private final ModelPart thorax, abdomen;

    public CrawlerModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        poseParts = root.getAllParts().toList();
        biteAnimator = new com.alkimor.regnum.client.render.AdaptedModelAnimator(poseParts);
        thorax = root.getChild("thorax");
        abdomen = root.getChild("abdomen");
        head = root.getChild("head");
        mandibleLeft = head.getChild("mandible_l");
        mandibleRight = head.getChild("mandible_r");
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
    public void setupAnim(CrawlerEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        for (ModelPart part : poseParts) part.resetPose();
        head.yRot += Mth.clamp(netHeadYaw, -45.0F, 45.0F) * Mth.DEG_TO_RAD;
        head.xRot += Mth.clamp(headPitch, -25.0F, 30.0F) * Mth.DEG_TO_RAD;

        float stride = Mth.clamp(limbSwingAmount * 1.8F, 0.0F, 1.0F);
        float idle = Mth.sin(ageInTicks * 0.11F);
        float partialTick = Mth.clamp(ageInTicks - entity.tickCount, 0.0F, 1.0F);
        float attack = Mth.sin(Mth.clamp(entity.getAttackAnim(partialTick), 0.0F, 1.0F) * Mth.PI);
        boolean lunging = entity.getDeltaMovement().y > 0.12;
        head.y += idle * 0.12F;
        head.xRot += -0.32F * attack + (lunging ? -0.3F : idle * 0.025F);
        float gape = idle * 0.025F + attack * 0.42F;
        mandibleLeft.yRot += gape;
        mandibleRight.yRot -= gape;
        mandibleLeft.xRot -= attack * 0.18F;
        mandibleRight.xRot -= attack * 0.18F;

        gait.apply(limbSwing, stride, 0.72F, 2.6F, 2.0F);
        thorax.zRot += Mth.sin(limbSwing * 0.72F) * stride * 0.055F;
        abdomen.yRot -= Mth.sin(limbSwing * 0.72F - 0.6F) * stride * 0.07F;
        float swing = entity.getAttackAnim(partialTick);
        if (swing > 0) {
            biteAnimator.update(swing * 6);
            biteAnimator.startKeyframe(2);
            biteAnimator.rotate(head, -0.35F, 0, 0);
            biteAnimator.move(head, 0, -0.5F, 1.1F);
            biteAnimator.rotate(mandibleLeft, 0, 0.45F, 0);
            biteAnimator.rotate(mandibleRight, 0, -0.45F, 0);
            biteAnimator.endKeyframe();
            biteAnimator.startKeyframe(1);
            biteAnimator.rotate(head, 0.28F, 0, 0);
            biteAnimator.move(head, 0, 0.35F, -1.8F);
            biteAnimator.rotate(thorax, 0.1F, 0, 0);
            biteAnimator.endKeyframe();
            biteAnimator.resetKeyframe(3);
        }
        if (lunging) {
            head.xRot -= 0.22F;
            for (int i = 0; i < upperLegs.length; i++) upperLegs[i].zRot -= (i < 4 ? -1.0F : 1.0F) * 0.18F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vertices, int light, int overlay,
                               int color) {
        root.render(pose, vertices, light, overlay, color);
    }
}
