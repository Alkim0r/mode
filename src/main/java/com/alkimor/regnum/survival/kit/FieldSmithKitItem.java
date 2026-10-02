package com.alkimor.regnum.survival.kit;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.survival.Perk;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Походный набор кузнеца: ремонт в пути. Чинит предмет во второй руке, иначе самый изношенный доспех. */
public class FieldSmithKitItem extends Item {
    public FieldSmithKitItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack kit = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(kit);
        ItemStack target = sp.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!target.isDamageableItem() || !target.isDamaged()) {
            target = ItemStack.EMPTY;
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack a = sp.getItemBySlot(s);
                if (a.isDamageableItem() && a.isDamaged() && (target.isEmpty() || a.getDamageValue() * target.getMaxDamage() > target.getDamageValue() * a.getMaxDamage())) target = a;
            }
        }
        if (target.isEmpty()) {
            Text.bar(sp, "Нечего чинить: возьмите повреждённый предмет во вторую руку", ChatFormatting.GRAY);
            return InteractionResultHolder.fail(kit);
        }
        int lvl = Skills.level(sp, Skill.SMITHING);
        float part = (0.15f + 0.002f * lvl) * (Skills.has(sp, Perk.SM_REPAIR) ? 1.25f : 1f);
        int fix = Math.max(1, Math.round(target.getMaxDamage() * part));
        target.setDamageValue(Math.max(0, target.getDamageValue() - fix));
        level.playSound(null, sp.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.5f, 1.6f);
        Text.bar(sp, "Починено: " + target.getHoverName().getString(), ChatFormatting.GREEN);
        Skills.addXp(sp, Skill.SMITHING, 3);
        kit.hurtAndBreak(1, sp, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        sp.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.consume(kit);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("ПКМ — починить предмет во второй руке или изношенный доспех").withStyle(ChatFormatting.GRAY));
    }
}
