package com.alkimor.regnum.client.model;

import com.alkimor.regnum.Regnum;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.ArrayList;
import java.util.List;

/** Сгенерировано tools/modelgen — не редактировать вручную. Модель: unit_steppe_spearman. */
public final class UnitSteppeSpearmanModel {
    private UnitSteppeSpearmanModel() {}

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Regnum.id("unit_steppe_spearman"), "main");

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(94, 0).addBox(-4.0F, -0.25F, -2.5F, 8F, 7F, 5F, new CubeDeformation(0.0F, -0.25F, -0.25F)).texOffs(32, 14).addBox(-3.5F, 6.5F, -2.0F, 7F, 4F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(144, 14).addBox(-4.0F, 9.75F, -2.5F, 8F, 3F, 5F, new CubeDeformation(0.0F, -0.25F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_neck = p_body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(60, 24).addBox(-1.5F, -1.0F, -1.5F, 3F, 1F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(220, 0).addBox(-2.75F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).texOffs(54, 14).addBox(-2.25F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(8, 24).addBox(-2.75F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)), PartPose.offsetAndRotation(-5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(236, 0).mirror().addBox(-1.25F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).mirror(false).texOffs(66, 14).mirror().addBox(-0.75F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(24, 24).mirror().addBox(-1.25F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false), PartPose.offsetAndRotation(5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(182, 0).addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(170, 14).addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).texOffs(78, 14).addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)), PartPose.offsetAndRotation(-1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(198, 0).mirror().addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(186, 14).mirror().addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false).texOffs(96, 14).mirror().addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)).mirror(false), PartPose.offsetAndRotation(1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -7.0F, -3.5F, 7F, 7F, 7F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_helm = p_head.addOrReplaceChild("helm", CubeListBuilder.create().texOffs(146, 0).addBox(-4.5F, -6.9F, -4.5F, 9F, 2F, 9F, new CubeDeformation(-0.2F, -0.1F, -0.2F)).texOffs(0, 14).addBox(-4.0F, -8.8F, -4.0F, 8F, 2F, 8F, new CubeDeformation(-0.4F, 0.0F, -0.4F)).texOffs(202, 14).addBox(-3.0F, -10.7F, -3.0F, 6F, 2F, 6F, new CubeDeformation(-0.4F, -0.1F, -0.4F)).texOffs(48, 24).addBox(-1.5F, -12.4F, -1.5F, 3F, 2F, 3F, new CubeDeformation(-0.1F, -0.2F, -0.1F)).texOffs(78, 24).addBox(-0.5F, -14.8F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.0F, -0.4F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_aventail = p_head.addOrReplaceChild("aventail", CubeListBuilder.create().texOffs(120, 0).addBox(-4.0F, -5.0F, -0.75F, 8F, 6F, 5F, new CubeDeformation(0.0F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_plume = p_helm.addOrReplaceChild("plume", CubeListBuilder.create().texOffs(0, 24).addBox(-1.0F, -5.0F, -1.0F, 2F, 5F, 2F, new CubeDeformation(-0.4F, 0.0F, -0.4F)), PartPose.offsetAndRotation(0F, -14.2F, 0.3F, -0.7854F, 0.0000F, 0.0000F));
        PartDefinition p_skirt = p_body.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(44, 0).addBox(-4.85F, 10.0F, -3.0F, 5F, 7F, 6F, new CubeDeformation(-0.35F, 0.0F, -0.4F)).texOffs(66, 0).mirror().addBox(-0.15F, 10.0F, -3.0F, 5F, 7F, 6F, new CubeDeformation(-0.35F, 0.0F, -0.4F)).mirror(false), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_belt = p_body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(226, 14).addBox(-4.5F, 9.25F, -2.5F, 9F, 2F, 5F, new CubeDeformation(-0.25F, -0.25F, 0.0F)).texOffs(82, 24).addBox(-1.0F, 9.25F, -3.0F, 2F, 2F, 1F, new CubeDeformation(0.0F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_shield = p_left_arm.addOrReplaceChild("shield", CubeListBuilder.create().texOffs(28, 0).addBox(0.0F, -0.5F, -3.5F, 1F, 7F, 7F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(40, 24).addBox(0.0F, 0.5F, -4.5F, 1F, 5F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(44, 24).addBox(0.0F, 0.5F, 3.5F, 1F, 5F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(72, 24).addBox(1.0F, 2.0F, -1.0F, 1F, 2F, 2F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(2.6F, 4F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_spear_mantle = p_body.addOrReplaceChild("spear_mantle", CubeListBuilder.create().texOffs(114, 14).addBox(-4.5F, 0.0F, -3.0F, 9F, 2F, 6F, new CubeDeformation(0.0F, 0.0F, -0.3F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_spear_baldric = p_body.addOrReplaceChild("spear_baldric", CubeListBuilder.create().texOffs(88, 0).addBox(-1.0F, -0.2F, -3.15F, 2F, 11F, 1F, new CubeDeformation(-0.3F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, -0.4363F));
        PartDefinition p_spear_pennon = p_body.addOrReplaceChild("spear_pennon", CubeListBuilder.create().texOffs(214, 0).addBox(0.0F, 0.0F, -0.2F, 2F, 9F, 1F, new CubeDeformation(0.0F, 0.0F, -0.2F)), PartPose.offsetAndRotation(3.8F, 4F, 2.6F, 0.0000F, 0.0000F, 0.0000F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
    }

    /** Декоративные детали (скрываются в облегчённом режиме). */
    public static List<ModelPart> decor(ModelPart root) {
        List<ModelPart> list = new ArrayList<>();
        list.add(root.getChild("head").getChild("helm").getChild("plume"));
        list.add(root.getChild("body").getChild("spear_pennon"));
        return list;
    }
}
