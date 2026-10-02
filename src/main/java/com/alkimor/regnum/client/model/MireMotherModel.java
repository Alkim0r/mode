package com.alkimor.regnum.client.model;

import com.alkimor.regnum.Regnum;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.ArrayList;
import java.util.List;

/** Сгенерировано tools/modelgen — не редактировать вручную. Модель: mire_mother. */
public final class MireMotherModel {
    private MireMotherModel() {}

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Regnum.id("mire_mother"), "main");

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 20).addBox(-4.0F, -9.0F, -4.0F, 8F, 9F, 8F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_nose = p_head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(4, 37).addBox(-1.0F, -1.0F, -3.0F, 2F, 3F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, -4F, -4F, -0.2618F, 0.0000F, 0.0000F));
        PartDefinition p_hair = p_head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(64, 0).addBox(-4.5F, -9.5F, 0.0F, 9F, 14F, 5F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_branch_l = p_head.addOrReplaceChild("branch_l", CubeListBuilder.create().texOffs(124, 20).addBox(-0.5F, -7.0F, -0.5F, 1F, 7F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(3F, -9F, 0F, 0.0000F, 0.0000F, -0.3491F));
        PartDefinition p_branch_l2 = p_branch_l.addOrReplaceChild("branch_l2", CubeListBuilder.create().texOffs(14, 37).addBox(-0.5F, -4.0F, -0.5F, 1F, 4F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, -6F, 0F, 0.0000F, 0.0000F, -0.7854F));
        PartDefinition p_branch_r = p_head.addOrReplaceChild("branch_r", CubeListBuilder.create().texOffs(0, 37).addBox(-0.5F, -7.0F, -0.5F, 1F, 7F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(-3F, -9F, 0F, 0.0000F, 0.0000F, 0.3491F));
        PartDefinition p_branch_r2 = p_branch_r.addOrReplaceChild("branch_r2", CubeListBuilder.create().texOffs(18, 37).addBox(-0.5F, -4.0F, -0.5F, 1F, 4F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, -5F, 0F, 0.0000F, 0.0000F, 0.7854F));
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_torso = p_body.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(92, 0).addBox(-5.0F, 0.0F, -3.0F, 10F, 12F, 6F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.2094F, 0.0000F, 0.0000F));
        PartDefinition p_shawl = p_torso.addOrReplaceChild("shawl", CubeListBuilder.create().texOffs(56, 20).addBox(-5.5F, -0.5F, -3.5F, 11F, 6F, 7F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_skirt = p_body.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, 9.0F, -4.0F, 12F, 12F, 8F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_lantern = p_torso.addOrReplaceChild("lantern", CubeListBuilder.create().texOffs(22, 37).addBox(-1.0F, 0.0F, -1.5F, 2F, 3F, 2F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(4F, 9F, -3F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 0).addBox(-2.0F, -2.0F, -1.5F, 3F, 17F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(92, 20).addBox(-2.5F, -2.5F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(-6F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_claws_r = p_right_arm.addOrReplaceChild("claws_r", CubeListBuilder.create().texOffs(38, 37).addBox(-2.0F, 15.0F, -1.5F, 1F, 3F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(30, 37).addBox(-0.5F, 15.0F, -1.5F, 1F, 4F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(42, 37).addBox(0.0F, 15.0F, 0.5F, 1F, 3F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(52, 0).addBox(-1.0F, -2.0F, -1.5F, 3F, 17F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(108, 20).addBox(-1.5F, -2.5F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(6F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_claws_l = p_left_arm.addOrReplaceChild("claws_l", CubeListBuilder.create().texOffs(46, 37).addBox(0.0F, 15.0F, -1.5F, 1F, 3F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(34, 37).addBox(1.5F, 15.0F, -1.5F, 1F, 4F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(50, 37).addBox(-1.0F, 15.0F, 0.5F, 1F, 3F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(32, 20).addBox(-1.5F, 0.0F, -1.5F, 3F, 12F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(-2F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(44, 20).addBox(-1.5F, 0.0F, -1.5F, 3F, 12F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(2F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Декоративные детали (скрываются в облегчённом режиме). */
    public static List<ModelPart> decor(ModelPart root) {
        List<ModelPart> list = new ArrayList<>();
        list.add(root.getChild("head").getChild("hair"));
        list.add(root.getChild("head").getChild("branch_l"));
        list.add(root.getChild("head").getChild("branch_l").getChild("branch_l2"));
        list.add(root.getChild("head").getChild("branch_r"));
        list.add(root.getChild("head").getChild("branch_r").getChild("branch_r2"));
        list.add(root.getChild("body").getChild("torso").getChild("shawl"));
        list.add(root.getChild("body").getChild("torso").getChild("lantern"));
        list.add(root.getChild("right_arm").getChild("claws_r"));
        list.add(root.getChild("left_arm").getChild("claws_l"));
        return list;
    }
}
