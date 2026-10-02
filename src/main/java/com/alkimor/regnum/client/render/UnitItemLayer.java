package com.alkimor.regnum.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.alkimor.regnum.kingdom.SoldierEntity;
import com.alkimor.regnum.kingdom.SoldierType;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Предметы в руках, кроме ванильного щита — у наших моделей свой щит культуры на предплечье. */
public class UnitItemLayer<T extends LivingEntity, M extends EntityModel<T> & ArmedModel> extends ItemInHandLayer<T, M> {
    public UnitItemLayer(RenderLayerParent<T, M> parent, ItemInHandRenderer renderer) {
        super(parent, renderer);
    }

    @Override
    protected void renderArmWithItem(LivingEntity entity, ItemStack stack, ItemDisplayContext ctx, HumanoidArm arm,
                                     PoseStack pose, MultiBufferSource buffer, int light) {
        if (stack.is(Items.SHIELD)) return;
        if (entity instanceof SoldierEntity soldier &&
                ((soldier.getSoldierType() == SoldierType.MUSKETEER && stack.is(Items.CROSSBOW)) ||
                 (soldier.getSoldierType() == SoldierType.BOMBARDIER && stack.is(Items.TNT)))) return;
        super.renderArmWithItem(entity, stack, ctx, arm, pose, buffer, light);
    }
}
