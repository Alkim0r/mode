package com.alkimor.regnum.dungeon;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.core.RegnumModule;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import static com.alkimor.regnum.core.ModRegistries.*;

/** Модуль «Подземелья»: склепы в мире, босс Морграт, уникальный лут. */
public class DungeonModule implements RegnumModule {

    public static final ResourceKey<LootTable> CRYPT_CHEST_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Regnum.id("chests/crypt"));
    public static final ResourceKey<LootTable> CRYPT_TREASURE_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Regnum.id("chests/crypt_treasure"));
    public static final TagKey<Structure> CRYPTS_TAG = TagKey.create(Registries.STRUCTURE, Regnum.id("crypts"));

    public static final DeferredHolder<EntityType<?>, EntityType<CryptLordEntity>> CRYPT_LORD = ENTITIES.register("crypt_lord",
            () -> EntityType.Builder.of(CryptLordEntity::new, MobCategory.MONSTER)
                    .sized(1.35f, 4.65f).fireImmune().clientTrackingRange(10).build("crypt_lord"));

    public static final DeferredBlock<CryptAltarBlock> CRYPT_ALTAR = BLOCKS.register("crypt_altar",
            () -> new CryptAltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.DEEPSLATE_BRICKS).lightLevel(s -> 7)));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> CRYPT_ALTAR_ITEM = ITEMS.registerSimpleBlockItem("crypt_altar", CRYPT_ALTAR);

    public static final DeferredItem<Item> CRYPT_HEART = ITEMS.register("crypt_heart",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
    public static final DeferredItem<MorgrathBladeItem> MORGRATH_BLADE = ITEMS.register("morgrath_blade",
            () -> new MorgrathBladeItem(new Item.Properties().fireResistant().rarity(Rarity.EPIC)
                    .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 5, -2.5f))));
    public static final DeferredItem<DeferredSpawnEggItem> CRYPT_LORD_EGG = ITEMS.register("crypt_lord_spawn_egg",
            () -> new DeferredSpawnEggItem(CRYPT_LORD, 0x1E1B24, 0x7B2FBE, new Item.Properties()));

    public static final DeferredHolder<StructureType<?>, StructureType<CryptStructure>> CRYPT_STRUCTURE = STRUCTURE_TYPES.register("crypt",
            () -> cryptType());
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CRYPT_PIECE = STRUCTURE_PIECES.register("crypt_piece",
            () -> (StructurePieceType.ContextlessType) CryptPiece::new);

    public static final TagKey<Structure> CAMPS_TAG = TagKey.create(Registries.STRUCTURE, Regnum.id("bandit_camps"));
    public static final DeferredHolder<StructureType<?>, StructureType<CampStructure>> CAMP_STRUCTURE = STRUCTURE_TYPES.register("bandit_camp",
            () -> campType());
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CAMP_PIECE = STRUCTURE_PIECES.register("bandit_camp_piece",
            () -> (StructurePieceType.ContextlessType) CampPiece::new);

    private static StructureType<CryptStructure> cryptType() {
        return () -> CryptStructure.CODEC;
    }

    private static StructureType<CampStructure> campType() {
        return () -> CampStructure.CODEC;
    }

    @Override
    public String id() {
        return "dungeons";
    }

    @Override
    public String title() {
        return "Подземелья и боссы";
    }

    @Override
    public void init(IEventBus modBus) {
        modBus.addListener(DungeonModule::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(CRYPT_LORD.get(), CryptLordEntity.createAttributes().build());
    }
}
