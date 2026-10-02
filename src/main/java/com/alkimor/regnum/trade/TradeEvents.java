package com.alkimor.regnum.trade;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.kingdom.BanditEntity;
import com.alkimor.regnum.kingdom.KingdomData;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import com.alkimor.regnum.wanderers.WandererDialogs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Риск в пути: с тяжёлым грузом на караван чаще нападают разбойники. */
public final class TradeEvents {
    private TradeEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 600 != 0) return;
        if (p.isCreative() || p.isSpectator() || p.level().dimension() != Level.OVERWORLD) return;
        ServerLevel sl = p.serverLevel();
        if (sl.getDifficulty() == Difficulty.PEACEFUL) return;
        int crates = Trading.cratesCarried(p);
        if (crates < 6) return;
        if (KingdomData.get(sl.getServer()).at(p.blockPosition()) != null) return; // в своих стенах безопасно
        float chance = Math.min(0.2f, 0.04f + crates * 0.004f);
        if (Skills.has(p, com.alkimor.regnum.survival.Perk.TR_GUARD)) chance *= 0.5f;
        if (p.getRandom().nextFloat() >= chance) return;

        Text.bad(p, "Разбойники почуяли богатый караван — на вас напали!");
        double a = p.getRandom().nextDouble() * Math.PI * 2;
        int n = 3 + p.getRandom().nextInt(2) + crates / 12;
        for (int i = 0; i < n; i++) {
            double aa = a + (p.getRandom().nextDouble() - 0.5) * 0.8;
            int x = p.getBlockX() + (int) (Math.cos(aa) * 20), z = p.getBlockZ() + (int) (Math.sin(aa) * 20);
            BlockPos at = new BlockPos(x, sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            WandererDialogs.spawnBandit(sl, at, i == 0 && crates >= 12 ? BanditEntity.CAPTAIN : (p.getRandom().nextBoolean() ? BanditEntity.ARCHER : BanditEntity.THUG), p);
        }
    }
}
