package com.alkimor.regnum.client;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.kingdom.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Opt-in live demonstration in an isolated dev world; no ordinary gameplay hooks. */
@EventBusSubscriber(modid = Regnum.MODID, value = Dist.CLIENT)
public final class BattleShowcase {
    private static final int Y = 200;
    private static final SoldierType[] ROSTER = {SoldierType.SHIELDMAN, SoldierType.SHIELDMAN,
            SoldierType.SPEARMAN, SoldierType.SPEARMAN, SoldierType.KNIGHT, SoldierType.GREATSWORD,
            SoldierType.ARCHER, SoldierType.CROSSBOW, SoldierType.MUSKETEER, SoldierType.BOMBARDIER};
    private static final List<SoldierEntity> army = new ArrayList<>();
    private static volatile boolean initialized;
    private static boolean scheduled, previousNext, previousRepeat, previousFollow;
    private static boolean followCamera = Boolean.getBoolean("regnum.captureBattleFrames");
    private static int ready, intermission, capturedRound = -1;
    private static volatile int round, age;
    private static Mob boss;
    private static String bossName;
    private static int lastQueenAction = -1, lastQueenPhase = -1;
    private static final int[] castAction = {-1, -1}, castAge = {-1, -1}, castEntity = {-1, -1};
    private static final int captureRound = net.minecraft.util.Mth.clamp(Integer.getInteger("regnum.battleStartRound", 0), 0, 4);
    private static final String motionPrefix = (captureRound == 0 ? "queen_motion_" : "boss_motion_" + (captureRound + 1) + "_")
            + System.currentTimeMillis() + "_";
    private static int motionFrames, previousMotionAge = -1;
    private static boolean motionFinished, tourAdvancing, captureWindowPrepared, invalidCaptureLogged;
    private static volatile boolean broodVisible;
    private static boolean queenDefeatedAnnounced;
    private static List<com.alkimor.regnum.mine.CrawlerEntity> remainingBrood = List.of();
    private static int tourRoundFrames;
    private static final java.util.Map<java.util.UUID, Vec3> broodPositions = new java.util.HashMap<>();

    private BattleShowcase() {}
    private static boolean enabled() { return Boolean.getBoolean("regnum.battleShowcase"); }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        if (!enabled()) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (Boolean.getBoolean("regnum.captureBattleFrames") && !captureWindowPrepared) {
            // Prepare only this opted-in demo window, once; never fight later user minimization.
            long handle = mc.getWindow().getWindow();
            GLFW.glfwRestoreWindow(handle);
            GLFW.glfwSetWindowSize(handle, 1280, 720);
            captureWindowPrepared = true;
        }

