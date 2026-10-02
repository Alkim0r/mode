package com.alkimor.regnum.survival.kit;

import com.alkimor.regnum.Regnum;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import static com.alkimor.regnum.core.ModRegistries.*;

/** Инструменты классов: разведчик, охотник, лекарь, мастер, воин, полководец. */
public final class KitModule {
    private KitModule() {}

    // ---------------- эффекты
    public static final DeferredHolder<MobEffect, MobEffect> SHARPENED = EFFECTS.register("sharpened",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xC8D0D8) {}
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, Regnum.id("effect.sharpened"), 2.0, AttributeModifier.Operation.ADD_VALUE));

    // ---------------- блоки
    public static final DeferredBlock<LockedChestBlock> LOCKED_CHEST = BLOCKS.register("locked_chest",
            () -> new LockedChestBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(-1.0f, 3600000.0f).sound(SoundType.WOOD)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LockedChestBlockEntity>> LOCKED_CHEST_BE = BLOCK_ENTITIES.register("locked_chest",
            () -> BlockEntityType.Builder.of(LockedChestBlockEntity::new, LOCKED_CHEST.get()).build(null));
    public static final DeferredItem<BlockItem> LOCKED_CHEST_ITEM = ITEMS.registerSimpleBlockItem("locked_chest", LOCKED_CHEST);

    public static final DeferredBlock<BearTrapBlock> BEAR_TRAP = BLOCKS.register("bear_trap",
            () -> new BearTrapBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 6.0f).sound(SoundType.CHAIN).noOcclusion()));
    public static final DeferredItem<BlockItem> BEAR_TRAP_ITEM = ITEMS.registerSimpleBlockItem("bear_trap", BEAR_TRAP);

    // ---------------- сущности
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownWeaponEntity>> THROWN_WEAPON = ENTITIES.register("thrown_weapon",
            () -> EntityType.Builder.<ThrownWeaponEntity>of(ThrownWeaponEntity::new, MobCategory.MISC).sized(0.4f, 0.4f)
                    .clientTrackingRange(6).updateInterval(10).build("thrown_weapon"));
    public static final DeferredHolder<EntityType<?>, EntityType<SmokeBombEntity>> SMOKE_BOMB_ENTITY = ENTITIES.register("smoke_bomb",
            () -> EntityType.Builder.<SmokeBombEntity>of(SmokeBombEntity::new, MobCategory.MISC).sized(0.25f, 0.25f)
                    .clientTrackingRange(6).updateInterval(10).build("smoke_bomb"));

    public static final DeferredHolder<EntityType<?>, EntityType<WildfireFlaskEntity>> WILDFIRE_FLASK_ENTITY = ENTITIES.register("wildfire_flask",
            () -> EntityType.Builder.<WildfireFlaskEntity>of(WildfireFlaskEntity::new, MobCategory.MISC).sized(0.25f, 0.25f)
                    .clientTrackingRange(6).updateInterval(10).build("wildfire_flask"));

    // ---------------- предметы
    public static final DeferredItem<WildfireFlaskItem> WILDFIRE_FLASK = ITEMS.register("wildfire_flask",
            () -> new WildfireFlaskItem(new Item.Properties().stacksTo(8).rarity(Rarity.RARE)));
    public static final DeferredItem<WhetstoneItem> WHETSTONE = ITEMS.register("whetstone",
            () -> new WhetstoneItem(new Item.Properties().durability(8)));
    public static final DeferredItem<Item> LOCKPICK = ITEMS.register("lockpick",
            () -> new LockpickItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<SmokeBombItem> SMOKE_BOMB = ITEMS.register("smoke_bomb",
            () -> new SmokeBombItem(new Item.Properties().stacksTo(8)));
    public static final DeferredItem<GrapplingHookItem> GRAPPLING_HOOK = ITEMS.register("grappling_hook",
            () -> new GrapplingHookItem(new Item.Properties().durability(96).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ThrowingWeaponItem> JAVELIN = ITEMS.register("javelin",
            () -> new ThrowingWeaponItem(7f, 1.9f, new Item.Properties().stacksTo(8)));
    public static final DeferredItem<ThrowingWeaponItem> THROWING_AXE = ITEMS.register("throwing_axe",
            () -> new ThrowingWeaponItem(6f, 1.5f, new Item.Properties().stacksTo(6)));
    public static final DeferredItem<HerbalMortarItem> HERBAL_MORTAR = ITEMS.register("herbal_mortar",
            () -> new HerbalMortarItem(new Item.Properties().durability(32)));
    public static final DeferredItem<FieldSmithKitItem> FIELD_SMITH_KIT = ITEMS.register("field_smith_kit",
            () -> new FieldSmithKitItem(new Item.Properties().durability(12)));
    public static final DeferredItem<WarBannerItem> WAR_BANNER = ITEMS.register("war_banner",
            () -> new WarBannerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static void init() {}
}
