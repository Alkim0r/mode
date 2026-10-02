package com.alkimor.regnum.client;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.kingdom.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Опциональная «экскурсия» для видео: облёт мира, основание города, постройки, армия, стычка с разбойниками.
 * Включается только свойством -Dregnum.tour=true и только в мире regnum-tour; обычная игра не затрагивается.
 * Бои с боссами после экскурсии ведёт {@link BattleShowcase}. Кадры идут подряд: tour_NNNNN.png.
 */
@EventBusSubscriber(modid = Regnum.MODID, value = Dist.CLIENT)
public final class TourCapture {
    public static volatile boolean done;
    private static volatile int serverT = -120;
    private static volatile boolean running;
    private static int frames, lastGrabT = Integer.MIN_VALUE, ready;
    private static boolean scheduled, finished;
    private static BlockPos center;
    private static City city;
    private static final List<Entity> actors = new ArrayList<>();
    private static final BuildingType[] BUILD = {BuildingType.BARRACKS, BuildingType.MARKET, BuildingType.WATCHTOWER,
            BuildingType.TRAINING_GROUND, BuildingType.SMITHY, BuildingType.LIBRARY};
    private static final String[] BUILD_CAP = {"Казарма: отсюда нанимают войска", "Рынок: торговля и налоги",
            "Сторожевая башня: раннее предупреждение о набегах", "Учебный плац: бойцы тренируют приёмы",
            "Кузница: броня и оружие для армии", "Библиотека: наука и технологии"};

    private TourCapture() {}

    public static boolean enabled() { return Boolean.getBoolean("regnum.tour"); }

    public static boolean tourWorld(MinecraftServer s) {
        return s.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize()
                .getFileName().toString().equals("regnum-tour");
    }

    public static void caption(String text) {
        Regnum.LOGGER.info("[TOUR] CAPTION {} {}", frames, text);
    }

