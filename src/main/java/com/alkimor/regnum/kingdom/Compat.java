package com.alkimor.regnum.kingdom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

import java.util.HashSet;
import java.util.Set;

/**
 * Мягкая совместимость с Create и Create Aeronautics: без них мод работает как обычно (ничего не загружается), с ними
 * механизмы рядом с ратушей, кузницей и складом дают «технологическое преимущество», а воздушные военачальники
 * переносят экипаж вместе с собой на дирижабль/самолёт.
 */
public final class Compat {
    private Compat() {}

    public static boolean create() {
        return loaded("create");
    }

    public static boolean aeronautics() {
        return loaded("aeronautics") || loaded("simulated") || loaded("create_aeronautics");
    }

    private static boolean loaded(String id) {
        try {
            return ModList.get() != null && ModList.get().isLoaded(id);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isTechNamespace(String ns) {
        return ns.equals("create") || ns.equals("aeronautics") || ns.equals("simulated") || ns.equals("create_aeronautics")
                || ns.startsWith("create_") || ns.equals("sable");
    }

    public static boolean isTechBlock(BlockState s) {
        if (s.isAir()) return false;
        return isTechNamespace(BuiltInRegistries.BLOCK.getKey(s.getBlock()).getNamespace());
    }

    /** Сколько «механических» блоков вокруг ратуши, кузниц и складов города. */
    public static int techBlocks(ServerLevel ow, City c) {
        if (!create() && !aeronautics()) return 0;
        Set<Long> seen = new HashSet<>();
        int n = 0;
        java.util.List<BlockPos> centers = new java.util.ArrayList<>();
        centers.add(c.hall);
        for (var e : c.buildings.entrySet()) {
            if (e.getValue() == BuildingType.SMITHY || e.getValue() == BuildingType.WAREHOUSE) centers.add(BlockPos.of(e.getKey()));
        }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (BlockPos ctr : centers) {
            if (!ow.isLoaded(ctr)) continue;
            for (int dx = -6; dx <= 6; dx++) for (int dy = -3; dy <= 4; dy++) for (int dz = -6; dz <= 6; dz++) {
                m.set(ctr.getX() + dx, ctr.getY() + dy, ctr.getZ() + dz);
                if (!ow.hasChunkAt(m)) continue;
                if (!seen.add(m.asLong())) continue;
                if (isTechBlock(ow.getBlockState(m))) n++;
            }
        }
        return n;
    }

    /** Уровень механизации 0..3 (по 10 блоков на уровень). */
    public static int mechanization(ServerLevel ow, City c) {
        return Math.min(3, techBlocks(ow, c) / 10);
    }
}
