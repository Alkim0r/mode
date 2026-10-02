package com.alkimor.regnum.dungeon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** Склеп: мавзолей на поверхности, лестница вниз, коридор со склепами и зал босса. */
public class CryptStructure extends Structure {
    public static final MapCodec<CryptStructure> CODEC = simpleCodec(CryptStructure::new);

    public CryptStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int x = cp.getMiddleBlockX();
        int z = cp.getMiddleBlockZ();
        int surface = ctx.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        int floor = ctx.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
        if (surface != floor) return Optional.empty(); // под водой не строим
        if (surface - 16 <= ctx.heightAccessor().getMinBuildHeight() + 4) return Optional.empty();
        BlockPos pos = new BlockPos(x, surface, z);
        return Optional.of(new GenerationStub(pos, builder -> builder.addPiece(new CryptPiece(pos))));
    }

    @Override
    public StructureType<?> type() {
        return DungeonModule.CRYPT_STRUCTURE.get();
    }
}
