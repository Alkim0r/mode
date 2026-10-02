package com.alkimor.regnum.trade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Ящик торгового груза. Тяжёлый: сильно нагружает инвентарь. */
public class TradeGoodItem extends Item {
    public final TradeGood good;

    public TradeGoodItem(TradeGood good, Properties props) {
        super(props);
        this.good = good;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Торговый груз · базовая цена " + good.base + " изумр.").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Дёшев в регионе: " + good.producer().title).withStyle(ChatFormatting.GRAY));
        TradeOrigin o = stack.get(TradeModule.ORIGIN.get());
        if (o != null) {
            tooltip.add(Component.literal("Закуплен: " + Region.byId(o.region()).title + " (" + o.pos().getX() + ", " + o.pos().getZ() + ")")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("Происхождение неизвестно (краденое?)").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.literal("Чем дальше довезёте — тем дороже продадите. Тяжёлый груз!").withStyle(ChatFormatting.DARK_GRAY));
    }
}
