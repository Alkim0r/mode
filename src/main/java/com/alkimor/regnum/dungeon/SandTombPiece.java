package com.alkimor.regnum.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Песчаная гробница: ступенчатая пирамида-вход, лестница вниз, ниши с мумиями и тронный зал царицы. */
public class SandTombPiece extends ProceduralPiece {
    private static final int DEPTH = 10;

    public SandTombPiece(BlockPos surface) {
        super(RegionsModule.TOMB_PIECE.get(), surface, new BoundingBox(surface.getX() - 11, surface.getY() - 1 - DEPTH - 1, surface.getZ() - 7,
                surface.getX() + 11, surface.getY() + 7, surface.getZ() + 45));
    }

    public SandTombPiece(CompoundTag tag) {
        super(RegionsModule.TOMB_PIECE.get(), tag);
    }

    private static BlockState wall(RandomSource r) {
        int n = r.nextInt(10);
        if (n < 5) return Blocks.SANDSTONE.defaultBlockState();
        if (n < 8) return Blocks.CUT_SANDSTONE.defaultBlockState();
        if (n < 9) return Blocks.SMOOTH_SANDSTONE.defaultBlockState();
        return Blocks.CHISELED_SANDSTONE.defaultBlockState();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource rnd, BoundingBox cb, ChunkPos chunk, BlockPos pivot) {
        int f = g - DEPTH;
        // ступенчатая пирамида
        foundation(level, cb, cx - 5, cz - 5, cx + 5, cz + 5, g - 1, 4, Blocks.SANDSTONE.defaultBlockState());
        for (int layer = 0; layer <= 5; layer++) {
            int r = 5 - layer, y = g + 1 + layer;
            for (int x = cx - r; x <= cx + r; x++)
                for (int z = cz - r; z <= cz + r; z++) {
                    boolean edge = Math.abs(x - cx) == r || Math.abs(z - cz) == r;
                    set(level, cb, x, y, z, edge ? wall(rnd) : Blocks.AIR.defaultBlockState());
                }
        }
        fill(level, cb, cx - 4, g, cz - 4, cx + 4, g, cz + 4, Blocks.SMOOTH_SANDSTONE.defaultBlockState());
        fill(level, cb, cx, g + 1, cz - 5, cx, g + 2, cz - 5, Blocks.AIR.defaultBlockState());
        set(level, cb, cx - 1, g + 3, cz - 6, Blocks.ORANGE_TERRACOTTA.defaultBlockState());
        set(level, cb, cx + 1, g + 3, cz - 6, Blocks.ORANGE_TERRACOTTA.defaultBlockState());

        // лестница вниз (на юг)
        BlockState stair = Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        for (int k = 0; k < DEPTH; k++) {
            int z = cz - 3 + k, y = g - k;
            for (int x = cx - 1; x <= cx + 1; x++) {
                set(level, cb, x, y - 1, z, wall(rnd));
                set(level, cb, x, y, z, stair);
                for (int h = 1; h <= 4; h++) if (y + h <= g || k < 3) set(level, cb, x, y + h, z, Blocks.AIR.defaultBlockState());
                if (y + 5 <= g) set(level, cb, x, y + 5, z, wall(rnd));
            }
            for (int h = 0; h <= 4; h++) {
                if (y + h <= g) {
                    set(level, cb, cx - 2, y + h, z, wall(rnd));
                    set(level, cb, cx + 2, y + h, z, wall(rnd));
                }
            }
        }

        // лабиринт: 23 x 17, ловушки (плита над динамитом), тупики с сундуками и мумиями
        int c0 = cz - 3 + DEPTH;
        maze(level, cb, rnd, c0, f);

        // тронный зал
        int h0 = c0 + 16, h1 = cz + 43;
        room(level, cb, rnd, cx - 10, f, h0, cx + 10, f + 8, h1, SandTombPiece::wall);
        for (int x = cx - 9; x <= cx + 9; x++)
            for (int z = h0 + 1; z <= h1 - 1; z++)
                set(level, cb, x, f, z, (Math.abs(x - cx) + Math.abs(z - (h0 + h1) / 2)) % 4 == 0
                        ? Blocks.ORANGE_TERRACOTTA.defaultBlockState() : Blocks.CUT_SANDSTONE.defaultBlockState());
        fill(level, cb, cx - 1, f + 1, h0, cx + 1, f + 3, h0, Blocks.CAVE_AIR.defaultBlockState());
        int[][] pillars = {{-6, 5}, {6, 5}, {-6, 14}, {6, 14}};
        for (int[] p : pillars) {
            for (int y = f + 1; y <= f + 7; y++) set(level, cb, cx + p[0], y, h0 + p[1], Blocks.CHISELED_SANDSTONE.defaultBlockState());
            set(level, cb, cx + p[0] + (p[0] < 0 ? 1 : -1), f + 1, h0 + p[1], Blocks.LANTERN.defaultBlockState());
        }
        fill(level, cb, cx - 3, f + 1, h1 - 6, cx + 3, f + 1, h1 - 1, Blocks.SMOOTH_SANDSTONE.defaultBlockState());
        set(level, cb, cx, f + 2, h1 - 4, RegionsModule.SUN_ALTAR.get().defaultBlockState());
        set(level, cb, cx - 2, f + 2, h1 - 2, Blocks.GOLD_BLOCK.defaultBlockState());
        set(level, cb, cx + 2, f + 2, h1 - 2, Blocks.GOLD_BLOCK.defaultBlockState());
        chest(level, cb, rnd, cx - 9, f + 1, h1 - 1, Direction.EAST, RegionsModule.TOMB_LOOT);
        lockedChest(level, cb, rnd, cx + 9, f + 1, h1 - 1, Direction.WEST, RegionsModule.TOMB_LOOT, 2);
        for (int i = 0; i < 8; i++) {
            int x = cx - 8 + rnd.nextInt(17), z = h0 + 2 + rnd.nextInt(10);
            set(level, cb, x, f + 1, z, rnd.nextBoolean() ? Blocks.SAND.defaultBlockState() : Blocks.DEAD_BUSH.defaultBlockState());
        }
    }

