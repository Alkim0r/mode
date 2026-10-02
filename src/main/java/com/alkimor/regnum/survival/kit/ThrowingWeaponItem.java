package com.alkimor.regnum.survival.kit;

import com.alkimor.regnum.survival.Perk;
import com.alkimor.regnum.survival.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Метательное оружие: дротик, топорик. Подбирается после броска (иногда ломается). */
public class ThrowingWeaponItem extends Item {
    public final float damage;
    public final float speed;

    public ThrowingWeaponItem(float damage, float speed, Properties p) {
        super(p);
        this.damage = damage;
        this.speed = speed;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.8f, 1.3f);
        if (!level.isClientSide) {
            ThrownWeaponEntity e = new ThrownWeaponEntity(level, player, stack.copyWithCount(1), damage);
            float sp = speed * (Skills.has(player, Perk.TW_RANGE) ? 1.25f : 1f);
            e.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, sp, 1.2f);
            level.addFreshEntity(e);
        }
        player.getCooldowns().addCooldown(this, 14);
        boolean save = !level.isClientSide && Skills.has(player, Perk.TW_SAVE) && player.getRandom().nextFloat() < 0.2f;
        if (!save) stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("ПКМ — метнуть (" + (int) damage + " урона). Навык: Метательное").withStyle(ChatFormatting.GRAY));
    }
}
