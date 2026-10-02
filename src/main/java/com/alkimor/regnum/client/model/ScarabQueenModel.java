package com.alkimor.regnum.client.model;

import com.alkimor.regnum.Regnum;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.ArrayList;
import java.util.List;

/** Сгенерировано tools/modelgen — не редактировать вручную. Модель: scarab_queen. */
public final class ScarabQueenModel {
    private ScarabQueenModel() {}

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Regnum.id("scarab_queen"), "main");

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(64, 0).addBox(-4.0F, -8.0F, -4.0F, 8F, 8F, 8F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_nemes = p_head.addOrReplaceChild("nemes", CubeListBuilder.create().texOffs(84, 18).addBox(-4.5F, -8.6F, -4.5F, 9F, 3F, 9F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(0, 34).addBox(-4.5F, -6.0F, 3.5F, 9F, 11F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_flap_l = p_head.addOrReplaceChild("flap_l", CubeListBuilder.create().texOffs(64, 18).addBox(4.0F, -6.0F, -3.0F, 1F, 10F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_flap_r = p_head.addOrReplaceChild("flap_r", CubeListBuilder.create().texOffs(74, 18).addBox(-5.0F, -6.0F, -3.0F, 1F, 10F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_uraeus = p_head.addOrReplaceChild("uraeus", CubeListBuilder.create().texOffs(20, 46).addBox(-0.5F, -11.0F, -5.0F, 1F, 3F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(24, 46).addBox(-1.0F, -11.5F, -5.2F, 2F, 2F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(96, 0).addBox(-4.0F, 0.0F, -2.0F, 8F, 12F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(20, 34).addBox(-4.5F, 10.0F, -2.6F, 9F, 7F, 5F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_collar = p_body.addOrReplaceChild("collar", CubeListBuilder.create().texOffs(48, 34).addBox(-5.0F, -0.5F, -3.0F, 10F, 4F, 6F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(80, 34).addBox(-4.5F, 8.0F, -2.5F, 9F, 2F, 5F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_wing_r = p_body.addOrReplaceChild("wing_r", CubeListBuilder.create().texOffs(0, 0).addBox(-15.0F, 0.0F, 0.0F, 15F, 17F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(-1.5F, 1F, 2F, 0.0000F, 0.4363F, -0.3491F));
        PartDefinition p_wing_l = p_body.addOrReplaceChild("wing_l", CubeListBuilder.create().texOffs(32, 0).addBox(0.0F, 0.0F, 0.0F, 15F, 17F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(1.5F, 1F, 2F, 0.0000F, -0.4363F, 0.3491F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 18).addBox(-3.0F, -2.0F, -2.0F, 4F, 12F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(-5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_bracer_r = p_right_arm.addOrReplaceChild("bracer_r", CubeListBuilder.create().texOffs(108, 34).addBox(-3.5F, 6.0F, -2.5F, 5F, 2F, 5F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(16, 18).addBox(-1.0F, -2.0F, -2.0F, 4F, 12F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_bracer_l = p_left_arm.addOrReplaceChild("bracer_l", CubeListBuilder.create().texOffs(0, 46).addBox(-1.5F, 6.0F, -2.5F, 5F, 2F, 5F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(32, 18).addBox(-2.0F, 0.0F, -2.0F, 4F, 12F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(-1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(48, 18).addBox(-2.0F, 0.0F, -2.0F, 4F, 12F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_eye_gem = p_head.addOrReplaceChild("eye_gem", CubeListBuilder.create().texOffs(30, 46).addBox(-0.5F, -9.0F, -4.7F, 1F, 1F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Декоративные детали (скрываются в облегчённом режиме). */
    public static List<ModelPart> decor(ModelPart root) {
        List<ModelPart> list = new ArrayList<>();
        list.add(root.getChild("head").getChild("uraeus"));
        list.add(root.getChild("body").getChild("wing_r"));
        list.add(root.getChild("body").getChild("wing_l"));
        list.add(root.getChild("right_arm").getChild("bracer_r"));
        list.add(root.getChild("left_arm").getChild("bracer_l"));
        return list;
    }
}
