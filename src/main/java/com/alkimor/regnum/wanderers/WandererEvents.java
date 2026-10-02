package com.alkimor.regnum.wanderers;

import com.alkimor.regnum.core.RegnumConfig;
import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.kingdom.BanditEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class WandererEvents {
    private WandererEvents() {}

    /** Раз в минуту рядом с игроком может появиться странник. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 1200 != 0 || !RegnumConfig.WANDERERS_ENABLED.get()) return;
        ServerLevel ow = event.getServer().overworld();
        for (ServerPlayer p : ow.players()) {
            double chance = RegnumConfig.WANDERER_SPAWN_CHANCE.get() * (com.alkimor.regnum.survival.Skills.has(p, com.alkimor.regnum.survival.Perk.CH_FAME) ? 1.6 : 1.0);
            if (p.isSpectator() || ow.random.nextDouble() >= chance) continue;
            int near = ow.getEntitiesOfClass(WandererEntity.class, new AABB(p.blockPosition()).inflate(96)).size();
            if (near >= 2) continue;
            trySpawnNear(ow, p);
        }
    }

    private static void trySpawnNear(ServerLevel ow, ServerPlayer p) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double a = ow.random.nextDouble() * Math.PI * 2;
            int d = 24 + ow.random.nextInt(18);
            int x = p.getBlockX() + (int) (Math.cos(a) * d);
            int z = p.getBlockZ() + (int) (Math.sin(a) * d);
            if (!ow.hasChunkAt(new BlockPos(x, 0, z))) continue;
            int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!ow.getBlockState(pos.below()).isSolid() || !ow.getFluidState(pos).isEmpty() || !ow.getBlockState(pos).isAir()) continue;
            WandererEntity w = WanderersModule.WANDERER.get().create(ow);
            if (w == null) return;
            w.moveTo(x + 0.5, y, z + 0.5, ow.random.nextFloat() * 360f, 0f);
            w.setup(Persona.random(ow.random));
            w.finalizeSpawn(ow, ow.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            ow.addFreshEntity(w);
            return;
        }
    }

    /** Проверка карт сокровищ: игрок пришёл на место — тайник или засада. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            TreasureMark mark = s.get(WanderersModule.TREASURE_MARK.get());
            if (mark == null) continue;
            double dx = p.getX() - mark.pos().getX(), dz = p.getZ() - mark.pos().getZ();
            if (dx * dx + dz * dz > 10 * 10) continue;
            s.remove(WanderersModule.TREASURE_MARK.get());
            reveal(p, mark);
            return;
        }
    }

    private static void reveal(ServerPlayer p, TreasureMark mark) {
        ServerLevel sl = p.serverLevel();
        int x = mark.pos().getX(), z = mark.pos().getZ();
        BlockPos chest = new BlockPos(x, sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        sl.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
        if (sl.getBlockEntity(chest) instanceof ChestBlockEntity be) {
            be.setLootTable(WandererDialogs.BANDIT_STASH, sl.random.nextLong());
        }
        if (mark.trap()) {
            Text.bad(p, "Засада! Тайник охраняют разбойники — торговец знал, куда вас посылает.");
            int n = 3 + sl.random.nextInt(3);
            for (int i = 0; i < n; i++) {
                double a = sl.random.nextDouble() * Math.PI * 2;
                int bx = x + (int) (Math.cos(a) * 8), bz = z + (int) (Math.sin(a) * 8);
                BlockPos at = new BlockPos(bx, sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz), bz);
                WandererDialogs.spawnBandit(sl, at, i == 0 ? BanditEntity.CAPTAIN : (sl.random.nextBoolean() ? BanditEntity.ARCHER : BanditEntity.THUG), p);
            }
        } else {
            Text.good(p, "Вы нашли тайник! Похоже, на этот раз карта не соврала.");
        }
    }
}
