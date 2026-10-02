package com.alkimor.regnum.trade;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/** Торговые регионы: определяются биомом рынка. От региона зависят цены товаров. */
public enum Region {
    PLAINS("равнины"),
    COLD("северные земли"),
    HOT_DRY("пустыни и саванны"),
    JUNGLE("джунгли"),
    COAST("побережье"),
    MOUNTAINS("горы"),
    SWAMP("болота");

    public final String title;

    Region(String title) {
        this.title = title;
    }

    public static Region byId(int id) {
        Region[] v = values();
        return id >= 0 && id < v.length ? v[id] : PLAINS;
    }

    public static Region at(Level level, BlockPos pos) {
        Holder<Biome> b = level.getBiome(pos);
        if (b.is(Biomes.SWAMP) || b.is(Biomes.MANGROVE_SWAMP)) return SWAMP;
        if (b.is(BiomeTags.IS_JUNGLE)) return JUNGLE;
        if (b.is(Biomes.DESERT) || b.is(BiomeTags.IS_BADLANDS) || b.is(BiomeTags.IS_SAVANNA)) return HOT_DRY;
        if (b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_BEACH) || b.is(BiomeTags.IS_RIVER)) return COAST;
        if (b.is(BiomeTags.IS_MOUNTAIN) || b.is(BiomeTags.IS_HILL)) return MOUNTAINS;
        if (b.is(BiomeTags.IS_TAIGA) || b.value().getBaseTemperature() < 0.3f) return COLD;
        return PLAINS;
    }
}
