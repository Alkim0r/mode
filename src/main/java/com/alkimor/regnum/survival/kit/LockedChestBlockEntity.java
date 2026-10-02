package com.alkimor.regnum.survival.kit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

/** Запертый сундук помнит, какой лут внутри и насколько сложный замок. */
public class LockedChestBlockEntity extends BlockEntity {
    @Nullable public ResourceKey<LootTable> loot;
    public long seed;
    /** Сложность замка: 0 — простой, 1 — добротный, 2 — мастерский. */
    public int tier;

    public LockedChestBlockEntity(BlockPos pos, BlockState state) {
        super(KitModule.LOCKED_CHEST_BE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        if (loot != null) tag.putString("Loot", loot.location().toString());
        tag.putLong("Seed", seed);
        tag.putInt("Tier", tier);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        loot = tag.contains("Loot") ? ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(tag.getString("Loot"))) : null;
        seed = tag.getLong("Seed");
        tier = tag.getInt("Tier");
    }
}
