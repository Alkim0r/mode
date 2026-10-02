package com.alkimor.regnum.client.model;

import com.alkimor.regnum.Regnum;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.ArrayList;
import java.util.List;

/** Сгенерировано tools/modelgen — не редактировать вручную. Модель: unit_sultanate_musketeer. */
public final class UnitSultanateMusketeerModel {
    private UnitSultanateMusketeerModel() {}

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Regnum.id("unit_sultanate_musketeer"), "main");

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(112, 0).addBox(-4.0F, -0.25F, -2.5F, 8F, 7F, 5F, new CubeDeformation(0.0F, -0.25F, -0.25F)).texOffs(36, 27).addBox(-3.5F, 6.5F, -2.0F, 7F, 4F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(118, 27).addBox(-4.0F, 9.75F, -2.5F, 8F, 3F, 5F, new CubeDeformation(0.0F, -0.25F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_neck = p_body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(16, 37).addBox(-1.5F, -1.0F, -1.5F, 3F, 1F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(202, 0).addBox(-2.75F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).texOffs(58, 27).addBox(-2.25F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(204, 27).addBox(-2.75F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)), PartPose.offsetAndRotation(-5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(218, 0).mirror().addBox(-1.25F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).mirror(false).texOffs(70, 27).mirror().addBox(-0.75F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(220, 27).mirror().addBox(-1.25F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false), PartPose.offsetAndRotation(5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(170, 0).addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(144, 27).addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).texOffs(82, 27).addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)), PartPose.offsetAndRotation(-1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(186, 0).mirror().addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(160, 27).mirror().addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false).texOffs(100, 27).mirror().addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)).mirror(false), PartPose.offsetAndRotation(1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(52, 0).addBox(-3.5F, -7.0F, -3.5F, 7F, 7F, 7F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_keffiyeh = p_head.addOrReplaceChild("keffiyeh", CubeListBuilder.create().texOffs(138, 0).addBox(-4.0F, -7.6F, -4.0F, 8F, 3F, 8F, new CubeDeformation(-0.1F, 0.0F, -0.1F)).texOffs(86, 0).addBox(-4.0F, -4.85F, -1.05F, 8F, 7F, 5F, new CubeDeformation(-0.1F, -0.25F, -0.05F)).texOffs(0, 27).addBox(-4.5F, -6.9F, -4.5F, 9F, 1F, 9F, new CubeDeformation(-0.4F, -0.1F, -0.4F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_skirt = p_body.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(8, 0).addBox(-5.0F, 10.0F, -3.0F, 10F, 11F, 6F, new CubeDeformation(-0.4F, 0.0F, -0.4F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_sash = p_body.addOrReplaceChild("sash", CubeListBuilder.create().texOffs(176, 27).addBox(-4.5F, 9.0F, -2.5F, 9F, 2F, 5F, new CubeDeformation(-0.2F, 0.0F, 0.0F)).texOffs(244, 27).addBox(1.75F, 10.5F, -2.85F, 2F, 5F, 1F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_powder_baldric = p_body.addOrReplaceChild("powder_baldric", CubeListBuilder.create().texOffs(80, 0).addBox(-1.0F, -0.3F, -3.15F, 2F, 12F, 1F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, -0.4363F));
        PartDefinition p_powder_charges = p_body.addOrReplaceChild("powder_charges", CubeListBuilder.create().texOffs(52, 37).addBox(-3.0F, 1.65F, -3.85F, 1F, 3F, 2F, new CubeDeformation(0.0F, -0.35F, -0.45F)).texOffs(58, 37).addBox(-1.6F, 3.15F, -3.85F, 1F, 3F, 2F, new CubeDeformation(0.0F, -0.35F, -0.45F)).texOffs(64, 37).addBox(-0.2F, 4.65F, -3.85F, 1F, 3F, 2F, new CubeDeformation(0.0F, -0.35F, -0.45F)).texOffs(70, 37).addBox(1.2F, 6.15F, -3.85F, 1F, 3F, 2F, new CubeDeformation(0.0F, -0.35F, -0.45F)).texOffs(76, 37).addBox(2.6F, 7.65F, -3.85F, 1F, 3F, 2F, new CubeDeformation(0.0F, -0.35F, -0.45F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_powder_horn = p_body.addOrReplaceChild("powder_horn", CubeListBuilder.create().texOffs(236, 27).addBox(0.0F, 0.0F, -1.0F, 2F, 4F, 2F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(82, 37).addBox(0.5F, 3.75F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0.0F, -0.25F, 0.0F)), PartPose.offsetAndRotation(4F, 8F, 0F, 0.0000F, 0.0000F, -0.3142F));
        PartDefinition p_musket = p_right_arm.addOrReplaceChild("musket", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -18.0F, -4.0F, 2F, 25F, 2F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(40, 0).addBox(-2.5F, 2.0F, -4.5F, 3F, 12F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(0, 37).addBox(-3.0F, 12.0F, -5.0F, 4F, 2F, 4F, new CubeDeformation(-0.2F, 0.0F, -0.3F)).texOffs(28, 37).addBox(-2.5F, -16.0F, -4.5F, 3F, 1F, 3F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).texOffs(40, 37).addBox(-2.5F, -5.0F, -4.5F, 3F, 1F, 3F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
    }

    /** Декоративные детали (скрываются в облегчённом режиме). */
    public static List<ModelPart> decor(ModelPart root) {
        List<ModelPart> list = new ArrayList<>();
        return list;
    }
}
