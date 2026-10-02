package com.alkimor.regnum.wanderers;

import com.alkimor.regnum.core.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/** «Чудо-эликсир» плута: то ли сила медведя, то ли расстройство желудка. */
public class TrickElixirItem extends Item {
    public TrickElixirItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player p) {
            if (level.random.nextBoolean()) {
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 180, 1));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 180, 0));
                Text.good(p, "По жилам разливается сила! Похоже, на этот раз торговец не соврал.");
            } else {
                p.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 10, 0));
                p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 15, 0));
                Text.bad(p, "Фу! Это же болотная вода с мухоморами. Вас обманули!");
            }
            if (!p.getAbilities().instabuild) stack.shrink(1);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("«Сделает тебя сильнее медведя!» — уверял торговец").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
