package com.alkimor.regnum.kingdom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Жезл командира.
 * ПКМ вдаль: по врагу — атаковать, по земле — выдвинуться строем, в небо — следовать.
 * Shift+ПКМ — командный экран. ПКМ по своему солдату — сменить отряд. ПКМ по жителю — вербовка.
 */
public class CommanderBatonItem extends Item {
    public CommanderBatonItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            if (sp.isShiftKeyDown() && sp.getXRot() < -60f && com.alkimor.regnum.survival.Skills.has(sp, com.alkimor.regnum.survival.Perk.LD_CRY)) {
                warCry(sp);
                player.getCooldowns().addCooldown(this, 20 * 120);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            if (sp.isShiftKeyDown()) ArmyCommands.sendInfo(sp, stack);
            else ArmyCommands.pointOrder(sp, stack);
        }
        player.getCooldowns().addCooldown(this, 8);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Боевой клич: солдаты рядом получают силу и скорость. */
    private static void warCry(ServerPlayer sp) {
        var sl = sp.serverLevel();
        sl.playSound(null, sp.blockPosition(), net.minecraft.sounds.SoundEvents.RAID_HORN.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 1.0f);
        int n = 0;
        for (SoldierEntity s : sl.getEntitiesOfClass(SoldierEntity.class, sp.getBoundingBox().inflate(20), s -> s.isOwnedBy(sp))) {
            s.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 200, 0));
            s.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 200, 0));
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER, s.getX(), s.getEyeY() + 0.5, s.getZ(), 2, 0.2, 0.2, 0.2, 0);
            n++;
        }
        com.alkimor.regnum.core.Text.gold(sp, "Боевой клич! Воодушевлены воины: " + n + ".");
        com.alkimor.regnum.survival.Skills.addXp(sp, com.alkimor.regnum.survival.Skill.LEADERSHIP, 5);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getPlayer() instanceof ServerPlayer sp) {
            if (sp.isShiftKeyDown()) ArmyCommands.sendInfo(sp, ctx.getItemInHand());
            else ArmyCommands.moveTo(sp, ctx.getItemInHand(), ctx.getClickedPos());
        }
        if (ctx.getPlayer() != null) ctx.getPlayer().getCooldowns().addCooldown(this, 8);
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            if (target instanceof SoldierEntity s && s.isOwnedBy(sp)) {
                ArmyCommands.cycleSquad(sp, s);
            } else if (target instanceof Villager v) {
                ArmyCommands.recruitVillager(sp, v);
            } else if (!(target instanceof Player)) {
                ArmyCommands.issue(sp, ArmyCommands.selectedSquad(stack), Order.ATTACK_TARGET, target.position(), sp.getYRot(),
                        ArmyCommands.selectedFormation(stack), target);
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        int squad = ArmyCommands.selectedSquad(stack);
        tooltip.add(Component.literal("Выбрано: " + ArmyCommands.squadName(squad) + ", строй: " + ArmyCommands.selectedFormation(stack).title).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("ПКМ по врагу — атаковать").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("ПКМ по земле — выдвинуться строем").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("ПКМ в небо — следовать за мной").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Shift+ПКМ — командный экран").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("ПКМ по солдату — сменить отряд, по жителю — вербовка (4 изумр.)").withStyle(ChatFormatting.GRAY));
    }
}
