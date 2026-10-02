package com.alkimor.regnum.survival.kit;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Кошка-крюк: зацепиться за блок до 26 блоков и подтянуться — на стену замка, на скалу, через пропасть. */
public class GrapplingHookItem extends Item {
    public static final double RANGE = 26;

    public GrapplingHookItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        Vec3 eye = sp.getEyePosition();
        Vec3 end = eye.add(sp.getViewVector(1f).scale(RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
        if (hit.getType() != HitResult.Type.BLOCK) {
            Text.bar(sp, "Крюку не за что зацепиться", ChatFormatting.GRAY);
            return InteractionResultHolder.fail(stack);
        }
        Vec3 target = hit.getLocation();
        Vec3 d = target.subtract(sp.position());
        double dist = d.length();
        Vec3 v = d.normalize().scale(Math.min(2.4, 0.9 + dist * 0.07)).add(0, 0.45, 0);
        sp.setDeltaMovement(v);
        sp.hurtMarked = true;
        sp.resetFallDistance();
        var sl = sp.serverLevel();
        for (int i = 0; i <= 16; i++) {
            Vec3 q = eye.lerp(target, i / 16.0);
            sl.sendParticles(ParticleTypes.CRIT, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, sp.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1f, 1.4f);
        level.playSound(null, hit.getBlockPos(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.8f, 1.2f);
        stack.hurtAndBreak(1, sp, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        sp.getCooldowns().addCooldown(this, 30);
        Skills.addXp(sp, Skill.ATHLETICS, 1);
        if (dist > 10) Skills.addXp(sp, Skill.ROGUERY, 1);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("ПКМ — зацепиться и подтянуться (до 26 блоков)").withStyle(ChatFormatting.GRAY));
    }
}
