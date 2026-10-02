package com.alkimor.regnum.client.model;

import com.alkimor.regnum.Regnum;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.ArrayList;
import java.util.List;

/** Сгенерировано tools/modelgen — не редактировать вручную. Модель: unit_west_heavy_cav. */
public final class UnitWestHeavyCavModel {
    private UnitWestHeavyCavModel() {}

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Regnum.id("unit_west_heavy_cav"), "main");

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(114, 0).addBox(-4.5F, -0.25F, -2.5F, 9F, 7F, 5F, new CubeDeformation(0.0F, -0.25F, 0.0F)).texOffs(84, 23).addBox(-3.5F, 6.5F, -2.0F, 7F, 4F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(22, 33).addBox(-4.0F, 9.75F, -2.5F, 8F, 3F, 5F, new CubeDeformation(0.0F, -0.25F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_neck = p_body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(186, 33).addBox(-1.5F, -1.0F, -1.5F, 3F, 1F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(142, 0).addBox(-3.0F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(106, 23).addBox(-2.25F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(146, 33).addBox(-2.75F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)), PartPose.offsetAndRotation(-5.5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(158, 0).mirror().addBox(-1.0F, -2.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(118, 23).mirror().addBox(-0.75F, 4.0F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(162, 33).mirror().addBox(-1.25F, 7.75F, -2.0F, 4F, 3F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false), PartPose.offsetAndRotation(5.5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(174, 0).addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(48, 33).addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).texOffs(190, 23).addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)), PartPose.offsetAndRotation(-1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(190, 0).mirror().addBox(-2.0F, 0.0F, -2.0F, 4F, 6F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(64, 33).mirror().addBox(-2.0F, 5.75F, -2.0F, 4F, 4F, 4F, new CubeDeformation(-0.25F, -0.25F, -0.25F)).mirror(false).texOffs(208, 23).mirror().addBox(-2.0F, 9.0F, -2.875F, 4F, 3F, 5F, new CubeDeformation(0.0F, 0.0F, -0.125F)).mirror(false), PartPose.offsetAndRotation(1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(86, 0).addBox(-3.5F, -7.0F, -3.5F, 7F, 7F, 7F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_helm = p_head.addOrReplaceChild("helm", CubeListBuilder.create().texOffs(32, 0).addBox(-4.5F, -8.15F, -4.5F, 9F, 9F, 9F, new CubeDeformation(-0.4F, -0.35F, -0.4F)).texOffs(40, 23).addBox(-4.0F, -8.5F, -4.0F, 8F, 1F, 8F, new CubeDeformation(-0.2F, -0.2F, -0.2F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_crest = p_helm.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(72, 23).addBox(-0.5F, -11.9F, -2.5F, 1F, 4F, 5F, new CubeDeformation(0.0F, -0.4F, 0.0F)).texOffs(198, 33).addBox(-2.5F, -10.5F, -0.5F, 5F, 2F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_surcoat = p_body.addOrReplaceChild("surcoat", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -0.1F, -3.0F, 10F, 17F, 6F, new CubeDeformation(-0.25F, 0.0F, -0.15F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_pauldron_r = p_right_arm.addOrReplaceChild("pauldron_r", CubeListBuilder.create().texOffs(206, 0).addBox(-3.5F, -3.0F, -3.0F, 5F, 4F, 6F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(226, 23).addBox(-4.0F, 0.5F, -3.0F, 5F, 2F, 6F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_pauldron_l = p_left_arm.addOrReplaceChild("pauldron_l", CubeListBuilder.create().texOffs(228, 0).mirror().addBox(-1.5F, -3.0F, -3.0F, 5F, 4F, 6F, new CubeDeformation(0.0F, 0.0F, 0.0F)).mirror(false).texOffs(0, 33).mirror().addBox(-1.5F, 0.5F, -3.0F, 5F, 2F, 6F, new CubeDeformation(-0.25F, 0.0F, -0.25F)).mirror(false), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_belt = p_body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(110, 33).addBox(-4.5F, 9.25F, -2.5F, 9F, 2F, 5F, new CubeDeformation(-0.25F, -0.25F, 0.0F)).texOffs(222, 33).addBox(-1.0F, 9.25F, -3.0F, 2F, 2F, 1F, new CubeDeformation(0.0F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_cape = p_body.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(130, 23).addBox(-5.5F, 0.0F, 0.0F, 11F, 7F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0.2F, 2.95F, 0.0873F, 0.0000F, 0.0000F));
        PartDefinition p_shield = p_left_arm.addOrReplaceChild("shield", CubeListBuilder.create().texOffs(68, 0).addBox(0.0F, -1.0F, -4.0F, 1F, 8F, 8F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(154, 23).addBox(0.0F, 7.0F, -3.0F, 1F, 2F, 6F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(178, 33).addBox(0.0F, 8.75F, -1.5F, 1F, 2F, 3F, new CubeDeformation(0.0F, -0.25F, 0.0F)), PartPose.offsetAndRotation(2.6F, 3F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_riding_boot_left = p_left_leg.addOrReplaceChild("riding_boot_left", CubeListBuilder.create().texOffs(0, 23).addBox(-2.5F, 5.65F, -2.5F, 5F, 5F, 5F, new CubeDeformation(-0.45F, -0.35F, -0.3F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_riding_spur_left = p_left_leg.addOrReplaceChild("riding_spur_left", CubeListBuilder.create().texOffs(210, 33).addBox(-0.5F, 10.0F, 1.65F, 1F, 1F, 2F, new CubeDeformation(0.0F, 0.0F, -0.15F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_riding_boot_right = p_right_leg.addOrReplaceChild("riding_boot_right", CubeListBuilder.create().texOffs(20, 23).addBox(-2.5F, 5.65F, -2.5F, 5F, 5F, 5F, new CubeDeformation(-0.45F, -0.35F, -0.3F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_riding_spur_right = p_right_leg.addOrReplaceChild("riding_spur_right", CubeListBuilder.create().texOffs(216, 33).addBox(-0.5F, 10.0F, 1.65F, 1F, 1F, 2F, new CubeDeformation(0.0F, 0.0F, -0.15F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_rider_sash = p_body.addOrReplaceChild("rider_sash", CubeListBuilder.create().texOffs(80, 33).addBox(-4.5F, 8.3F, -3.0F, 9F, 2F, 6F, new CubeDeformation(-0.1F, -0.4F, -0.3F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_cavalry_breastplate = p_body.addOrReplaceChild("cavalry_breastplate", CubeListBuilder.create().texOffs(168, 23).addBox(-5.0F, 0.7F, -3.1F, 10F, 7F, 1F, new CubeDeformation(-0.4F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_cavalry_crest = p_head.addOrReplaceChild("cavalry_crest", CubeListBuilder.create().texOffs(138, 33).addBox(-1.0F, -5.0F, -1.0F, 2F, 5F, 2F, new CubeDeformation(-0.25F, 0.0F, -0.25F)), PartPose.offsetAndRotation(0F, -8F, 0F, -0.2618F, 0.0000F, 0.0000F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
    }

    /** Декоративные детали (скрываются в облегчённом режиме). */
    public static List<ModelPart> decor(ModelPart root) {
        List<ModelPart> list = new ArrayList<>();
        list.add(root.getChild("head").getChild("helm").getChild("crest"));
        list.add(root.getChild("right_arm").getChild("pauldron_r"));
        list.add(root.getChild("left_arm").getChild("pauldron_l"));
        list.add(root.getChild("body").getChild("cape"));
        list.add(root.getChild("left_leg").getChild("riding_spur_left"));
        list.add(root.getChild("right_leg").getChild("riding_spur_right"));
        list.add(root.getChild("head").getChild("cavalry_crest"));
        return list;
    }
}
