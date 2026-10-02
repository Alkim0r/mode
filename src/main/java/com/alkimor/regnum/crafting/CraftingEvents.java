package com.alkimor.regnum.crafting;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class CraftingEvents {
    private CraftingEvents() {}

    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        int t = Tempering.temper(stack);
        if (t <= 0) return;
        if (stack.getItem() instanceof ArmorItem armor) {
            String slot = armor.getEquipmentSlot().getName();
            event.addModifier(Attributes.ARMOR,
                    new AttributeModifier(Regnum.id("temper_armor_" + slot), t, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.bySlot(armor.getEquipmentSlot()));
        } else if (Tempering.isTemperable(stack)) {
            event.addModifier(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(Regnum.id("temper_damage"), t, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        int t = Tempering.temper(event.getItemStack());
        if (t <= 0) return;
        String stars = "★".repeat(t) + "☆".repeat(Tempering.MAX_TEMPER - t);
        event.getToolTip().add(1, Component.literal("Закалка: " + stars).withStyle(ChatFormatting.GOLD));
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack out = event.getCrafting();
        if (out.getItem() instanceof TieredItem || out.getItem() instanceof ArmorItem) {
            Skills.addXp(event.getEntity(), Skill.SMITHING, 3);
        }
    }
}
