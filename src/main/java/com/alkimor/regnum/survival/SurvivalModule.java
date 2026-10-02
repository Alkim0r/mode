package com.alkimor.regnum.survival;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.core.RegnumModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

import static com.alkimor.regnum.core.ModRegistries.ATTACHMENTS;
import static com.alkimor.regnum.core.ModRegistries.EFFECTS;
import static com.alkimor.regnum.core.ModRegistries.ITEMS;

/** Модуль «Выживание»: навыки, травмы, медицина, собирательство. */
public class SurvivalModule implements RegnumModule {

    public static final Supplier<AttachmentType<SurvivorData>> SURVIVOR = ATTACHMENTS.register("survivor",
            () -> AttachmentType.builder(() -> new SurvivorData()).serialize(SurvivorData.CODEC).copyOnDeath().build());

    public static final ResourceKey<DamageType> BLEEDING_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Regnum.id("bleeding"));

    public static final DeferredHolder<MobEffect, AilmentEffect> BLEEDING = EFFECTS.register("bleeding",
            () -> new AilmentEffect(AilmentEffect.Kind.BLEEDING, 0x8A0303));
    public static final DeferredHolder<MobEffect, MobEffect> FRACTURE = EFFECTS.register("fracture",
            () -> new AilmentEffect(AilmentEffect.Kind.FRACTURE, 0xD8D2B8)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, Regnum.id("effect.fracture_speed"), -0.35, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.JUMP_STRENGTH, Regnum.id("effect.fracture_jump"), -0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, AilmentEffect> INFECTION = EFFECTS.register("infection",
            () -> new AilmentEffect(AilmentEffect.Kind.INFECTION, 0x5E7A1E));

    public static final DeferredItem<MedicalItem> BANDAGE = ITEMS.register("bandage",
            () -> new MedicalItem(MedicalItem.Kind.BANDAGE, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<MedicalItem> SPLINT = ITEMS.register("splint",
            () -> new MedicalItem(MedicalItem.Kind.SPLINT, new Item.Properties().stacksTo(8)));
    public static final DeferredItem<MedicalItem> HERBAL_DECOCTION = ITEMS.register("herbal_decoction",
            () -> new MedicalItem(MedicalItem.Kind.DECOCTION, new Item.Properties().stacksTo(8)));
    public static final DeferredItem<MedicalItem> MEDKIT = ITEMS.register("medkit",
            () -> new MedicalItem(MedicalItem.Kind.MEDKIT, new Item.Properties().stacksTo(4).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> HEALING_HERB = ITEMS.register("healing_herb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LIFE_ROOT = ITEMS.register("life_root",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE).food(new net.minecraft.world.food.FoodProperties.Builder()
                    .nutrition(4).saturationModifier(1.2f).alwaysEdible()
                    .effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION, 20 * 120, 3), 1.0f)
                    .effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 20 * 20, 1), 1.0f)
                    .build())));
    public static final DeferredItem<JournalItem> JOURNAL = ITEMS.register("survivor_journal",
            () -> new JournalItem(new Item.Properties().stacksTo(1)));

    @Override
    public String id() {
        return "survival";
    }

    @Override
    public String title() {
        return "Выживание";
    }

    @Override
    public void init(IEventBus modBus) {
        com.alkimor.regnum.survival.kit.KitModule.init();
        NeoForge.EVENT_BUS.register(SurvivalEvents.class);
        NeoForge.EVENT_BUS.register(Hygiene.class);
        NeoForge.EVENT_BUS.register(PerkEvents.class);
        NeoForge.EVENT_BUS.register(Respec.class);
        NeoForge.EVENT_BUS.register(Training.class);
        NeoForge.EVENT_BUS.register(Downed.class);
        NeoForge.EVENT_BUS.register(Needs.class);
        NeoForge.EVENT_BUS.register(Exploration.class);
        NeoForge.EVENT_BUS.register(Quality.class);
        NeoForge.EVENT_BUS.register(FoodVariety.class);
        NeoForge.EVENT_BUS.register(Seasons.class);
        NeoForge.EVENT_BUS.register(ArmorDirt.class);
    }
}
