package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Блок городской постройки (казарма, рынок, башня). Работает только на своей территории. */
public class BuildingBlock extends Block {
    private final BuildingType type;

    public BuildingBlock(BuildingType type, Properties props) {
        super(props);
        this.type = type;
    }

    public BuildingType type() {
        return type;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (ctx.getLevel() instanceof ServerLevel sl && ctx.getPlayer() != null) {
            City c = KingdomData.get(sl.getServer()).at(ctx.getClickedPos());
            if (c == null || !Council.can(c, ctx.getPlayer().getUUID(), Council.Role.BUILD) || sl.dimension() != Level.OVERWORLD) {
                Text.bad(ctx.getPlayer(), type.title + " ставится только на территории вашего города.");
                return null;
            }
            if (type.tech != null && !Science.has(sl.getServer(), c.owner, type.tech)) {
                Text.bad(ctx.getPlayer(), type.title + " требует технологию «" + type.tech.title + "» (/regnum science).");
                return null;
            }
            int limit = type == BuildingType.UNIVERSITY ? 1 + c.level / 2 : 1 + c.level;
            if (c.count(type) >= limit) {
                Text.bad(ctx.getPlayer(), "Предел построек «" + type.title + "» для города " + c.level + " уровня: " + limit + ". Развивайте город.");
                return null;
            }
        }
        return super.getStateForPlacement(ctx);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel sl && placer instanceof ServerPlayer player) {
            KingdomData data = KingdomData.get(sl.getServer());
            City c = data.at(pos);
            if (c != null) {
                c.buildings.put(pos.asLong(), type);
                String why = null;
                try {
                    why = CityBuildings.raise(sl, c, type, pos, player.getDirection());
                } catch (Throwable t) {
                    com.alkimor.regnum.Regnum.LOGGER.warn("[CityBuildings] {}", t.toString());
                }
                if (why != null) Text.bad(player, "Блок поставлен, но " + why);
                data.setDirty();
                player.sendSystemMessage(Text.of("Построено: " + type.title + " в «" + c.name + "». ", ChatFormatting.GREEN)
                        .append(Text.of(type.effect, ChatFormatting.GRAY)));
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel sl) {
            KingdomData data = KingdomData.get(sl.getServer());
            for (City c : data.all()) {
                if (c.buildings.remove(pos.asLong()) != null) {
                    data.setDirty();
                    break;
                }
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (type == BuildingType.WAREHOUSE && player instanceof ServerPlayer sp && Resource.of(stack) != null) {
            City c = KingdomData.get(sp.server).at(pos);
            if (c != null && c.buildings.containsKey(pos.asLong())) {
                Industry.deposit(sp, c, sp.isShiftKeyDown());
                return net.minecraft.world.ItemInteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && type == BuildingType.MARKET && !sp.isShiftKeyDown()) {
            com.alkimor.regnum.trade.Trading.openBlock(sp, pos);
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) {
            City c = KingdomData.get(sp.server).at(pos);
            if (c != null && c.buildings.containsKey(pos.asLong())) {
                if (Council.isMember(c, sp.getUUID())) {
                    CityActions.openScreen(sp, c.hall);
                } else {
                    Text.info(sp, type.title + " города «" + c.name + "».");
                }
            } else {
                Text.info(sp, "Эта постройка не принадлежит ни одному городу.");
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
