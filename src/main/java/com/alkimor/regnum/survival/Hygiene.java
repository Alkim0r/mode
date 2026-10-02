package com.alkimor.regnum.survival;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Чистота: грязь копится от пота, болота и шахты, смывается водой и дождём.
 * Грязного героя раны воспаляются втрое чаще, а при сильной грязи сама кожа может загноиться.
 */
public final class Hygiene {
    private Hygiene() {}

    public static final String KEY = "regnum_dirt";
    public static final int DIRTY = 600, FILTHY = 900;

    public static int dirt(Player p) {
        return p.getPersistentData().getInt(KEY);
    }

    public static boolean dirty(Player p) {
        return dirt(p) >= DIRTY;
    }

    public static String title(int d) {
        return d >= FILTHY ? "в грязи с головы до ног" : d >= DIRTY ? "грязный" : d >= 300 ? "запылённый" : "чистый";
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isCreative() || p.isSpectator() || p.tickCount % 40 != 0) return;
        if (!Injuries.enabled()) return;
        int d = dirt(p), before = d;
        BlockPos pos = p.blockPosition();
        boolean wet = p.isInWaterOrRain();
        if (wet) {
            d -= p.isInWater() ? 40 : 8;
        } else {
            d += 1;
            if (p.isSprinting()) d += 2;
            if (pos.getY() < 50) d += 1;
            if (p.level().getBiome(pos).is(BiomeTags.IS_JUNGLE) || p.level().getBlockState(pos.below()).is(Blocks.MUD)
                    || p.level().getBlockState(pos.below()).is(Blocks.MUDDY_MANGROVE_ROOTS)) d += 3;
        }
        d = Math.max(0, Math.min(1000, d));
        if (d == before) return;
        p.getPersistentData().putInt(KEY, d);
        if (before < DIRTY && d >= DIRTY) {
            p.displayClientMessage(Component.literal("Вы грязны. Раны будут воспаляться — окунитесь в воду.").withStyle(ChatFormatting.GOLD), true);
        } else if (before >= DIRTY && d < DIRTY) {
            p.displayClientMessage(Component.literal("Вы смыли грязь.").withStyle(ChatFormatting.AQUA), true);
        }
        // воспаление открытой раны
        if (d >= DIRTY && p.hasEffect(SurvivalModule.BLEEDING) && !p.hasEffect(SurvivalModule.INFECTION) && p.getRandom().nextFloat() < 0.12f) {
            p.addEffect(new MobEffectInstance(SurvivalModule.INFECTION, Injuries.INFECTION_STAGE_TICKS, 0));
            p.displayClientMessage(Component.literal("Грязь попала в рану — началось воспаление.").withStyle(ChatFormatting.DARK_GREEN), false);
        }
    }
}
