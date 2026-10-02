package com.alkimor.regnum.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Процедурный склеп. Все координаты считаются от (cx, g, cz), где g — уровень земли.
 * <pre>
 *  север (−Z)  мавзолей 7×7 → лестница вниз на 12 → коридор с двумя крипт-комнатами → зал босса 17×17
 * </pre>
 */
public class CryptPiece extends StructurePiece {
    private static final int DEPTH = 12;
    private final int cx, g, cz;

    public CryptPiece(BlockPos surface) {
        super(DungeonModule.CRYPT_PIECE.get(), 0, makeBox(surface.getX(), surface.getY() - 1, surface.getZ()));
        this.cx = surface.getX();
        this.g = surface.getY() - 1;
        this.cz = surface.getZ();
        setOrientation(null);
    }

    public CryptPiece(CompoundTag tag) {
        super(DungeonModule.CRYPT_PIECE.get(), tag);
        this.cx = tag.getInt("cx");
        this.g = tag.getInt("g");
        this.cz = tag.getInt("cz");
    }

    private static BoundingBox makeBox(int cx, int g, int cz) {
        int f = g - DEPTH;
        return new BoundingBox(cx - 9, f - 1, cz - 5, cx + 9, g + 6, cz + 38);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("cx", cx);
        tag.putInt("g", g);
        tag.putInt("cz", cz);
    }

