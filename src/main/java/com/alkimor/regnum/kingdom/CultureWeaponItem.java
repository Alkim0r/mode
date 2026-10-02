package com.alkimor.regnum.kingdom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/** Оружие культуры: свой силуэт, характеристики и особенность. */
public class CultureWeaponItem extends SwordItem {
    public enum Perk {
        NONE(""),
        REACH("Длинное: +1 к дальности удара"),
        BLEED("Режущее: 25% шанс вызвать кровотечение"),
        SHIELD_BREAK("Пробивает щиты: удар выбивает щит из рук"),
        ARMOR_PIERCE("Дробящее: часть урона проходит сквозь броню"),
        QUICK("Быстрое: короткий замах"),
        CLEAVE("Тяжёлое: широкий размах бьёт всех рядом");

        public final String text;

        Perk(String text) {
            this.text = text;
        }
    }

    public enum Kind {
        SPEAR(3, -2.7f, 1.0, Perk.REACH), PILUM(3, -2.8f, 1.0, Perk.REACH),
        BROAD(4, -2.6f, 0, Perk.NONE), STRAIGHT(3, -2.4f, 0, Perk.NONE), LONG(4, -2.6f, 0.5, Perk.REACH),
        SHORT(2, -1.9f, -0.25, Perk.QUICK), SABRE(3, -2.2f, 0, Perk.BLEED), SCIMITAR(4, -2.4f, 0, Perk.BLEED),
        SHAMSHIR(3, -2.1f, 0, Perk.BLEED), FALCATA(5, -2.7f, 0, Perk.NONE), CLAYMORE(6, -3.0f, 0.6, Perk.CLEAVE),
        AXE(5, -3.0f, 0, Perk.SHIELD_BREAK), MACE(4, -2.8f, 0, Perk.ARMOR_PIERCE);

        public final int damage;
        public final float speed;
        public final double reach;
        public final Perk perk;

        Kind(int damage, float speed, double reach, Perk perk) {
            this.damage = damage;
            this.speed = speed;
            this.reach = reach;
            this.perk = perk;
        }
    }

    private static final ResourceLocation REACH_ID = ResourceLocation.fromNamespaceAndPath("regnum", "weapon_reach");

    public final Kind kind;
    public final Culture culture;
    private final String lore;

    public CultureWeaponItem(Tier tier, Kind kind, Culture culture, String lore, Properties props) {
        super(tier, props.attributes(attributes(tier, kind)));
        this.kind = kind;
        this.culture = culture;
        this.lore = lore;
    }

    private static ItemAttributeModifiers attributes(Tier tier, Kind kind) {
        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, kind.damage + tier.getAttackDamageBonus(),
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, kind.speed,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        if (kind.reach != 0) {
            b.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(REACH_ID, kind.reach, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }
        return b.build();
    }

    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return kind.perk == Perk.SHIELD_BREAK || super.canDisableShield(stack, shield, entity, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(culture.title).withStyle(ChatFormatting.GOLD));
        if (!lore.isEmpty()) tooltip.add(Component.literal(lore).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (kind.perk != Perk.NONE) tooltip.add(Component.literal("◆ " + kind.perk.text).withStyle(ChatFormatting.DARK_AQUA));
    }
}
