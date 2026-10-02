package com.alkimor.regnum.worldgen;

import com.alkimor.regnum.Regnum;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Align hanging-entity NBT with the already transformed template anchor before Entity.load. */
@EventBusSubscriber(modid = Regnum.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ImportedAnchorProcessor extends StructureProcessor {
    public static final ImportedAnchorProcessor INSTANCE = new ImportedAnchorProcessor();
    private static final MapCodec<ImportedAnchorProcessor> CODEC = MapCodec.unit(INSTANCE);
    private static final StructureProcessorType<ImportedAnchorProcessor> TYPE = () -> CODEC;

    private ImportedAnchorProcessor() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.STRUCTURE_PROCESSOR, Regnum.id("imported_anchor"), () -> TYPE);
    }

    @Override
    protected StructureProcessorType<?> getType() { return TYPE; }

    @Override
    public StructureTemplate.StructureEntityInfo processEntity(LevelReader world, BlockPos seedPos,
            StructureTemplate.StructureEntityInfo original, StructureTemplate.StructureEntityInfo transformed,
            StructurePlaceSettings settings, StructureTemplate template) {
        String id = transformed.nbt.getString("id");
        if (!id.equals("minecraft:item_frame") && !id.equals("minecraft:glow_item_frame")
                && !id.equals("minecraft:painting") && !id.equals("minecraft:leash_knot")) return transformed;
        var tag = transformed.nbt.copy();
        tag.putInt("TileX", transformed.blockPos.getX());
        tag.putInt("TileY", transformed.blockPos.getY());
        tag.putInt("TileZ", transformed.blockPos.getZ());
        return new StructureTemplate.StructureEntityInfo(transformed.pos, transformed.blockPos, tag);
    }
}
