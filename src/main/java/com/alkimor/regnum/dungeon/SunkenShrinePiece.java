package com.alkimor.regnum.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Затопленное капище: руины на болоте, кольцо воды вокруг острова с алтарём. */
public class SunkenShrinePiece extends ProceduralPiece {
    private static final int R = 12;

    public SunkenShrinePiece(BlockPos surface) {
        super(RegionsModule.SHRINE_PIECE.get(), surface, new BoundingBox(surface.getX() - R, surface.getY() - 8, surface.getZ() - R,
                surface.getX() + R, surface.getY() + 10, surface.getZ() + R));
    }

    public SunkenShrinePiece(CompoundTag tag) {
        super(RegionsModule.SHRINE_PIECE.get(), tag);
    }

    private static BlockState wall(RandomSource r) {
        int n = r.nextInt(10);
        if (n < 4) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        if (n < 7) return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        if (n < 9) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        return Blocks.MUD_BRICKS.defaultBlockState();
    }

    private static BlockState floor(RandomSource r) {
        return r.nextInt(3) == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.MUD_BRICKS.defaultBlockState();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource rnd, BoundingBox cb, ChunkPos chunk, BlockPos pivot) {
        foundation(level, cb, cx - R, cz - R, cx + R, cz + R, g - 1, 6, Blocks.MUD.defaultBlockState());
        fillRandom(level, cb, rnd, cx - R, g, cz - R, cx + R, g, cz + R, SunkenShrinePiece::floor);
        fill(level, cb, cx - R, g + 1, cz - R, cx + R, g + 9, cz + R, Blocks.AIR.defaultBlockState());

        // стены-руины
        for (int x = cx - R; x <= cx + R; x++)
            for (int z = cz - R; z <= cz + R; z++) {
                if (Math.abs(x - cx) != R && Math.abs(z - cz) != R) continue;
                int h = 6 - (rnd.nextFloat() < 0.35f ? rnd.nextInt(3) : 0);
                for (int y = g + 1; y <= g + h; y++) set(level, cb, x, y, z, wall(rnd));
            }
        fill(level, cb, cx - 1, g + 1, cz + R, cx + 1, g + 3, cz + R, Blocks.AIR.defaultBlockState());

        // кольцо воды и остров
        for (int x = cx - 7; x <= cx + 7; x++)
            for (int z = cz - 7; z <= cz + 7; z++) {
                int d = Math.max(Math.abs(x - cx), Math.abs(z - cz));
                if (d == 5 || d == 6) {
                    set(level, cb, x, g - 1, z, Blocks.MUD.defaultBlockState());
                    set(level, cb, x, g, z, Blocks.WATER.defaultBlockState());
                    if (rnd.nextInt(6) == 0) set(level, cb, x, g + 1, z, Blocks.LILY_PAD.defaultBlockState());
                }
            }
        // мостки через воду
        for (int z = cz + 4; z <= cz + 7; z++) set(level, cb, cx, g + 1, z, Blocks.MANGROVE_SLAB.defaultBlockState());
        for (int x = cx - 2; x <= cx + 2; x++)
            for (int z = cz - 2; z <= cz + 2; z++) set(level, cb, x, g, z, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        set(level, cb, cx, g + 1, cz, RegionsModule.MIRE_ALTAR.get().defaultBlockState());

        // колонны с фонарями
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) {
                for (int y = g + 1; y <= g + 5; y++) set(level, cb, cx + sx * 9, y, cz + sz * 9, Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState());
                set(level, cb, cx + sx * 9, g + 6, cz + sz * 9, Blocks.LANTERN.defaultBlockState());
            }
        // мох и сундуки
        for (int i = 0; i < 30; i++) {
            int x = cx - R + 1 + rnd.nextInt(2 * R - 1), z = cz - R + 1 + rnd.nextInt(2 * R - 1);
            int d = Math.max(Math.abs(x - cx), Math.abs(z - cz));
            if (d > 7 && d < R) set(level, cb, x, g + 1, z, Blocks.MOSS_CARPET.defaultBlockState());
        }
        chest(level, cb, rnd, cx - 10, g + 1, cz - 10, Direction.SOUTH, RegionsModule.SHRINE_LOOT);
        lockedChest(level, cb, rnd, cx + 10, g + 1, cz - 10, Direction.SOUTH, RegionsModule.SHRINE_LOOT, 1);
        spawner(level, cb, rnd, cx - 10, g + 1, cz + 9, net.minecraft.world.entity.EntityType.DROWNED);
    }
}
