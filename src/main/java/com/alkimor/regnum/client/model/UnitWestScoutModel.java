package com.alkimor.regnum.client.model;

import com.alkimor.regnum.Regnum;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.ArrayList;
import java.util.List;

/** Сгенерировано tools/modelgen — не редактировать вручную. Модель: unit_west_scout. */
public final class UnitWestScoutModel {
    private UnitWestScoutModel() {}

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Regnum.id("unit_west_scout"), "main");

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(84, 0).addBox(-4.0F, -0.25F, -2.5F, 8F, 7F, 5F, new CubeDeformation(0.0F, -0.25F, -0.25F)).texOffs(56, 14).addBox(-3.5F, 6.5F, -2.0F, 7F, 4F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(176, 14).addBox(-4.0F, 9.75F, -2.5F, 8F, 3F, 5F, new CubeDeformation(0.0F, -0.25F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_neck = p_body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(138, 24).addBox(-1.5F, -1.0F, -1.5F, 3F, 1F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(236, 0).addBox(-2.75F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).texOffs(78, 14).addBox(-2.25F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(84, 24).addBox(-2.75F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)), PartPose.offsetAndRotation(-5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 14).mirror().addBox(-1.25F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).mirror(false).texOffs(90, 14).mirror().addBox(-0.75F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(100, 24).mirror().addBox(-1.25F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false), PartPose.offsetAndRotation(5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(204, 0).addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(202, 14).addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).texOffs(110, 14).addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)), PartPose.offsetAndRotation(-1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(220, 0).mirror().addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(218, 14).mirror().addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false).texOffs(128, 14).mirror().addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)).mirror(false), PartPose.offsetAndRotation(1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -7.0F, -3.5F, 7F, 7F, 7F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_hair = p_head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(142, 0).addBox(-4.0F, -7.25F, -4.0F, 8F, 3F, 8F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).texOffs(0, 24).addBox(-4.0F, -4.25F, -1.125F, 8F, 2F, 5F, new CubeDeformation(-0.25F, 0.0F, -0.125F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_helm = p_head.addOrReplaceChild("helm", CubeListBuilder.create().texOffs(110, 0).addBox(-4.0F, -7.8F, -4.0F, 8F, 3F, 8F, new CubeDeformation(-0.1F, 0.0F, -0.1F)).texOffs(40, 0).addBox(-5.5F, -5.35F, -5.5F, 11F, 1F, 11F, new CubeDeformation(0.0F, -0.15F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_skirt = p_body.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(174, 0).addBox(-4.5F, 10.0F, -3.0F, 9F, 5F, 6F, new CubeDeformation(0.0F, 0.0F, -0.4F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_belt = p_body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(56, 24).addBox(-4.5F, 9.25F, -2.5F, 9F, 2F, 5F, new CubeDeformation(-0.25F, -0.25F, 0.0F)).texOffs(182, 24).addBox(-1.0F, 9.25F, -3.0F, 2F, 2F, 1F, new CubeDeformation(0.0F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_quiver = p_body.addOrReplaceChild("quiver", CubeListBuilder.create().texOffs(28, 0).addBox(-1.5F, -1.0F, -0.25F, 3F, 10F, 3F, new CubeDeformation(0.0F, 0.0F, -0.25F)).texOffs(166, 24).addBox(-1.375F, -3.0F, 0.125F, 1F, 2F, 1F, new CubeDeformation(-0.125F, 0.0F, -0.125F)).texOffs(150, 24).addBox(0.125F, -3.75F, 0.625F, 1F, 3F, 1F, new CubeDeformation(-0.125F, -0.25F, -0.125F)).texOffs(188, 24).addBox(-0.625F, -2.75F, 1.125F, 1F, 2F, 1F, new CubeDeformation(-0.125F, -0.25F, -0.125F)), PartPose.offsetAndRotation(-1.5F, 1F, 2.4F, 0.0000F, 0.0000F, 0.4887F));
        PartDefinition p_riding_boot_left = p_left_leg.addOrReplaceChild("riding_boot_left", CubeListBuilder.create().texOffs(16, 14).addBox(-2.5F, 5.65F, -2.5F, 5F, 5F, 5F, new CubeDeformation(-0.45F, -0.35F, -0.3F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_riding_spur_left = p_left_leg.addOrReplaceChild("riding_spur_left", CubeListBuilder.create().texOffs(170, 24).addBox(-0.5F, 10.0F, 1.65F, 1F, 1F, 2F, new CubeDeformation(0.0F, 0.0F, -0.15F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_riding_boot_right = p_right_leg.addOrReplaceChild("riding_boot_right", CubeListBuilder.create().texOffs(36, 14).addBox(-2.5F, 5.65F, -2.5F, 5F, 5F, 5F, new CubeDeformation(-0.45F, -0.35F, -0.3F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_riding_spur_right = p_right_leg.addOrReplaceChild("riding_spur_right", CubeListBuilder.create().texOffs(176, 24).addBox(-0.5F, 10.0F, 1.65F, 1F, 1F, 2F, new CubeDeformation(0.0F, 0.0F, -0.15F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_rider_sash = p_body.addOrReplaceChild("rider_sash", CubeListBuilder.create().texOffs(26, 24).addBox(-4.5F, 8.3F, -3.0F, 9F, 2F, 6F, new CubeDeformation(-0.1F, -0.4F, -0.3F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_scout_mantle = p_body.addOrReplaceChild("scout_mantle", CubeListBuilder.create().texOffs(146, 14).addBox(-4.5F, -0.5F, -3.0F, 9F, 2F, 6F, new CubeDeformation(0.0F, 0.0F, -0.3F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_map_roll = p_body.addOrReplaceChild("map_roll", CubeListBuilder.create().texOffs(102, 14).addBox(0.0F, 0.0F, -1.0F, 2F, 6F, 2F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(154, 24).addBox(-0.5F, 2.5F, -1.5F, 3F, 1F, 3F, new CubeDeformation(-0.4F, 0.0F, -0.4F)), PartPose.offsetAndRotation(4.2F, 8F, 0F, 0.0000F, 0.0000F, -0.3491F));
        PartDefinition p_scout_bedroll = p_body.addOrReplaceChild("scout_bedroll", CubeListBuilder.create().texOffs(116, 24).addBox(-4.0F, 8.0F, 2.8F, 8F, 3F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
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
