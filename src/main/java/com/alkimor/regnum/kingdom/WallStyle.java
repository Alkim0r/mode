package com.alkimor.regnum.kingdom;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Виды стен, которые строят рабочие. Материал зависит от культуры города (порядок как в {@link Culture}). */
public enum WallStyle {
    PALISADE("Частокол", 4, 0, 4, "Брёвна в ряд. Дёшево и быстро, держит от зверей и мелких шаек."),
    WOOD("Деревянная стена", 5, 1, 3, "Доски со столбами. Выше частокола, горит."),
    STONE("Каменная стена", 6, 2, 2, "Очень толстая каменная стена с ходом для лучников."),
    FORTRESS("Крепостная стена", 7, 2, 2, "Высокая каменная стена с зубцами. Держит осаду."),
    CASTLE("Крепость культуры", 12, 0, 20, "Настоящие башни, ворота и стены вашей культуры. Строится по кускам, очень дорого.");

    public final String title;
    public final int height;
    /** Радиус утолщения вокруг линии: 0 — в один блок, 1 — в три, 2 — в пять. */
    public final int radius;
    /** Блоков на одну монету казны. */
    public final int blocksPerCoin;
    public final String desc;

    WallStyle(String title, int height, int radius, int blocksPerCoin, String desc) {
        this.title = title;
        this.height = height;
        this.radius = radius;
        this.blocksPerCoin = blocksPerCoin;
        this.desc = desc;
    }

    private static final Block[] STONE_MAIN = {Blocks.STONE_BRICKS, Blocks.BRICKS, Blocks.STONE_BRICKS, Blocks.COBBLESTONE,
            Blocks.CUT_SANDSTONE, Blocks.MOSSY_STONE_BRICKS};
    private static final Block[] PLANKS = {Blocks.SPRUCE_PLANKS, Blocks.OAK_PLANKS, Blocks.OAK_PLANKS, Blocks.DARK_OAK_PLANKS,
            Blocks.ACACIA_PLANKS, Blocks.SPRUCE_PLANKS};
    private static final Block[] LOGS = {Blocks.SPRUCE_LOG, Blocks.OAK_LOG, Blocks.OAK_LOG, Blocks.DARK_OAK_LOG,
            Blocks.ACACIA_LOG, Blocks.SPRUCE_LOG};

    private static int c(int culture) {
        return Math.floorMod(culture, 6);
    }

    /** Блок на высоте yOff ярусов от земли; null — в этой клетке пусто (зубец, проём). */
    public Block block(int culture, int yOff, int x, int z, int cellIdx) {
        int cu = c(culture);
        boolean top = yOff == height - 1;
        return switch (this) {
            case PALISADE -> LOGS[cu];
            case WOOD -> cellIdx % 4 == 0 ? LOGS[cu] : PLANKS[cu];
            case STONE -> yOff == 0 ? Blocks.COBBLESTONE : STONE_MAIN[cu];
            case CASTLE -> Blocks.STONE_BRICKS;
            case FORTRESS -> {
                if (top && ((x + z) & 1) == 0) yield null;
                yield yOff == 0 ? Blocks.STONE_BRICKS : STONE_MAIN[cu];
            }
        };
    }

    public static Block planksOf(int culture) {
        return PLANKS[c(culture)];
    }

    public static Block logOf(int culture) {
        return LOGS[c(culture)];
    }

    public static Block stoneOf(int culture) {
        return STONE_MAIN[c(culture)];
    }

    /** Технология, открывающая вид стены (null — доступна сразу). */
    public Science.Tech tech() {
        return switch (this) {
            case STONE -> Science.Tech.MASONRY;
            case FORTRESS -> Science.Tech.CONSTRUCTION;
            case CASTLE -> Science.Tech.ENGINEERING;
            default -> null;
        };
    }

    public static WallStyle byOrdinal(int i) {
        WallStyle[] v = values();
        return v[Math.floorMod(i, v.length)];
    }
}
