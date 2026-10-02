package com.alkimor.regnum.client.model;

import com.alkimor.regnum.Regnum;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.ArrayList;
import java.util.List;

/** Сгенерировано tools/modelgen — не редактировать вручную. Модель: unit_steppe_crossbow. */
public final class UnitSteppeCrossbowModel {
    private UnitSteppeCrossbowModel() {}

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Regnum.id("unit_steppe_crossbow"), "main");

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(72, 0).addBox(-4.0F, -0.25F, -2.5F, 8F, 7F, 5F, new CubeDeformation(0.0F, -0.25F, -0.25F)).texOffs(76, 14).addBox(-3.5F, 6.5F, -2.0F, 7F, 4F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(158, 14).addBox(-4.0F, 9.75F, -2.5F, 8F, 3F, 5F, new CubeDeformation(0.0F, -0.25F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_neck = p_body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(72, 24).addBox(-1.5F, -1.0F, -1.5F, 3F, 1F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(198, 0).addBox(-2.75F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).texOffs(98, 14).addBox(-2.25F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(28, 24).addBox(-2.75F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)), PartPose.offsetAndRotation(-5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(214, 0).mirror().addBox(-1.25F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).mirror(false).texOffs(110, 14).mirror().addBox(-0.75F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(44, 24).mirror().addBox(-1.25F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false), PartPose.offsetAndRotation(5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(156, 0).addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(184, 14).addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).texOffs(122, 14).addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)), PartPose.offsetAndRotation(-1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(172, 0).mirror().addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(200, 14).mirror().addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false).texOffs(140, 14).mirror().addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)).mirror(false), PartPose.offsetAndRotation(1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -7.0F, -3.5F, 7F, 7F, 7F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_hair = p_head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(0, 14).addBox(-4.0F, -7.25F, -4.0F, 8F, 2F, 8F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_braid = p_head.addOrReplaceChild("braid", CubeListBuilder.create().texOffs(134, 0).addBox(-1.0F, 0.0F, -0.25F, 2F, 9F, 2F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, -3F, 3.6F, 0.2094F, 0.0000F, 0.0000F));
        PartDefinition p_hat = p_head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(98, 0).addBox(-4.5F, -7.6F, -4.5F, 9F, 3F, 9F, new CubeDeformation(-0.2F, -0.4F, -0.2F)).texOffs(52, 14).addBox(-3.0F, -10.1F, -3.0F, 6F, 3F, 6F, new CubeDeformation(0.0F, -0.1F, 0.0F)).texOffs(60, 24).addBox(-1.5F, -11.8F, -1.5F, 3F, 2F, 3F, new CubeDeformation(0.0F, -0.2F, 0.0F)).texOffs(98, 24).addBox(-0.5F, -12.6F, -0.5F, 1F, 1F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_skirt = p_body.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(28, 0).addBox(-4.95F, 10.0F, -3.0F, 5F, 8F, 6F, new CubeDeformation(-0.25F, 0.0F, -0.4F)).texOffs(50, 0).mirror().addBox(-0.05F, 10.0F, -3.0F, 5F, 8F, 6F, new CubeDeformation(-0.25F, 0.0F, -0.4F)).mirror(false), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_belt = p_body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(0, 24).addBox(-4.5F, 9.25F, -2.5F, 9F, 2F, 5F, new CubeDeformation(-0.25F, -0.25F, 0.0F)).texOffs(92, 24).addBox(-1.0F, 9.25F, -3.0F, 2F, 2F, 1F, new CubeDeformation(0.0F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_bow_case = p_body.addOrReplaceChild("bow_case", CubeListBuilder.create().texOffs(188, 0).addBox(-0.25F, 0.0F, -1.5F, 2F, 7F, 3F, new CubeDeformation(-0.25F, 0.0F, 0.0F)), PartPose.offsetAndRotation(4.2F, 9F, 0F, 0.0000F, 0.0000F, -0.2094F));
        PartDefinition p_crossbow_vest = p_body.addOrReplaceChild("crossbow_vest", CubeListBuilder.create().texOffs(32, 14).addBox(-4.5F, 0.05F, -2.85F, 9F, 9F, 1F, new CubeDeformation(-0.4F, -0.25F, -0.15F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_bolt_case = p_body.addOrReplaceChild("bolt_case", CubeListBuilder.create().texOffs(142, 0).addBox(-1.5F, 0.0F, -2.0F, 3F, 7F, 4F, new CubeDeformation(-0.3F, 0.0F, -0.4F)).texOffs(84, 24).addBox(-0.85F, -1.5F, -1.15F, 1F, 2F, 1F, new CubeDeformation(-0.15F, 0.0F, -0.15F)).texOffs(88, 24).addBox(0.15F, -1.5F, -0.15F, 1F, 2F, 1F, new CubeDeformation(-0.15F, 0.0F, -0.15F)), PartPose.offsetAndRotation(-4.5F, 7.5F, 0F, 0.0000F, 0.0000F, 0.2094F));
        PartDefinition p_crossbow_brace = p_right_arm.addOrReplaceChild("crossbow_brace", CubeListBuilder.create().texOffs(216, 14).addBox(-2.8F, 3.5F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.1F, 0.0F, -0.1F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
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
