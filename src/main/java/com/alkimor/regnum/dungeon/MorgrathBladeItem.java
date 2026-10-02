package com.alkimor.regnum.dungeon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Клинок Морграта: иссушает врагов и возвращает владельцу часть жизни. */
public class MorgrathBladeItem extends SwordItem {
    public MorgrathBladeItem(Properties props) {
        super(Tiers.NETHERITE, props);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1), attacker);
            attacker.heal(1.5f);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Трофей Костяного Владыки").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Иссушение II на 3 с. и +1.5 здоровья за удар").withStyle(ChatFormatting.GRAY));
    }
}
