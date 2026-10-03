package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Трёхсторонняя арена для проверки боёв: босс, фракция А (15 бойцов игрока) и фракция Б (15 бойцов враждебного
 * королевства) стоят в углах треугольника на небесной платформе и бьют друг друга. Команда /regnum arena3 &lt;босс&gt;
 * (только оператор) или системное свойство regnum.arena3=&lt;босс&gt; для безголового прогона. Пишет в лог [REGNUM-ARENA3].
 * Платформа высоко над землёй, поэтому мир и постройки не страдают.
 */
public final class Arena3 {
    private Arena3() {}

    private static final SoldierType[] ROSTER = {SoldierType.SHIELDMAN, SoldierType.SHIELDMAN, SoldierType.SHIELDMAN, SoldierType.SPEARMAN, SoldierType.SPEARMAN,
            SoldierType.SPEARMAN, SoldierType.KNIGHT, SoldierType.KNIGHT, SoldierType.GREATSWORD, SoldierType.GREATSWORD,
            SoldierType.ARCHER, SoldierType.ARCHER, SoldierType.CROSSBOW, SoldierType.MUSKETEER, SoldierType.ARCHER};
    private static final int R = 26, SIDE = 15, MAX_TICKS = 20 * 60 * 10;

    public static final String TAG = "regnum_arena3";
    private static boolean active;
    private static ServerLevel level;
    private static Mob boss;
    private static Realm realm;
    private static final List<SoldierEntity> sideA = new ArrayList<>(), sideB = new ArrayList<>();
    private static long age;
    private static int baseY;
    private static BlockPos center;
    private static UUID viewer;
    private static GameType viewerMode;
    private static String bossKey = "";
    private static double bossMax;
    // диагностика: попадания по боссу и суммарный урон после всех множителей, сторона 0 = А, 1 = Б
    private static final int[] hits = new int[2];
    private static final double[] dealt = new double[2];
    private static final java.util.Map<Long, Integer> BASES = new java.util.HashMap<>();
    private static boolean autoDone, finished;

    private static EntityType<?> bossType(String key) {
        return switch (key) {
            case "mire" -> com.alkimor.regnum.dungeon.RegionsModule.MIRE_MOTHER.get();
            case "forge" -> com.alkimor.regnum.dungeon.RegionsModule.FORGEMASTER.get();
            case "scarab" -> com.alkimor.regnum.dungeon.RegionsModule.SCARAB_QUEEN.get();
            case "queen" -> com.alkimor.regnum.mine.MineModule.CRAWLER_QUEEN.get();
            case "crypt" -> com.alkimor.regnum.dungeon.DungeonModule.CRYPT_LORD.get();
            default -> null;
        };
    }

    public static boolean running() { return active; }

