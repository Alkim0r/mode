package com.alkimor.regnum.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Locale;
import java.util.Optional;

/** Региональный данж: вид задаётся в JSON полем "kind" (shrine / fortress / tomb). */
public class RegionStructure extends Structure {
    public enum Kind { SHRINE, FORTRESS, TOMB }

    public static final MapCodec<RegionStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("kind").forGetter(s -> s.kind.name().toLowerCase(Locale.ROOT))
    ).apply(i, (settings, kind) -> new RegionStructure(settings, Kind.valueOf(kind.toUpperCase(Locale.ROOT)))));

    private final Kind kind;

    public RegionStructure(StructureSettings settings, Kind kind) {
        super(settings);
        this.kind = kind;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int x = cp.getMiddleBlockX(), z = cp.getMiddleBlockZ();
        int surface = ctx.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        int floor = ctx.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
        int maxWater = kind == Kind.SHRINE ? 2 : 0;
        if (surface - floor > maxWater) return Optional.empty();
        if (floor - 16 <= ctx.heightAccessor().getMinBuildHeight() + 4) return Optional.empty();
        BlockPos pos = new BlockPos(x, floor, z);
        return Optional.of(new GenerationStub(pos, b -> b.addPiece(switch (kind) {
            case SHRINE -> new SunkenShrinePiece(pos);
            case FORTRESS -> new ForgeFortressPiece(pos);
            case TOMB -> new SandTombPiece(pos);
        })));
    }

    @Override
    public StructureType<?> type() {
        return RegionsModule.REGION_STRUCTURE.get();
    }
}
