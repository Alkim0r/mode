package com.alkimor.regnum.trade;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Рынки на блоках «Рынок» (по позиции). */
public class TradeData extends SavedData {
    private final Map<Long, MarketState> markets = new HashMap<>();

    public static TradeData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(TradeData::new, TradeData::load), "regnum_trade");
    }

    private static TradeData load(CompoundTag tag, HolderLookup.Provider provider) {
        TradeData d = new TradeData();
        CompoundTag m = tag.getCompound("markets");
        for (String k : m.getAllKeys()) {
            try {
                d.markets.put(Long.parseLong(k), MarketState.load(m.getCompound(k)));
            } catch (NumberFormatException ignored) {
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag m = new CompoundTag();
        markets.forEach((k, v) -> m.put(Long.toString(k), v.save()));
        tag.put("markets", m);
        return tag;
    }

    public MarketState market(long key, long day) {
        MarketState s = markets.computeIfAbsent(key, k -> new MarketState(day));
        s.decay(day);
        setDirty();
        return s;
    }
}
