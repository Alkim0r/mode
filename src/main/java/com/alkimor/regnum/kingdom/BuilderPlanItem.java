package com.alkimor.regnum.kingdom;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * План строителя: ПКМ по земле — точка стены, Shift+ПКМ — ворота,
 * ПКМ в воздух — отдать план строителям, Shift+ПКМ в воздух — сменить вид стены.
 */
public class BuilderPlanItem extends Item {
    public BuilderPlanItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getPlayer() instanceof ServerPlayer sp) Walls.addPoint(sp, ctx.getClickedPos(), sp.isShiftKeyDown());
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            if (sp.isShiftKeyDown()) Walls.cycleStyle(sp);
            else Walls.confirm(sp);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("ПКМ по земле: точка стены").withStyle(net.minecraft.ChatFormatting.GRAY));
        tip.add(Component.literal("Shift+ПКМ по земле: ворота").withStyle(net.minecraft.ChatFormatting.GRAY));
        tip.add(Component.literal("ПКМ в воздух: начать стройку").withStyle(net.minecraft.ChatFormatting.GRAY));
        tip.add(Component.literal("Shift+ПКМ в воздух: вид стены").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
