package com.alkimor.regnum.dungeon;

import com.alkimor.regnum.core.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Уникальные предметы региональных боссов. */
public final class RegionItems {
    private RegionItems() {}

    /** Посох Топи: ПКМ — ядовитое облако там, куда смотришь. */
    public static class MireStaff extends Item {
        public MireStaff(Properties p) {
            super(p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level instanceof ServerLevel sl) {
                HitResult hr = player.pick(24, 1f, false);
                Vec3 at = hr.getType() == HitResult.Type.MISS ? player.getEyePosition().add(player.getLookAngle().scale(12)) : hr.getLocation();
                AreaEffectCloud cloud = new AreaEffectCloud(sl, at.x, at.y, at.z);
                cloud.setOwner(player);
                cloud.setRadius(2.8f);
                cloud.setDuration(100);
                cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
                sl.addFreshEntity(cloud);
                stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
            player.getCooldowns().addCooldown(this, 60);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal("Трофей Матушки Топи").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
            tooltip.add(Component.literal("ПКМ — ядовитое облако (до 24 блоков)").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Звёздный клинок: поджигает врагов. */
    public static class StarBlade extends SwordItem {
        public StarBlade(Properties p) {
            super(Tiers.NETHERITE, p);
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            if (!attacker.level().isClientSide) target.igniteForSeconds(4);
            return super.hurtEnemy(stack, target, attacker);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal("Выкован из Звёздного железа").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
            tooltip.add(Component.literal("Поджигает врагов на 4 с.").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Амулет солнца: ПКМ — огнестойкость и регенерация, перезарядка 2 минуты. */
    public static class SunAmulet extends Item {
        public SunAmulet(Properties p) {
            super(p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 90, 0));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 15, 1));
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 90, 0));
                Text.bar(player, "Амулет вспыхивает солнечным светом", ChatFormatting.GOLD);
            }
            player.getCooldowns().addCooldown(this, 20 * 120);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal("Солнечный янтарь в золотой оправе").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
            tooltip.add(Component.literal("ПКМ: огнестойкость, регенерация II, ночное зрение").withStyle(ChatFormatting.GRAY));
        }
    }
}
