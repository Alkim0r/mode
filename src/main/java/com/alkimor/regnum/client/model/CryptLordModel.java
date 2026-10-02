package com.alkimor.regnum.client.model;

import com.alkimor.regnum.Regnum;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import java.util.ArrayList;
import java.util.List;

/** Сгенерировано tools/modelgen — не редактировать вручную. Модель: crypt_lord. */
public final class CryptLordModel {
    private CryptLordModel() {}

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Regnum.id("crypt_lord"), "main");

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(24, 0).addBox(-4.0F, -8.0F, -4.0F, 8F, 8F, 8F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_crown = p_head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 23).addBox(-4.5F, -10.0F, -4.5F, 9F, 2F, 9F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(76, 34).addBox(-4.5F, -12.0F, -4.5F, 1F, 2F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(44, 34).addBox(-0.5F, -13.0F, -4.5F, 1F, 3F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(80, 34).addBox(3.5F, -12.0F, -4.5F, 1F, 2F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(84, 34).addBox(-4.5F, -12.0F, 3.5F, 1F, 2F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(88, 34).addBox(3.5F, -12.0F, 3.5F, 1F, 2F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(92, 34).addBox(-1.0F, -9.6F, -4.9F, 2F, 1F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_horn_l = p_head.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(48, 34).addBox(0.0F, -1.0F, -1.0F, 3F, 2F, 2F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(4F, -6F, 0F, 0.0000F, 0.0000F, -0.4363F));
        PartDefinition p_horn_l_tip = p_horn_l.addOrReplaceChild("horn_l_tip", CubeListBuilder.create().texOffs(98, 34).addBox(0.0F, -0.5F, -0.5F, 3F, 1F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(3F, 0F, 0F, 0.0000F, 0.0000F, -0.6981F));
        PartDefinition p_horn_r = p_head.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(58, 34).addBox(-3.0F, -1.0F, -1.0F, 3F, 2F, 2F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(-4F, -6F, 0F, 0.0000F, 0.0000F, 0.4363F));
        PartDefinition p_horn_r_tip = p_horn_r.addOrReplaceChild("horn_r_tip", CubeListBuilder.create().texOffs(106, 34).addBox(-3.0F, -0.5F, -0.5F, 3F, 1F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(-3F, 0F, 0F, 0.0000F, 0.0000F, 0.6981F));
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(56, 0).addBox(-4.0F, 0.0F, -2.0F, 8F, 12F, 4F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(36, 23).addBox(-4.5F, 9.0F, -2.5F, 9F, 6F, 5F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_collar = p_body.addOrReplaceChild("collar", CubeListBuilder.create().texOffs(12, 34).addBox(-5.0F, -1.0F, -3.0F, 10F, 2F, 6F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_cape = p_body.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(0, 0).addBox(-5.5F, 0.0F, 0.0F, 11F, 22F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 2.2F, 0.1396F, 0.0000F, 0.0000F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(80, 0).addBox(-1.5F, -2.0F, -1.0F, 2F, 12F, 2F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(-5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_pauldron_r = p_right_arm.addOrReplaceChild("pauldron_r", CubeListBuilder.create().texOffs(64, 23).addBox(-3.5F, -3.5F, -3.0F, 5F, 4F, 6F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_spike_r = p_pauldron_r.addOrReplaceChild("spike_r", CubeListBuilder.create().texOffs(68, 34).addBox(-2.5F, -6.0F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(88, 0).addBox(-0.5F, -2.0F, -1.0F, 2F, 12F, 2F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(5F, 2F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_pauldron_l = p_left_arm.addOrReplaceChild("pauldron_l", CubeListBuilder.create().texOffs(86, 23).addBox(-1.5F, -3.5F, -3.0F, 5F, 4F, 6F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_spike_l = p_pauldron_l.addOrReplaceChild("spike_l", CubeListBuilder.create().texOffs(72, 34).addBox(1.5F, -6.0F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(96, 0).addBox(-1.0F, 0.0F, -1.0F, 2F, 12F, 2F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(108, 23).addBox(-1.5F, 6.0F, -1.5F, 3F, 6F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(-1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(104, 0).addBox(-1.0F, 0.0F, -1.0F, 2F, 12F, 2F, new CubeDeformation(0.0F, 0.0F, 0.0F)).texOffs(0, 34).addBox(-1.5F, 6.0F, -1.5F, 3F, 6F, 3F, new CubeDeformation(0.0F, 0.0F, 0.0F)), PartPose.offsetAndRotation(1.9F, 12F, 0F, 0.0000F, 0.0000F, 0.0000F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Декоративные детали (скрываются в облегчённом режиме). */
    public static List<ModelPart> decor(ModelPart root) {
        List<ModelPart> list = new ArrayList<>();
        list.add(root.getChild("head").getChild("horn_l"));
        list.add(root.getChild("head").getChild("horn_l").getChild("horn_l_tip"));
        list.add(root.getChild("head").getChild("horn_r"));
        list.add(root.getChild("head").getChild("horn_r").getChild("horn_r_tip"));
        list.add(root.getChild("body").getChild("cape"));
        list.add(root.getChild("right_arm").getChild("pauldron_r").getChild("spike_r"));
        list.add(root.getChild("left_arm").getChild("pauldron_l").getChild("spike_l"));
        return list;
    }
}
