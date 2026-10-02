package com.alkimor.regnum.survival.kit;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Точильный камень воина: оружие в другой руке наносит +2 урона 90 секунд. */
public class WhetstoneItem extends Item {
    public WhetstoneItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stone = player.getItemInHand(hand);
        ItemStack weapon = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!(weapon.getItem() instanceof TieredItem)) {
            if (!level.isClientSide) Text.bar(player, "Возьмите оружие во вторую руку", ChatFormatting.GRAY);
            return InteractionResultHolder.fail(stone);
        }
        if (player instanceof ServerPlayer sp) {
            sp.addEffect(new MobEffectInstance(KitModule.SHARPENED, 20 * 90, 0));
            level.playSound(null, sp.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.8f, 1.3f);
            Text.bar(sp, "Клинок заточен: +2 урона на 90 с", ChatFormatting.AQUA);
            Skills.addXp(sp, Skill.SMITHING, 2);
            stone.hurtAndBreak(1, sp, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            sp.getCooldowns().addCooldown(this, 40);
        }
        return InteractionResultHolder.sidedSuccess(stone, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Оружие во второй руке + ПКМ — заточить (+2 урона, 90 с)").withStyle(ChatFormatting.GRAY));
    }
}
