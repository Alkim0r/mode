package com.alkimor.regnum.kingdom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Столица королевства из готовых построек культуры: крепостная стена с башнями и двумя воротами,
 * замок-ратуша в центре и кольцо городских зданий.
 */
public final class Capital {
    private Capital() {}

    /** Полусторона квадрата стены (от центра до оси стены). */
    public static final int HALF = 44;
    /** Радиус площадки, которую нужно выровнять. */
    public static final int PAD = HALF + 12;

    /** Выравнивает площадку (2·PAD+1)² до уровня g: срезает деревья и холмы, засыпает ямы. */
    public static void terraform(ServerLevel ow, int cx, int cz, int g) {
        for (int x = cx - PAD; x <= cx + PAD; x++) {
            for (int z = cz - PAD; z <= cz + PAD; z++) {
                ow.getChunk(x >> 4, z >> 4);
                for (int y = g + 26; y > g; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!ow.getBlockState(p).isAir()) ow.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
                }
                for (int y = g; y >= g - 10; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState cur = ow.getBlockState(p);
                    boolean soft = cur.isAir() || cur.canBeReplaced() || !cur.getFluidState().isEmpty()
                            || cur.is(net.minecraft.tags.BlockTags.LEAVES) || cur.is(net.minecraft.tags.BlockTags.LOGS);
                    if (y == g) ow.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2 | 16);
                    else if (soft) ow.setBlock(p, Blocks.DIRT.defaultBlockState(), 2 | 16);
                    else break;
                }
            }
        }
    }

    /** Ставит схему в точку с учётом уровня земли g (площадка уже ровная). */
    private static void put(ServerLevel ow, String id, int x, int z, int g, int quarter, net.minecraft.world.level.block.Block found) {
        if (id == null || !Prefab.exists(ow, id)) return;
        Prefab.place(ow, id, x, g, z, Prefab.rotFor(quarter), found, Integer.MAX_VALUE);
    }

    /** Строит всё; возвращает точку входа в замок (где встанет правитель). */
    public static BlockPos build(ServerLevel ow, Realm r, int cx, int g, int cz) {
        Culture cu = Culture.byId(r.culture);
        WallKit kit = WallKit.of(r.culture);
        int h = HALF;
        // стена: по часовой, ворота на севере и юге
        List<int[]> pts = new ArrayList<>(List.of(new int[]{cx - h, cz - h}, new int[]{cx, cz - h}, new int[]{cx + h, cz - h},
                new int[]{cx + h, cz + h}, new int[]{cx, cz + h}, new int[]{cx - h, cz + h}));
        List<Boolean> gates = List.of(false, true, false, false, true, false);
        List<WallKit.Op> ops = kit.plan(pts, gates, true);
        for (WallKit.Op o : ops) {
            if (!Prefab.exists(ow, o.id())) continue;
            put(ow, o.id(), o.x(), o.z(), g, o.quarter(), kit.foundation);
        }
        // замок в центре
        String keep = Prefab.find(ow, cu.id, "fundamentals", "townhall", 34);
        put(ow, keep, cx, cz, g, 0, kit.foundation);
        // кольцо городских зданий
        String[] kw = {"tavern", "residence", "blacksmith", "library", "residence", "hospital", "warehouse", "residence"};
        String[] cat = {"fundamentals", "fundamentals", "craftsmanship", "education", "fundamentals", "fundamentals", "craftsmanship", "fundamentals"};
        int[][] slot = {{0, -27}, {26, -26}, {28, 0}, {26, 26}, {0, 27}, {-26, 26}, {-28, 0}, {-26, -26}};
        for (int i = 0; i < slot.length; i++) {
            String id = Prefab.find(ow, cu.id, cat[i], kw[i], 22);
            if (id == null) id = Prefab.find(ow, cu.id, "fundamentals", "residence", 22);
            int dx = slot[i][0], dz = slot[i][1];
            int q = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? 1 : 3) : (dz > 0 ? 2 : 0);
            put(ow, id, cx + dx, cz + dz, g, q, kit.foundation);
        }
        // вход: от южных ворот к замку
        return new BlockPos(cx, ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz + 17), cz + 17);
    }
}
