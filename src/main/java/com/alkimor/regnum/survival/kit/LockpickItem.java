package com.alkimor.regnum.survival.kit;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Отмычка: ПКМ по запертому сундуку. Шанс растёт с Плутовством. */
public class LockpickItem extends Item {
    public LockpickItem(Properties p) {
        super(p);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("ПКМ по запертому сундуку — вскрыть").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Шанс зависит от Плутовства и сложности замка").withStyle(ChatFormatting.DARK_GRAY));
    }
}
