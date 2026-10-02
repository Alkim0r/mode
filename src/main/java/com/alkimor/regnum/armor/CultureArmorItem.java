package com.alkimor.regnum.armor;

import com.alkimor.regnum.kingdom.Culture;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Деталь брони культуры. Полный комплект даёт бонус ({@link ArmorModule}). */
public class CultureArmorItem extends ArmorItem {
    public final Culture culture;

    public CultureArmorItem(Culture culture, Holder<ArmorMaterial> material, Type type, Item.Properties props) {
        super(material, type, props);
        this.culture = culture;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tip, flag);
        tip.add(Component.literal("Комплект: " + ArmorModule.setName(culture)).withStyle(ChatFormatting.GOLD));
        tip.add(Component.literal("Полный комплект — " + ArmorModule.setBonusText(culture)).withStyle(ChatFormatting.DARK_AQUA));
    }
}
