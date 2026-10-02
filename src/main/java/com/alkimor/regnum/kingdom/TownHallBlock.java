package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.RegnumConfig;
import com.alkimor.regnum.core.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

import java.util.UUID;

/** Ратуша — сердце города. Поставив её, игрок основывает город. */
public class TownHallBlock extends Block {
    private static final String[] NAMES = {"Белоград", "Светлогорье", "Ярославец", "Звенигород", "Новоград", "Каменец",
            "Велиград", "Дубравск", "Златополь", "Ветровск", "Горислав", "Медвежий Угол", "Сосновец", "Красноречье"};

    public TownHallBlock(Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (level instanceof ServerLevel sl && player != null) {
            if (sl.dimension() != Level.OVERWORLD) {
                Text.bad(player, "Города можно основывать только в верхнем мире.");
                return null;
            }
            KingdomData data = KingdomData.get(sl.getServer());
            if (data.ownedBy(player.getUUID()).size() >= RegnumConfig.MAX_CITIES_PER_PLAYER.get()) {
                Text.bad(player, "У вас уже максимальное число городов (" + RegnumConfig.MAX_CITIES_PER_PLAYER.get() + ").");
                return null;
            }
            if (data.tooClose(ctx.getClickedPos(), 32)) {
                Text.bad(player, "Слишком близко к другому городу — территории не должны пересекаться.");
                return null;
            }
        }
        return super.getStateForPlacement(ctx);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel sl) || !(placer instanceof ServerPlayer player)) return;
        KingdomData data = KingdomData.get(sl.getServer());
        String name = stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)
                ? stack.getHoverName().getString()
                : NAMES[sl.random.nextInt(NAMES.length)];
        City c = new City(UUID.randomUUID(), player.getUUID(), name, pos.immutable());
        long day = sl.getDayTime() / 24000L;
        c.lastEconomyDay = day;
        c.nextRaidDay = day + 2;
        c.culture = Culture.forBiome(sl.getBiome(pos)).ordinal();
        data.add(c);
        sl.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0f, 1.0f);
        player.sendSystemMessage(Text.of("🏰 Основан город «" + name + "»! ", ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Text.of("Территория: " + c.radius() + " блоков. ПКМ по ратуше — управление городом.", ChatFormatting.GRAY)));
        Text.info(player, "Культура города: " + Culture.byId(c.culture).title + " (меняется в ратуше).");
        Text.info(player, "Постройте казарму, чтобы нанимать войска. Первый набег разбойников — через 2 дня.");
        com.alkimor.regnum.survival.Skills.addXp(player, com.alkimor.regnum.survival.Skill.STEWARD, 60);
        com.alkimor.regnum.survival.Skills.addXp(player, com.alkimor.regnum.survival.Skill.LEADERSHIP, 30);
        com.alkimor.regnum.survival.Skills.chronicle(player, "Основан город «" + name + "»");
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel sl) {
            KingdomData data = KingdomData.get(sl.getServer());
            City c = data.byHall(pos);
            if (c != null) {
                data.remove(c);
                ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(c.owner);
                if (owner != null) {
                    Text.bad(owner, "Ратуша разрушена — город «" + c.name + "» пал. Казна (" + c.treasury + ") потеряна.");
                }
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            CityActions.openScreen(sp, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
