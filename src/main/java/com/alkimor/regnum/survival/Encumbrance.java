package com.alkimor.regnum.survival;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.core.RegnumConfig;
import com.alkimor.regnum.core.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * Перегруз (в духе Project Zomboid): чем полнее инвентарь, тем медленнее игрок.
 * Выносливость поднимает переносимый вес. Ходьба с перегрузом тренирует Выносливость.
 */
public final class Encumbrance {
    private Encumbrance() {}

    private static final ResourceLocation ID = Regnum.id("encumbrance");

    public static float load(ServerPlayer p) {
        float load = 0f;
        Inventory inv = p.getInventory();
        for (ItemStack s : inv.items) {
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof com.alkimor.regnum.trade.TradeGoodItem) {
                load += 0.8f * s.getCount();
                continue;
            }
            float fill = s.getCount() / (float) s.getMaxStackSize();
            float w = s.getItem() instanceof BlockItem ? 1.2f : 1.0f;
            load += (0.3f + 0.7f * fill) * w;
        }
        for (ItemStack a : inv.armor) if (!a.isEmpty()) load += 1.0f;
        return load;
    }

    public static float capacity(ServerPlayer p) {
        float cap = 24f + 0.12f * Skills.level(p, Skill.ATHLETICS);
        if (Skills.has(p, Perk.AT_PORTER)) cap *= 1.25f;
        if (Skills.has(p, Trait.STRONG)) cap *= 1.25f;
        if (Skills.has(p, Trait.FEEBLE)) cap *= 0.8f;
        return cap;
    }

    public static void update(ServerPlayer p) {
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        double penalty = 0;
        if (RegnumConfig.ENCUMBRANCE_ENABLED.get() && !p.isCreative() && !p.isSpectator()) {
            float load = load(p), cap = capacity(p);
            if (load > cap) penalty = Math.min(0.45, 0.1 + (load - cap) / cap * 1.5);
        }
        AttributeModifier cur = speed.getModifier(ID);
        double curPenalty = cur == null ? 0 : -cur.amount();
        if (Math.abs(curPenalty - penalty) > 0.01) {
            speed.removeModifier(ID);
            if (penalty > 0) {
                speed.addTransientModifier(new AttributeModifier(ID, -penalty, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                if (curPenalty == 0) Text.bar(p, "Перегруз! Вы двигаетесь медленнее (−" + Math.round(penalty * 100) + "%)", ChatFormatting.GOLD);
            } else {
                Text.bar(p, "Ноша стала легче", ChatFormatting.GRAY);
            }
        }
        if (penalty > 0 && p.tickCount % 60 == 0 && p.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
            Skills.addXp(p, Skill.ATHLETICS, 1);
        }
    }
}
