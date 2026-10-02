package com.alkimor.regnum.kingdom;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/** Культура королевства: облик войск, гербы, оружие (как фракции Bannerlord). */
public enum Culture {
    NORTH("north", "Северное княжество", "шишаки, чешуя, каплевидные щиты"),
    EMPIRE("empire", "Аврелийская империя", "галеа, сегментата, скутумы"),
    WEST("west", "Королевство Вальдмарк", "шапели, топфхельмы, гербовые сюрко"),
    STEPPE("steppe", "Степная орда", "остроконечные шлемы, ламелляр, халаты"),
    SULTANATE("sultanate", "Песчаный султанат", "тюрбаны, кольчужные бармицы, халаты"),
    CLANS("clans", "Лесные кланы", "косы, синяя краска, клетчатые килты");

    public final String id;
    public final String title;
    public final String look;

    Culture(String id, String title, String look) {
        this.id = id;
        this.title = title;
        this.look = look;
    }

    public static Culture byId(int i) {
        Culture[] v = values();
        return v[Math.floorMod(i, v.length)];
    }

    /** Культура по умолчанию — по биому, где основан город. */
    public static Culture forBiome(Holder<Biome> b) {
        if (b.is(Biomes.DESERT) || b.is(BiomeTags.IS_BADLANDS)) return SULTANATE;
        if (b.is(BiomeTags.IS_SAVANNA) || b.is(Biomes.WINDSWEPT_SAVANNA)) return STEPPE;
        if (b.is(BiomeTags.IS_TAIGA) || b.is(Biomes.SNOWY_PLAINS) || b.is(Biomes.ICE_SPIKES) || b.is(Biomes.GROVE)) return NORTH;
        if (b.is(BiomeTags.IS_JUNGLE) || b.is(Biomes.MEADOW) || b.is(BiomeTags.IS_BEACH)) return EMPIRE;
        if (b.is(Biomes.DARK_FOREST) || b.is(Biomes.OLD_GROWTH_BIRCH_FOREST) || b.is(Biomes.SWAMP) || b.is(Biomes.MANGROVE_SWAMP)
                || b.is(Biomes.FLOWER_FOREST)) return CLANS;
        if (b.is(Biomes.SUNFLOWER_PLAINS) || b.is(Biomes.PLAINS) || b.is(Biomes.FOREST) || b.is(Biomes.BIRCH_FOREST)) return WEST;
        // чужие биомы (Terralith, Regions Unexplored и т.п.): по климату и типу местности
        float t = b.value().getBaseTemperature();
        if (t <= 0.25f) return NORTH;
        if (t >= 1.5f) return SULTANATE;
        if (t >= 0.95f) return b.value().hasPrecipitation() ? EMPIRE : STEPPE;
        if (b.is(BiomeTags.IS_MOUNTAIN) || b.is(BiomeTags.IS_HILL) || b.is(BiomeTags.IS_FOREST)) return CLANS;
        return WEST;
    }
}
