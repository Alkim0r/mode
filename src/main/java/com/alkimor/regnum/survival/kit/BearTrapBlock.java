package com.alkimor.regnum.survival.kit;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.kingdom.SoldierEntity;
import com.alkimor.regnum.survival.Perk;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Капкан: зажимает ногу — враг обездвижен и ранен. ПКМ — взвести снова.
 * Ловушка мастера (перк «Мастер ловушек» у того, кто поставил) держит дольше и бьёт сильнее.
 */
public class BearTrapBlock extends Block {
    public static final MapCodec<BearTrapBlock> CODEC = simpleCodec(BearTrapBlock::new);
    public static final BooleanProperty ARMED = BooleanProperty.create("armed");
    public static final BooleanProperty MASTER = BooleanProperty.create("master");
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 3, 14);

    public BearTrapBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(ARMED, true).setValue(MASTER, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(ARMED, MASTER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        boolean master = ctx.getPlayer() != null && Skills.has(ctx.getPlayer(), Perk.EN_TRAPS);
        if (ctx.getPlayer() != null && !ctx.getLevel().isClientSide) Skills.addXp(ctx.getPlayer(), Skill.ENGINEERING, 3);
        return defaultBlockState().setValue(MASTER, master);
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState s, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState s, Direction d, BlockState n, net.minecraft.world.level.LevelAccessor l, BlockPos p, BlockPos np) {
        return d == Direction.DOWN && !canSurvive(s, l, p) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : s;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity e) {
        if (level.isClientSide || !state.getValue(ARMED) || !(e instanceof LivingEntity le)) return;
        if (e instanceof Player p && (p.isCreative() || Skills.has(p, Perk.EN_INVENTOR) || p.isCrouching())) return;
        if (e instanceof SoldierEntity s && s.getOwnerPlayer() != null && Skills.has(s.getOwnerPlayer(), Perk.EN_INVENTOR)) return;
        boolean master = state.getValue(MASTER);
        level.setBlock(pos, state.setValue(ARMED, false), 3);
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.2f, 0.7f);
        le.hurt(level.damageSources().generic(), master ? 7f : 4f);
        int t = master ? 120 : 70;
        le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, t, 6));
        le.setDeltaMovement(Vec3.ZERO);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 10, 0.2, 0.1, 0.2, 0.1);
        }
        if (le instanceof Player p) Text.bar(p, "Капкан! Нога зажата.", ChatFormatting.RED);
        if (le instanceof net.minecraft.world.entity.Mob m && m.getRandom().nextFloat() < 0.5f) m.getNavigation().stop();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(ARMED)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(ARMED, true), 3);
            level.playSound(null, pos, SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.BLOCKS, 1f, 0.8f);
            Skills.addXp(player, Skill.ENGINEERING, 1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx, java.util.List<net.minecraft.network.chat.Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.literal("Поставьте на землю. Пригнитесь, чтобы пройти мимо своего капкана.")
                .withStyle(ChatFormatting.GRAY));
    }
}
