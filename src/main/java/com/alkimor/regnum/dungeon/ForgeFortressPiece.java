package com.alkimor.regnum.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Гномья кузня-крепость: зал с лавовыми каналами, четыре Угольных сердца питают Горнового. */
public class ForgeFortressPiece extends ProceduralPiece {
    private static final int R = 13;

    public ForgeFortressPiece(BlockPos surface) {
        super(RegionsModule.FORTRESS_PIECE.get(), surface, new BoundingBox(surface.getX() - R, surface.getY() - 12, surface.getZ() - R,
                surface.getX() + R, surface.getY() + 13, surface.getZ() + R));
    }

    public ForgeFortressPiece(CompoundTag tag) {
        super(RegionsModule.FORTRESS_PIECE.get(), tag);
    }

    private static BlockState wall(RandomSource r) {
        int n = r.nextInt(10);
        if (n < 5) return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        if (n < 7) return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        if (n < 9) return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        return Blocks.GILDED_BLACKSTONE.defaultBlockState();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource rnd, BoundingBox cb, ChunkPos chunk, BlockPos pivot) {
        foundation(level, cb, cx - R, cz - R, cx + R, cz + R, g - 1, 11, Blocks.COBBLED_DEEPSLATE.defaultBlockState());
        fill(level, cb, cx - R, g + 1, cz - R, cx + R, g + 13, cz + R, Blocks.AIR.defaultBlockState());
        for (int x = cx - R; x <= cx + R; x++)
            for (int z = cz - R; z <= cz + R; z++)
                set(level, cb, x, g, z, ((x + z) & 1) == 0 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState());

        // стены с зубцами
        for (int x = cx - R; x <= cx + R; x++)
            for (int z = cz - R; z <= cz + R; z++) {
                if (Math.abs(x - cx) != R && Math.abs(z - cz) != R) continue;
                for (int y = g + 1; y <= g + 10; y++) set(level, cb, x, y, z, wall(rnd));
                if (((x + z) & 1) == 0) set(level, cb, x, g + 11, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
            }
        // ворота на севере
        fill(level, cb, cx - 1, g + 1, cz - R, cx + 1, g + 4, cz - R, Blocks.AIR.defaultBlockState());
        fill(level, cb, cx - 2, g + 5, cz - R, cx + 2, g + 5, cz - R, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());

        // лавовые каналы за решётками
        for (int z = cz - 10; z <= cz + 10; z++)
            for (int sx = -1; sx <= 1; sx += 2) {
                int x = cx + sx * 11;
                set(level, cb, x, g - 1, z, Blocks.BLACKSTONE.defaultBlockState());
                set(level, cb, x, g, z, Blocks.LAVA.defaultBlockState());
                set(level, cb, x, g + 1, z, Blocks.IRON_BARS.defaultBlockState());
                set(level, cb, x, g + 2, z, Blocks.IRON_BARS.defaultBlockState());
            }

        // Угольные сердца на постаментах
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) {
                int x = cx + sx * 7, z = cz + sz * 6;
                set(level, cb, x, g + 1, z, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                set(level, cb, x, g + 2, z, RegionsModule.EMBER_CORE.get().defaultBlockState());
                for (int y = g + 3; y <= g + 9; y++) set(level, cb, x, y, z, Blocks.CHAIN.defaultBlockState());
            }

        // помост и алтарь у южной стены
        fill(level, cb, cx - 3, g + 1, cz + 7, cx + 3, g + 1, cz + 11, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        set(level, cb, cx, g + 2, cz + 9, RegionsModule.FORGE_ALTAR.get().defaultBlockState());
        set(level, cb, cx - 2, g + 2, cz + 10, Blocks.BLAST_FURNACE.defaultBlockState());
        set(level, cb, cx + 2, g + 2, cz + 10, Blocks.ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, Direction.EAST));
        set(level, cb, cx - 3, g + 2, cz + 11, Blocks.LAVA_CAULDRON.defaultBlockState());
        set(level, cb, cx + 3, g + 2, cz + 11, Blocks.LAVA_CAULDRON.defaultBlockState());

        chest(level, cb, rnd, cx - 10, g + 1, cz + 11, Direction.NORTH, RegionsModule.FORTRESS_LOOT);
        lockedChest(level, cb, rnd, cx + 10, g + 1, cz + 11, Direction.NORTH, RegionsModule.FORTRESS_LOOT, 2);
        spawner(level, cb, rnd, cx, g + 1, cz - 9, net.minecraft.world.entity.EntityType.MAGMA_CUBE);
    }
}
