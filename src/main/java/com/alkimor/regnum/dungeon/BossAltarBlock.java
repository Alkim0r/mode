package com.alkimor.regnum.dungeon;

import com.alkimor.regnum.core.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

/** Универсальный алтарь призыва босса: игрок сам решает, когда начать испытание. */
public class BossAltarBlock extends Block {
    public static final BooleanProperty ACTIVATED = BlockStateProperties.ENABLED;
    private final Supplier<? extends EntityType<? extends Mob>> boss;
    private final int dx, dz;
    private final String speaker, wakeLine;

    public BossAltarBlock(Properties props, Supplier<? extends EntityType<? extends Mob>> boss, int dx, int dz, String speaker, String wakeLine) {
        super(props);
        this.boss = boss;
        this.dx = dx;
        this.dz = dz;
        this.speaker = speaker;
        this.wakeLine = wakeLine;
        registerDefaultState(stateDefinition.any().setValue(ACTIVATED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVATED);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        if (state.getValue(ACTIVATED)) {
            Text.info(player, "Алтарь остыл. Хозяин этого места уже пробуждён.");
            return InteractionResult.CONSUME;
        }
        if (sl.getDifficulty() == Difficulty.PEACEFUL) {
            Text.info(player, "Ничего не происходит (мирная сложность).");
            return InteractionResult.CONSUME;
        }
        Mob mob = boss.get().create(sl);
        if (mob == null) return InteractionResult.FAIL;
        BlockPos spawn = pos.offset(dx, 0, dz);
        mob.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 180f, 0f);
        mob.finalizeSpawn(sl, sl.getCurrentDifficultyAt(spawn), MobSpawnType.TRIGGERED, null);
        mob.restrictTo(pos, 22);
        mob.setTarget(player);
        sl.addFreshEntity(mob);
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.alkimor.regnum.story.BossIntros.intro(sl, mob, mob.position(), mob.getYRot(), 40, sp);
        }
        sl.setBlock(pos, state.setValue(ACTIVATED, true), 3);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt != null) {
            bolt.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
            bolt.setVisualOnly(true);
            sl.addFreshEntity(bolt);
        }
        sl.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0f, 1.1f);
        for (Player p : sl.players()) {
            if (p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 48 * 48) {
                p.sendSystemMessage(Text.of(speaker + ": ", ChatFormatting.DARK_RED, ChatFormatting.BOLD).append(Text.of(wakeLine, ChatFormatting.GOLD)));
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVATED) && random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + random.nextDouble(), pos.getY() + 1.2, pos.getZ() + random.nextDouble(), 0, 0.3, 0);
        }
    }
}
