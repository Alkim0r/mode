package com.alkimor.regnum.client.model;

import com.alkimor.regnum.Regnum;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.ArrayList;
import java.util.List;

/** Сгенерировано tools/modelgen — не редактировать вручную. Модель: unit_empire_knight. */
public final class UnitEmpireKnightModel {
    private UnitEmpireKnightModel() {}

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Regnum.id("unit_empire_knight"), "main");

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(22, 21).addBox(-4.5F, -0.25F, -2.5F, 9F, 7F, 5F, new CubeDeformation(0.0F, -0.25F, 0.0F)).texOffs(84, 35).addBox(-3.5F, 6.5F, -2.0F, 7F, 4F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(0, 53).addBox(-4.0F, 9.75F, -2.5F, 8F, 3F, 5F, new CubeDeformation(0.0F, -0.25F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_neck = p_body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 61).addBox(-1.5F, -1.0F, -1.5F, 3F, 1F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(94, 21).addBox(-3.0F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(106, 35).addBox(-2.25F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(86, 53).addBox(-2.75F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)), PartPose.offsetAndRotation(-5.5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(110, 21).mirror().addBox(-1.0F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(0, 45).mirror().addBox(-0.75F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(102, 53).mirror().addBox(-1.25F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false), PartPose.offsetAndRotation(5.5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 35).addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(26, 53).addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).texOffs(52, 45).addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)), PartPose.offsetAndRotation(-1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 35).mirror().addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(42, 53).mirror().addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false).texOffs(70, 45).mirror().addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)).mirror(false), PartPose.offsetAndRotation(1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(76, 0).addBox(-3.5F, -7.0F, -3.5F, 7F, 7F, 7F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_helm = p_head.addOrReplaceChild("helm", CubeListBuilder.create().texOffs(44, 0).addBox(-4.0F, -8.05F, -4.0F, 8F, 9F, 8F, new CubeDeformation(0.0F, -0.45F, 0.0F)).texOffs(88, 45).addBox(-3.0F, -9.5F, -3.0F, 6F, 2F, 6F, new CubeDeformation(-0.2F, -0.1F, -0.2F)).texOffs(26, 61).addBox(-0.5F, -11.0F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0.0F, -0.4F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_plume = p_helm.addOrReplaceChild("plume", CubeListBuilder.create().texOffs(32, 35).addBox(-1.0F, -7.0F, -1.0F, 2F, 7F, 2F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, -10.4F, 0F, -0.6109F, 0.0000F, 0.0000F));
        PartDefinition p_pauldron_r = p_right_arm.addOrReplaceChild("pauldron_r", CubeListBuilder.create().texOffs(50, 21).addBox(-3.5F, -3.25F, -3.0F, 5F, 5F, 6F, new CubeDeformation(0.0F, -0.25F, 0.0F)).texOffs(40, 35).addBox(-4.0F, 1.0F, -3.0F, 5F, 3F, 6F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_pauldron_l = p_left_arm.addOrReplaceChild("pauldron_l", CubeListBuilder.create().texOffs(72, 21).mirror().addBox(-1.5F, -3.25F, -3.0F, 5F, 5F, 6F, new CubeDeformation(0.0F, -0.25F, 0.0F)).mirror(false).texOffs(62, 35).mirror().addBox(-1.5F, 1.0F, -3.0F, 5F, 3F, 6F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).mirror(false), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_skirt = p_body.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(104, 0).addBox(-5.1F, 10.0F, -3.0F, 5F, 8F, 6F, new CubeDeformation(-0.1F, 0.0F, -0.4F)).texOffs(0, 21).mirror().addBox(0.1F, 10.0F, -3.0F, 5F, 8F, 6F, new CubeDeformation(-0.1F, 0.0F, -0.4F)).mirror(false), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_belt = p_body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(58, 53).addBox(-4.5F, 9.25F, -2.5F, 9F, 2F, 5F, new CubeDeformation(-0.25F, -0.25F, 0.0F)).texOffs(20, 61).addBox(-1.0F, 9.25F, -3.0F, 2F, 2F, 1F, new CubeDeformation(0.0F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_cape = p_body.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(0, 0).addBox(-5.5F, 0.0F, 0.0F, 11F, 20F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0.2F, 2.7F, 0.0873F, 0.0000F, 0.0000F));
        PartDefinition p_shield = p_left_arm.addOrReplaceChild("shield", CubeListBuilder.create().texOffs(24, 0).addBox(0.0F, -1.5F, -4.5F, 1F, 9F, 9F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(12, 45).addBox(0.0F, -0.5F, -5.5F, 1F, 7F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(16, 45).addBox(0.0F, -0.5F, 4.5F, 1F, 7F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(20, 45).addBox(0.0F, -2.5F, -3.5F, 1F, 1F, 7F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(36, 45).addBox(0.0F, 7.5F, -3.5F, 1F, 1F, 7F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(12, 61).addBox(0.6F, 2.0F, -1.0F, 2F, 2F, 2F, new CubeDeformation(-0.4F, 0.0F, 0.0F)), PartPose.offsetAndRotation(2.6F, 3F, 0F, 0.0000F, 0.0000F, 0.0000F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Декоративные детали (скрываются в облегчённом режиме). */
    public static List<ModelPart> decor(ModelPart root) {
        List<ModelPart> list = new ArrayList<>();
        list.add(root.getChild("head").getChild("helm").getChild("plume"));
        list.add(root.getChild("right_arm").getChild("pauldron_r"));
        list.add(root.getChild("left_arm").getChild("pauldron_l"));
        list.add(root.getChild("body").getChild("cape"));
        return list;
    }
}
