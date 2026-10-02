package com.alkimor.regnum.crafting;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
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

/** Свиток мастерства: награда отшельника и из сундуков — даёт опыт случайного ремесленного/боевого навыка. */
public class MasteryScrollItem extends Item {
    private static final Skill[] POOL = Skill.values();

    public MasteryScrollItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            Skill s = POOL[sp.getRandom().nextInt(POOL.length)];
            Text.gold(sp, "Вы изучаете свиток... Записи посвящены навыку «" + s.title + "».");
            // В творческом режиме опыт навыков не начисляется — свиток просто показывает навык.
            Skills.addXp(sp, s, 120);
            if (!sp.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("ПКМ — изучить (+120 опыта случайного навыка)").withStyle(ChatFormatting.GRAY));
    }
}
