package com.alkimor.regnum.client.model;

import com.alkimor.regnum.Regnum;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.ArrayList;
import java.util.List;

/** Сгенерировано tools/modelgen — не редактировать вручную. Модель: unit_empire_greatsword. */
public final class UnitEmpireGreatswordModel {
    private UnitEmpireGreatswordModel() {}

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Regnum.id("unit_empire_greatsword"), "main");

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(128, 0).addBox(-4.5F, -0.25F, -2.5F, 9F, 7F, 5F, new CubeDeformation(0.0F, -0.25F, 0.0F)).texOffs(68, 21).addBox(-3.5F, 6.5F, -2.0F, 7F, 4F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(0, 31).addBox(-4.0F, 9.75F, -2.5F, 8F, 3F, 5F, new CubeDeformation(0.0F, -0.25F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_neck = p_body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(132, 31).addBox(-1.5F, -1.0F, -1.5F, 3F, 1F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(200, 0).addBox(-3.0F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(90, 21).addBox(-2.25F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(100, 31).addBox(-2.75F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)), PartPose.offsetAndRotation(-5.5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(216, 0).mirror().addBox(-1.0F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(102, 21).mirror().addBox(-0.75F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(116, 31).mirror().addBox(-1.25F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false), PartPose.offsetAndRotation(5.5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(232, 0).addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(26, 31).addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).texOffs(172, 21).addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)), PartPose.offsetAndRotation(-1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 21).mirror().addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(42, 31).mirror().addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false).texOffs(190, 21).mirror().addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)).mirror(false), PartPose.offsetAndRotation(1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(56, 0).addBox(-3.5F, -7.0F, -3.5F, 7F, 7F, 7F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_helm = p_head.addOrReplaceChild("helm", CubeListBuilder.create().texOffs(24, 0).addBox(-4.0F, -8.05F, -4.0F, 8F, 9F, 8F, new CubeDeformation(0.0F, -0.45F, 0.0F)).texOffs(208, 21).addBox(-3.0F, -9.5F, -3.0F, 6F, 2F, 6F, new CubeDeformation(-0.2F, -0.1F, -0.2F)).texOffs(168, 31).addBox(-0.5F, -11.0F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0.0F, -0.4F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_plume = p_helm.addOrReplaceChild("plume", CubeListBuilder.create().texOffs(16, 21).addBox(-1.0F, -7.0F, -1.0F, 2F, 7F, 2F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, -10.4F, 0F, -0.6109F, 0.0000F, 0.0000F));
        PartDefinition p_pauldron_r = p_right_arm.addOrReplaceChild("pauldron_r", CubeListBuilder.create().texOffs(156, 0).addBox(-3.5F, -3.25F, -3.0F, 5F, 5F, 6F, new CubeDeformation(0.0F, -0.25F, 0.0F)).texOffs(24, 21).addBox(-4.0F, 1.0F, -3.0F, 5F, 3F, 6F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_pauldron_l = p_left_arm.addOrReplaceChild("pauldron_l", CubeListBuilder.create().texOffs(178, 0).mirror().addBox(-1.5F, -3.25F, -3.0F, 5F, 5F, 6F, new CubeDeformation(0.0F, -0.25F, 0.0F)).mirror(false).texOffs(46, 21).mirror().addBox(-1.5F, 1.0F, -3.0F, 5F, 3F, 6F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).mirror(false), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_skirt = p_body.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(84, 0).addBox(-5.1F, 10.0F, -3.0F, 5F, 8F, 6F, new CubeDeformation(-0.1F, 0.0F, -0.4F)).texOffs(106, 0).mirror().addBox(0.1F, 10.0F, -3.0F, 5F, 8F, 6F, new CubeDeformation(-0.1F, 0.0F, -0.4F)).mirror(false), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_belt = p_body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(72, 31).addBox(-4.5F, 9.25F, -2.5F, 9F, 2F, 5F, new CubeDeformation(-0.25F, -0.25F, 0.0F)).texOffs(162, 31).addBox(-1.0F, 9.25F, -3.0F, 2F, 2F, 1F, new CubeDeformation(0.0F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_cape = p_body.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(0, 0).addBox(-5.5F, 0.0F, 0.0F, 11F, 20F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0.2F, 2.7F, 0.0873F, 0.0000F, 0.0000F));
        PartDefinition p_heavy_visor = p_head.addOrReplaceChild("heavy_visor", CubeListBuilder.create().texOffs(144, 31).addBox(-4.0F, -4.05F, -4.05F, 8F, 3F, 1F, new CubeDeformation(-0.4F, -0.35F, -0.15F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_heavy_gorget = p_body.addOrReplaceChild("heavy_gorget", CubeListBuilder.create().texOffs(114, 21).addBox(-3.5F, -0.7F, -3.0F, 7F, 2F, 6F, new CubeDeformation(-0.3F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_heavy_gauntlet_left = p_left_arm.addOrReplaceChild("heavy_gauntlet_left", CubeListBuilder.create().texOffs(140, 21).addBox(-1.25F, 6.5F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.05F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_heavy_gauntlet_right = p_right_arm.addOrReplaceChild("heavy_gauntlet_right", CubeListBuilder.create().texOffs(156, 21).addBox(-2.75F, 6.5F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.05F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_heavy_apron = p_body.addOrReplaceChild("heavy_apron", CubeListBuilder.create().texOffs(58, 31).addBox(-3.0F, 10.0F, -2.95F, 6F, 6F, 1F, new CubeDeformation(0.0F, 0.0F, -0.15F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
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
