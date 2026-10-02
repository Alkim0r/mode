package com.alkimor.regnum.client.model;

import com.alkimor.regnum.Regnum;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.ArrayList;
import java.util.List;

/** Сгенерировано tools/modelgen — не редактировать вручную. Модель: unit_clans_knight. */
public final class UnitClansKnightModel {
    private UnitClansKnightModel() {}

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Regnum.id("unit_clans_knight"), "main");

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(34, 19).addBox(-4.5F, -0.25F, -2.5F, 9F, 7F, 5F, new CubeDeformation(0.0F, -0.25F, 0.0F)).texOffs(78, 32).addBox(-3.5F, 6.5F, -2.0F, 7F, 4F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(72, 42).addBox(-4.0F, 9.75F, -2.5F, 8F, 3F, 5F, new CubeDeformation(0.0F, -0.25F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_neck = p_body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 58).addBox(-1.5F, -1.0F, -1.5F, 3F, 1F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(62, 19).addBox(-3.0F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(100, 32).addBox(-2.25F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(44, 50).addBox(-2.75F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)), PartPose.offsetAndRotation(-5.5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(78, 19).mirror().addBox(-1.0F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(112, 32).mirror().addBox(-0.75F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(60, 50).mirror().addBox(-1.25F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false), PartPose.offsetAndRotation(5.5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(94, 19).addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(98, 42).addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).texOffs(36, 42).addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)), PartPose.offsetAndRotation(-1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(110, 19).mirror().addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(0, 50).mirror().addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false).texOffs(54, 42).mirror().addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)).mirror(false), PartPose.offsetAndRotation(1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(42, 0).addBox(-3.5F, -7.0F, -3.5F, 7F, 7F, 7F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_beard = p_head.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(92, 50).addBox(-2.5F, 0.0F, -3.6F, 5F, 3F, 2F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_wolf_head = p_head.addOrReplaceChild("wolf_head", CubeListBuilder.create().texOffs(0, 19).addBox(-4.0F, -8.45F, -4.5F, 8F, 4F, 9F, new CubeDeformation(0.0F, -0.25F, -0.3F)).texOffs(76, 50).addBox(-2.0F, -7.8F, -7.7F, 4F, 3F, 4F, new CubeDeformation(0.0F, -0.2F, -0.3F)).texOffs(20, 58).addBox(-3.8F, -10.0F, -2.7F, 2F, 2F, 2F, new CubeDeformation(-0.2F, 0.0F, -0.3F)).texOffs(28, 58).addBox(1.8F, -10.0F, -2.7F, 2F, 2F, 2F, new CubeDeformation(-0.2F, 0.0F, -0.3F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_wolf_pelt = p_body.addOrReplaceChild("wolf_pelt", CubeListBuilder.create().texOffs(44, 32).addBox(-5.5F, -1.0F, -3.0F, 11F, 3F, 6F, new CubeDeformation(-0.3F, 0.0F, 0.0F)).texOffs(0, 0).addBox(-5.0F, 1.5F, 2.4F, 10F, 18F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_torc = p_neck.addOrReplaceChild("torc", CubeListBuilder.create().texOffs(106, 50).addBox(-2.0F, -0.375F, -2.0F, 4F, 1F, 4F, new CubeDeformation(0.0F, -0.125F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_pauldron_r = p_right_arm.addOrReplaceChild("pauldron_r", CubeListBuilder.create().texOffs(0, 32).addBox(-3.5F, -3.25F, -3.0F, 5F, 4F, 6F, new CubeDeformation(0.0F, -0.25F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_pauldron_l = p_left_arm.addOrReplaceChild("pauldron_l", CubeListBuilder.create().texOffs(22, 32).mirror().addBox(-1.5F, -3.25F, -3.0F, 5F, 4F, 6F, new CubeDeformation(0.0F, -0.25F, 0.0F)).mirror(false), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_skirt = p_body.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(70, 0).addBox(-5.0F, 10.0F, -3.0F, 10F, 7F, 6F, new CubeDeformation(0.0F, 0.0F, -0.4F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_belt = p_body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(16, 50).addBox(-4.5F, 9.25F, -2.5F, 9F, 2F, 5F, new CubeDeformation(-0.25F, -0.25F, 0.0F)).texOffs(36, 58).addBox(-1.0F, 9.25F, -3.0F, 2F, 2F, 1F, new CubeDeformation(0.0F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_shield = p_left_arm.addOrReplaceChild("shield", CubeListBuilder.create().texOffs(22, 0).addBox(0.0F, -1.5F, -4.5F, 1F, 9F, 9F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(124, 32).addBox(0.0F, -0.5F, -5.5F, 1F, 7F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(0, 42).addBox(0.0F, -0.5F, 4.5F, 1F, 7F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(4, 42).addBox(0.0F, -2.5F, -3.5F, 1F, 1F, 7F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(20, 42).addBox(0.0F, 7.5F, -3.5F, 1F, 1F, 7F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(12, 58).addBox(0.6F, 2.0F, -1.0F, 2F, 2F, 2F, new CubeDeformation(-0.4F, 0.0F, 0.0F)), PartPose.offsetAndRotation(2.6F, 3F, 0F, 0.0000F, 0.0000F, 0.0000F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Декоративные детали (скрываются в облегчённом режиме). */
    public static List<ModelPart> decor(ModelPart root) {
        List<ModelPart> list = new ArrayList<>();
        list.add(root.getChild("head").getChild("beard"));
        list.add(root.getChild("body").getChild("wolf_pelt"));
        list.add(root.getChild("right_arm").getChild("pauldron_r"));
        list.add(root.getChild("left_arm").getChild("pauldron_l"));
        return list;
    }
}
