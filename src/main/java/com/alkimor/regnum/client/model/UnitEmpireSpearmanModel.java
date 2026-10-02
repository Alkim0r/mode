package com.alkimor.regnum.client.model;

import com.alkimor.regnum.Regnum;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.ArrayList;
import java.util.List;

/** Сгенерировано tools/modelgen — не редактировать вручную. Модель: unit_empire_spearman. */
public final class UnitEmpireSpearmanModel {
    private UnitEmpireSpearmanModel() {}

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Regnum.id("unit_empire_spearman"), "main");

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(56, 0).addBox(-4.0F, -0.25F, -2.5F, 8F, 7F, 5F, new CubeDeformation(0.0F, -0.25F, -0.25F)).texOffs(16, 24).addBox(-3.5F, 6.5F, -2.0F, 7F, 4F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(168, 24).addBox(-4.0F, 9.75F, -2.5F, 8F, 3F, 5F, new CubeDeformation(0.0F, -0.25F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_neck = p_body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(144, 34).addBox(-1.5F, -1.0F, -1.5F, 3F, 1F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(232, 0).addBox(-2.75F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).texOffs(38, 24).addBox(-2.25F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(88, 34).addBox(-2.75F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)), PartPose.offsetAndRotation(-5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 24).mirror().addBox(-1.25F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).mirror(false).texOffs(50, 24).mirror().addBox(-0.75F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(104, 34).mirror().addBox(-1.25F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false), PartPose.offsetAndRotation(5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(144, 0).addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(194, 24).addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).texOffs(102, 24).addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)), PartPose.offsetAndRotation(-1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(160, 0).mirror().addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(210, 24).mirror().addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false).texOffs(120, 24).mirror().addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)).mirror(false), PartPose.offsetAndRotation(1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(22, 0).addBox(-3.5F, -7.0F, -3.5F, 7F, 7F, 7F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_hair = p_head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(176, 0).addBox(-4.0F, -7.5F, -4.0F, 8F, 3F, 8F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).texOffs(64, 34).addBox(-4.0F, -4.75F, 0.125F, 8F, 3F, 4F, new CubeDeformation(-0.25F, 0.0F, -0.375F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_helm = p_head.addOrReplaceChild("helm", CubeListBuilder.create().texOffs(82, 0).addBox(-4.0F, -7.6F, -4.0F, 8F, 3F, 8F, new CubeDeformation(-0.1F, 0.0F, -0.1F)).texOffs(120, 34).addBox(-4.0F, -4.6F, 0.6F, 8F, 2F, 4F, new CubeDeformation(-0.1F, 0.0F, -0.4F)).texOffs(162, 34).addBox(-4.5F, -3.7F, 2.25F, 9F, 1F, 3F, new CubeDeformation(-0.2F, -0.1F, -0.25F)).texOffs(226, 24).addBox(-4.2F, -5.0F, -3.6F, 1F, 4F, 3F, new CubeDeformation(-0.2F, 0.0F, 0.0F)).texOffs(234, 24).addBox(3.2F, -5.0F, -3.6F, 1F, 4F, 3F, new CubeDeformation(-0.2F, 0.0F, 0.0F)).texOffs(196, 34).addBox(-4.0F, -6.4F, -4.3F, 8F, 1F, 1F, new CubeDeformation(-0.1F, -0.2F, -0.2F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_crest = p_helm.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(208, 0).addBox(-1.0F, -10.5F, -3.5F, 2F, 3F, 7F, new CubeDeformation(-0.4F, 0.0F, 0.0F)).texOffs(192, 34).addBox(-0.5F, -8.6F, -0.5F, 1F, 1F, 1F, new CubeDeformation(-0.1F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_pauldron_r = p_right_arm.addOrReplaceChild("pauldron_r", CubeListBuilder.create().texOffs(62, 24).addBox(-3.25F, -3.0F, -2.5F, 5F, 3F, 5F, new CubeDeformation(-0.25F, 0.0F, 0.0F)).texOffs(0, 34).addBox(-3.25F, -0.5F, -2.5F, 4F, 2F, 5F, new CubeDeformation(0.0F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_pauldron_l = p_left_arm.addOrReplaceChild("pauldron_l", CubeListBuilder.create().texOffs(82, 24).mirror().addBox(-1.75F, -3.0F, -2.5F, 5F, 3F, 5F, new CubeDeformation(-0.25F, 0.0F, 0.0F)).mirror(false).texOffs(18, 34).mirror().addBox(-1.25F, -0.5F, -2.5F, 4F, 2F, 5F, new CubeDeformation(0.0F, 0.0F, -0.25F)).mirror(false), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_pteruges = p_body.addOrReplaceChild("pteruges", CubeListBuilder.create().texOffs(114, 0).addBox(-4.5F, 10.0F, -3.0F, 9F, 5F, 6F, new CubeDeformation(0.0F, 0.0F, -0.4F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_belt = p_body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(36, 34).addBox(-4.5F, 9.25F, -2.5F, 9F, 2F, 5F, new CubeDeformation(-0.25F, -0.25F, 0.0F)).texOffs(186, 34).addBox(-1.0F, 9.25F, -3.0F, 2F, 2F, 1F, new CubeDeformation(0.0F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_shield = p_left_arm.addOrReplaceChild("shield", CubeListBuilder.create().texOffs(0, 0).addBox(-0.25F, -3.0F, -4.5F, 2F, 15F, 9F, new CubeDeformation(-0.25F, 0.0F, 0.0F)).texOffs(156, 34).addBox(1.2F, 3.0F, -1.0F, 1F, 2F, 2F, new CubeDeformation(-0.1F, 0.0F, 0.0F)), PartPose.offsetAndRotation(2.6F, 1F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_spear_mantle = p_body.addOrReplaceChild("spear_mantle", CubeListBuilder.create().texOffs(138, 24).addBox(-4.5F, 0.0F, -3.0F, 9F, 2F, 6F, new CubeDeformation(0.0F, 0.0F, -0.3F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_spear_baldric = p_body.addOrReplaceChild("spear_baldric", CubeListBuilder.create().texOffs(50, 0).addBox(-1.0F, -0.2F, -3.15F, 2F, 11F, 1F, new CubeDeformation(-0.3F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, -0.4363F));
        PartDefinition p_spear_pennon = p_body.addOrReplaceChild("spear_pennon", CubeListBuilder.create().texOffs(226, 0).addBox(0.0F, 0.0F, -0.2F, 2F, 9F, 1F, new CubeDeformation(0.0F, 0.0F, -0.2F)), PartPose.offsetAndRotation(3.8F, 4F, 2.6F, 0.0000F, 0.0000F, 0.0000F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
    }

    /** Декоративные детали (скрываются в облегчённом режиме). */
    public static List<ModelPart> decor(ModelPart root) {
        List<ModelPart> list = new ArrayList<>();
        list.add(root.getChild("head").getChild("helm").getChild("crest"));
        list.add(root.getChild("right_arm").getChild("pauldron_r"));
        list.add(root.getChild("left_arm").getChild("pauldron_l"));
        list.add(root.getChild("body").getChild("spear_pennon"));
        return list;
    }
}
