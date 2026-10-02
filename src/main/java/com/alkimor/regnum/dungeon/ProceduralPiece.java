package com.alkimor.regnum.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.function.Function;

/**
 * Основа процедурных данжей: опорная точка (cx, g, cz), где g — уровень земли,
 * и помощники для стен, комнат, сундуков и спаунеров. Блоки ставятся только внутри
 * текущего чанка (cb), поэтому генерация безопасна для любого размера.
 */
public abstract class ProceduralPiece extends StructurePiece {
    protected final int cx, g, cz;

    protected ProceduralPiece(StructurePieceType type, BlockPos surface, BoundingBox box) {
        super(type, 0, box);
        this.cx = surface.getX();
        this.g = surface.getY() - 1;
        this.cz = surface.getZ();
        setOrientation(null);
    }

    protected ProceduralPiece(StructurePieceType type, CompoundTag tag) {
        super(type, tag);
        this.cx = tag.getInt("cx");
        this.g = tag.getInt("g");
        this.cz = tag.getInt("cz");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("cx", cx);
        tag.putInt("g", g);
        tag.putInt("cz", cz);
    }

    protected static void set(WorldGenLevel level, BoundingBox cb, int x, int y, int z, BlockState s) {
        BlockPos p = new BlockPos(x, y, z);
        if (cb.isInside(p)) level.setBlock(p, s, 2);
    }

    protected static void fill(WorldGenLevel level, BoundingBox cb, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
                    set(level, cb, x, y, z, s);
    }

    /** Заполнить объём, выбирая блок по случайности. */
    protected static void fillRandom(WorldGenLevel level, BoundingBox cb, RandomSource rnd, int x0, int y0, int z0, int x1, int y1, int z1,
                                     Function<RandomSource, BlockState> palette) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
                    set(level, cb, x, y, z, palette.apply(rnd));
    }

    /** Полая коробка: оболочка из палитры, внутри воздух. */
    protected static void room(WorldGenLevel level, BoundingBox cb, RandomSource rnd, int x0, int y0, int z0, int x1, int y1, int z1,
                               Function<RandomSource, BlockState> palette) {
        for (int x = x0; x <= x1; x++)
            for (int y = y0; y <= y1; y++)
                for (int z = z0; z <= z1; z++) {
                    boolean shell = x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1;
                    set(level, cb, x, y, z, shell ? palette.apply(rnd) : Blocks.CAVE_AIR.defaultBlockState());
                }
    }

    /** Фундамент: заполнить пустоту под площадкой, чтобы постройка не висела. */
    protected static void foundation(WorldGenLevel level, BoundingBox cb, int x0, int z0, int x1, int z1, int topY, int depth, BlockState s) {
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++)
                for (int y = topY; y > topY - depth; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (cb.isInside(p) && (level.getBlockState(p).canBeReplaced() || !level.getFluidState(p).isEmpty())) level.setBlock(p, s, 2);
                }
    }

    protected void chest(WorldGenLevel level, BoundingBox cb, RandomSource rnd, int x, int y, int z, Direction facing, ResourceKey<LootTable> loot) {
        createChest(level, cb, rnd, new BlockPos(x, y, z), loot, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
    }

    /** Запертый сундук (вскрывается отмычкой): лучшая добыча для разведчиков. */
    protected static void lockedChest(WorldGenLevel level, BoundingBox cb, RandomSource rnd, int x, int y, int z, Direction facing,
                                      ResourceKey<LootTable> loot, int tier) {
        BlockPos p = new BlockPos(x, y, z);
        if (!cb.isInside(p)) return;
        com.alkimor.regnum.survival.kit.LockedChestBlock.place(level, p, facing, loot, tier, rnd.nextLong());
    }

    protected static void spawner(WorldGenLevel level, BoundingBox cb, RandomSource rnd, int x, int y, int z, EntityType<?> type) {
        BlockPos p = new BlockPos(x, y, z);
        if (!cb.isInside(p)) return;
        level.setBlock(p, Blocks.SPAWNER.defaultBlockState(), 2);
        if (level.getBlockEntity(p) instanceof SpawnerBlockEntity be) be.setEntityId(type, rnd);
    }
}
