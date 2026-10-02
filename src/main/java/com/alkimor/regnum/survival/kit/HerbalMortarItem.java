package com.alkimor.regnum.survival.kit;

import com.alkimor.regnum.core.InvUtil;
import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.dungeon.RegionsModule;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import com.alkimor.regnum.survival.SurvivalModule;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Ступка травника: из трав выходит вдвое больше снадобий, чем при простом смешивании. */
public class HerbalMortarItem extends Item {
    public HerbalMortarItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack mortar = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(mortar);
        boolean made = false;
        if (InvUtil.count(sp, RegionsModule.ROT_ROOT.get()) >= 1 && InvUtil.count(sp, SurvivalModule.HEALING_HERB.get()) >= 1
                && InvUtil.count(sp, Items.GLASS_BOTTLE) >= 2) {
            InvUtil.take(sp, RegionsModule.ROT_ROOT.get(), 1);
            InvUtil.take(sp, SurvivalModule.HEALING_HERB.get(), 1);
            InvUtil.take(sp, Items.GLASS_BOTTLE, 2);
            InvUtil.give(sp, new ItemStack(RegionsModule.ANTIDOTE.get(), 3));
            Text.good(sp, "Растёрто: Противоядие ×3");
            made = true;
        } else if (InvUtil.count(sp, SurvivalModule.HEALING_HERB.get()) >= 2 && InvUtil.count(sp, Items.GLASS_BOTTLE) >= 2) {
            InvUtil.take(sp, SurvivalModule.HEALING_HERB.get(), 2);
            InvUtil.take(sp, Items.GLASS_BOTTLE, 2);
            InvUtil.give(sp, new ItemStack(SurvivalModule.HERBAL_DECOCTION.get(), 2));
            Text.good(sp, "Растёрто: Целебный отвар ×2");
            made = true;
        } else if (InvUtil.count(sp, SurvivalModule.HEALING_HERB.get()) >= 1 && InvUtil.count(sp, Items.PAPER) >= 1) {
            InvUtil.take(sp, SurvivalModule.HEALING_HERB.get(), 1);
            InvUtil.take(sp, Items.PAPER, 1);
            InvUtil.give(sp, new ItemStack(SurvivalModule.BANDAGE.get(), 2));
            Text.good(sp, "Сделано: Бинт с травами ×2");
            made = true;
        }
        if (!made) {
            Text.bar(sp, "Нужно: 2 травы + 2 бутылки, или корень+трава+2 бутылки, или трава+бумага", ChatFormatting.GRAY);
            return InteractionResultHolder.fail(mortar);
        }
        level.playSound(null, sp.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.7f, 1.4f);
        Skills.addXp(sp, Skill.MEDICINE, 6);
        mortar.hurtAndBreak(1, sp, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        sp.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(mortar);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("ПКМ — растереть травы: вдвое больше отваров").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("2 травы + 2 бутылки → 2 отвара; корень + трава + 2 бутылки → 3 противоядия").withStyle(ChatFormatting.DARK_GRAY));
    }
}
