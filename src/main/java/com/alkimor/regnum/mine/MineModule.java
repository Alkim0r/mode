package com.alkimor.regnum.mine;

import com.alkimor.regnum.core.RegnumModule;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import static com.alkimor.regnum.core.ModRegistries.ENTITIES;
import static com.alkimor.regnum.core.ModRegistries.ITEMS;

/** Тёмные шахты: стаи шахтных ползунов в глубоких пещерах и Королева ползунов в самой глубине. */
public class MineModule implements RegnumModule {
    public static final DeferredHolder<EntityType<?>, EntityType<CrawlerEntity>> CRAWLER = ENTITIES.register("crawler",
            () -> EntityType.Builder.of(CrawlerEntity::new, MobCategory.MONSTER).sized(1.1f, 0.8f).clientTrackingRange(8).build("crawler"));
    public static final DeferredHolder<EntityType<?>, EntityType<CrawlerQueenEntity>> CRAWLER_QUEEN = ENTITIES.register("crawler_queen",
            () -> EntityType.Builder.of(CrawlerQueenEntity::new, MobCategory.MONSTER).sized(4.2f, 2.85f).clientTrackingRange(12).build("crawler_queen"));

    public static final DeferredItem<DeferredSpawnEggItem> CRAWLER_EGG = ITEMS.register("crawler_spawn_egg",
            () -> new DeferredSpawnEggItem(CRAWLER, 0x1B1712, 0xB5651D, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> CRAWLER_QUEEN_EGG = ITEMS.register("crawler_queen_spawn_egg",
            () -> new DeferredSpawnEggItem(CRAWLER_QUEEN, 0x0E0C0A, 0xD94E1F, new Item.Properties()));
    /** Хитиновая пластина — трофей с ползунов, прочный материал для будущих рецептов. */
    public static final DeferredItem<Item> CHITIN_PLATE = ITEMS.register("chitin_plate", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    @Override
    public String id() {
        return "mine";
    }

    @Override
    public String title() {
        return "Тёмные шахты";
    }

    @Override
    public void init(IEventBus modBus) {
        modBus.addListener(MineModule::attributes);
        NeoForge.EVENT_BUS.register(MineHorror.class);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(CRAWLER.get(), CrawlerEntity.createAttributes().build());
        event.put(CRAWLER_QUEEN.get(), CrawlerQueenEntity.createAttributes().build());
    }
}