    public static void grab(Minecraft mc) {
        String name = "tour_" + String.format(java.util.Locale.ROOT, "%05d", frames++) + ".png";
        net.minecraft.client.Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> {});
    }

    public static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        Regnum.LOGGER.info("[TOUR] DONE frames={}", frames);
        mc.execute(mc::stop);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        if (!enabled()) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        mc.options.hideGui = true;
        if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen onboarding) {
            onboarding.onClose(); return;
        }
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null || mc.level == null || !tourWorld(server)) return;
        if (!scheduled && ++ready >= 100) {
            scheduled = true;
            server.execute(() -> {
                try { setup(server); running = true; }
                catch (Throwable t) { Regnum.LOGGER.error("[TOUR] SETUP FAILED", t); finish(mc); }
            });
        }
        if (!running || done || mc.screen != null) return;
        int t = serverT;
        if (t > 0 && t % 2 == 0 && t != lastGrabT) { lastGrabT = t; grab(mc); }
    }

    private static ServerPlayer observer(MinecraftServer s) { return s.getPlayerList().getPlayers().get(0); }

    private static void setup(MinecraftServer server) {
        ServerLevel level = server.overworld();
        ServerPlayer p = observer(server);
        p.setGameMode(GameType.SPECTATOR);
        level.setDayTime(1500);
        level.setWeatherParameters(999999, 0, false, false);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        center = findSite(level);
        Regnum.LOGGER.info("[TOUR] site={}", center);
        Vec3 c = cam(60, 36, 0);
        p.teleportTo(level, c.x, c.y, c.z, 0, 20);
    }

    private static BlockPos findSite(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int r = 30; r <= 150; r += 30) {
            for (int a = 0; a < 8; a++) {
                int x = spawn.getX() + (int) (Math.cos(a * Math.PI / 4) * r);
                int z = spawn.getZ() + (int) (Math.sin(a * Math.PI / 4) * r);
                int min = 999, max = -999;
                boolean wet = false;
                for (int dx = -24; dx <= 24; dx += 12) for (int dz = -24; dz <= 24; dz += 12) {
                    int h = hgt(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
                    int hw = hgt(level, Heightmap.Types.WORLD_SURFACE, x + dx, z + dz);
                    int hf = hgt(level, Heightmap.Types.OCEAN_FLOOR, x + dx, z + dz);
                    if (hw != hf && level.getFluidState(new BlockPos(x + dx, hw - 1, z + dz)).is(net.minecraft.tags.FluidTags.WATER)) wet = true;
                    min = Math.min(min, h); max = Math.max(max, h);
                }
                if (wet) continue;
                int score = (max - min) * 10 + r / 12;
                if (score < bestScore) { bestScore = score; best = new BlockPos(x, hgt(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z); }
            }
            if (bestScore <= 30) break;
        }
        return best != null ? best : new BlockPos(spawn.getX(), hgt(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ()), spawn.getZ());
    }

    private static int hgt(ServerLevel level, Heightmap.Types type, int x, int z) {
        level.getChunk(x >> 4, z >> 4, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);
        return level.getHeight(type, x, z);
    }

    private static Vec3 cam(double radius, double height, double angleDeg) {
        double a = Math.toRadians(angleDeg);
        return new Vec3(center.getX() + 0.5 + Math.cos(a) * radius, center.getY() + height, center.getZ() + 0.5 + Math.sin(a) * radius);
    }

    private static void look(MinecraftServer s, Vec3 from, Vec3 to) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        observer(s).teleportTo(s.overworld(), from.x, from.y, from.z, yaw, pitch);
    }

    private static double lerp(double a, double b, double k) { return a + (b - a) * Math.max(0, Math.min(1, k)); }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        if (!enabled() || !running || done) return;
        MinecraftServer s = event.getServer();
        if (!tourWorld(s) || s.getPlayerList().getPlayers().isEmpty()) return;
        ServerLevel level = s.overworld();
        int t = ++serverT;
        Vec3 target = new Vec3(center.getX() + 0.5, center.getY() + 4, center.getZ() + 0.5);
        try {
            if (t < 0) { look(s, cam(60, 36, 0), target); return; }
            if (t == 1) caption("Regnum: Королевства и Легенды — большой мир, в котором вы строите своё королевство");
            if (t < 300) {
                look(s, cam(lerp(70, 50, t / 300.0), lerp(40, 30, t / 300.0), t * 0.4), target);
            } else if (t < 360) {
                if (t == 300) caption("Основание королевства: ставим Ратушу");
                look(s, cam(lerp(50, 30, (t - 300) / 60.0), lerp(30, 16, (t - 300) / 60.0), 120 + (t - 300) * 0.4), target);
            } else {
                // орбита вокруг города
                double ang = 144 + (t - 360) * 0.25;
                double rad = t < 900 ? 30 : 22;
                look(s, cam(rad, t < 900 ? 16 : 9, ang), target);
            }
            if (t == 330) foundCity(s, level);
            for (int i = 0; i < BUILD.length; i++) {
                if (t == 400 + i * 50 && city != null) {
                    caption(BUILD_CAP[i]);
                    double a = Math.toRadians(i * 60 + 30);
                    BlockPos at = center.offset((int) Math.round(Math.cos(a) * 10), 0, (int) Math.round(Math.sin(a) * 10));
                    net.minecraft.core.Direction out = net.minecraft.core.Direction.getNearest(Math.cos(a), 0, Math.sin(a));
                    String why = CityBuildings.raise(level, city, BUILD[i], at, out);
                    Regnum.LOGGER.info("[TOUR] build {} -> {}", BUILD[i], why);
                }
            }
            if (t == 760) { caption("Армия города: щитоносцы, копейщики, рыцари, стрелки, мушкетёры, бомбардиры"); spawnArmy(s, level); }
            if (t == 950) { caption("Набег разбойников — бойцы защищают город"); spawnBandits(level); }
            if (t == 1400) {
                for (Entity e : actors) e.discard();
                actors.clear();
                for (Entity e : level.getEntities((Entity) null, new AABB(center).inflate(60),
                        e -> e instanceof SoldierEntity || e instanceof BanditEntity)) e.discard();
                observer(s).teleportTo(level, -12.5, 206, -15.5, -38, 18);
            }
            if (t == 1500) {
                done = true;
                Regnum.LOGGER.info("[TOUR] SCENES_DONE frames={}", frames);
            }
        } catch (Throwable ex) {
            Regnum.LOGGER.error("[TOUR] tick failed t=" + t, ex);
        }
    }

    private static void foundCity(MinecraftServer s, ServerLevel level) {
        ServerPlayer p = observer(s);
        BlockPos hall = center;
        var block = KingdomModule.TOWN_HALL.get();
        level.setBlock(hall, block.defaultBlockState(), 3);
        block.setPlacedBy(level, hall, block.defaultBlockState(), p, new ItemStack(KingdomModule.TOWN_HALL_ITEM.get()));
        city = KingdomData.get(s).byHall(hall);
        if (city != null) { city.treasury = 9999; city.level = 3; }
        Regnum.LOGGER.info("[TOUR] city={}", city == null ? "null" : city.name);
    }

    private static void spawnArmy(MinecraftServer s, ServerLevel level) {
        ServerPlayer p = observer(s);
        SoldierType[] roster = {SoldierType.SHIELDMAN, SoldierType.SHIELDMAN, SoldierType.SPEARMAN, SoldierType.SPEARMAN,
                SoldierType.KNIGHT, SoldierType.GREATSWORD, SoldierType.ARCHER, SoldierType.CROSSBOW,
                SoldierType.MUSKETEER, SoldierType.BOMBARDIER};
        for (int i = 0; i < roster.length; i++) {
            SoldierEntity e = KingdomModule.SOLDIER.get().create(level);
            if (e == null) continue;
            e.setup(roster[i], p.getUUID(), city == null ? null : city.id, 1);
            double x = center.getX() + 0.5 - 6 + (i % 5) * 3, z = center.getZ() + 0.5 + (i < 5 ? 5 : 8);
            int y = hgt(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z);
            e.moveTo(x, y, z, 0, 0);
            e.setPersistenceRequired();
            e.command(Order.GUARD, new Vec3(x, y, z), 0, Formation.LINE, i, 10, null);
            level.addFreshEntity(e);
            actors.add(e);
        }
    }

    private static void spawnBandits(ServerLevel level) {
        for (int i = 0; i < 8; i++) {
            BanditEntity b = KingdomModule.BANDIT.get().create(level);
            if (b == null) continue;
            b.setup(i % 3);
            double a = Math.toRadians(200 + i * 14);
            double x = center.getX() + 0.5 + Math.cos(a) * 26, z = center.getZ() + 0.5 + Math.sin(a) * 26;
            int y = hgt(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z);
            b.moveTo(x, y, z, 0, 0);
            b.setPersistenceRequired();
            level.addFreshEntity(b);
            actors.add(b);
            Entity soldier = actors.get(Math.min(i, 9));
            if (soldier instanceof SoldierEntity se) b.setTarget(se);
        }
    }
}
