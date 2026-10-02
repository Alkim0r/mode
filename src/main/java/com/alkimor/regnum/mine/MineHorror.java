package com.alkimor.regnum.mine;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.kingdom.KingdomData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Страх шахт: в глубокой тёмной пещере на игрока выходят стаи ползунов, слышны шорохи и скрежет.
 * Свет, факелы и город рядом отпугивают. После череды убитых ползунов в самой глубине просыпается Королева.
 */
public final class MineHorror {
    private MineHorror() {}

    public static final String KILLS = "regnum_crawler_kills", QUEEN_LAST = "regnum_queen_last", PACK_LAST = "regnum_pack_last";
    public static final int QUEEN_AFTER_KILLS = 14;
    public static final long QUEEN_COOLDOWN = 48000L, PACK_COOLDOWN = 2400L;

    private static final class Awakening {
        ServerPlayer p;
        int left = 140;
    }

    private static final List<Awakening> AWAKE = new ArrayList<>();

    /** Глубоко, под землёй и в темноте. */
    public static boolean inMine(ServerPlayer p) {
        if (p.level().dimension() != Level.OVERWORLD || p.isCreative() || p.isSpectator()) return false;
        BlockPos pos = p.blockPosition();
        ServerLevel sl = p.serverLevel();
        return pos.getY() < 40 && !sl.canSeeSky(pos) && sl.getMaxLocalRawBrightness(pos) <= 5;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 100 != 37) return;
        ServerLevel sl = p.serverLevel();
        if (sl.getDifficulty() == Difficulty.PEACEFUL || !inMine(p)) return;
        long now = sl.getGameTime();
        // атмосфера: шорохи и скрежет за спиной
        if (sl.random.nextInt(100) < 14) scare(p, sl);
        // стая
        long last = p.getPersistentData().getLong(PACK_LAST);
        if (now - last < PACK_COOLDOWN && last != 0) return;
        int depthBonus = p.getBlockY() < 0 ? 8 : 0;
        if (sl.random.nextInt(100) >= 9 + depthBonus) return;
        int near = sl.getEntitiesOfClass(CrawlerEntity.class, p.getBoundingBox().inflate(48)).size();
        if (near >= 8) return;
        if (spawnPack(p, p.getBlockY() < 0 ? 4 : 3) > 0) p.getPersistentData().putLong(PACK_LAST, now);
    }

    private static void scare(ServerPlayer p, ServerLevel sl) {
        double a = sl.random.nextDouble() * Math.PI * 2;
        double x = p.getX() + Math.cos(a) * 14, z = p.getZ() + Math.sin(a) * 14;
        switch (sl.random.nextInt(4)) {
            case 0 -> sl.playSound(null, x, p.getY(), z, SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 1.2f, 0.45f);
            case 1 -> sl.playSound(null, x, p.getY(), z, SoundEvents.GRAVEL_BREAK, SoundSource.AMBIENT, 1.4f, 0.5f);
            case 2 -> sl.playSound(null, x, p.getY(), z, SoundEvents.SKELETON_STEP, SoundSource.HOSTILE, 1.0f, 0.4f);
            default -> {
                sl.playSound(null, p.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.2f, 0.7f);
                p.displayClientMessage(Text.of("Где-то в темноте скрежещут когти…", ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
            }
        }
    }

    /** Пытается вывести стаю вне поля зрения, в тёмных местах. Возвращает число появившихся. */
    public static int spawnPack(ServerPlayer p, int n) {
        ServerLevel sl = p.serverLevel();
        KingdomData data = KingdomData.get(p.server);
        Vec3 look = p.getLookAngle();
        int spawned = 0;
        BlockPos center = null;
        for (int tries = 0; tries < 30 && spawned < n; tries++) {
            double a = sl.random.nextDouble() * Math.PI * 2, r = 12 + sl.random.nextDouble() * 10;
            if (center != null) { a = sl.random.nextDouble() * Math.PI * 2; r = 1 + sl.random.nextDouble() * 3; }
            Vec3 base = center == null ? p.position() : Vec3.atBottomCenterOf(center);
            int x = (int) Math.floor(base.x + Math.cos(a) * r), z = (int) Math.floor(base.z + Math.sin(a) * r);
            BlockPos found = null;
            for (int dy = 6; dy >= -6; dy--) {
                BlockPos bp = new BlockPos(x, p.getBlockY() + dy, z);
                if (sl.getBlockState(bp.below()).isSolidRender(sl, bp.below()) && sl.getBlockState(bp).isAir() && sl.getBlockState(bp.above()).isAir()) { found = bp; break; }
            }
            if (found == null || sl.getMaxLocalRawBrightness(found) > 4 || data.at(found) != null) continue;
            Vec3 to = Vec3.atBottomCenterOf(found).subtract(p.position());
            if (center == null && to.normalize().dot(look) > 0.5) continue; // не прямо перед глазами
            CrawlerEntity c = MineModule.CRAWLER.get().create(sl);
            if (c == null) continue;
            c.moveTo(found.getX() + 0.5, found.getY(), found.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
            if (!sl.noCollision(c)) continue;
            c.finalizeSpawn(sl, sl.getCurrentDifficultyAt(found), MobSpawnType.EVENT, null);
            sl.addFreshEntity(c);
            if (center == null) center = found;
            spawned++;
        }
        if (spawned > 0) {
            sl.playSound(null, p.blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 1.0f, 0.4f);
            p.displayClientMessage(Text.of("В темноте движутся тени — это стая!", ChatFormatting.DARK_RED), true);
        }
        return spawned;
    }

    // ------------------------------------------------------------------ Королева

    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof CrawlerEntity c) || c.getTags().contains("regnum_minion")) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        int k = p.getPersistentData().getInt(KILLS) + 1;
        p.getPersistentData().putInt(KILLS, k);
        if (p.getBlockY() >= 20) return;
        if (k == QUEEN_AFTER_KILLS / 2) p.sendSystemMessage(Text.of("Стены шахты чуть слышно дрожат… Их слишком много.", ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        if (k >= QUEEN_AFTER_KILLS) tryAwaken(p, false);
    }

    public static boolean queenAlive(ServerLevel sl, Vec3 around) {
        return !sl.getEntitiesOfClass(CrawlerQueenEntity.class, new net.minecraft.world.phys.AABB(around, around).inflate(160)).isEmpty();
    }

    public static boolean tryAwaken(ServerPlayer p, boolean force) {
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        long last = p.getPersistentData().getLong(QUEEN_LAST);
        if (!force && (last != 0 && now - last < QUEEN_COOLDOWN)) return false;
        if (queenAlive(sl, p.position())) return false;
        for (Awakening a : AWAKE) if (a.p == p) return false;
        p.getPersistentData().putLong(QUEEN_LAST, now);
        p.getPersistentData().putInt(KILLS, 0);
        Awakening a = new Awakening();
        a.p = p;
        AWAKE.add(a);
        p.sendSystemMessage(Text.of("Земля содрогается. Что-то огромное просыпается в глубине…", ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (AWAKE.isEmpty()) return;
        Iterator<Awakening> it = AWAKE.iterator();
        while (it.hasNext()) {
            Awakening a = it.next();
            ServerPlayer p = a.p;
            if (p.hasDisconnected() || p.isRemoved()) { it.remove(); continue; }
            ServerLevel sl = p.serverLevel();
            a.left--;
            if (a.left % 8 == 0) {
                sl.playSound(null, p.blockPosition(), SoundEvents.GRAVEL_BREAK, SoundSource.AMBIENT, 1.5f, 0.4f);
                sl.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.GRAVEL.defaultBlockState()), p.getX(), p.getY() + 2.5, p.getZ(), 25, 4, 0.5, 4, 0);
            }
            if (a.left <= 0) {
                it.remove();
                if (!spawnQueen(p)) {
                    p.sendSystemMessage(Text.of("Дрожь стихла… но тьма ещё помнит.", ChatFormatting.DARK_GRAY));
                    p.getPersistentData().putInt(KILLS, QUEEN_AFTER_KILLS - 4);
                    p.getPersistentData().putLong(QUEEN_LAST, 0);
                }
            }
        }
    }

    /** Королева выходит из стены на открытой площадке в 10–18 шагах от героя. */
    public static boolean spawnQueen(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        for (int tries = 0; tries < 60; tries++) {
            double a = sl.random.nextDouble() * Math.PI * 2, r = 10 + sl.random.nextDouble() * 8;
            int x = (int) Math.floor(p.getX() + Math.cos(a) * r), z = (int) Math.floor(p.getZ() + Math.sin(a) * r);
            for (int dy = 5; dy >= -5; dy--) {
                BlockPos bp = new BlockPos(x, p.getBlockY() + dy, z);
                if (!roomFor(sl, bp)) continue;
                CrawlerQueenEntity q = MineModule.CRAWLER_QUEEN.get().create(sl);
                if (q == null) return false;
                float yaw = (float) Math.toDegrees(Math.atan2(-(p.getX() - bp.getX()), p.getZ() - bp.getZ()));
                q.moveTo(bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5, yaw, 0f);
                q.finalizeSpawn(sl, sl.getCurrentDifficultyAt(bp), MobSpawnType.EVENT, null);
                sl.addFreshEntity(q);
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()), q.getX(), q.getY() + 1, q.getZ(), 80, 1.5, 1, 1.5, 0.1);
                com.alkimor.regnum.story.BossIntros.intro(sl, q, q.position(), yaw, 40, p);
                return true;
            }
        }
        return false;
    }

    private static boolean roomFor(ServerLevel sl, BlockPos bp) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (!sl.getBlockState(bp.offset(x, -1, z)).isSolidRender(sl, bp.offset(x, -1, z))) return false;
            for (int y = 0; y < 3; y++) if (!sl.getBlockState(bp.offset(x, y, z)).isAir()) return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("mine").requires(s -> s.hasPermission(2))
                .then(Commands.literal("pack").executes(ctx -> spawnPack(ctx.getSource().getPlayerOrException(), 4)))
                .then(Commands.literal("queen").executes(ctx -> tryAwaken(ctx.getSource().getPlayerOrException(), true) ? 1 : 0))));
    }
}
