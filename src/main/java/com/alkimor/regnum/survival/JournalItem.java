package com.alkimor.regnum.survival;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Дневник выжившего: открывает экран навыков и состояния. */
public class JournalItem extends Item {
    public JournalItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) Skills.sync(sp, Skills.data(sp).created()
                ? com.alkimor.regnum.core.network.SkillSyncPayload.OPEN_JOURNAL : com.alkimor.regnum.core.network.SkillSyncPayload.OPEN_CREATION);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("ПКМ — герой: атрибуты, навыки, перки, черты, летопись и род").withStyle(ChatFormatting.GRAY));
    }
}
