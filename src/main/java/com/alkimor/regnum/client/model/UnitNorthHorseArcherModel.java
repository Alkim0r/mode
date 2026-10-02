package com.alkimor.regnum.client.model;

import com.alkimor.regnum.Regnum;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.ArrayList;
import java.util.List;

/** Сгенерировано tools/modelgen — не редактировать вручную. Модель: unit_north_horse_archer. */
public final class UnitNorthHorseArcherModel {
    private UnitNorthHorseArcherModel() {}

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Regnum.id("unit_north_horse_archer"), "main");

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(84, 0).addBox(-4.0F, -0.25F, -2.5F, 8F, 7F, 5F, new CubeDeformation(0.0F, -0.25F, -0.25F)).texOffs(64, 14).addBox(-3.5F, 6.5F, -2.0F, 7F, 4F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(146, 14).addBox(-4.0F, 9.75F, -2.5F, 8F, 3F, 5F, new CubeDeformation(0.0F, -0.25F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_neck = p_body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(84, 24).addBox(-1.5F, -1.0F, -1.5F, 3F, 1F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(216, 0).addBox(-2.75F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).texOffs(86, 14).addBox(-2.25F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(52, 24).addBox(-2.75F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)), PartPose.offsetAndRotation(-5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(232, 0).mirror().addBox(-1.25F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).mirror(false).texOffs(98, 14).mirror().addBox(-0.75F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(68, 24).mirror().addBox(-1.25F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false), PartPose.offsetAndRotation(5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(152, 0).addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(172, 14).addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).texOffs(110, 14).addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)), PartPose.offsetAndRotation(-1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(168, 0).mirror().addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(188, 14).mirror().addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false).texOffs(128, 14).mirror().addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)).mirror(false), PartPose.offsetAndRotation(1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -7.0F, -3.5F, 7F, 7F, 7F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_hair = p_head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(184, 0).addBox(-4.0F, -7.5F, -4.0F, 8F, 3F, 8F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).texOffs(28, 24).addBox(-4.0F, -4.75F, 0.125F, 8F, 3F, 4F, new CubeDeformation(-0.25F, 0.0F, -0.375F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_hat = p_head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(120, 0).addBox(-4.0F, -7.85F, -4.0F, 8F, 3F, 8F, new CubeDeformation(0.0F, -0.25F, 0.0F)).texOffs(40, 14).addBox(-3.0F, -10.2F, -3.0F, 6F, 3F, 6F, new CubeDeformation(0.0F, -0.2F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_skirt = p_body.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(40, 0).addBox(-4.85F, 10.0F, -3.0F, 5F, 7F, 6F, new CubeDeformation(-0.35F, 0.0F, -0.4F)).texOffs(62, 0).mirror().addBox(-0.15F, 10.0F, -3.0F, 5F, 7F, 6F, new CubeDeformation(-0.35F, 0.0F, -0.4F)).mirror(false), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_belt = p_body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(0, 24).addBox(-4.5F, 9.25F, -2.5F, 9F, 2F, 5F, new CubeDeformation(-0.25F, -0.25F, 0.0F)).texOffs(116, 24).addBox(-1.0F, 9.25F, -3.0F, 2F, 2F, 1F, new CubeDeformation(0.0F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_quiver = p_body.addOrReplaceChild("quiver", CubeListBuilder.create().texOffs(28, 0).addBox(-1.5F, -1.0F, -0.25F, 3F, 10F, 3F, new CubeDeformation(0.0F, 0.0F, -0.25F)).texOffs(100, 24).addBox(-1.375F, -3.0F, 0.125F, 1F, 2F, 1F, new CubeDeformation(-0.125F, 0.0F, -0.125F)).texOffs(96, 24).addBox(0.125F, -3.75F, 0.625F, 1F, 3F, 1F, new CubeDeformation(-0.125F, -0.25F, -0.125F)).texOffs(122, 24).addBox(-0.625F, -2.75F, 1.125F, 1F, 2F, 1F, new CubeDeformation(-0.125F, -0.25F, -0.125F)), PartPose.offsetAndRotation(-1.5F, 1F, 2.4F, 0.0000F, 0.0000F, 0.4887F));
        PartDefinition p_riding_boot_left = p_left_leg.addOrReplaceChild("riding_boot_left", CubeListBuilder.create().texOffs(0, 14).addBox(-2.5F, 5.65F, -2.5F, 5F, 5F, 5F, new CubeDeformation(-0.45F, -0.35F, -0.3F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_riding_spur_left = p_left_leg.addOrReplaceChild("riding_spur_left", CubeListBuilder.create().texOffs(104, 24).addBox(-0.5F, 10.0F, 1.65F, 1F, 1F, 2F, new CubeDeformation(0.0F, 0.0F, -0.15F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_riding_boot_right = p_right_leg.addOrReplaceChild("riding_boot_right", CubeListBuilder.create().texOffs(20, 14).addBox(-2.5F, 5.65F, -2.5F, 5F, 5F, 5F, new CubeDeformation(-0.45F, -0.35F, -0.3F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_riding_spur_right = p_right_leg.addOrReplaceChild("riding_spur_right", CubeListBuilder.create().texOffs(110, 24).addBox(-0.5F, 10.0F, 1.65F, 1F, 1F, 2F, new CubeDeformation(0.0F, 0.0F, -0.15F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_rider_sash = p_body.addOrReplaceChild("rider_sash", CubeListBuilder.create().texOffs(204, 14).addBox(-4.5F, 8.3F, -3.0F, 9F, 2F, 6F, new CubeDeformation(-0.1F, -0.4F, -0.3F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_bow_case = p_body.addOrReplaceChild("bow_case", CubeListBuilder.create().texOffs(110, 0).addBox(0.0F, 0.0F, -1.5F, 2F, 8F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(4F, 7F, 0F, 0.0000F, 0.0000F, -0.2094F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
    }

    /** Декоративные детали (скрываются в облегчённом режиме). */
    public static List<ModelPart> decor(ModelPart root) {
        List<ModelPart> list = new ArrayList<>();
        list.add(root.getChild("body").getChild("quiver"));
        list.add(root.getChild("left_leg").getChild("riding_spur_left"));
        list.add(root.getChild("right_leg").getChild("riding_spur_right"));
        return list;
    }
}
