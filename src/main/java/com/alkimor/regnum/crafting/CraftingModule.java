package com.alkimor.regnum.crafting;

import com.alkimor.regnum.core.RegnumModule;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import static com.alkimor.regnum.core.ModRegistries.BLOCKS;
import static com.alkimor.regnum.core.ModRegistries.COMPONENTS;
import static com.alkimor.regnum.core.ModRegistries.ITEMS;

/** Модуль «Ремёсла». Первое ремесло — кузнечное дело (закалка оружия и брони). */
public class CraftingModule implements RegnumModule {

    /** Ступень закалки предмета 0..5. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TEMPER = COMPONENTS.registerComponentType("temper",
            b -> b.persistent(Codec.intRange(0, Tempering.MAX_TEMPER)).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredBlock<MasterForgeBlock> MASTER_FORGE = BLOCKS.register("master_forge",
            () -> new MasterForgeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5f)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE).lightLevel(s -> 12)));
    public static final DeferredItem<BlockItem> MASTER_FORGE_ITEM = ITEMS.registerSimpleBlockItem("master_forge", MASTER_FORGE);

    public static final DeferredItem<MasteryScrollItem> MASTERY_SCROLL = ITEMS.register("mastery_scroll",
            () -> new MasteryScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    @Override
    public String id() {
        return "crafting";
    }

    @Override
    public String title() {
        return "Ремёсла";
    }

    @Override
    public void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(CraftingEvents.class);
    }
}
