package com.alkimor.regnum.survival.kit;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.survival.Perk;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Запертый сундук: лучшая добыча данжей и лагерей. Сломать нельзя — только вскрыть отмычкой.
 * Шанс зависит от Плутовства и сложности замка; неудача может сломать отмычку.
 */
public class LockedChestBlock extends BaseEntityBlock {
    public static final MapCodec<LockedChestBlock> CODEC = simpleCodec(LockedChestBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public LockedChestBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LockedChestBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(KitModule.LOCKPICK.get())) {
            if (!level.isClientSide) Text.bar(player, "Сундук заперт. Нужна отмычка.", ChatFormatting.GRAY);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) return ItemInteractionResult.SUCCESS;
        int tier = level.getBlockEntity(pos) instanceof LockedChestBlockEntity be ? be.tier : 0;
        int rog = Skills.level(sp, Skill.ROGUERY);
        float chance = 0.45f - 0.15f * tier + 0.006f * rog;
        boolean success = sp.isCreative() || sp.getRandom().nextFloat() < chance;
        if (success) {
            unlock(sl, pos, state);
            sl.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.8f, 1.6f);
            Text.good(sp, "Замок поддался!");
            Skills.addXp(sp, Skill.ROGUERY, 8 + 6 * tier);
        } else {
            float breakChance = Skills.has(sp, Perk.RG_KING) ? 0f : Skills.has(sp, Perk.RG_LOCK) ? 0.25f : 0.5f;
            sl.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 0.8f, 1.4f);
            if (sp.getRandom().nextFloat() < breakChance) {
                stack.shrink(1);
                sl.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
                Text.bar(sp, "Отмычка сломалась (шанс вскрыть " + Math.round(chance * 100) + "%)", ChatFormatting.RED);
            } else {
                Text.bar(sp, "Не вышло... (шанс " + Math.round(chance * 100) + "%)", ChatFormatting.GOLD);
            }
            Skills.addXp(sp, Skill.ROGUERY, 2);
        }
        return ItemInteractionResult.SUCCESS;
    }

    public static void unlock(ServerLevel sl, BlockPos pos, BlockState state) {
        LockedChestBlockEntity be = sl.getBlockEntity(pos) instanceof LockedChestBlockEntity b ? b : null;
        var loot = be != null ? be.loot : null;
        long seed = be != null ? be.seed : sl.random.nextLong();
        sl.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, state.getValue(FACING)), 3);
        if (loot != null && sl.getBlockEntity(pos) instanceof ChestBlockEntity chest) chest.setLootTable(loot, seed);
        sl.sendParticles(ParticleTypes.WAX_OFF, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 10, 0.3, 0.2, 0.3, 0.05);
    }

    /** Для генерации структур: поставить запертый сундук с лутом. */
    public static void place(net.minecraft.world.level.WorldGenLevel level, BlockPos pos, Direction facing,
                             net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot, int tier, long seed) {
        level.setBlock(pos, KitModule.LOCKED_CHEST.get().defaultBlockState().setValue(FACING, facing), 2);
        if (level.getBlockEntity(pos) instanceof LockedChestBlockEntity be) {
            be.loot = loot;
            be.seed = seed;
            be.tier = tier;
        }
    }
}
