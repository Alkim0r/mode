package com.alkimor.regnum.wanderers;

import com.alkimor.regnum.core.RegnumModule;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import static com.alkimor.regnum.core.ModRegistries.*;

/** Модуль «Странники»: NPC, которые помогают, обманывают, заманивают в ловушки и испытывают. */
public class WanderersModule implements RegnumModule {

    public static final DeferredHolder<EntityType<?>, EntityType<WandererEntity>> WANDERER = ENTITIES.register("wanderer",
            () -> EntityType.Builder.of(WandererEntity::new, MobCategory.CREATURE).sized(0.6f, 1.95f).clientTrackingRange(10).build("wanderer"));

    public static final DeferredItem<TrickElixirItem> TRICK_ELIXIR = ITEMS.register("trick_elixir",
            () -> new TrickElixirItem(new Item.Properties().stacksTo(4)));
    public static final DeferredItem<DeferredSpawnEggItem> WANDERER_EGG = ITEMS.register("wanderer_spawn_egg",
            () -> new DeferredSpawnEggItem(WANDERER, 0x6B4E2E, 0xD9C27A, new Item.Properties()));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TreasureMark>> TREASURE_MARK = COMPONENTS.registerComponentType("treasure_mark",
            b -> b.persistent(TreasureMark.CODEC).networkSynchronized(TreasureMark.STREAM_CODEC));

    @Override
    public String id() {
        return "wanderers";
    }

    @Override
    public String title() {
        return "Странники";
    }

    @Override
    public void init(IEventBus modBus) {
        modBus.addListener(WanderersModule::attributes);
        NeoForge.EVENT_BUS.register(WandererEvents.class);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(WANDERER.get(), WandererEntity.createAttributes().build());
    }
}
