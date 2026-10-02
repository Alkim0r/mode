package com.alkimor.regnum.client.model;

import com.alkimor.regnum.Regnum;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.ArrayList;
import java.util.List;

/** Сгенерировано tools/modelgen — не редактировать вручную. Модель: forgemaster. */
public final class ForgemasterModel {
    private ForgemasterModel() {}

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Regnum.id("forgemaster"), "main");

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(22, 34).addBox(-4.0F, -12.0F, -5.5F, 8F, 10F, 8F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, -7F, -2F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_crest = p_head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(54, 34).addBox(-1.0F, -14.0F, -6.0F, 2F, 3F, 10F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(40, 0).addBox(-9.0F, -2.0F, -6.0F, 18F, 12F, 11F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(76, 55).addBox(-4.5F, 10.0F, -3.0F, 9F, 5F, 6F, new CubeDeformation(0.5F, 0.5F, 0.5F)), PartPose.offsetAndRotation(0F, -7F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_chimney_l = p_body.addOrReplaceChild("chimney_l", CubeListBuilder.create().texOffs(64, 55).addBox(-7.0F, -11.0F, 2.0F, 3F, 9F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(18, 68).addBox(-7.5F, -12.0F, 1.5F, 4F, 1F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_chimney_r = p_body.addOrReplaceChild("chimney_r", CubeListBuilder.create().texOffs(106, 55).addBox(4.0F, -9.0F, 2.0F, 3F, 7F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(34, 68).addBox(3.5F, -10.0F, 1.5F, 4F, 1F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 0).addBox(-13.0F, -2.5F, -3.0F, 4F, 28F, 6F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, -7F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_pauldron_r = p_right_arm.addOrReplaceChild("pauldron_r", CubeListBuilder.create().texOffs(78, 34).addBox(-14.5F, -4.5F, -4.0F, 6F, 5F, 8F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_hammer = p_right_arm.addOrReplaceChild("hammer", CubeListBuilder.create().texOffs(50, 68).addBox(-12.0F, 24.0F, -1.0F, 2F, 3F, 2F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(0, 55).addBox(-16.0F, 26.5F, -4.0F, 10F, 5F, 8F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(20, 0).addBox(9.0F, -2.5F, -3.0F, 4F, 28F, 6F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, -7F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_pauldron_l = p_left_arm.addOrReplaceChild("pauldron_l", CubeListBuilder.create().texOffs(36, 55).addBox(8.5F, -4.5F, -4.0F, 6F, 5F, 8F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_ember_fist = p_left_arm.addOrReplaceChild("ember_fist", CubeListBuilder.create().texOffs(0, 68).addBox(9.0F, 25.0F, -2.5F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(98, 0).addBox(-3.5F, -3.0F, -3.0F, 6F, 16F, 5F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(-4F, 11F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_knee_r = p_right_leg.addOrReplaceChild("knee_r", CubeListBuilder.create().texOffs(58, 68).addBox(-4.0F, 3.0F, -4.0F, 7F, 4F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 34).addBox(-3.5F, -3.0F, -3.0F, 6F, 16F, 5F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(5F, 11F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_knee_l = p_left_leg.addOrReplaceChild("knee_l", CubeListBuilder.create().texOffs(74, 68).addBox(-4.0F, 3.0F, -4.0F, 7F, 4F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Декоративные детали (скрываются в облегчённом режиме). */
    public static List<ModelPart> decor(ModelPart root) {
        List<ModelPart> list = new ArrayList<>();
        list.add(root.getChild("head").getChild("crest"));
        list.add(root.getChild("body").getChild("chimney_l"));
        list.add(root.getChild("body").getChild("chimney_r"));
        list.add(root.getChild("right_arm").getChild("pauldron_r"));
        list.add(root.getChild("left_arm").getChild("pauldron_l"));
        list.add(root.getChild("right_leg").getChild("knee_r"));
        list.add(root.getChild("left_leg").getChild("knee_l"));
        return list;
    }
}
