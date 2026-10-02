package com.alkimor.regnum.client.model;

import com.alkimor.regnum.Regnum;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.ArrayList;
import java.util.List;

/** Сгенерировано tools/modelgen — не редактировать вручную. Модель: unit_steppe_musketeer. */
public final class UnitSteppeMusketeerModel {
    private UnitSteppeMusketeerModel() {}

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Regnum.id("unit_steppe_musketeer"), "main");

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(98, 0).addBox(-4.0F, -0.25F, -2.5F, 8F, 7F, 5F, new CubeDeformation(0.0F, -0.25F, -0.25F)).texOffs(56, 27).addBox(-3.5F, 6.5F, -2.0F, 7F, 4F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(138, 27).addBox(-4.0F, 9.75F, -2.5F, 8F, 3F, 5F, new CubeDeformation(0.0F, -0.25F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_neck = p_body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(36, 37).addBox(-1.5F, -1.0F, -1.5F, 3F, 1F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(210, 0).addBox(-2.75F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).texOffs(78, 27).addBox(-2.25F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(224, 27).addBox(-2.75F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)), PartPose.offsetAndRotation(-5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(226, 0).mirror().addBox(-1.25F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).mirror(false).texOffs(90, 27).mirror().addBox(-0.75F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(240, 27).mirror().addBox(-1.25F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false), PartPose.offsetAndRotation(5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(168, 0).addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(164, 27).addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).texOffs(102, 27).addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)), PartPose.offsetAndRotation(-1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(184, 0).mirror().addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(180, 27).mirror().addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false).texOffs(120, 27).mirror().addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)).mirror(false), PartPose.offsetAndRotation(1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(20, 0).addBox(-3.5F, -7.0F, -3.5F, 7F, 7F, 7F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_hair = p_head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(0, 27).addBox(-4.0F, -7.25F, -4.0F, 8F, 2F, 8F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_braid = p_head.addOrReplaceChild("braid", CubeListBuilder.create().texOffs(160, 0).addBox(-1.0F, 0.0F, -0.25F, 2F, 9F, 2F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, -3F, 3.6F, 0.2094F, 0.0000F, 0.0000F));
        PartDefinition p_hat = p_head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(124, 0).addBox(-4.5F, -7.6F, -4.5F, 9F, 3F, 9F, new CubeDeformation(-0.2F, -0.4F, -0.2F)).texOffs(32, 27).addBox(-3.0F, -10.1F, -3.0F, 6F, 3F, 6F, new CubeDeformation(0.0F, -0.1F, 0.0F)).texOffs(24, 37).addBox(-1.5F, -11.8F, -1.5F, 3F, 2F, 3F, new CubeDeformation(0.0F, -0.2F, 0.0F)).texOffs(112, 37).addBox(-0.5F, -12.6F, -0.5F, 1F, 1F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_skirt = p_body.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(48, 0).addBox(-4.95F, 10.0F, -3.0F, 5F, 8F, 6F, new CubeDeformation(-0.25F, 0.0F, -0.4F)).texOffs(70, 0).mirror().addBox(-0.05F, 10.0F, -3.0F, 5F, 8F, 6F, new CubeDeformation(-0.25F, 0.0F, -0.4F)).mirror(false), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_belt = p_body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(196, 27).addBox(-4.5F, 9.25F, -2.5F, 9F, 2F, 5F, new CubeDeformation(-0.25F, -0.25F, 0.0F)).texOffs(102, 37).addBox(-1.0F, 9.25F, -3.0F, 2F, 2F, 1F, new CubeDeformation(0.0F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_bow_case = p_body.addOrReplaceChild("bow_case", CubeListBuilder.create().texOffs(200, 0).addBox(-0.25F, 0.0F, -1.5F, 2F, 7F, 3F, new CubeDeformation(-0.25F, 0.0F, 0.0F)), PartPose.offsetAndRotation(4.2F, 9F, 0F, 0.0000F, 0.0000F, -0.2094F));
        PartDefinition p_powder_baldric = p_body.addOrReplaceChild("powder_baldric", CubeListBuilder.create().texOffs(92, 0).addBox(-1.0F, -0.3F, -3.15F, 2F, 12F, 1F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, -0.4363F));
        PartDefinition p_powder_charges = p_body.addOrReplaceChild("powder_charges", CubeListBuilder.create().texOffs(72, 37).addBox(-3.0F, 1.65F, -3.85F, 1F, 3F, 2F, new CubeDeformation(0.0F, -0.35F, -0.45F)).texOffs(78, 37).addBox(-1.6F, 3.15F, -3.85F, 1F, 3F, 2F, new CubeDeformation(0.0F, -0.35F, -0.45F)).texOffs(84, 37).addBox(-0.2F, 4.65F, -3.85F, 1F, 3F, 2F, new CubeDeformation(0.0F, -0.35F, -0.45F)).texOffs(90, 37).addBox(1.2F, 6.15F, -3.85F, 1F, 3F, 2F, new CubeDeformation(0.0F, -0.35F, -0.45F)).texOffs(96, 37).addBox(2.6F, 7.65F, -3.85F, 1F, 3F, 2F, new CubeDeformation(0.0F, -0.35F, -0.45F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_powder_horn = p_body.addOrReplaceChild("powder_horn", CubeListBuilder.create().texOffs(0, 37).addBox(0.0F, 0.0F, -1.0F, 2F, 4F, 2F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(108, 37).addBox(0.5F, 3.75F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0.0F, -0.25F, 0.0F)), PartPose.offsetAndRotation(4F, 8F, 0F, 0.0000F, 0.0000F, -0.3142F));
        PartDefinition p_musket = p_right_arm.addOrReplaceChild("musket", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -18.0F, -4.0F, 2F, 25F, 2F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(8, 0).addBox(-2.5F, 2.0F, -4.5F, 3F, 12F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(8, 37).addBox(-3.0F, 12.0F, -5.0F, 4F, 2F, 4F, new CubeDeformation(-0.2F, 0.0F, -0.3F)).texOffs(48, 37).addBox(-2.5F, -16.0F, -4.5F, 3F, 1F, 3F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).texOffs(60, 37).addBox(-2.5F, -5.0F, -4.5F, 3F, 1F, 3F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
    }

    /** Декоративные детали (скрываются в облегчённом режиме). */
    public static List<ModelPart> decor(ModelPart root) {
        List<ModelPart> list = new ArrayList<>();
        list.add(root.getChild("head").getChild("braid"));
        list.add(root.getChild("body").getChild("bow_case"));
        return list;
    }
}
