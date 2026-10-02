package com.alkimor.regnum.dungeon;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.core.RegnumModule;
import com.alkimor.regnum.dungeon.boss.ForgemasterEntity;
import com.alkimor.regnum.dungeon.boss.MireMotherEntity;
import com.alkimor.regnum.dungeon.boss.ScarabQueenEntity;
import com.alkimor.regnum.survival.MedicalItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
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

/**
 * Модуль «Регионы»: у каждого региона свой данж, свой босс и свой редкий трофей.
 * Болота — Затопленное капище (Матушка Топь), горы — Гномья кузня (Горновой),
 * пустыня — Песчаная гробница (Сехмет-ра).
 */
public class RegionsModule implements RegnumModule {

    // --- Лут
    public static final ResourceKey<LootTable> SHRINE_LOOT = loot("chests/sunken_shrine");
    public static final ResourceKey<LootTable> FORTRESS_LOOT = loot("chests/forge_fortress");
    public static final ResourceKey<LootTable> TOMB_LOOT = loot("chests/sand_tomb");
    public static final TagKey<Structure> REGION_DUNGEONS = TagKey.create(Registries.STRUCTURE, Regnum.id("region_dungeons"));

    private static ResourceKey<LootTable> loot(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, Regnum.id(path));
    }

    // --- Боссы
    public static final DeferredHolder<EntityType<?>, EntityType<MireMotherEntity>> MIRE_MOTHER = ENTITIES.register("mire_mother",
            () -> EntityType.Builder.of(MireMotherEntity::new, MobCategory.MONSTER).sized(1.5f, 4.8f).clientTrackingRange(10).build("mire_mother"));
    public static final DeferredHolder<EntityType<?>, EntityType<ForgemasterEntity>> FORGEMASTER = ENTITIES.register("forgemaster",
            () -> EntityType.Builder.of(ForgemasterEntity::new, MobCategory.MONSTER).sized(2.85f, 5.7f).fireImmune().clientTrackingRange(10).build("forgemaster"));
    public static final DeferredHolder<EntityType<?>, EntityType<ScarabQueenEntity>> SCARAB_QUEEN = ENTITIES.register("scarab_queen",
            () -> EntityType.Builder.of(ScarabQueenEntity::new, MobCategory.MONSTER).sized(1.35f, 4.2f).clientTrackingRange(10).build("scarab_queen"));

    // --- Блоки
    private static BlockBehaviour.Properties altarProps(MapColor c) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.DEEPSLATE_BRICKS).lightLevel(s -> 7);
    }

    public static final DeferredBlock<BossAltarBlock> MIRE_ALTAR = BLOCKS.register("mire_altar",
            () -> new BossAltarBlock(altarProps(MapColor.COLOR_GREEN), MIRE_MOTHER, 0, -3, "Матушка Топь",
                    "Кто пришёл в мои воды? Тёплая кровь... как давно я её не пробовала!"));
    public static final DeferredBlock<BossAltarBlock> FORGE_ALTAR = BLOCKS.register("forge_altar",
            () -> new BossAltarBlock(altarProps(MapColor.COLOR_ORANGE), FORGEMASTER, 0, -7, "Горновой",
                    "Кузня не ждала гостей. Тебя переплавят на гвозди!"));
    public static final DeferredBlock<BossAltarBlock> SUN_ALTAR = BLOCKS.register("sun_altar",
            () -> new BossAltarBlock(altarProps(MapColor.GOLD), SCARAB_QUEEN, 0, -8, "Сехмет-ра",
                    "Смертный в моей гробнице? Пески примут ещё одну жертву!"));
    public static final DeferredBlock<Block> EMBER_CORE = BLOCKS.register("ember_core",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.FIRE).strength(4.0f, 6.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERRACK).lightLevel(s -> 15)));

    public static final DeferredItem<BlockItem> MIRE_ALTAR_ITEM = ITEMS.registerSimpleBlockItem("mire_altar", MIRE_ALTAR);
    public static final DeferredItem<BlockItem> FORGE_ALTAR_ITEM = ITEMS.registerSimpleBlockItem("forge_altar", FORGE_ALTAR);
    public static final DeferredItem<BlockItem> SUN_ALTAR_ITEM = ITEMS.registerSimpleBlockItem("sun_altar", SUN_ALTAR);
    public static final DeferredItem<BlockItem> EMBER_CORE_ITEM = ITEMS.registerSimpleBlockItem("ember_core", EMBER_CORE);

    // --- Трофеи и предметы
    public static final DeferredItem<Item> ROT_ROOT = ITEMS.register("rot_root",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> STAR_IRON = ITEMS.register("star_iron",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
    public static final DeferredItem<Item> SUN_AMBER = ITEMS.register("sun_amber",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<MedicalItem> ANTIDOTE = ITEMS.register("antidote",
            () -> new MedicalItem(MedicalItem.Kind.ANTIDOTE, new Item.Properties().stacksTo(8).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<RegionItems.MireStaff> MIRE_STAFF = ITEMS.register("mire_staff",
            () -> new RegionItems.MireStaff(new Item.Properties().durability(150).rarity(Rarity.EPIC)));
    public static final DeferredItem<RegionItems.StarBlade> STAR_BLADE = ITEMS.register("star_blade",
            () -> new RegionItems.StarBlade(new Item.Properties().fireResistant().rarity(Rarity.EPIC)
                    .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 6, -2.4f))));
    public static final DeferredItem<RegionItems.SunAmulet> SUN_AMULET = ITEMS.register("sun_amulet",
            () -> new RegionItems.SunAmulet(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<DeferredSpawnEggItem> MIRE_MOTHER_EGG = ITEMS.register("mire_mother_spawn_egg",
            () -> new DeferredSpawnEggItem(MIRE_MOTHER, 0x2F4A1C, 0x7FBF3F, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> FORGEMASTER_EGG = ITEMS.register("forgemaster_spawn_egg",
            () -> new DeferredSpawnEggItem(FORGEMASTER, 0x3A3A3E, 0xFF7A1A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> SCARAB_QUEEN_EGG = ITEMS.register("scarab_queen_spawn_egg",
            () -> new DeferredSpawnEggItem(SCARAB_QUEEN, 0xD9C27A, 0x2B6CB0, new Item.Properties()));

    // --- Структуры
    public static final DeferredHolder<StructureType<?>, StructureType<RegionStructure>> REGION_STRUCTURE = STRUCTURE_TYPES.register("region_dungeon",
            () -> regionType());
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SHRINE_PIECE = STRUCTURE_PIECES.register("sunken_shrine",
            () -> (StructurePieceType.ContextlessType) SunkenShrinePiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> FORTRESS_PIECE = STRUCTURE_PIECES.register("forge_fortress",
            () -> (StructurePieceType.ContextlessType) ForgeFortressPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> TOMB_PIECE = STRUCTURE_PIECES.register("sand_tomb",
            () -> (StructurePieceType.ContextlessType) SandTombPiece::new);

    private static StructureType<RegionStructure> regionType() {
        return () -> RegionStructure.CODEC;
    }

    @Override
    public String id() {
        return "regions";
    }

    @Override
    public String title() {
        return "Регионы: данжи и боссы";
    }

    @Override
    public void init(IEventBus modBus) {
        modBus.addListener(RegionsModule::attributes);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.alkimor.regnum.dungeon.boss.BossRules.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.alkimor.regnum.dungeon.boss.BossTactics.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.alkimor.regnum.dungeon.BossHook.class);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(MIRE_MOTHER.get(), MireMotherEntity.createAttributes().build());
        event.put(FORGEMASTER.get(), ForgemasterEntity.createAttributes().build());
        event.put(SCARAB_QUEEN.get(), ScarabQueenEntity.createAttributes().build());
    }
}
