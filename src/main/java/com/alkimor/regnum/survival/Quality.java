package com.alkimor.regnum.survival;

import com.alkimor.regnum.Regnum;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Качество ковки (идея «Overgeared», переписано под Regnum): сделанное героем оружие, инструмент и броня получают
 * качество от навыка «Кузнечное дело» и удачи — от плохого до шедевра. Качество меняет прочность и силу удара/защиту.
 */
public final class Quality {
    private Quality() {}

    public static final String KEY = "regnum_quality";
    public static final String[] NAMES = {"Плохое", "Обычное", "Хорошее", "Мастерское", "Шедевр"};
    private static final ChatFormatting[] COLORS = {ChatFormatting.GRAY, ChatFormatting.WHITE, ChatFormatting.GREEN, ChatFormatting.AQUA, ChatFormatting.LIGHT_PURPLE};
    private static final float[] DURABILITY = {0.8f, 1.0f, 1.15f, 1.35f, 1.6f};
    private static final double[] POWER = {-0.08, 0, 0.05, 0.12, 0.2};

    public static boolean forgeable(ItemStack s) {
        return s.isDamageableItem() && (s.getItem() instanceof TieredItem || s.getItem() instanceof ArmorItem);
    }

    /** Номер качества по навыку (0..100) и случайному броску 0..100. */
    public static int tier(int skill, double roll) {
        double v = roll + skill * 0.5;
        return v < 25 ? 0 : v < 70 ? 1 : v < 95 ? 2 : v < 120 ? 3 : 4;
    }

    public static int of(ItemStack s) {
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        return cd == null || !cd.copyTag().contains(KEY) ? -1 : cd.copyTag().getInt(KEY);
    }

    /** Применяет качество к предмету один раз. */
    public static void apply(ItemStack s, int tier) {
        if (!forgeable(s) || of(s) >= 0) return;
        s.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, cd -> cd.update(t -> t.putInt(KEY, tier)));
        int max = s.getMaxDamage();
        s.set(DataComponents.MAX_DAMAGE, Math.max(1, Math.round(max * DURABILITY[tier])));
        if (POWER[tier] != 0) {
            ItemAttributeModifiers base = s.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
            ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
            double dmg = 0; boolean hasDmg = false;
            for (var e : base.modifiers()) {
                b.add(e.attribute(), e.modifier(), e.slot());
                if (e.attribute().equals(Attributes.ATTACK_DAMAGE)) { dmg += e.modifier().amount(); hasDmg = true; }
            }
            if (s.getItem() instanceof ArmorItem ai) {
                var group = EquipmentSlotGroup.bySlot(ai.getEquipmentSlot());
                double armor = ai.getDefense();
                b.add(Attributes.ARMOR, new AttributeModifier(Regnum.id("quality_armor"), Math.round(armor * POWER[tier] * 10) / 10.0, AttributeModifier.Operation.ADD_VALUE), group);
            } else if (hasDmg) {
                b.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Regnum.id("quality_damage"), Math.round(dmg * POWER[tier] * 10) / 10.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            }
            s.set(DataComponents.ATTRIBUTE_MODIFIERS, b.build());
        }
        Component name = Component.translatable(s.getItem().getDescriptionId());
        s.set(DataComponents.CUSTOM_NAME, Component.literal(NAMES[tier] + " ").append(name).withStyle(st -> st.withItalic(false).withColor(COLORS[tier])));
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !Injuries.enabled()) return;
        ItemStack s = event.getCrafting();
        if (!forgeable(s)) return;
        int skill = Skills.level(p, Skill.SMITHING);
        int t = tier(skill, p.getRandom().nextDouble() * 100);
        apply(s, t);
        Skills.addXp(p, Skill.SMITHING, 3f + t * 2);
        if (t != 1) p.displayClientMessage(Component.literal("Работа вышла: " + NAMES[t].toLowerCase()).withStyle(COLORS[t]), true);
    }
}
