package com.alkimor.regnum.dungeon;

import com.alkimor.regnum.kingdom.BanditEntity;
import com.alkimor.regnum.kingdom.KingdomModule;
import com.alkimor.regnum.wanderers.WandererDialogs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Лагерь строится по рельефу: каждый элемент ставится на свою высоту земли. */
public class CampPiece extends StructurePiece {
    private static final int R = 10;
    private final int cx, cy, cz;

    public CampPiece(BlockPos center) {
        super(DungeonModule.CAMP_PIECE.get(), 0, new BoundingBox(center.getX() - R, center.getY() - 8, center.getZ() - R,
                center.getX() + R, center.getY() + 12, center.getZ() + R));
        cx = center.getX();
        cy = center.getY();
        cz = center.getZ();
        setOrientation(null);
    }

    public CampPiece(CompoundTag tag) {
        super(DungeonModule.CAMP_PIECE.get(), tag);
        cx = tag.getInt("cx");
        cy = tag.getInt("cy");
        cz = tag.getInt("cz");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("cx", cx);
        tag.putInt("cy", cy);
        tag.putInt("cz", cz);
    }

    private int ground(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource rnd,
                            BoundingBox cb, ChunkPos chunkPos, BlockPos pivot) {
        // вытоптанная поляна
        for (int x = cx - R + 1; x <= cx + R - 1; x++)
            for (int z = cz - R + 1; z <= cz + R - 1; z++) {
                int dx = x - cx, dz = z - cz;
                if (dx * dx + dz * dz > (R - 1) * (R - 1)) continue;
                int y = ground(level, x, z);
                for (int h = 0; h < 4; h++) set(level, cb, new BlockPos(x, y + h, z), Blocks.AIR.defaultBlockState());
                if (rnd.nextInt(3) == 0) set(level, cb, new BlockPos(x, y - 1, z), Blocks.COARSE_DIRT.defaultBlockState());
                else if (rnd.nextInt(5) == 0) set(level, cb, new BlockPos(x, y - 1, z), Blocks.DIRT_PATH.defaultBlockState());
            }
        // костёр
        int gy = ground(level, cx, cz);
        set(level, cb, new BlockPos(cx, gy, cz), Blocks.CAMPFIRE.defaultBlockState());
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos log = new BlockPos(cx, 0, cz).relative(d, 2);
            set(level, cb, log.atY(ground(level, log.getX(), log.getZ())), Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, d.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X));
        }
        // три шатра
        tent(level, cb, cx - 6, cz - 3, Blocks.BROWN_WOOL.defaultBlockState());
        tent(level, cb, cx + 4, cz - 5, Blocks.RED_WOOL.defaultBlockState());
        tent(level, cb, cx - 1, cz + 5, Blocks.GRAY_WOOL.defaultBlockState());
        // награбленное
        BlockPos chest = new BlockPos(cx + 3, 0, cz + 2);
        chest = chest.atY(ground(level, chest.getX(), chest.getZ()));
        if (cb.isInside(chest)) com.alkimor.regnum.survival.kit.LockedChestBlock.place(level, chest, Direction.WEST, WandererDialogs.BANDIT_STASH, 1, rnd.nextLong());
        BlockPos barrel = new BlockPos(cx + 4, 0, cz + 2);
        set(level, cb, barrel.atY(ground(level, barrel.getX(), barrel.getZ())), Blocks.BARREL.defaultBlockState());
        BlockPos hay = new BlockPos(cx + 4, 0, cz + 3);
        set(level, cb, hay.atY(ground(level, hay.getX(), hay.getZ())), Blocks.HAY_BLOCK.defaultBlockState());
        // частокол с черепами
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3 + 0.3;
            int x = cx + (int) Math.round(Math.cos(a) * (R - 2)), z = cz + (int) Math.round(Math.sin(a) * (R - 2));
            int y = ground(level, x, z);
            set(level, cb, new BlockPos(x, y, z), Blocks.SPRUCE_FENCE.defaultBlockState());
            set(level, cb, new BlockPos(x, y + 1, z), i % 2 == 0 ? Blocks.SKELETON_SKULL.defaultBlockState() : Blocks.LANTERN.defaultBlockState());
        }

        // охрана — один раз, в чанке центра лагеря
        BlockPos center = new BlockPos(cx, gy, cz);
        if (cb.isInside(center)) {
            int n = 3 + rnd.nextInt(3);
            for (int i = 0; i <= n; i++) {
                double a = rnd.nextDouble() * Math.PI * 2;
                int x = cx + (int) (Math.cos(a) * 4), z = cz + (int) (Math.sin(a) * 4);
                BlockPos p = new BlockPos(x, ground(level, x, z), z);
                if (!cb.isInside(p)) continue;
                BanditEntity b = KingdomModule.BANDIT.get().create(level.getLevel());
                if (b == null) continue;
                b.moveTo(x + 0.5, p.getY(), z + 0.5, rnd.nextFloat() * 360f, 0f);
                b.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
                b.setup(i == 0 ? BanditEntity.CAPTAIN : (rnd.nextBoolean() ? BanditEntity.ARCHER : BanditEntity.THUG));
                if (i == 0) b.addTag(BanditEntity.CAMP_BOSS_TAG);
                b.setPersistenceRequired();
                level.addFreshEntity(b);
            }
        }
    }

    private void tent(WorldGenLevel level, BoundingBox cb, int x, int z, BlockState wool) {
        int y = ground(level, x, z);
        for (int dz = 0; dz < 3; dz++) {
            set(level, cb, new BlockPos(x - 1, y, z + dz), wool);
            set(level, cb, new BlockPos(x + 1, y, z + dz), wool);
            set(level, cb, new BlockPos(x, y + 1, z + dz), wool);
        }
        set(level, cb, new BlockPos(x, y - 1, z + 1), Blocks.SPRUCE_PLANKS.defaultBlockState());
    }

    private static void set(WorldGenLevel level, BoundingBox cb, BlockPos p, BlockState s) {
        if (cb.isInside(p)) level.setBlock(p, s, 2);
    }
}
