package com.alkimor.regnum.trade;

import com.alkimor.regnum.core.RegnumModule;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;
import java.util.Map;

import static com.alkimor.regnum.core.ModRegistries.COMPONENTS;
import static com.alkimor.regnum.core.ModRegistries.ITEMS;

/** Модуль «Торговля»: товары-грузы, региональные цены, рынки, купцы, засады на караваны. */
public class TradeModule implements RegnumModule {

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TradeOrigin>> ORIGIN = COMPONENTS.registerComponentType("trade_origin",
            b -> b.persistent(TradeOrigin.CODEC).networkSynchronized(TradeOrigin.STREAM_CODEC));

    public static final Map<TradeGood, DeferredItem<TradeGoodItem>> GOODS = new EnumMap<>(TradeGood.class);

    static {
        for (TradeGood g : TradeGood.values()) {
            GOODS.put(g, ITEMS.register(g.id, () -> new TradeGoodItem(g, new Item.Properties().stacksTo(16))));
        }
    }

    @Override
    public String id() {
        return "trade";
    }

    @Override
    public String title() {
        return "Торговля";
    }

    @Override
    public void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(TradeEvents.class);
        NeoForge.EVENT_BUS.register(TradeNews.class);
    }
}
