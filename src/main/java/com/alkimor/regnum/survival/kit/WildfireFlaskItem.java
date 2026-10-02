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

/** Дикий огонь: склянка с липким пламенем. Не гаснет, жжёт даже то, что не берёт сталь. */
public class WildfireFlaskItem extends Item {
    public WildfireFlaskItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.8f, 0.5f);
        if (!level.isClientSide) {
            WildfireFlaskEntity e = new WildfireFlaskEntity(level, player);
            e.setItem(stack);
            e.shootFromRotation(player, player.getXRot(), player.getYRot(), -10f, 0.9f, 1f);
            level.addFreshEntity(e);
        }
        player.getCooldowns().addCooldown(this, 20);
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Бросить: липкое пламя жжёт всё в круге несколько секунд").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Единственное, что по-настоящему берёт Ходоков Ночи").withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.literal("Жжёт и вас, и ваших людей. Не бросайте под ноги").withStyle(ChatFormatting.DARK_RED));
    }
}