        if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen onboarding) {
            onboarding.onClose(); return;
        }
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null || mc.level == null) return;
        if (!demoWorld(server)) return;
        if (TourCapture.enabled() && !TourCapture.done) return;
        if (!scheduled && ++ready >= 100) {
            scheduled = true;
            server.execute(() -> {
                try { setup(server); startRound(server, net.minecraft.util.Mth.clamp(Integer.getInteger("regnum.battleStartRound", 0), 0, 4)); initialized = true; }
                catch (Throwable t) { Regnum.LOGGER.error("[REGNUM-BATTLE] SETUP FAILED", t); }
            });
        }
        if (!initialized || mc.screen != null) return;
        if (boss != null) traceCast(mc.level.getEntity(boss.getId()), 1);
        if (TourCapture.enabled()) {
            mc.options.hideGui = true;
            if (age > 0 && age != previousMotionAge && age % 2 == 0 && tourRoundFrames < 120) {
                previousMotionAge = age; tourRoundFrames++; TourCapture.grab(mc);
            }
            if (tourRoundFrames >= 120 && !tourAdvancing) {
                tourAdvancing = true;
                int nxt = round + 1;
                if (nxt >= 5) TourCapture.finish(mc);
                else server.execute(() -> startRound(server, nxt));
            }
        }
        if (Boolean.getBoolean("regnum.captureBattleFrames") && !motionFinished) {
            if (round != captureRound || motionFrames >= 150) {
                motionFinished = true;
                Regnum.LOGGER.info("[REGNUM-BATTLE] MOTION_CAPTURE frames={} prefix={}", motionFrames, motionPrefix);
            } else if (age > 0 && age != previousMotionAge && age % 2 == 0
                    && (!Boolean.getBoolean("regnum.captureBroodFrames") || broodVisible)) {
                if (mc.getMainRenderTarget().width < 64 || mc.getMainRenderTarget().height < 64) {
                    if (!invalidCaptureLogged) Regnum.LOGGER.warn(
                            "[REGNUM-BATTLE] Capture suspended: framebuffer={}x{}; restore demo window",
                            mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);
                    invalidCaptureLogged = true;
                } else {
                invalidCaptureLogged = false;
                previousMotionAge = age;
                String file = motionPrefix + String.format(java.util.Locale.ROOT, "%04d", motionFrames++) + ".png";
                net.minecraft.client.Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), msg -> {});
                }
            }
        }
        if (age >= 200 && capturedRound != round) {
            capturedRound = round;
            net.minecraft.client.Screenshot.grab(mc.gameDirectory, "battle_" + (round + 1) + "_10seconds.png",
                    mc.getMainRenderTarget(), msg -> {});
            Regnum.LOGGER.info("[REGNUM-BATTLE] screenshot round={} (live battle)", round + 1);
        }
        long window = mc.getWindow().getWindow();
        boolean next = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_N) == GLFW.GLFW_PRESS;
        boolean repeat = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        boolean follow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F) == GLFW.GLFW_PRESS;
        if (follow && !previousFollow) followCamera = !followCamera;
        if (followCamera && boss != null) {
            Entity visibleBoss = mc.level.getEntity(boss.getId());
            if (visibleBoss != null && visibleBoss.isAlive()) {
                Vec3 focus = visibleBoss.position().add(0, visibleBoss.getBbHeight() * 0.55, 0);
                Vec3 desired = visibleBoss.position().add(-8, Math.max(4, visibleBoss.getBbHeight() + 1), -8);
                desired = new Vec3(net.minecraft.util.Mth.clamp(desired.x, -21, 21), desired.y,
                        net.minecraft.util.Mth.clamp(desired.z, -21, 21));
                Vec3 position = mc.player.position().lerp(desired, 0.15);
                mc.player.setPos(position);
                Vec3 look = focus.subtract(mc.player.getEyePosition());
                float yaw = (float) Math.toDegrees(Math.atan2(look.z, look.x)) - 90;
                float pitch = (float) -Math.toDegrees(Math.atan2(look.y, look.horizontalDistance()));
                mc.player.setYRot(mc.player.getYRot() + net.minecraft.util.Mth.wrapDegrees(yaw - mc.player.getYRot()) * 0.2F);
                mc.player.setXRot(net.minecraft.util.Mth.lerp(0.2F, mc.player.getXRot(), pitch));
            }
        }
        if (next && !previousNext) server.execute(() -> startRound(server, (round + 1) % 5));
        if (repeat && !previousRepeat) server.execute(() -> startRound(server, round));
        previousNext = next; previousRepeat = repeat;
        previousFollow = follow;
    }

    // Opt-in demo only: compare packet-driven client timing with real server cast timing.
    private static void traceCast(Entity entity, int side) {
        if (!(entity instanceof com.alkimor.regnum.dungeon.boss.VisualActor state)) return;
        int action = state.getVisualAction(), elapsed = state.getVisualActionElapsed();
        int impact = state.getVisualActionImpact();
        boolean newCast = entity.getId() != castEntity[side] || action != castAction[side]
                || elapsed < castAge[side];
        boolean release = action != 0 && castAge[side] < impact && elapsed >= impact;
        if (newCast || release) Regnum.LOGGER.info(
                "[REGNUM-CAST] side={} entity={} action={} elapsed={} impact={} duration={} phase={} release={}",
                side == 0 ? "server" : "client", entity.getId(), action, elapsed, impact,
                state.getVisualActionDuration(), state.getVisualPhase(), release);
        castEntity[side] = entity.getId(); castAction[side] = action; castAge[side] = elapsed;
    }

    private static ServerPlayer observer(MinecraftServer server) { return server.getPlayerList().getPlayers().get(0); }

    private static boolean demoWorld(MinecraftServer server) {
        return server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize()
                .getFileName().toString().matches("codex-showcase-20261002|regnum-tour");
    }

    private static void setup(MinecraftServer server) {
        ServerLevel level = server.overworld();
        var observer = observer(server);
        observer.setGameMode(GameType.SPECTATOR);
        level.setDayTime(6000);
        level.setWeatherParameters(999999, 0, false, false);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(false, server);
        // Thirteen solid block rows block the Queen's ten-block charge.
        for (int x = -36; x <= 36; x++) for (int z = -36; z <= 36; z++) {
            BlockPos floor = new BlockPos(x, Y, z);
            int edge = Math.max(Math.abs(x), Math.abs(z));
            level.setBlock(floor.below(), Blocks.DEEPSLATE.defaultBlockState(), 2);
            level.setBlock(floor, (edge >= 24 ? Blocks.CHISELED_STONE_BRICKS :
                    (x + z) % 5 == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState(), 2);
            for (int y = Y + 1; y <= Y + 15; y++) {
                var material = edge >= 24 && y <= Y + 11 ?
                        ((x % 6 == 0 || z % 6 == 0) ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_BRICKS) : Blocks.AIR;
                level.setBlock(new BlockPos(x, y, z), material.defaultBlockState(), 2);
            }
        }
        verifyQueenDashWalls(level);
        observer.sendSystemMessage(Component.literal("Демонстрация: 10 бойцов против босса. N — следующий босс, R — повтор, F — слежение за боссом. WASD + мышь — свободная камера, Esc — пауза. Бои идут с обычными HP и уроном."));
    }

    // Demo-only regression probe calls the actual server dash helper against the actual arena blocks.
    private static void verifyQueenDashWalls(ServerLevel level) {
        var queen = com.alkimor.regnum.mine.MineModule.CRAWLER_QUEEN.get().create(level);
        if (queen == null) throw new IllegalStateException("Cannot create dash probe");
        try {
            var dash = queen.getClass().getDeclaredMethod("safeDashEnd", ServerLevel.class, Vec3.class, double.class);
            dash.setAccessible(true);
            queen.moveTo(0, Y + 1, 0);
            Vec3 openEnd = (Vec3) dash.invoke(queen, level, new Vec3(1, 0, 0), 10.0);
            if (Math.abs(openEnd.distanceTo(queen.position()) - 10) > 0.001)
                throw new IllegalStateException("Open dash must reach exactly ten blocks");
            for (Vec3 direction : new Vec3[]{new Vec3(1, 0, 0), new Vec3(-1, 0, 0), new Vec3(0, 0, 1), new Vec3(0, 0, -1)}) {
                queen.moveTo(direction.x * 20, Y + 1, direction.z * 20);
                Vec3 end = (Vec3) dash.invoke(queen, level, direction, 10.0);
                double travel = end.distanceTo(queen.position());
                if (travel > 2.5 || travel < 0 || !level.noCollision(queen, queen.getBoundingBox().move(end.subtract(queen.position()))))
                    throw new IllegalStateException("Queen dash crossed arena wall: " + end);
            }
            Regnum.LOGGER.info("[REGNUM-BATTLE] DASH-WALLS OK: open=10 blocks, all four walls stop full hitbox");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Queen dash regression probe failed", e);
        }
    }

    private static void startRound(MinecraftServer server, int selected) {
        ServerLevel level = server.overworld();
        // A previous round can have escaped during a bug; remove its tracked combatants too.
        if (boss != null) boss.discard();
        for (SoldierEntity soldier : army) soldier.discard();
        tourRoundFrames = 0; tourAdvancing = false; previousMotionAge = -1;
        round = selected; age = 0; intermission = 0; army.clear(); capturedRound = -1;
        lastQueenAction = -1; lastQueenPhase = -1;
        BattleDodgeTelemetry.reset();
        broodPositions.clear();
        broodVisible = false;
        queenDefeatedAnnounced = false;
        remainingBrood = List.of();
        // Only the demo's elevated arena is cleared, including previous minions/projectiles.
        for (Entity entity : level.getEntities((Entity) null, new AABB(-45, Y - 4, -45, 45, Y + 20, 45),
                e -> !(e instanceof net.minecraft.world.entity.player.Player))) entity.discard();
        var type = switch (round) {
            case 0 -> com.alkimor.regnum.mine.MineModule.CRAWLER_QUEEN.get();
            case 1 -> com.alkimor.regnum.dungeon.DungeonModule.CRYPT_LORD.get();
            case 2 -> com.alkimor.regnum.dungeon.RegionsModule.MIRE_MOTHER.get();
            case 3 -> com.alkimor.regnum.dungeon.RegionsModule.FORGEMASTER.get();
            default -> com.alkimor.regnum.dungeon.RegionsModule.SCARAB_QUEEN.get();
        };
        boss = (Mob) type.create(level);
        if (boss == null) throw new IllegalStateException("Cannot create battle boss");
        boss.moveTo(0.5, Y + 1, 7.5, 180, 0);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(boss.blockPosition()), MobSpawnType.COMMAND, null);
        boss.setPersistenceRequired();
        level.addFreshEntity(boss);
        bossName = boss.getName().getString();
        if (TourCapture.enabled()) TourCapture.caption("Бой " + (round + 1) + "/5: " + bossName + " против 10 бойцов");
        ServerPlayer player = observer(server);
        player.setGameMode(GameType.SPECTATOR);
        for (int i = 0; i < ROSTER.length; i++) {
            SoldierEntity soldier = KingdomModule.SOLDIER.get().create(level);
            if (soldier == null) throw new IllegalStateException("Cannot create demo soldier");
            soldier.setup(ROSTER[i], player.getUUID(), null, 1);
            soldier.moveTo(-4.5 + (i % 5) * 2.5, Y + 1, i < 5 ? -5.5 : -10.5, 0, 0);
            soldier.setPersistenceRequired();
            soldier.command(Order.ATTACK_TARGET, new Vec3(0.5, Y + 1, 0.5), 0, Formation.LINE, i, 10, boss.getUUID());
            soldier.setTarget(boss);
            level.addFreshEntity(soldier);
            army.add(soldier);
        }
        boss.setTarget(army.get(0));
        player.teleportTo(level, -12.5, Y + 6.0, -15.5, -38, 18);
        player.sendSystemMessage(Component.literal("Бой " + (round + 1) + "/5: " + bossName + " против 10 бойцов."));
        Regnum.LOGGER.info("[REGNUM-BATTLE] START round={} boss={} hp={} army=10 AI=true", round + 1, bossName, boss.getHealth());
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        if (!enabled() || !initialized || boss == null || event.getServer().getPlayerList().getPlayers().isEmpty() || !demoWorld(event.getServer())) return;
        MinecraftServer server = event.getServer();
        traceCast(boss, 0);
        if (intermission > 0) {
            if (--intermission == 0) startRound(server, (round + 1) % 5);
            return;
        }
        age++;
        BattleDodgeTelemetry.tick(army, age);
        if (boss instanceof com.alkimor.regnum.mine.CrawlerQueenEntity queen) {
            if (age % 40 == 0) {
                for (var crawler : queen.level().getEntitiesOfClass(com.alkimor.regnum.mine.CrawlerEntity.class,
                        new AABB(-45, Y - 2, -45, 45, Y + 20, 45),
                        c -> c.isAlive() && c.getTags().contains("regnum_minion"))) {
                    broodVisible = true;
                    Vec3 previous = broodPositions.put(crawler.getUUID(), crawler.position());
                    var target = crawler.getTarget();
                    Regnum.LOGGER.info("[REGNUM-BROOD] t={} id={} moved={} target={} targetAlive={} noAI={} navigationDone={} pos={}",
                            age, crawler.getUUID(), previous == null ? -1 : previous.distanceTo(crawler.position()),
                            target == null ? "none" : target.getType().toShortString(),
                            target != null && target.isAlive(), crawler.isNoAi(), crawler.getNavigation().isDone(), crawler.position());
                }
            }
            int action = queen.getVisualAction(), phase = queen.getVisualPhase();
            if (action != lastQueenAction || phase != lastQueenPhase) {
                Regnum.LOGGER.info("[REGNUM-BATTLE] QUEEN t={} action={} phase={} duration={} pos={}",
                        age, action, phase, queen.getVisualActionDuration(), queen.blockPosition());
                lastQueenAction = action; lastQueenPhase = phase;
            }
        }
        if (age % 20 == 0 && boss.isAlive() && (Math.abs(boss.getX()) > 36 || Math.abs(boss.getZ()) > 36 || boss.getY() < Y - 2)) {
            Regnum.LOGGER.error("[REGNUM-BATTLE] ARENA ESCAPE round={} pos={}", round + 1, boss.blockPosition());
            observer(server).sendSystemMessage(Component.literal("Ошибка демонстрации: босс вышел за арену. Следующий бой через 10 секунд."));
            intermission = 200;
            return;
        }
        long alive = army.stream().filter(Entity::isAlive).count();
        if (round == 0 && !boss.isAlive() && (!queenDefeatedAnnounced || age % 20 == 0)) {
            remainingBrood = server.overworld().getEntitiesOfClass(com.alkimor.regnum.mine.CrawlerEntity.class,
                    new AABB(-45, Y - 2, -45, 45, Y + 20, 45),
                    c -> c.isAlive() && c.getTags().contains("regnum_minion"));
            if (!queenDefeatedAnnounced) {
                queenDefeatedAnnounced = true;
                if (!remainingBrood.isEmpty()) observer(server).sendSystemMessage(Component.literal(
                        "Королева повержена, но выводок ещё сражается. Отряд добивает оставшихся ползунов."));
                Regnum.LOGGER.info("[REGNUM-BATTLE] QUEEN_DOWN t={} brood={} army={}", age, remainingBrood.size(), alive);
            }
            // The demo commander changes its explicit attack order after the boss falls.
            // Ordinary soldier AI and combat statistics remain unchanged.
            for (int i = 0; i < army.size(); i++) {
                SoldierEntity soldier = army.get(i);
                if (!soldier.isAlive() || (soldier.getTarget() != null && soldier.getTarget().isAlive())) continue;
                com.alkimor.regnum.mine.CrawlerEntity nearest = null;
                double distance = Double.MAX_VALUE;
                for (var crawler : remainingBrood) {
                    double candidate = soldier.distanceToSqr(crawler);
                    if (candidate < distance) { distance = candidate; nearest = crawler; }
                }
                if (nearest != null) {
                    soldier.command(Order.ATTACK_TARGET, nearest.position(), 0, Formation.LINE, i, 10, nearest.getUUID());
                    soldier.setTarget(nearest);
                    Regnum.LOGGER.info("[REGNUM-BATTLE] BROOD_ORDER t={} soldier={} target={}", age, soldier.getId(), nearest.getId());
                }
            }
        }
        long broodAlive = remainingBrood.stream().filter(Entity::isAlive).count();
        if (age % 20 == 0) {
            observer(server).displayClientMessage(Component.literal(bossName + " · HP " + Math.round(boss.getHealth()) +
                    "/" + Math.round(boss.getMaxHealth()) + " · Бойцы " + alive + "/10" +
                    (!boss.isAlive() && broodAlive > 0 ? " · Выводок " + broodAlive : "") + " · N: дальше · R: повтор"), true);
        }
        if (age % 100 == 0) {
            long fighting = army.stream().filter(Entity::isAlive).filter(s -> s.getTarget() != null).count();
            double armyHp = army.stream().filter(Entity::isAlive).mapToDouble(SoldierEntity::getHealth).sum();
            String action = boss instanceof com.alkimor.regnum.mine.CrawlerQueenEntity q ? " action=" + q.getVisualAction() + " phase=" + q.getVisualPhase() : "";
            Regnum.LOGGER.info("[REGNUM-BATTLE] t={} bossHP={} armyAlive={} armyHP={} targets={} pos={}{}", age, boss.getHealth(), alive, Math.round(armyHp), fighting, boss.blockPosition(), action);
        }
        boolean enemiesDefeated = !boss.isAlive() && broodAlive == 0;
        if (enemiesDefeated || alive == 0 || age >= 1800) {
            String result = alive == 0 ? (enemiesDefeated ? "Обе стороны уничтожены" :
                    !boss.isAlive() ? "Победа выводка" : "Победа босса") :
                    enemiesDefeated ? "Победа отряда" : "Время демонстрации истекло";
            observer(server).sendSystemMessage(Component.literal(result + ". Следующий бой через 10 секунд; N — сразу, R — повтор."));
            Regnum.LOGGER.info("[REGNUM-BATTLE] END round={} result={} ticks={} army={} brood={}", round + 1, result, age, alive, broodAlive);
            intermission = 200;
        }
    }
}
