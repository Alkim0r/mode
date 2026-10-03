package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.RegnumModule;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import static com.alkimor.regnum.core.ModRegistries.*;

/** Модуль «Королевство»: города, постройки, армия, приказы, набеги. */
public class KingdomModule implements RegnumModule {

    // --- Сущности
    public static final DeferredHolder<EntityType<?>, EntityType<SoldierEntity>> SOLDIER = ENTITIES.register("soldier",
            () -> EntityType.Builder.of(SoldierEntity::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(10).build("soldier"));
    public static final DeferredHolder<EntityType<?>, EntityType<BanditEntity>> BANDIT = ENTITIES.register("bandit",
            () -> EntityType.Builder.of(BanditEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8).build("bandit"));

    public static final DeferredHolder<EntityType<?>, EntityType<CatapultEntity>> CATAPULT = ENTITIES.register("catapult",
            () -> EntityType.Builder.of(CatapultEntity::new, MobCategory.MISC).sized(1.9f, 2.6f).clientTrackingRange(12).build("catapult"));
    public static final DeferredHolder<EntityType<?>, EntityType<CaravanEntity>> CARAVAN = ENTITIES.register("caravan",
            () -> EntityType.Builder.of(CaravanEntity::new, MobCategory.CREATURE).sized(0.6f, 1.95f).clientTrackingRange(10).build("caravan"));
    public static final DeferredHolder<EntityType<?>, EntityType<SiegeTowerEntity>> SIEGE_TOWER = ENTITIES.register("siege_tower",
            () -> EntityType.Builder.of(SiegeTowerEntity::new, MobCategory.MISC).sized(2.9f, 9.0f).clientTrackingRange(16).build("siege_tower"));
    public static final DeferredHolder<EntityType<?>, EntityType<SiegeBoulderEntity>> SIEGE_BOULDER = ENTITIES.register("siege_boulder",
            () -> EntityType.Builder.<SiegeBoulderEntity>of(SiegeBoulderEntity::new, MobCategory.MISC).sized(0.6f, 0.6f).clientTrackingRange(12).updateInterval(2).build("siege_boulder"));

    // --- Блоки
    private static BlockBehaviour.Properties buildingProps(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(4.0f, 12.0f).sound(SoundType.WOOD);
    }

    public static final DeferredBlock<TownHallBlock> TOWN_HALL = BLOCKS.register("town_hall",
            () -> new TownHallBlock(buildingProps(MapColor.GOLD).strength(6.0f, 1200.0f)));
    public static final DeferredBlock<BuildingBlock> BARRACKS = BLOCKS.register("barracks",
            () -> new BuildingBlock(BuildingType.BARRACKS, buildingProps(MapColor.COLOR_RED)));
    public static final DeferredBlock<BuildingBlock> MARKET = BLOCKS.register("market",
            () -> new BuildingBlock(BuildingType.MARKET, buildingProps(MapColor.EMERALD)));
    public static final DeferredBlock<BuildingBlock> WATCHTOWER = BLOCKS.register("watchtower",
            () -> new BuildingBlock(BuildingType.WATCHTOWER, buildingProps(MapColor.STONE)));

    public static final DeferredBlock<BuildingBlock> TRAINING_GROUND = BLOCKS.register("training_ground",
            () -> new BuildingBlock(BuildingType.TRAINING_GROUND, buildingProps(MapColor.WOOD)));
    public static final DeferredBlock<BuildingBlock> BUILDER_HUT = BLOCKS.register("builder_hut",
            () -> new BuildingBlock(BuildingType.BUILDER_HUT, buildingProps(MapColor.WOOD)));
    public static final DeferredItem<BlockItem> BUILDER_HUT_ITEM = ITEMS.registerSimpleBlockItem("builder_hut", BUILDER_HUT);
    public static final DeferredItem<BuilderPlanItem> BUILDER_PLAN = ITEMS.register("builder_plan",
            () -> new BuilderPlanItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BlockItem> TRAINING_GROUND_ITEM = ITEMS.registerSimpleBlockItem("training_ground", TRAINING_GROUND);
    public static final DeferredItem<BlockItem> TOWN_HALL_ITEM = ITEMS.registerSimpleBlockItem("town_hall", TOWN_HALL, new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredBlock<BuildingBlock> LIBRARY = BLOCKS.register("library",
            () -> new BuildingBlock(BuildingType.LIBRARY, buildingProps(MapColor.COLOR_BROWN)));
    public static final DeferredBlock<BuildingBlock> UNIVERSITY = BLOCKS.register("university",
            () -> new BuildingBlock(BuildingType.UNIVERSITY, buildingProps(MapColor.COLOR_PURPLE)));
    public static final DeferredBlock<BuildingBlock> INFIRMARY = BLOCKS.register("infirmary",
            () -> new BuildingBlock(BuildingType.INFIRMARY, buildingProps(MapColor.SNOW)));
    public static final DeferredBlock<BuildingBlock> SMITHY = BLOCKS.register("smithy",
            () -> new BuildingBlock(BuildingType.SMITHY, buildingProps(MapColor.METAL)));
    public static final DeferredBlock<BuildingBlock> WAREHOUSE = BLOCKS.register("warehouse",
            () -> new BuildingBlock(BuildingType.WAREHOUSE, buildingProps(MapColor.WOOD)));
    public static final DeferredBlock<BuildingBlock> STABLE = BLOCKS.register("stable",
            () -> new BuildingBlock(BuildingType.STABLE, buildingProps(MapColor.DIRT)));
    public static final DeferredItem<BlockItem> LIBRARY_ITEM = ITEMS.registerSimpleBlockItem("library", LIBRARY);
    public static final DeferredItem<BlockItem> UNIVERSITY_ITEM = ITEMS.registerSimpleBlockItem("university", UNIVERSITY);
    public static final DeferredItem<BlockItem> INFIRMARY_ITEM = ITEMS.registerSimpleBlockItem("infirmary", INFIRMARY);
    public static final DeferredItem<BlockItem> SMITHY_ITEM = ITEMS.registerSimpleBlockItem("smithy", SMITHY);
    public static final DeferredItem<BlockItem> WAREHOUSE_ITEM = ITEMS.registerSimpleBlockItem("warehouse", WAREHOUSE);
    public static final DeferredItem<BlockItem> STABLE_ITEM = ITEMS.registerSimpleBlockItem("stable", STABLE);
    public static final DeferredItem<BlockItem> BARRACKS_ITEM = ITEMS.registerSimpleBlockItem("barracks", BARRACKS);
    public static final DeferredItem<BlockItem> MARKET_ITEM = ITEMS.registerSimpleBlockItem("market", MARKET);
    public static final DeferredItem<BlockItem> WATCHTOWER_ITEM = ITEMS.registerSimpleBlockItem("watchtower", WATCHTOWER);

    // --- Предметы
    public static final DeferredItem<CommanderBatonItem> COMMANDER_BATON = ITEMS.register("commander_baton",
            () -> new CommanderBatonItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<DeferredSpawnEggItem> BANDIT_EGG = ITEMS.register("bandit_spawn_egg",
            () -> new DeferredSpawnEggItem(BANDIT, 0x4A3B2A, 0x8B1A1A, new Item.Properties()));

    // --- Компоненты жезла
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BATON_SQUAD = COMPONENTS.registerComponentType("baton_squad",
            b -> b.persistent(Codec.intRange(0, 4)).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BATON_FORMATION = COMPONENTS.registerComponentType("baton_formation",
            b -> b.persistent(Codec.intRange(0, 3)).networkSynchronized(ByteBufCodecs.VAR_INT));

    @Override
    public String id() {
        return "kingdom";
    }

    @Override
    public String title() {
        return "Королевство";
    }

    @Override
    public void init(IEventBus modBus) {
        CultureWeapons.init();
        modBus.addListener(KingdomModule::attributes);
        NeoForge.EVENT_BUS.register(KingdomEvents.class);
        NeoForge.EVENT_BUS.register(WeaponPerks.class);
        NeoForge.EVENT_BUS.register(Territory.class);
        NeoForge.EVENT_BUS.register(Prisoners.class);
        NeoForge.EVENT_BUS.register(Exchange.class);
        NeoForge.EVENT_BUS.register(Labor.class);
        NeoForge.EVENT_BUS.register(Priority.class);
        NeoForge.EVENT_BUS.register(CaravanRoute.class);
        NeoForge.EVENT_BUS.register(Mood.class);
        NeoForge.EVENT_BUS.register(Sortie.class);
        NeoForge.EVENT_BUS.register(FireControl.class);
        NeoForge.EVENT_BUS.register(Arena3.class);
        NeoForge.EVENT_BUS.register(Ruins.class);
        NeoForge.EVENT_BUS.register(SquadRoles.class);
        NeoForge.EVENT_BUS.register(Specialization.class);
        NeoForge.EVENT_BUS.register(Discoveries.class);
        NeoForge.EVENT_BUS.register(Walls.class);
        NeoForge.EVENT_BUS.register(Realms.class);
        NeoForge.EVENT_BUS.register(Campaign.class);
        NeoForge.EVENT_BUS.register(Combat.class);
        NeoForge.EVENT_BUS.register(Espionage.class);
        NeoForge.EVENT_BUS.register(Science.class);
        NeoForge.EVENT_BUS.register(Industry.class);
        NeoForge.EVENT_BUS.register(Diplomacy.class);
        NeoForge.EVENT_BUS.register(Contracts.class);
        NeoForge.EVENT_BUS.register(Fortune.class);
        NeoForge.EVENT_BUS.register(Legends.class);
        NeoForge.EVENT_BUS.register(Council.class);
        NeoForge.EVENT_BUS.register(BattleReport.class);
        NeoForge.EVENT_BUS.register(Marks.class);
        NeoForge.EVENT_BUS.register(FieldCommand.class);
        NeoForge.EVENT_BUS.register(Health.class);
        NeoForge.EVENT_BUS.register(Events.class);
        NeoForge.EVENT_BUS.register(VillageAid.class);
        NeoForge.EVENT_BUS.register(Villages.class);
        NeoForge.EVENT_BUS.register(PrefabCommands.class);
        NeoForge.EVENT_BUS.addListener(KingdomManager::onServerTick);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(SOLDIER.get(), SoldierEntity.createAttributes().build());
        event.put(BANDIT.get(), BanditEntity.createAttributes().build());
        event.put(CARAVAN.get(), CaravanEntity.createAttributes().build());
        event.put(CATAPULT.get(), CatapultEntity.createAttributes().build());
        event.put(SIEGE_TOWER.get(), SiegeTowerEntity.createAttributes().build());
    }
}
