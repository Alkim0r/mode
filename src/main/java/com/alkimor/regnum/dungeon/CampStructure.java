package com.alkimor.regnum.dungeon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** Лагерь разбойников: шатры, костёр, награбленное и охрана во главе с атаманом. */
public class CampStructure extends Structure {
    public static final MapCodec<CampStructure> CODEC = simpleCodec(CampStructure::new);

    public CampStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int x = cp.getMiddleBlockX(), z = cp.getMiddleBlockZ();
        int surface = ctx.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        int floor = ctx.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
        if (surface != floor) return Optional.empty();
        BlockPos pos = new BlockPos(x, surface, z);
        return Optional.of(new GenerationStub(pos, b -> b.addPiece(new CampPiece(pos))));
    }

    @Override
    public StructureType<?> type() {
        return DungeonModule.CAMP_STRUCTURE.get();
    }
}