    public static String start(ServerLevel sl, BlockPos c, String key, ServerPlayer watcher) {
        if (active) return "Арена уже идёт: /regnum arena3 stop";
        EntityType<?> type = bossType(key);
        if (type == null) return "Боссы: crypt, mire, forge, scarab, queen.";
        level = sl;
        center = c;
        // высота платформы запоминается по месту: повторный запуск (в т.ч. с позиции зрителя в небе) не должен её поднимать
        long placeKey = BlockPos.asLong(c.getX(), 0, c.getZ());
        Integer known = BASES.get(placeKey);
        baseY = known != null ? known
                : Math.min(sl.getMaxBuildHeight() - 20, Math.max(sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, c.getX(), c.getZ()), c.getY()) + 25);
        BASES.put(placeKey, baseY);
        // чанки арены держим загруженными, иначе существа в них не живут
        for (int cx = (c.getX() - R) >> 4; cx <= (c.getX() + R) >> 4; cx++) for (int cz = (c.getZ() - R) >> 4; cz <= (c.getZ() + R) >> 4; cz++) {
            sl.setChunkForced(cx, cz, true);
            sl.getChunk(cx, cz);
        }
        // платформа с бортиком, воздух над ней
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d > R) continue;
            for (int y = 1; y <= 14; y++) sl.setBlock(new BlockPos(c.getX() + x, baseY + y, c.getZ() + z), Blocks.AIR.defaultBlockState(), 2);
            sl.setBlock(new BlockPos(c.getX() + x, baseY, c.getZ() + z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
            if (d > R - 1.5) for (int y = 1; y <= 3; y++) sl.setBlock(new BlockPos(c.getX() + x, baseY + y, c.getZ() + z), Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
        }
        // чистим только временных участников прошлых запусков (метка), на любой высоте колонки арены; чужие мобы, предметы и подставки не трогаются
        for (Entity e : sl.getEntities((Entity) null, new AABB(c.getX() - R - 4, sl.getMinBuildHeight(), c.getZ() - R - 4, c.getX() + R + 4, sl.getMaxBuildHeight() + 64, c.getZ() + R + 4), e -> e.getTags().contains(TAG))) e.discard();

        realm = new Realm(UUID.randomUUID(), "Арена-Б", "Тест", Culture.EMPIRE.ordinal(), c.getX(), c.getZ());
        realm.state = Realm.WAR;
        realm.known = 2;
        KingdomData data = KingdomData.get(sl.getServer());
        data.addRealm(realm);

        UUID ownerA = watcher != null ? watcher.getUUID() : UUID.nameUUIDFromBytes("regnum-arena3-A".getBytes());
        sideA.clear();
        sideB.clear();
        bossKey = key;
        Vec3 pb = vertex(0), pa = vertex(1), pbb = vertex(2);
        boss = (Mob) type.create(sl);
        boss.moveTo(pb.x, baseY + 1, pb.z, yawTo(pb), 0);
        boss.finalizeSpawn(sl, sl.getCurrentDifficultyAt(boss.blockPosition()), MobSpawnType.COMMAND, null);
        boss.setPersistenceRequired();
        boss.addTag(TAG);
        sl.addFreshEntity(boss);
        bossMax = boss.getMaxHealth();
        for (int i = 0; i < SIDE; i++) {
            SoldierEntity a = KingdomModule.SOLDIER.get().create(sl);
            a.setup(ROSTER[i], ownerA, null, 1);
            a.addTag(TAG);
            place(a, pa, i);
            a.command(Order.ATTACK_TARGET, boss.position(), yawTo(pa), Formation.LINE, i, SIDE, boss.getUUID());
            a.setTarget(boss);
            sl.addFreshEntity(a);
            sideA.add(a);
            SoldierEntity b = Realms.spawn(sl, realm, ROSTER[i], BlockPos.containing(pbb.x, baseY + 1, pbb.z), 1);
            if (b == null) continue;
            place(b, pbb, i);
            b.addTag(TAG);
            b.setPersistenceRequired();
            b.command(Order.ATTACK_TARGET, boss.position(), yawTo(pbb), Formation.LINE, i, SIDE, boss.getUUID());
            b.setTarget(boss);
            sideB.add(b);
        }
        boss.setTarget(sideA.get(0));
        if (watcher != null) {
            viewer = watcher.getUUID();
            viewerMode = watcher.gameMode.getGameModeForPlayer();
            watcher.setGameMode(GameType.SPECTATOR);
            watcher.teleportTo(sl, c.getX() + 0.5, baseY + 11, c.getZ() + R * 0.9, 180, 25);
        }
        Regnum.LOGGER.info("[REGNUM-ARENA3] spawned boss alive={} A alive={} B alive={} baseY={}", boss.isAlive(), alive(sideA), alive(sideB), baseY);
        active = true;
        finished = false;
        age = 0;
        hits[0] = hits[1] = 0;
        dealt[0] = dealt[1] = 0;
        Regnum.LOGGER.info("[REGNUM-ARENA3] START boss={} hp={} A={} B={}", key, boss.getHealth(), sideA.size(), sideB.size());
        return null;
    }

    private static Vec3 vertex(int i) {
        double ang = Math.toRadians(90 + 120 * i);
        return new Vec3(center.getX() + 0.5 + Math.cos(ang) * 16, baseY + 1, center.getZ() + 0.5 + Math.sin(ang) * 16);
    }

    private static float yawTo(Vec3 from) {
        double dx = center.getX() + 0.5 - from.x, dz = center.getZ() + 0.5 - from.z;
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private static void place(SoldierEntity s, Vec3 v, int i) {
        double ox = -4.5 + (i % 5) * 2.0, oz = (i / 5) * 2.0;
        s.moveTo(v.x + ox, baseY + 1, v.z + oz, yawTo(v), 0);
        s.setPersistenceRequired();
    }

    public static void stop(MinecraftServer server, String why) {
        if (!active) return;
        active = false;
        for (SoldierEntity s : sideA) s.discard();
        for (SoldierEntity s : sideB) s.discard();
        if (boss != null) boss.discard();
        sideA.clear();
        sideB.clear();
        KingdomData data = KingdomData.get(server);
        if (realm != null) { data.realms().removeIf(r -> r.id.equals(realm.id)); data.setDirty(); }
        if (level != null && center != null) {
            // снести платформу и бортик, чтобы небо осталось чистым
            for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) {
                if (Math.sqrt(x * x + z * z) > R) continue;
                for (int y = 0; y <= 4; y++) level.setBlock(new BlockPos(center.getX() + x, baseY + y, center.getZ() + z), Blocks.AIR.defaultBlockState(), 2);
            }
        }
        if (viewer != null) {
            ServerPlayer p = server.getPlayerList().getPlayer(viewer);
            if (p != null) {
                // платформа снесена: возвращаем зрителя на землю, иначе он упадёт с высоты в своём обычном режиме
                if (level != null && center != null) {
                    int gy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, center.getX(), center.getZ());
                    p.teleportTo(level, center.getX() + 0.5, gy + 0.5, center.getZ() + 0.5, p.getYRot(), 0);
                }
                if (viewerMode != null) p.setGameMode(viewerMode);
            }
            viewer = null;
        }
        if (level != null && center != null)
            for (int cx = (center.getX() - R) >> 4; cx <= (center.getX() + R) >> 4; cx++) for (int cz = (center.getZ() - R) >> 4; cz <= (center.getZ() + R) >> 4; cz++) level.setChunkForced(cx, cz, false);
        Regnum.LOGGER.info("[REGNUM-ARENA3] STOP {}", why);
    }

    /** Безопасная очистка при остановке сервера: зритель возвращается в свой режим, временная держава удаляется. */
    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        if (active) stop(e.getServer(), "shutdown");
    }

    private static long alive(List<SoldierEntity> l) { return l.stream().filter(Entity::isAlive).count(); }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        String auto = System.getProperty("regnum.arena3");
        if (!autoDone && auto != null && !auto.isEmpty() && server.getTickCount() == 200) {
            autoDone = true;
            String err = start(server.overworld(), new BlockPos(0, 64, 0), auto, null);
            if (err != null) Regnum.LOGGER.warn("[REGNUM-ARENA3] {}", err);
        }
        if (!active || finished) return;
        age++;
        // бойцы-ветераны А и Б не должны вернуться на HOLD, пока босс жив; после смерти босса фракции дерутся между собой
        long a = alive(sideA), b = alive(sideB);
        boolean bossAlive = boss != null && boss.isAlive();
        if (age % 100 == 0) {
            Regnum.LOGGER.info("[REGNUM-ARENA3] t={} bossHP={}/{} A={}/{} B={}/{}", age, bossAlive ? Math.round(boss.getHealth()) : 0, Math.round(bossMax), a, SIDE, b, SIDE);
            Regnum.LOGGER.info("[REGNUM-ARENA3] diag t={} A: на боссе {} на Б {} попаданий {} урон {} | Б: на боссе {} на А {} попаданий {} урон {}", age,
                    targeting(sideA, boss), targeting(sideA, sideB), hits[0], Math.round(dealt[0]),
                    targeting(sideB, boss), targeting(sideB, sideA), hits[1], Math.round(dealt[1]));
            if (viewer != null) {
                ServerPlayer p = server.getPlayerList().getPlayer(viewer);
                if (p != null) Text.bar(p, "Босс " + (bossAlive ? Math.round(boss.getHealth()) : 0) + " | А " + a + "/15 | Б " + b + "/15", net.minecraft.ChatFormatting.GOLD);
            }
        }
        if (!bossAlive && age % 20 == 0) {
            // босс пал — оставшиеся фракции целятся друг в друга
            for (SoldierEntity s : sideA) if (s.isAlive() && (s.getTarget() == null || !s.getTarget().isAlive())) retarget(s, sideB);
            for (SoldierEntity s : sideB) if (s.isAlive() && (s.getTarget() == null || !s.getTarget().isAlive())) retarget(s, sideA);
        }
        if ((!bossAlive && (a == 0 || b == 0)) || (bossAlive && a == 0 && b == 0) || age >= MAX_TICKS) {
            String res = !bossAlive ? (a == 0 && b == 0 ? "никто" : a > 0 ? "фракция А" : "фракция Б") : "босс";
            finished = true;
            Regnum.LOGGER.info("[REGNUM-ARENA3] END winner={} ticks={} bossHP={} A={} B={} hitsA={} hitsB={} dmgA={} dmgB={}", res, age, bossAlive ? Math.round(boss.getHealth()) : 0, a, b, hits[0], hits[1], Math.round(dealt[0]), Math.round(dealt[1]));
            if (viewer != null) {
                ServerPlayer p = server.getPlayerList().getPlayer(viewer);
                if (p != null) Text.gold(p, "Арена окончена. Победитель: " + res + " (А " + a + ", Б " + b + ").");
            }
            if (auto != null && !auto.isEmpty() && viewer == null) { stop(server, "auto"); server.halt(false); return; }
            if (viewer == null) stop(server, "done");
        }
    }

    private static long targeting(List<SoldierEntity> side, net.minecraft.world.entity.LivingEntity t) {
        return side.stream().filter(x -> x.isAlive() && x.getTarget() == t).count();
    }

    private static long targeting(List<SoldierEntity> side, List<SoldierEntity> others) {
        return side.stream().filter(x -> x.isAlive() && x.getTarget() instanceof SoldierEntity o && others.contains(o)).count();
    }

    @SubscribeEvent
    public static void onBossHurt(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post ev) {
        if (!active || boss == null || ev.getEntity() != boss) return;
        if (!(ev.getSource().getEntity() instanceof SoldierEntity s)) return;
        int side = sideA.contains(s) ? 0 : sideB.contains(s) ? 1 : -1;
        if (side < 0) return;
        hits[side]++;
        dealt[side] += ev.getNewDamage();
    }

    private static void retarget(SoldierEntity s, List<SoldierEntity> enemies) {
        SoldierEntity best = null;
        double bd = Double.MAX_VALUE;
        for (SoldierEntity o : enemies) {
            if (!o.isAlive()) continue;
            double d = s.distanceToSqr(o);
            if (d < bd) { bd = d; best = o; }
        }
        if (best != null) {
            s.setTarget(best);
            s.command(Order.ATTACK_TARGET, best.position(), s.getYRot(), Formation.LINE, 0, 1, best.getUUID());
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("arena3").requires(s -> s.hasPermission(2))
                .then(Commands.literal("stop").executes(ctx -> {
                    stop(ctx.getSource().getServer(), "command");
                    return 1;
                }))
                .then(Commands.argument("boss", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    String err = start(p.serverLevel(), p.blockPosition(), StringArgumentType.getString(ctx, "boss"), p);
                    if (err != null) { Text.bad(p, err); return 0; }
                    Text.good(p, "Арена: босс против двух фракций по 15. Вы наблюдаете (режим наблюдателя вернётся по окончании: /regnum arena3 stop).");
                    return 1;
                }))));
    }
}
