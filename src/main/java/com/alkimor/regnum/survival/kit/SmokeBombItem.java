package com.alkimor.regnum.survival.kit;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Дымовая шашка: облако дыма ослепляет врагов и сбивает их с цели; бросивший в дыму становится невидим. */
public class SmokeBombItem extends Item {
    public SmokeBombItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6f, 0.6f);
        if (!level.isClientSide) {
            SmokeBombEntity e = new SmokeBombEntity(level, player);
            e.setItem(stack);
            e.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, 1.1f, 1f);
            level.addFreshEntity(e);
        }
        player.getCooldowns().addCooldown(this, 30);
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Бросить: дым ослепляет врагов и сбивает их с цели").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("В дыму вы невидимы несколько секунд").withStyle(ChatFormatting.DARK_GRAY));
    }
}
