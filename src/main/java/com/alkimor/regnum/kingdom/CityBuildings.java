package com.alkimor.regnum.kingdom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;

/**
 * Здания города из схем культуры: рядом с поставленным блоком постройки вырастает здание (стоимость — из казны).
 */
public final class CityBuildings {
    private CityBuildings() {}

    /** Категория/имя схемы для вида постройки. */
    private static String[] spec(BuildingType t) {
        return switch (t) {
            case BARRACKS -> new String[]{"military", "barracks"};
            case MARKET -> new String[]{"fundamentals", "tavern"};
            case WATCHTOWER -> new String[]{"military", "guardtower"};
            case TRAINING_GROUND -> new String[]{"military", "archery"};
            case BUILDER_HUT -> new String[]{"fundamentals", "builder"};
            case LIBRARY -> new String[]{"education", "library"};
            case UNIVERSITY -> new String[]{"education", "university"};
            case INFIRMARY -> new String[]{"fundamentals", "hospital"};
            case SMITHY -> new String[]{"craftsmanship", "metallurgy_blacksmith"};
            case WAREHOUSE -> new String[]{"craftsmanship", "storage_warehouse"};
            case STABLE -> new String[]{"craftsmanship", "carpentry_sawmill"};
        };
    }

    /** Лучший по уровню города вариант схемы (1/3/5), который существует. */
    public static String pick(ServerLevel l, City c, BuildingType t) {
        String[] sp = spec(t);
        String base = Culture.byId(c.culture).id + "/" + sp[0] + "/" + sp[1];
        int target = c.level >= 4 ? 5 : c.level >= 2 ? 3 : 1;
        for (int v : new int[]{5, 3, 1}) {
            if (v > target) continue;
            if (Prefab.exists(l, base + v)) return base + v;
        }
        return null;
    }

    /** Четверть поворота, при которой фасад схемы смотрит в сторону front. */
    private static int quarterFacing(Direction front) {
        return switch (front) {
            case SOUTH -> 0;
            case WEST -> 1;
            case NORTH -> 2;
            default -> 3;
        };
    }

    /** Результат: null — построено, иначе причина отказа. */
    public static String raise(ServerLevel l, City c, BuildingType t, BlockPos block, Direction look) {
        String id = pick(l, c, t);
        if (id == null) return null; // схемы нет — остаётся просто блок
        Direction d = look.getAxis().isHorizontal() ? look : Direction.NORTH;
        int q = quarterFacing(d.getOpposite());
        Rotation rot = Prefab.rotFor(q);
        BlockPos sz = Prefab.size(l, id, rot);
        int depth = d.getAxis() == Direction.Axis.X ? sz.getX() : sz.getZ();
        int cx = block.getX() + d.getStepX() * (depth / 2 + 2);
        int cz = block.getZ() + d.getStepZ() * (depth / 2 + 2);
        int hx = sz.getX() / 2, hz = sz.getZ() / 2;
        for (int[] s : new int[][]{{-hx, -hz}, {hx, -hz}, {-hx, hz}, {hx, hz}, {0, 0}}) {
            City at = Territory.cityAt(l, new BlockPos(cx + s[0], block.getY(), cz + s[1]));
            if (at != c) return "здание не помещается в границы города — отойдите от края.";
        }
        int cost = Math.max(1, Prefab.blocks(l, id) / (WallStyle.CASTLE.blocksPerCoin * 3));
        if (c.treasury < cost) return "для здания нужно " + cost + " в казне (сейчас " + c.treasury + ").";
        int g = Prefab.groundAt(l, cx, cz, hx, hz);
        c.treasury -= cost;
        Prefab.place(l, id, cx, g, cz, rot, WallKit.of(c.culture).foundation, Integer.MAX_VALUE);
        return null;
    }
}