    private void maze(WorldGenLevel level, BoundingBox cb, RandomSource rnd, int z0, int f) {
        final int W = 23, D = 17;
        java.util.Random r = new java.util.Random(cx * 341873128712L + cz * 132897987541L + 7);
        boolean[][] open = new boolean[W][D];
        java.util.ArrayDeque<int[]> st = new java.util.ArrayDeque<>();
        open[11][1] = true;
        st.push(new int[]{11, 1});
        int[][] dirs = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};
        while (!st.isEmpty()) {
            int[] c = st.peek();
            java.util.List<int[]> opts = new java.util.ArrayList<>();
            for (int[] d : dirs) {
                int nx = c[0] + d[0], nz = c[1] + d[1];
                if (nx >= 1 && nx < W - 1 && nz >= 1 && nz < D - 1 && !open[nx][nz]) opts.add(d);
            }
            if (opts.isEmpty()) { st.pop(); continue; }
            int[] d = opts.get(r.nextInt(opts.size()));
            open[c[0] + d[0] / 2][c[1] + d[1] / 2] = true;
            open[c[0] + d[0]][c[1] + d[1]] = true;
            st.push(new int[]{c[0] + d[0], c[1] + d[1]});
        }
        open[11][0] = true;
        open[11][D - 1] = true;
        RandomSource lr = RandomSource.create(r.nextLong());
        for (int i = 0; i < W; i++)
            for (int j = 0; j < D; j++) {
                int x = cx - 11 + i, z = z0 + j;
                set(level, cb, x, f, z, Blocks.CUT_SANDSTONE.defaultBlockState());
                set(level, cb, x, f + 4, z, wall(rnd));
                for (int y = f + 1; y <= f + 3; y++)
                    set(level, cb, x, y, z, open[i][j] ? Blocks.CAVE_AIR.defaultBlockState() : wall(rnd));
            }
        // содержимое клеток
        for (int i = 1; i < W; i += 2)
            for (int j = 1; j < D; j += 2) {
                if (!open[i][j] || (i == 11 && (j == 1 || j == D - 2))) continue;
                int n = 0;
                for (int[] d : dirs) {
                    int nx = i + d[0] / 2, nz = j + d[1] / 2;
                    if (nx >= 0 && nx < W && nz >= 0 && nz < D && open[nx][nz]) n++;
                }
                int x = cx - 11 + i, z = z0 + j;
                if (n == 1) {
                    int roll = lr.nextInt(10);
                    if (roll < 4) chest(level, cb, lr, x, f + 1, z, Direction.NORTH, RegionsModule.TOMB_LOOT);
                    else if (roll < 6) spawner(level, cb, lr, x, f + 1, z, EntityType.HUSK);
                    else set(level, cb, x, f + 1, z, Blocks.COBWEB.defaultBlockState());
                } else if (j > 3 && lr.nextInt(100) < 14) {
                    set(level, cb, x, f, z, Blocks.TNT.defaultBlockState());
                    set(level, cb, x, f + 1, z, Blocks.STONE_PRESSURE_PLATE.defaultBlockState());
                } else if (lr.nextInt(100) < 12) {
                    set(level, cb, x, f + 3, z, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
                }
            }
    }
}