    // ------------------------------------------------------------------ генерация

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource rnd,
                            BoundingBox cb, net.minecraft.world.level.ChunkPos chunkPos, BlockPos pivot) {
        final int f = g - DEPTH; // пол подземелья
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState cave = Blocks.CAVE_AIR.defaultBlockState();

        // ---- Мавзолей на поверхности
        for (int x = cx - 3; x <= cx + 3; x++)
            for (int z = cz - 3; z <= cz + 3; z++) {
                // фундамент, чтобы не висел над обрывом
                for (int y = g - 1; y >= g - 5; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (cb.isInside(p) && level.getBlockState(p).canBeReplaced()) set(level, cb, p, Blocks.COBBLESTONE.defaultBlockState());
                }
                set(level, cb, x, g, z, Blocks.POLISHED_ANDESITE.defaultBlockState());
                boolean wall = x == cx - 3 || x == cx + 3 || z == cz - 3 || z == cz + 3;
                for (int y = g + 1; y <= g + 4; y++) {
                    set(level, cb, x, y, z, wall ? brick(rnd) : air);
                }
                set(level, cb, x, g + 5, z, Blocks.STONE_BRICK_SLAB.defaultBlockState());
                // очистить растительность над крышей
                set(level, cb, x, g + 6, z, air);
            }
        // колонны на углах и вход с севера
        for (int y = g + 1; y <= g + 5; y++) {
            set(level, cb, cx - 3, y, cz - 3, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
            set(level, cb, cx + 3, y, cz - 3, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
            set(level, cb, cx - 3, y, cz + 3, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
            set(level, cb, cx + 3, y, cz + 3, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        }
        set(level, cb, cx, g + 1, cz - 3, air);
        set(level, cb, cx, g + 2, cz - 3, air);
        set(level, cb, cx - 1, g + 2, cz - 4, Blocks.SOUL_LANTERN.defaultBlockState());
        set(level, cb, cx + 1, g + 2, cz - 4, Blocks.SOUL_LANTERN.defaultBlockState());
        set(level, cb, cx - 1, g + 1, cz - 4, Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState());
        set(level, cb, cx + 1, g + 1, cz - 4, Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState());

        // ---- Лестница вниз (на юг)
        BlockState stair = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        for (int k = 0; k < DEPTH; k++) {
            int z = cz - 1 + k;
            int y = g - k;
            for (int x = cx - 1; x <= cx + 1; x++) {
                set(level, cb, x, y - 1, z, brick(rnd));
                set(level, cb, x, y, z, stair);
                for (int h = 1; h <= 4; h++) if (y + h <= g || k < 4) set(level, cb, x, y + h, z, h + y > g ? air : cave);
                if (y + 5 <= g) set(level, cb, x, y + 5, z, brick(rnd));
            }
            for (int h = 0; h <= 4; h++) {
                if (y + h <= g) {
                    set(level, cb, cx - 2, y + h, z, brick(rnd));
                    set(level, cb, cx + 2, y + h, z, brick(rnd));
                }
            }
            if (k % 4 == 3) set(level, cb, cx - 1, y + 1, z, Blocks.SOUL_LANTERN.defaultBlockState());
        }

        // ---- Коридор
        int c0 = cz - 1 + DEPTH, c1 = cz + 20;
        room(level, cb, rnd, cx - 2, f, c0, cx + 2, f + 5, c1);
        // проём из лестницы в коридор
        for (int x = cx - 1; x <= cx + 1; x++)
            for (int y = f + 1; y <= f + 4; y++) set(level, cb, x, y, c0, Blocks.CAVE_AIR.defaultBlockState());
        for (int z = c0; z <= c1; z += 3) {
            if (rnd.nextInt(3) == 0) set(level, cb, cx - 1, f + 4, z, Blocks.COBWEB.defaultBlockState());
            if (rnd.nextInt(3) == 0) set(level, cb, cx + 1, f + 4, z, Blocks.COBWEB.defaultBlockState());
        }
        set(level, cb, cx + 1, f + 1, cz + 12, Blocks.SOUL_LANTERN.defaultBlockState());

        // ---- Боковые крипты со спаунерами
        room(level, cb, rnd, cx - 8, f, cz + 12, cx - 2, f + 5, cz + 18);
        room(level, cb, rnd, cx + 2, f, cz + 12, cx + 8, f + 5, cz + 18);
        for (int y = f + 1; y <= f + 2; y++) {
            set(level, cb, cx - 2, y, cz + 15, cave);
            set(level, cb, cx + 2, y, cz + 15, cave);
        }
        spawner(level, cb, rnd, cx - 5, f + 1, cz + 15, EntityType.SKELETON);
        spawner(level, cb, rnd, cx + 5, f + 1, cz + 15, EntityType.ZOMBIE);
        // саркофаги
        for (int z = cz + 13; z <= cz + 17; z += 4) {
            set(level, cb, cx - 7, f + 1, z, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            set(level, cb, cx + 7, f + 1, z, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        }
        createChest(level, cb, rnd, new BlockPos(cx - 7, f + 1, cz + 15), DungeonModule.CRYPT_CHEST_LOOT,
                Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST));

        // ---- Зал босса
        int h0 = cz + 20, h1 = cz + 38;
        room(level, cb, rnd, cx - 9, f, h0, cx + 9, f + 9, h1);
        for (int x = cx - 8; x <= cx + 8; x++)
            for (int z = h0 + 1; z <= h1 - 1; z++)
                set(level, cb, x, f, z, ((x + z) & 1) == 0 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState());
        // проход из коридора
        for (int x = cx - 1; x <= cx + 1; x++)
            for (int y = f + 1; y <= f + 3; y++) set(level, cb, x, y, h0, cave);
        // колонны
        int[][] pillars = {{-5, 25}, {5, 25}, {-5, 31}, {5, 31}};
        for (int[] pl : pillars) {
            for (int y = f + 1; y <= f + 8; y++) set(level, cb, cx + pl[0], y, cz + pl[1], Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            set(level, cb, cx + pl[0] + (pl[0] < 0 ? 1 : -1), f + 1, cz + pl[1], Blocks.SOUL_LANTERN.defaultBlockState());
        }
        // помост и алтарь
        for (int x = cx - 3; x <= cx + 3; x++)
            for (int z = cz + 33; z <= cz + 37; z++) set(level, cb, x, f + 1, z, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        for (int x = cx - 3; x <= cx + 3; x++)
            set(level, cb, x, f + 1, cz + 32, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
        set(level, cb, cx, f + 2, cz + 35, DungeonModule.CRYPT_ALTAR.get().defaultBlockState());
        set(level, cb, cx - 2, f + 2, cz + 36, Blocks.SOUL_CAMPFIRE.defaultBlockState());
        set(level, cb, cx + 2, f + 2, cz + 36, Blocks.SOUL_CAMPFIRE.defaultBlockState());
        // сокровищница
        if (cb.isInside(new BlockPos(cx - 8, f + 1, cz + 37))) com.alkimor.regnum.survival.kit.LockedChestBlock.place(level,
                new BlockPos(cx - 8, f + 1, cz + 37), Direction.EAST, DungeonModule.CRYPT_TREASURE_LOOT, 2, rnd.nextLong());
        createChest(level, cb, rnd, new BlockPos(cx + 8, f + 1, cz + 37), DungeonModule.CRYPT_CHEST_LOOT,
                Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST));
        // кости и паутина
        for (int i = 0; i < 6; i++) {
            int x = cx - 7 + rnd.nextInt(15);
            int z = h0 + 2 + rnd.nextInt(10);
            set(level, cb, x, f + 1, z, rnd.nextBoolean() ? Blocks.BONE_BLOCK.defaultBlockState() : Blocks.COBWEB.defaultBlockState());
        }
    }

    /** Полая комната: стены/пол/потолок из кирпича, внутри воздух. */
    private void room(WorldGenLevel level, BoundingBox cb, RandomSource rnd, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = x0; x <= x1; x++)
            for (int y = y0; y <= y1; y++)
                for (int z = z0; z <= z1; z++) {
                    boolean shell = x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1;
                    set(level, cb, x, y, z, shell ? brick(rnd) : Blocks.CAVE_AIR.defaultBlockState());
                }
    }

    private void spawner(WorldGenLevel level, BoundingBox cb, RandomSource rnd, int x, int y, int z, EntityType<?> type) {
        BlockPos p = new BlockPos(x, y, z);
        if (!cb.isInside(p)) return;
        level.setBlock(p, Blocks.SPAWNER.defaultBlockState(), 2);
        if (level.getBlockEntity(p) instanceof SpawnerBlockEntity be) {
            be.setEntityId(type, rnd);
        }
    }

    private static BlockState brick(RandomSource rnd) {
        int r = rnd.nextInt(10);
        if (r < 4) return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        if (r < 6) return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        if (r < 8) return Blocks.STONE_BRICKS.defaultBlockState();
        if (r < 9) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
    }

    private static void set(WorldGenLevel level, BoundingBox cb, int x, int y, int z, BlockState s) {
        set(level, cb, new BlockPos(x, y, z), s);
    }

    private static void set(WorldGenLevel level, BoundingBox cb, BlockPos p, BlockState s) {
        if (cb.isInside(p)) level.setBlock(p, s, 2);
    }
}
