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
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Алтарь склепа: испытание начинается, когда игрок сам пробуждает Владыку. */
public class CryptAltarBlock extends Block {
    public static final BooleanProperty ACTIVATED = BlockStateProperties.ENABLED;

    public CryptAltarBlock(Properties props) {
        super(props);
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
            Text.info(player, "Алтарь безмолвен. Владыка этого склепа уже пробуждён.");
            return InteractionResult.CONSUME;
        }
        if (sl.getDifficulty() == Difficulty.PEACEFUL) {
            Text.info(player, "Мёртвые спят спокойно (мирная сложность).");
            return InteractionResult.CONSUME;
        }
        CryptLordEntity lord = DungeonModule.CRYPT_LORD.get().create(sl);
        if (lord == null) return InteractionResult.FAIL;
        BlockPos spawn = pos.offset(0, 0, -6);
        lord.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 180f, 0f);
        lord.finalizeSpawn(sl, sl.getCurrentDifficultyAt(spawn), MobSpawnType.TRIGGERED, null);
        lord.restrictTo(pos, 20);
        lord.setTarget(player);
        sl.addFreshEntity(lord);
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.alkimor.regnum.story.BossIntros.intro(sl, lord, lord.position(), lord.getYRot(), 40, sp);
        }
        sl.setBlock(pos, state.setValue(ACTIVATED, true), 3);

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt != null) {
            bolt.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
            bolt.setVisualOnly(true);
            sl.addFreshEntity(bolt);
        }
        sl.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0f, 0.8f);
        for (Player p : sl.players()) {
            if (p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 48 * 48) {
                p.sendSystemMessage(Text.of("Морграт: ", ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                        .append(Text.of("Кто посмел потревожить мой сон? Твои кости украсят мой трон!", ChatFormatting.LIGHT_PURPLE)));
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVATED) && random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(), 0, 0.02, 0);
        }
    }
}
