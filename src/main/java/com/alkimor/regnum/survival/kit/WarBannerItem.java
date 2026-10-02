package com.alkimor.regnum.survival.kit;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.kingdom.SoldierEntity;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Боевое знамя полководца: поднять над головой — солдаты рядом стойче и лечатся (раз в 3 минуты). */
public class WarBannerItem extends Item {
    public WarBannerItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        var sl = sp.serverLevel();
        int n = 0;
        for (SoldierEntity s : sl.getEntitiesOfClass(SoldierEntity.class, sp.getBoundingBox().inflate(16), s -> s.isOwnedBy(sp))) {
            s.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 30, 0));
            s.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 30, 0));
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, s.getX(), s.getEyeY() + 0.4, s.getZ(), 3, 0.3, 0.2, 0.3, 0);
            n++;
        }
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 30, 0));
        level.playSound(null, sp.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 1.2f, 1.3f);
        Text.gold(sp, "Знамя поднято! Воинов под знаменем: " + n + ".");
        Skills.addXp(sp, Skill.LEADERSHIP, 3 + n);
        sp.getCooldowns().addCooldown(this, 20 * 180);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("ПКМ — поднять знамя: солдаты рядом получают регенерацию и защиту на 30 с").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Перезарядка 3 минуты").withStyle(ChatFormatting.DARK_GRAY));
    }
}
