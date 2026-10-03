package com.alkimor.regnum.client;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.client.screen.CityScreen;
import com.alkimor.regnum.client.screen.CommandScreen;
import com.alkimor.regnum.client.screen.JournalScreen;
import com.alkimor.regnum.client.screen.TradeScreen;
import com.alkimor.regnum.core.ModRegistries;
import com.alkimor.regnum.core.network.ArmyInfoPayload;
import com.alkimor.regnum.core.network.CityInfoPayload;
import com.alkimor.regnum.core.network.TradeInfoPayload;
import com.alkimor.regnum.dungeon.DungeonModule;
import com.alkimor.regnum.dungeon.RegionsModule;
import com.alkimor.regnum.kingdom.BanditEntity;
import com.alkimor.regnum.kingdom.KingdomModule;
import com.alkimor.regnum.kingdom.SoldierEntity;
import com.alkimor.regnum.kingdom.SoldierType;
import com.alkimor.regnum.wanderers.Persona;
import com.alkimor.regnum.wanderers.WandererEntity;
import com.alkimor.regnum.wanderers.WanderersModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Визуальный автотест (-Dregnum.visualtest=true, задача runVisualTest):
 * загружает мир, расставляет мобов, боссов, блоки, облетает данжи и делает скриншоты в run-visual/screenshots.
 */
@EventBusSubscriber(modid = Regnum.MODID, value = Dist.CLIENT)
public final class ClientVisualTest {
    private ClientVisualTest() {}

    private record Step(String name, Consumer<MinecraftServer> server, Runnable client, int delay, boolean gui) {}

    private static final List<Step> STEPS = new ArrayList<>();
    private static final List<Entity> scene = new ArrayList<>();
    private static final java.util.Set<String> REQUIRED_STEPS = new java.util.HashSet<>();
    private static ServerLevel sceneLevel;
    private static int index = -1, timer = 0, readyTicks = 0;
    private static boolean shotPending = false;
    private static volatile boolean serverReady = true;
    private static volatile Throwable serverFailure;
    private static Runnable pendingClient;
    private static boolean originalMineLite;

    private static final int Y = 200;
    private static BlockPos DT_AT;
    private static BlockPos DT_ARENA;
    private static java.util.UUID crawlerTestLossTarget;
    private static java.util.UUID crawlerTestBackupTarget;
    private static com.alkimor.regnum.mine.CrawlerQueenEntity crawlerTestQueen;
    private static SoldierEntity crawlerTestPrimary;
    private static SoldierEntity crawlerTestBackup;
    private static SoldierEntity crawlerTestFlank;
    private static final java.util.Set<java.util.UUID> crawlerTestReplacementTargets = new java.util.HashSet<>();
    private static long crawlerTestTargetLostAt = -1L;
    private static final java.util.Map<java.util.UUID, net.minecraft.world.phys.Vec3> crawlerTestPositionsAtLoss = new java.util.HashMap<>();
    private static final List<com.alkimor.regnum.mine.CrawlerEntity> crawlerTestMinions = new ArrayList<>();
    private static java.util.UUID cryptBattleBossId;
    private static final java.util.Set<Integer> cryptBattleActionsSeen = new java.util.HashSet<>();
    private static int cryptBattlePeakPhase;

    private static boolean enabled() {
        return Boolean.getBoolean("regnum.visualtest");
    }

    private static void log(String s) {
        Regnum.LOGGER.info("[REGNUM-VISUAL] {}", s);
    }

    // ================================================================== сценарий

    private static void build() {
        STEPS.add(new Step(null, ClientVisualTest::setupWorld, null, 60, false));
        STEPS.add(new Step("00_world_flora_overview", ClientVisualTest::floraShowcase, null, 100, false));
        STEPS.add(new Step("00_world_flora_close_west", s -> { floraShowcase(s); floraCamera(s, -11.5); }, null, 100, false));
        STEPS.add(new Step("00_world_flora_close_east", s -> { floraShowcase(s); floraCamera(s, 11.5); }, null, 100, false));
        STEPS.add(new Step("00_world_natural_flora", ClientVisualTest::naturalFlora, null, 300, false));
        STEPS.add(new Step("00_world_natural_forest", s -> naturalFlora(s, true), null, 300, false));

        for (int culture = 0; culture < 6; culture++) {
            final int selectedCulture = culture;
            for (int group = 0; group < (SoldierType.values().length + 3) / 4; group++) {
                final int first = group * 4;
                STEPS.add(new Step("01_army_" + com.alkimor.regnum.kingdom.Culture.byId(culture).id + "_" + group, s -> {
                    clearScene();
                    SoldierType[] types = SoldierType.values();
                    for (int i = first; i < Math.min(first + 4, types.length); i++) {
                        SoldierEntity e = KingdomModule.SOLDIER.get().create(level(s));
                        e.setup(types[i], player(s).getUUID(), null, 1);
                        e.setCulture(com.alkimor.regnum.kingdom.Culture.byId(selectedCulture));
                        place(e, -6 + (i - first) * 4, 0);
                    }
                    camera(s, 0, -11, 2);
                }, null, 80, false));
            }
        }

        STEPS.add(new Step("31_wall", sv -> {
            clearScene();
            ServerLevel l = level(sv);
            BlockPos hall = new BlockPos(0, Y + 1, -30);
            var city = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), player(sv).getUUID(), "Тест", hall);
            city.treasury = 500;
            city.culture = 1;
            var job = com.alkimor.regnum.kingdom.WallJob.plan(List.of(new BlockPos(-14, Y, 8), new BlockPos(0, Y, 8), new BlockPos(14, Y, 8)),
                    List.of(false, true, false), com.alkimor.regnum.kingdom.WallStyle.FORTRESS);
            // достроено две трети: строитель работает на правом краю
            int limit = job.total() * 2 / 3;
            while (job.cell < limit && com.alkimor.regnum.kingdom.Walls.step(l, city, job) != com.alkimor.regnum.kingdom.Walls.Step.DONE) { }
            var hut = KingdomModule.BUILDER_HUT.get().defaultBlockState();
            l.setBlock(new BlockPos(-6, Y + 1, 3), hut, 3);
            net.minecraft.world.entity.npc.Villager b = EntityType.VILLAGER.create(l);
            b.setVillagerData(b.getVillagerData().setProfession(net.minecraft.world.entity.npc.VillagerProfession.MASON).setLevel(2));
            b.setNoAi(true);
            b.setCustomName(net.minecraft.network.chat.Component.literal("Строитель"));
            b.setCustomNameVisible(true);
            place(b, 7, 5);
            camera(sv, 2, -9, 12);
        }, null, 80, false));

        for (int c = 0; c < 6; c++) {
            final int cu = c;
            String cid = com.alkimor.regnum.kingdom.Culture.byId(c).id;
            STEPS.add(new Step("32_capital_" + cid, sv -> {
                clearScene();
                ServerLevel l = level(sv);
                for (int x = -75; x <= 75; x++)
                    for (int z = -75; z <= 75; z++) {
                        l.setBlock(new BlockPos(x, Y, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                        for (int y = Y + 1; y <= Y + 50; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                var kd = com.alkimor.regnum.kingdom.KingdomData.get(sv);
                var realm = new com.alkimor.regnum.kingdom.Realm(java.util.UUID.randomUUID(), "Тестовое королевство", "правитель", cu, 0, 0);
                kd.addRealm(realm);
                com.alkimor.regnum.kingdom.Realms.buildAt(l, realm, 0, Y, 0);
                for (var e : l.getEntitiesOfClass(com.alkimor.regnum.kingdom.SoldierEntity.class, new net.minecraft.world.phys.AABB(-60, Y - 5, -60, 60, Y + 30, 60))) scene.add(e);
                player(sv).teleportTo(l, 0.5, Y + 62, 108.5, 180f, 32f);
            }, null, 200, false));
            STEPS.add(new Step("33_capital_in_" + cid, sv -> player(sv).teleportTo(level(sv), 0.5, Y + 20, 62.5, 180f, 14f), null, 80, false));
        }

        for (int c = 0; c < 6; c++) {
            final int cu = c;
            STEPS.add(new Step("40_kit_" + com.alkimor.regnum.kingdom.Culture.byId(c).id, sv -> {
                clearScene();
                ServerLevel l = level(sv);
                for (int x = -75; x <= 75; x++)
                    for (int z = -30; z <= 30; z++) {
                        l.setBlock(new BlockPos(x, Y, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                        for (int y = Y + 1; y <= Y + 36; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                com.alkimor.regnum.kingdom.WallKit.of(cu).showcase(l, -30, 0, 60, true);
                player(sv).teleportTo(l, 0.5, Y + 14, -42.5, 0f, 16f);
            }, null, 160, false));
            STEPS.add(new Step("41_kit_back_" + com.alkimor.regnum.kingdom.Culture.byId(c).id, sv -> {
                player(sv).teleportTo(level(sv), 0.5, Y + 14, 42.5, 180f, 16f);
            }, null, 80, false));
        }

        String[] dts = {"undead_crypt", "creeping_crypt", "toxic_lair", "lone_citadel", "desert_ruins", "bunker", "stray_fort", "witch_villa", "illager_camp"};
        for (String dn : dts) {
            STEPS.add(new Step("50_dt_" + dn, sv -> {
                clearScene();
                com.alkimor.regnum.dungeon.BossHook.lastArena = null;
                ServerLevel l = level(sv);
                var reg = l.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
                var h = reg.getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, com.alkimor.regnum.Regnum.id("dt/" + dn)));
                String stepName = "50_dt_" + dn;
                if (h.isEmpty()) {
                    log("DUNGEON_LOCATOR_MISS " + dn + ": structure registry entry missing");
                    if (REQUIRED_STEPS.contains(stepName)) throw new IllegalStateException("Required dungeon is not registered: regnum:dt/" + dn);
                    return;
                }
                BlockPos origin = l.getSharedSpawnPos();
                log("DUNGEON_LOCATOR_SEARCH " + dn + " origin=" + origin.getX() + "," + origin.getZ() + " radiusChunks=120");
                var found = l.getChunkSource().getGenerator().findNearestMapStructure(l, net.minecraft.core.HolderSet.direct(h.get()), origin, 120, false);
                if (found == null) {
                    log("DUNGEON_LOCATOR_MISS " + dn + ": no candidate near shared spawn");
                    if (REQUIRED_STEPS.contains(stepName)) throw new IllegalStateException("Required dungeon was not located near shared spawn: regnum:dt/" + dn);
                    return;
                }
                BlockPos at = found.getFirst();
                log("DUNGEON_LOCATOR_FOUND " + dn + " at=" + at.getX() + "," + at.getY() + "," + at.getZ());
                for (int dx = -9; dx <= 9; dx++)
                    for (int dz = -9; dz <= 9; dz++) l.getChunk((at.getX() >> 4) + dx, (at.getZ() >> 4) + dz);
                com.alkimor.regnum.dungeon.BossHook.flush(l);
                int gy = l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, at.getX(), at.getZ());
                DT_AT = at;
                if (dn.equals("undead_crypt")) {
                    DT_ARENA = findCryptArena(l, at);
                    if (DT_ARENA == null) {
                        log("DUNGEON_ARENA_MISS undead_crypt: no matching crypt altar near located structure");
                        if (REQUIRED_STEPS.contains("51_dt_arena_undead_crypt")
                                || REQUIRED_STEPS.contains("52_dt_boss_undead_crypt"))
                            throw new IllegalStateException("Required crypt arena not found near the selected structure");
                    } else {
                        log("DUNGEON_ARENA_MATCH undead_crypt at=" + DT_ARENA.toShortString());
                        // The selected crypt is underground. A camera ten blocks over the arena
                        // starts inside natural stone; use a high interior shot below the roof.
                        player(sv).teleportTo(l, DT_ARENA.getX() + 0.5, DT_ARENA.getY() + 4,
                                DT_ARENA.getZ() - 5.5, 0f, 12f);
                    }
                } else {
                    player(sv).teleportTo(l, at.getX() + 0.5, gy + 35, at.getZ() - 55.5, 0f, 30f);
                }
            }, null, 500, false));
            STEPS.add(new Step("51_dt_arena_" + dn, sv -> {
                var a = dn.equals("undead_crypt") ? DT_ARENA : com.alkimor.regnum.dungeon.BossHook.lastArena;
                if (a == null) {
                    log("DUNGEON_ARENA_MISS " + dn + ": BossHook produced no arena");
                    if (REQUIRED_STEPS.contains("51_dt_arena_" + dn)) throw new IllegalStateException("Required boss arena was not generated: " + dn);
                    return;
                }
                log("DUNGEON_ARENA_FOUND " + dn + " at=" + a.getX() + "," + a.getY() + "," + a.getZ());
                DT_ARENA = a;
                player(sv).teleportTo(level(sv), a.getX() + 0.5, a.getY() + 3, a.getZ() - 5.5, 0f, 20f);
            }, null, 300, false));
        }

        STEPS.add(new Step("52_dt_boss_undead_crypt", sv -> {
            clearScene();
            ServerLevel l = level(sv);
            BlockPos arena = DT_ARENA;
            if (arena == null) {
                log("DUNGEON_BOSS_MISS undead_crypt: no arena position");
                if (REQUIRED_STEPS.contains("52_dt_boss_undead_crypt")) throw new IllegalStateException("Required crypt boss showcase has no arena");
                return;
            }
            BlockPos altarPos = arena.offset(0, 0, 6);
            if (!l.getBlockState(altarPos).is(DungeonModule.CRYPT_ALTAR.get())) {
                log("DUNGEON_BOSS_MISS undead_crypt: crypt altar absent at " + altarPos.toShortString());
                if (REQUIRED_STEPS.contains("52_dt_boss_undead_crypt")) throw new IllegalStateException("Crypt summon altar not found at generated arena");
                return;
            }
            var lord = DungeonModule.CRYPT_LORD.get().create(l);
            if (lord == null) throw new IllegalStateException("Crypt Lord entity factory returned null");
            lord.moveTo(arena.getX() + 0.5, arena.getY(), arena.getZ() + 0.5, 180f, 0f);
            lord.finalizeSpawn(l, l.getCurrentDifficultyAt(arena), MobSpawnType.TRIGGERED, null);
            lord.restrictTo(altarPos, 20);
            lord.setTarget(player(sv));
            if (!l.addFreshEntity(lord)) throw new IllegalStateException("Crypt Lord was rejected by the test world");
            cryptBattleBossId = lord.getUUID();
            cryptBattleActionsSeen.clear();
            cryptBattlePeakPhase = 1;
            scene.add(lord);
            player(sv).teleportTo(l, arena.getX() + 0.5, arena.getY() + 1, arena.getZ() - 6.5, 0f, -10f);
            log("DUNGEON_BOSS_SPAWNED undead_crypt: real CryptLordEntity at=" + arena.toShortString());
        }, null, 50, true));

        STEPS.add(new Step("53_dt_live_crypt_phase2_a", ClientVisualTest::startCryptBattle, null, 25, false));
        STEPS.add(new Step("54_dt_live_crypt_phase2_b", s -> sampleCryptBattle(s, "phase2_b"), null, 25, false));
        STEPS.add(new Step("55_dt_live_crypt_phase2_c", s -> sampleCryptBattle(s, "phase2_c"), null, 25, false));
        STEPS.add(new Step("56_dt_live_crypt_phase3_a", ClientVisualTest::forceCryptPhaseThree, null, 25, false));
        STEPS.add(new Step("57_dt_live_crypt_phase3_b", s -> sampleCryptBattle(s, "phase3_b"), null, 25, false));
        STEPS.add(new Step("58_dt_live_crypt_phase3_c", s -> sampleCryptBattle(s, "phase3_c"), null, 25, false));
        STEPS.add(new Step("59_dt_live_crypt_fight_summary", ClientVisualTest::finishCryptBattle, null, 1, false));

        // 60: осадные орудия и здания игроков
        STEPS.add(new Step("60_siege", sv -> {
            clearScene();
            ServerLevel l = level(sv);
            for (int x = -50; x <= 50; x++)
                for (int z = -40; z <= 40; z++) {
                    l.setBlock(new BlockPos(x, Y, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    for (int y = Y + 1; y <= Y + 30; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            for (int x = -40; x <= 40; x++)
                for (int y = Y + 1; y <= Y + 8; y++)
                    for (int z = 18; z <= 20; z++) l.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
            var kd = com.alkimor.regnum.kingdom.KingdomData.get(sv);
            var realm = new com.alkimor.regnum.kingdom.Realm(java.util.UUID.randomUUID(), "Осаждающие", "правитель", 1, 0, -200);
            kd.addRealm(realm);
            var cat = KingdomModule.CATAPULT.get().create(l);
            cat.setRealm(realm.id);
            cat.moveTo(-14.5, Y + 1, -8.5, 0f, 0f);
            l.addFreshEntity(cat);
            scene.add(cat);
            var tw = KingdomModule.SIEGE_TOWER.get().create(l);
            tw.setRealm(realm.id);
            tw.moveTo(10.5, Y + 1, 2.5, 0f, 0f);
            l.addFreshEntity(tw);
            scene.add(tw);
            var cv = KingdomModule.CARAVAN.get().create(l);
            cv.moveTo(-3.5, Y + 1, -2.5, 0f, 0f);
            l.addFreshEntity(cv);
            scene.add(cv);
            player(sv).teleportTo(l, -3.5, Y + 6, -22.5, 0f, 10f);
        }, null, 260, false));
        STEPS.add(new Step("60_siege_fire", sv -> {
            ServerLevel l = level(sv);
            for (var e : l.getEntitiesOfClass(com.alkimor.regnum.kingdom.CatapultEntity.class, new net.minecraft.world.phys.AABB(-60, Y - 5, -60, 60, Y + 30, 60))) l.broadcastEntityEvent(e, (byte) 4);
            player(sv).teleportTo(l, -14.5, Y + 3, -20.5, 0f, 8f);
        }, null, 16, false));
        STEPS.add(new Step("60_siege_fire2", sv -> {}, null, 40, false));
        STEPS.add(new Step("61_tower_side", sv -> player(sv).teleportTo(level(sv), 10.5, Y + 6, -14.5, 0f, 12f), null, 100, false));
        String[] bt = {"BARRACKS", "MARKET", "WATCHTOWER", "TRAINING_GROUND", "BUILDER_HUT"};
        for (int bi = 0; bi < bt.length; bi++) {
            final int bix = bi;
            STEPS.add(new Step("62_building_" + bt[bi].toLowerCase(), sv -> {
                clearScene();
                ServerLevel l = level(sv);
                for (int x = -40; x <= 40; x++)
                    for (int z = -40; z <= 40; z++) {
                        l.setBlock(new BlockPos(x, Y, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                        for (int y = Y + 1; y <= Y + 40; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                var kd = com.alkimor.regnum.kingdom.KingdomData.get(sv);
                var c = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "Витрина", new BlockPos(0, Y, 0));
                c.treasury = 9999;
                c.level = 3;
                c.culture = 0;
                kd.add(c);
                String why = com.alkimor.regnum.kingdom.CityBuildings.raise(l, c, com.alkimor.regnum.kingdom.BuildingType.valueOf(bt[bix]), new BlockPos(0, Y, -20), net.minecraft.core.Direction.SOUTH);
                kd.remove(c);
                com.alkimor.regnum.Regnum.LOGGER.info("[VISUAL] building {} -> {}", bt[bix], why);
                player(sv).teleportTo(l, 0.5, Y + 12, -48.5, 0f, 22f);
            }, null, 120, false));
        }

        STEPS.add(new Step("02_bandits_wanderers", s -> {
            clearScene();
            for (int i = 0; i < 3; i++) {
                BanditEntity b = KingdomModule.BANDIT.get().create(level(s));
                b.setup(i);
                place(b, -6 + i * 2, 0);
            }
            Persona[] ps = {Persona.GUIDE, Persona.TRICKSTER, Persona.HERMIT, Persona.MERCHANT, Persona.NOBLE};
            for (int i = 0; i < ps.length; i++) {
                WandererEntity w = WanderersModule.WANDERER.get().create(level(s));
                w.setup(ps[i]);
                place(w, 1 + i * 2, 0);
            }
            WandererEntity child = WanderersModule.WANDERER.get().create(level(s));
            child.setup(Persona.HEIR);
            child.setChild(true);
            place(child, 0, -2);
            camera(s, 0, -9, 10);
        }, null, 60, false));

        STEPS.add(new Step("02b_army_closeup", s -> {
            clearScene();
            int[][] picks = {{0, 3}, {1, 1}, {2, 3}, {3, 3}, {4, 1}, {5, 3}};
            for (int i = 0; i < picks.length; i++) {
                SoldierEntity e = KingdomModule.SOLDIER.get().create(level(s));
                e.setup(SoldierType.values()[picks[i][1]], player(s).getUUID(), null, 1);
                e.setCulture(com.alkimor.regnum.kingdom.Culture.byId(picks[i][0]));
                place(e, -5 + i * 2, 0);
                e.setYRot(150f + i * 12f);
                e.yBodyRot = 150f + i * 12f;
                e.setYHeadRot(150f + i * 12f);
            }
            camera(s, 0, -5, 12);
        }, null, 60, false));

        bossShot("10_crypt_lord", () -> DungeonModule.CRYPT_LORD.get());
        bossShot("11_mire_mother", () -> RegionsModule.MIRE_MOTHER.get());
        bossShot("12_forgemaster", () -> RegionsModule.FORGEMASTER.get());
        bossShot("13_scarab_queen", () -> RegionsModule.SCARAB_QUEEN.get());
        STEPS.add(new Step(null, s -> {}, () -> originalMineLite = com.alkimor.regnum.core.RegnumClientConfig.LITE_MODE.get(), 1, false));
        mineShot("14_crawler_closeup", com.alkimor.regnum.mine.MineModule.CRAWLER::get, 1.0F, true);
        mineShot("15_crawler_queen_phase1", com.alkimor.regnum.mine.MineModule.CRAWLER_QUEEN::get, 1.0F, true);
        mineShot("16_crawler_queen_phase2", com.alkimor.regnum.mine.MineModule.CRAWLER_QUEEN::get, 0.62F, true);
        mineShot("17_crawler_queen_phase3", com.alkimor.regnum.mine.MineModule.CRAWLER_QUEEN::get, 0.28F, true);
        mineShot("14_crawler_lite", com.alkimor.regnum.mine.MineModule.CRAWLER::get, 1.0F, true);
        mineShot("15_crawler_queen_phase1_lite", com.alkimor.regnum.mine.MineModule.CRAWLER_QUEEN::get, 1.0F, true);
        mineShot("16_crawler_queen_phase2_lite", com.alkimor.regnum.mine.MineModule.CRAWLER_QUEEN::get, 0.62F, true);
        mineShot("17_crawler_queen_phase3_lite", com.alkimor.regnum.mine.MineModule.CRAWLER_QUEEN::get, 0.28F, true);
        broodShot(false);
        broodShot(true);
        STEPS.add(new Step("19_crawler_live_brood", ClientVisualTest::liveQueenBrood, null, 140, false));
        STEPS.add(new Step("20_crawler_brood_target_loss", ClientVisualTest::removeCrawlerBroodTarget, null, 50, false));
        STEPS.add(new Step("21_crawler_brood_retarget", ClientVisualTest::checkCrawlerBroodRetarget, null, 1, false));
        STEPS.add(new Step(null, s -> {}, ClientVisualTest::checkMineModels, 1, false));
        STEPS.add(new Step(null, s -> {}, () -> com.alkimor.regnum.core.RegnumClientConfig.LITE_MODE.set(originalMineLite), 1, false));
        STEPS.add(new Step(null, s -> {}, ClientVisualTest::checkAnimationReset, 5, false));

        STEPS.add(new Step("03_blocks", s -> {
            clearScene();
            ServerLevel l = level(s);
            List<Block> blocks = new ArrayList<>();
            for (var h : ModRegistries.BLOCKS.getEntries()) blocks.add(h.get());
            for (int i = 0; i < blocks.size(); i++) {
                l.setBlock(new BlockPos(-blocks.size() + i * 2, Y + 1, 2), blocks.get(i).defaultBlockState(), 3);
            }
            camera(s, 0, -8, 15);
        }, null, 60, false));

        STEPS.add(new Step("04_items_a", s -> giveItems(s, 0), null, 30, true));
        STEPS.add(new Step("05_items_b", s -> giveItems(s, 9), null, 30, true));
        STEPS.add(new Step("06_items_c", s -> giveItems(s, 18), null, 30, true));
        STEPS.add(new Step("07_items_d", s -> giveItems(s, 27), null, 30, true));

        dungeon("crypts", "20_crypt", DungeonModule.CRYPT_ALTAR.get(), 2, -11);
        dungeon("sunken_shrine", "21_shrine", RegionsModule.MIRE_ALTAR.get(), 3, -9);
        dungeon("forge_fortress", "22_fortress", RegionsModule.FORGE_ALTAR.get(), 3, -17);
        dungeon("sand_tomb", "23_tomb", RegionsModule.SUN_ALTAR.get(), 2, -14);
        dungeon("bandit_camps", "24_camp", null, 0, 0);

        STEPS.add(new Step("29_creation", s -> {}, () -> Minecraft.getInstance().setScreen(new com.alkimor.regnum.client.screen.CreationScreen()), 30, true));
        STEPS.add(new Step("30_journal", s -> {
            var p = player(s);
            var d = com.alkimor.regnum.survival.Skills.data(p);
            d.setXp(com.alkimor.regnum.survival.Skill.ONE_HANDED, com.alkimor.regnum.survival.Skill.xpFor(57));
            d.setXp(com.alkimor.regnum.survival.Skill.BOW, com.alkimor.regnum.survival.Skill.xpFor(31));
            d.setXp(com.alkimor.regnum.survival.Skill.LEADERSHIP, com.alkimor.regnum.survival.Skill.xpFor(26));
            d.setXp(com.alkimor.regnum.survival.Skill.ATHLETICS, com.alkimor.regnum.survival.Skill.xpFor(44));
            d.setFocus(com.alkimor.regnum.survival.Skill.ONE_HANDED, 3);
            d.setFocus(com.alkimor.regnum.survival.Skill.BOW, 1);
            d.choose(com.alkimor.regnum.survival.Perk.OH_HEAVY);
            d.addCharXp(9000);
            com.alkimor.regnum.survival.Skills.sync(p, false);
        }, () -> Minecraft.getInstance().setScreen(new JournalScreen()), 30, true));
        STEPS.add(new Step("30b_perks", s -> {}, () -> {
            JournalScreen js = new JournalScreen();
            Minecraft.getInstance().setScreen(js);
            js.openPage(1);
        }, 30, true));
        STEPS.add(new Step("31_city", s -> {}, () -> Minecraft.getInstance().setScreen(new CityScreen(new CityInfoPayload(BlockPos.ZERO,
                "Тестоград", "Князь", 2, 40, 7, 55, 30, 6, 13, 1, 1, 1, 2, false, 1, 60, 20, 12, 6, 3, 2, 1, 32, 1, 5, 1, new int[com.alkimor.regnum.kingdom.SoldierType.values().length], new int[com.alkimor.regnum.core.network.CityInfoPayload.EXTRA_LEN]))), 30, true));
        STEPS.add(new Step("32_command", s -> {}, () -> Minecraft.getInstance().setScreen(new CommandScreen(
                new ArmyInfoPayload(new int[]{6, 3, 2, 1, 0}, new int[]{-1, 0, 2, 3, -1}, 1, 0))), 30, true));
        STEPS.add(new Step("33_trade", s -> {}, () -> Minecraft.getInstance().setScreen(new TradeScreen(new TradeInfoPayload(false, 0L,
                "Рынок города «Тестоград»", "равнины", new int[]{7, 18, 22, 14, 8, 11, 9, 27}, new int[]{5, 12, 15, 10, 6, 8, 7, 19},
                new int[]{0, 2, 0, 0, 1, 0, 0, 0}, new int[]{-1, 1, 0, 0, 0, 0, -1, 1}, 5))), 30, true));
    }

    private static void bossShot(String name, java.util.function.Supplier<EntityType<?>> type) {
        STEPS.add(new Step(name, s -> {
            clearScene();
            Entity e = type.get().create(level(s));
            if (e instanceof Mob m) m.finalizeSpawn(level(s), level(s).getCurrentDifficultyAt(BlockPos.ZERO), MobSpawnType.COMMAND, null);
            place(e, 0, 2);
            e.setYRot(180f);
            if (e instanceof Mob m) {
                m.yBodyRot = 180f;
                m.setYHeadRot(180f);
            }
            SoldierEntity comparison = KingdomModule.SOLDIER.get().create(level(s));
            if (comparison == null) throw new IllegalStateException("Cannot create boss scale reference");
            comparison.setup(SoldierType.SWORDSMAN, player(s).getUUID(), null, 1);
            place(comparison, 3, 2);
            player(s).teleportTo(level(s), -3.5, Y + 4.0, -6.5, -24.0F, 10.0F);
        }, null, 60, false));
    }

    private static void mineShot(String name, java.util.function.Supplier<EntityType<?>> type,
                                 float healthRatio, boolean aggressive) {
        STEPS.add(new Step(name, s -> {
            clearScene();
            ServerLevel level = level(s);
            Entity entity = type.get().create(level);
            if (entity instanceof Mob mob) {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.ZERO), MobSpawnType.COMMAND, null);
                mob.setHealth(Math.max(1.0F, mob.getMaxHealth() * healthRatio));
                mob.setAggressive(aggressive);
            }
            place(entity, 0, 2);
            entity.setYRot(180.0F);
            if (entity instanceof Mob mob) {
                mob.yBodyRot = 180.0F;
                mob.setYHeadRot(180.0F);
            }
            boolean queen = entity instanceof com.alkimor.regnum.mine.CrawlerQueenEntity;
            player(s).teleportTo(level, queen ? -3.5 : -1.3, Y + (queen ? 3.0 : 1.6), queen ? -2.5 : -0.7,
                    queen ? -39.0F : -29.0F, queen ? 26.0F : 23.0F);
        }, () -> com.alkimor.regnum.core.RegnumClientConfig.LITE_MODE.set(name.endsWith("_lite")), 40, false));
    }

    private static void checkMineModels() {
        Minecraft mc = Minecraft.getInstance();
        var root = mc.getEntityModels().bakeLayer(com.alkimor.regnum.client.model.CrawlerQueenModel.LAYER);
        var model = new com.alkimor.regnum.client.model.CrawlerQueenModel(root);
        var queen = com.alkimor.regnum.mine.MineModule.CRAWLER_QUEEN.get().create(mc.level);
        if (queen == null) throw new IllegalStateException("Cannot create queen test entity");
        var abdomen = root.getChild("abdomen");
        var fissures = abdomen.getChild("phase_two_fissures");
        var core = abdomen.getChild("phase_three_core");
        for (boolean lite : new boolean[]{false, true}) {
            com.alkimor.regnum.core.RegnumClientConfig.LITE_MODE.set(lite);
            for (float ratio : new float[]{1.0F, 0.62F, 0.28F, 1.0F}) {
                queen.setHealth(queen.getMaxHealth() * ratio);
                model.setupAnim(queen, 2.0F, 0.8F, 30.0F, 15.0F, 8.0F);
                if (fissures.visible != (ratio <= 0.66F) || core.visible != (ratio <= 0.33F))
                    throw new IllegalStateException("Queen phase visibility does not reset, lite=" + lite);
                float expectedSpread = (ratio <= 0.33F ? 0.78F : ratio <= 0.66F ? 0.38F : 0.0F);
                float base = abdomen.getChild("shell_flap_l").getInitialPose().zRot;
                float actual = abdomen.getChild("shell_flap_l").zRot - base;
                if (Math.abs(actual - expectedSpread) > 0.016F)
                    throw new IllegalStateException("Queen shell spread does not match phase");
                for (var part : com.alkimor.regnum.client.model.CrawlerQueenGeometry.decor(root)) {
                    boolean visible = !com.alkimor.regnum.core.RegnumClientConfig.decorHidden();
                    if (part.visible != visible) throw new IllegalStateException("Queen decor does not respect Lite");
                }
                var parts = root.getAllParts().toList();
                float[][] expected = new float[parts.size()][6];
                for (int i = 0; i < parts.size(); i++) {
                    var p = parts.get(i);
                    expected[i] = new float[]{p.x, p.y, p.z, p.xRot, p.yRot, p.zRot};
                }
                for (int frame = 0; frame < 60; frame++) model.setupAnim(queen, 2.0F, 0.8F, 30.0F, 15.0F, 8.0F);
                for (int i = 0; i < parts.size(); i++) {
                    var p = parts.get(i);
                    float[] actualPose = {p.x, p.y, p.z, p.xRot, p.yRot, p.zRot};
                    for (int axis = 0; axis < actualPose.length; axis++)
                        if (!Float.isFinite(actualPose[axis]) || Math.abs(expected[i][axis] - actualPose[axis]) > 0.00001F)
                            throw new IllegalStateException("Queen pose drift: part=" + i + " axis=" + axis);
                }
            }
        }
        log("queen phase transitions/Lite/60 repeated animation frames: OK");
        checkQueenActionPoses(mc);
        checkChitinGait(mc);
        checkImportedAnchors(mc);
    }

    /** Forward-transform ankle pivots: stance must not sink through the model floor. */
    private static void checkImportedAnchors(Minecraft mc) {
        int probes = 0;
        var processor = com.alkimor.regnum.worldgen.ImportedAnchorProcessor.INSTANCE;
        for (String id : new String[]{"minecraft:item_frame", "minecraft:glow_item_frame", "minecraft:painting", "minecraft:leash_knot"}) {
            var source = new net.minecraft.nbt.CompoundTag();
            source.putString("id", id);
            source.putInt("TileX", 2); source.putInt("TileY", 1); source.putInt("TileZ", 3);
            source.putString("ProbePayload", "preserved");
            for (var rotation : net.minecraft.world.level.block.Rotation.values())
                for (var mirror : net.minecraft.world.level.block.Mirror.values()) {
                    var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings()
                            .setRotation(rotation).setMirror(mirror);
                    var anchor = net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
                            .transform(new BlockPos(2, 1, 3), mirror, rotation, BlockPos.ZERO).offset(300, 80, -400);
                    var input = new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo(
                            net.minecraft.world.phys.Vec3.atCenterOf(anchor), anchor, source);
                    var output = processor.processEntity(mc.level, new BlockPos(300, 80, -400), input, input, settings, null);
                    if (output.nbt == source || output.nbt.getInt("TileX") != anchor.getX()
                            || output.nbt.getInt("TileY") != anchor.getY() || output.nbt.getInt("TileZ") != anchor.getZ()
                            || !output.nbt.getString("ProbePayload").equals("preserved")
                            || source.getInt("TileX") != 2 || !output.pos.equals(input.pos))
                        throw new IllegalStateException("Imported anchor transform corrupt: " + id + "/" + rotation + "/" + mirror);
                    probes++;
                }
        }
        var mobTag = new net.minecraft.nbt.CompoundTag();
        mobTag.putString("id", "minecraft:zombie");
        var mobInfo = new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo(
                net.minecraft.world.phys.Vec3.ZERO, BlockPos.ZERO, mobTag);
        if (processor.processEntity(mc.level, BlockPos.ZERO, mobInfo, mobInfo,
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(), null) != mobInfo)
            throw new IllegalStateException("Anchor processor changed ordinary mob");
        log("imported hanging anchors rotations/mirrors/payload/source preservation: OK, probes=" + probes);
    }

    private static void checkChitinGait(Minecraft mc) {
        int tested = 0;
        for (boolean queen : new boolean[]{false, true}) {
            var layer = queen ? com.alkimor.regnum.client.model.CrawlerQueenModel.LAYER
                    : com.alkimor.regnum.client.model.CrawlerModel.LAYER;
            var root = mc.getEntityModels().bakeLayer(layer);
            var q = new QueenPoseProbe(mc.level);
            var c = com.alkimor.regnum.mine.MineModule.CRAWLER.get().create(mc.level);
            var qm = queen ? new com.alkimor.regnum.client.model.CrawlerQueenModel(root) : null;
            var cm = queen ? null : new com.alkimor.regnum.client.model.CrawlerModel(root);
            if (c == null) throw new IllegalStateException("Cannot create gait probe");
            for (boolean lite : new boolean[]{false, true}) {
                com.alkimor.regnum.core.RegnumClientConfig.LITE_MODE.set(lite);
                float minimumAnkle = 24;
                for (int frame = 0; frame < 96; frame++) {
                    float swing = frame * 0.23F;
                    if (queen) qm.setupAnim(q, swing, 1, 40, 0, 0);
                    else cm.setupAnim(c, swing, 1, 40, 0, 0);
                    for (String side : new String[]{"l", "r"})
                        for (String row : new String[]{"front", "mid_front", "mid_back", "rear"}) {
                            String name = "leg_" + row + "_" + side;
                            var hip = root.getChild(name);
                            var knee = hip.getChild(name + "_shin");
                            var foot = knee.getChild(name + "_foot");
                            var pose = new com.mojang.blaze3d.vertex.PoseStack();
                            root.translateAndRotate(pose);
                            hip.translateAndRotate(pose);
                            knee.translateAndRotate(pose);
                            foot.translateAndRotate(pose);
                            var ankle = pose.last().pose().transform(new org.joml.Vector4f(0, 0, 0, 1));
                            minimumAnkle = Math.min(minimumAnkle, ankle.y * 16);
                            float maximumLift = queen ? 3.25F : 2.05F;
                            if (!Float.isFinite(ankle.y) || ankle.y > 24.01F / 16 || ankle.y < (24 - maximumLift) / 16)
                                throw new IllegalStateException("Foot leaves floor envelope: queen=" + queen
                                        + " leg=" + name + " frame=" + frame + " y=" + ankle.y * 16);
                            tested++;
                        }
                }
                if (24 - minimumAnkle < (queen ? 2.5F : 1.5F))
                    throw new IllegalStateException("Chitin return stroke is visually too small: queen=" + queen);
            }
        }
        log("chitin stance/lift ground envelope Full+Lite: OK, ankle samples=" + tested);
    }

    /** Input probe for pose assertions; never spawned or used outside the test profile. */
    private static final class QueenPoseProbe extends com.alkimor.regnum.mine.CrawlerQueenEntity {
        int action, elapsed, duration, impact, phase = 1;
        QueenPoseProbe(net.minecraft.world.level.Level level) {
            super(com.alkimor.regnum.mine.MineModule.CRAWLER_QUEEN.get(), level);
            tickCount = 40;
        }
        @Override public int getVisualAction() { return action; }
        @Override public int getVisualActionTicks() { return elapsed; }
        @Override public int getVisualActionDuration() { return duration; }
        @Override public int getVisualActionImpact() { return impact; }
        @Override public int getVisualPhase() { return phase; }
    }

    private static void checkQueenActionPoses(Minecraft mc) {
        var root = mc.getEntityModels().bakeLayer(com.alkimor.regnum.client.model.CrawlerQueenModel.LAYER);
        var model = new com.alkimor.regnum.client.model.CrawlerQueenModel(root);
        var probe = new QueenPoseProbe(mc.level);
        var head = root.getChild("head");
        var sac = head.getChild("acid_sac_l");
        int[][] clips = {{1, 20, 30, 22}, {1, 23, 30, 22},
                {1, 26, 36, 28}, {1, 29, 36, 28}, {1, 15, 25, 17}, {1, 18, 25, 17},
                {2, 20, 32, 26}, {2, 28, 32, 26}, {3, 8, 50, 0}, {4, 12, 24, 0}, {5, 15, 40, 30}, {6, 15, 30, 0}};
        for (int[] clip : clips) {
            probe.action = clip[0]; probe.elapsed = clip[1]; probe.duration = clip[2];
            probe.impact = clip[3];
            model.setupAnim(probe, 0, 0, 40, 0, 0);
            if (probe.action == 1 && probe.elapsed < probe.impact && (sac.xScale <= 1.2F || head.xRot >= -0.2F))
                throw new IllegalStateException("Acid windup not readable");
            if (probe.action == 1 && probe.elapsed > probe.impact && head.xRot <= 0.05F)
                throw new IllegalStateException("Acid release does not follow server impact");
            if (probe.action == 3 && head.xRot < 0.4F) throw new IllegalStateException("Stun pose not readable");
            if (probe.action == 2 && probe.elapsed > probe.impact && head.z > head.getInitialPose().z - 1.5F)
                throw new IllegalStateException("Charge does not extend after impact");
            if (probe.action == 4) {
                var rear = root.getChild("leg_rear_l");
                if (rear.zRot - rear.getInitialPose().zRot < 0.2F) throw new IllegalStateException("Brood rear legs do not rise");
            }
            var parts = root.getAllParts().toList();
            float[][] expected = new float[parts.size()][];
            for (int i = 0; i < parts.size(); i++) expected[i] = minePose(parts.get(i));
            for (int frame = 0; frame < 60; frame++) model.setupAnim(probe, 0, 0, 40, 0, 0);
            for (int i = 0; i < parts.size(); i++) {
                float[] actual = minePose(parts.get(i));
                for (int axis = 0; axis < actual.length; axis++)
                    if (!Float.isFinite(actual[axis]) || Math.abs(actual[axis] - expected[i][axis]) > 0.00001F)
                        throw new IllegalStateException("Queen action pose drifts: action=" + probe.action + " part=" + i);
            }
            probe.action = 0; probe.elapsed = 0; probe.duration = 0;
            model.setupAnim(probe, 0, 0, 40, 0, 0);
            if (sac.xScale != 1.0F || sac.yScale != 1.0F) throw new IllegalStateException("Acid sac scale persists after action");
        }
        probe.phase = 3;
        probe.setHealth(probe.getMaxHealth());
        model.setupAnim(probe, 0, 0, 40, 0, 0);
        if (!root.getChild("abdomen").getChild("phase_three_core").visible)
            throw new IllegalStateException("Synced phase lost after healing");
        log("queen difficulty impacts17/22/28 + charge/stun/brood/cavein/darkness, scale reset, healing phase: OK (pose input probe)");
    }

    private static float[] minePose(net.minecraft.client.model.geom.ModelPart part) {
        return new float[]{part.x, part.y, part.z, part.xRot, part.yRot, part.zRot, part.xScale, part.yScale, part.zScale};
    }

    private static void broodShot(boolean lite) {
        STEPS.add(new Step("18_crawler_brood" + (lite ? "_lite" : ""), s -> {
            clearScene();
            var queen = com.alkimor.regnum.mine.MineModule.CRAWLER_QUEEN.get().create(level(s));
            if (queen == null) throw new IllegalStateException("Cannot create brood queen");
            place(queen, 0, 3);
            for (int[] at : new int[][]{{-3, 0}, {3, 0}, {-3, 5}, {3, 5}}) {
                var crawler = com.alkimor.regnum.mine.MineModule.CRAWLER.get().create(level(s));
                if (crawler == null) throw new IllegalStateException("Cannot create brood crawler");
                place(crawler, at[0], at[1]);
            }
            player(s).teleportTo(level(s), -5.5, Y + 4.0, -7.5, -28.0F, 23.0F);
        }, () -> com.alkimor.regnum.core.RegnumClientConfig.LITE_MODE.set(lite), 40, false));
    }

    /** A real server-side Queen summons its own brood; soldiers are harmless, visible test targets. */
    private static void liveQueenBrood(MinecraftServer server) {
        clearScene();
        ServerLevel level = level(server);
        ServerPlayer viewer = player(server);
        viewer.setGameMode(GameType.CREATIVE); // keep the observer out of the Queen/minion target sets
        viewer.getAbilities().invulnerable = true;
        viewer.getAbilities().flying = true;
        viewer.onUpdateAbilities();

        SoldierEntity first = crawlerTestSoldier(server, 5, 0);
        SoldierEntity backup = crawlerTestSoldier(server, -7, 1);
        SoldierEntity flank = crawlerTestSoldier(server, 1, 8);
        crawlerTestPrimary = first;
        crawlerTestBackup = backup;
        crawlerTestFlank = flank;
        crawlerTestLossTarget = first.getUUID();
        crawlerTestBackupTarget = backup.getUUID();
        crawlerTestReplacementTargets.clear();
        crawlerTestReplacementTargets.add(backup.getUUID());
        crawlerTestReplacementTargets.add(flank.getUUID());
        crawlerTestTargetLostAt = -1L;
        crawlerTestPositionsAtLoss.clear();

        var queen = com.alkimor.regnum.mine.MineModule.CRAWLER_QUEEN.get().create(level);
        if (queen == null) throw new IllegalStateException("Cannot create live brood Queen");
        queen.moveTo(0.5, Y + 1, 0.5, 0f, 0f);
        queen.finalizeSpawn(level, level.getCurrentDifficultyAt(queen.blockPosition()), MobSpawnType.COMMAND, null);
        queen.setHealth(queen.getMaxHealth() * 0.65f); // starts in phase two; ability logic remains live
        queen.setInvulnerable(true); // keep the showcase running while soldiers engage
        queen.setTarget(first);
        queen.setPersistenceRequired();
        if (!level.addFreshEntity(queen)) throw new IllegalStateException("Live brood Queen was rejected by the test world");
        crawlerTestQueen = queen;
        scene.add(queen);

        // Keep the full Queen and the summoned ring in frame even if she lunges toward a target.
        viewer.teleportTo(level, 0.5, Y + 10.0, -20.5, 0f, 19f);
        log("LIVE_BROOD_SETUP queen=" + queen.getUUID() + " phaseHp=" + queen.getHealth()
                + " primary=" + first.getUUID() + " backup=" + backup.getUUID());
    }

    private static SoldierEntity crawlerTestSoldier(MinecraftServer server, int x, int z) {
        ServerLevel level = level(server);
        SoldierEntity soldier = KingdomModule.SOLDIER.get().create(level);
        if (soldier == null) throw new IllegalStateException("Cannot create brood test target");
        soldier.setup(SoldierType.SPEARMAN, player(server).getUUID(), null, 1);
        soldier.moveTo(x + 0.5, Y + 1, z + 0.5, 180f, 0f);
        soldier.setNoAi(true);
        soldier.setInvulnerable(true);
        soldier.setPersistenceRequired();
        if (!level.addFreshEntity(soldier)) throw new IllegalStateException("Brood test target was rejected by the test world");
        scene.add(soldier);
        return soldier;
    }

    /** Kill a minion's live target under controlled conditions and require a real replacement/movement. */
    private static void removeCrawlerBroodTarget(MinecraftServer server) {
        ServerLevel level = level(server);
        if (crawlerTestQueen == null || crawlerTestQueen.isRemoved()) throw new IllegalStateException("Live Queen disappeared before target-loss test");
        // The Queen has already had 7 seconds to summon naturally; hold her now so her attacks do not
        // consume the test dummies before the scripted target-loss transition.
        crawlerTestQueen.setNoAi(true);
        var minions = level.getEntitiesOfClass(com.alkimor.regnum.mine.CrawlerEntity.class,
                crawlerTestQueen.getBoundingBox().inflate(32),
                e -> e.isAlive() && e.isMinion());
        if (minions.isEmpty()) throw new IllegalStateException("Queen did not naturally summon any brood within 7 seconds");
        SoldierEntity lossTarget = crawlerTestPrimary != null && crawlerTestPrimary.isAlive() ? crawlerTestPrimary
                : crawlerTestBackup != null && crawlerTestBackup.isAlive() ? crawlerTestBackup
                : crawlerTestFlank != null && crawlerTestFlank.isAlive() ? crawlerTestFlank : null;
        if (lossTarget == null) throw new IllegalStateException("All local brood targets died before target-loss trigger");
        crawlerTestLossTarget = lossTarget.getUUID();
        crawlerTestReplacementTargets.clear();
        for (SoldierEntity candidate : List.of(crawlerTestPrimary, crawlerTestBackup, crawlerTestFlank))
            if (candidate != null && candidate.isAlive() && candidate != lossTarget)
                crawlerTestReplacementTargets.add(candidate.getUUID());
        if (crawlerTestReplacementTargets.isEmpty()) throw new IllegalStateException("No living replacement soldier remains");
        crawlerTestMinions.clear();
        crawlerTestMinions.addAll(minions);
        crawlerTestPositionsAtLoss.clear();
        for (var minion : minions) {
            minion.setTarget(lossTarget);
            crawlerTestPositionsAtLoss.put(minion.getUUID(), minion.position());
        }
        crawlerTestTargetLostAt = level.getGameTime();
        log("LIVE_BROOD_LOSS_TARGET id=" + lossTarget.getUUID() + " pos=" + lossTarget.position()
                + " health=" + lossTarget.getHealth() + " replacements=" + crawlerTestReplacementTargets);
        lossTarget.kill();
        log("LIVE_BROOD_TARGET_LOST t=" + crawlerTestTargetLostAt + " minions=" + minions.size()
                + " backup=" + crawlerTestBackupTarget);
        logCrawlerBrood(level, minions, "target-lost");
    }

    private static void checkCrawlerBroodRetarget(MinecraftServer server) {
        ServerLevel level = level(server);
        if (crawlerTestTargetLostAt < 0) throw new IllegalStateException("Brood target-loss marker was not set");
        long elapsed = level.getGameTime() - crawlerTestTargetLostAt;
        SoldierEntity backup = crawlerTestBackup;
        var minions = crawlerTestMinions.stream().filter(e -> e.isAlive() && e.isMinion()).toList();
        int switched = 0, switchedToBackup = 0, moved = 0;
        for (var minion : minions) {
            var old = crawlerTestPositionsAtLoss.get(minion.getUUID());
            if (old != null && old.distanceToSqr(minion.position()) > 0.04) moved++;
            if (minion.getTarget() instanceof SoldierEntity target
                    && crawlerTestReplacementTargets.contains(target.getUUID())) {
                switched++;
                if (backup != null && target == backup) switchedToBackup++;
            }
        }
        log("LIVE_BROOD_RETARGET elapsedTicks=" + elapsed + " alive=" + minions.size()
                + " switchedToReplacement=" + switched + " switchedToBackup=" + switchedToBackup
                + " moved=" + moved + " backupAlive=" + (backup != null && backup.isAlive()));
        logCrawlerBrood(level, minions, "retarget-check");
        if (backup == null || !backup.isAlive() || elapsed > 60 || minions.isEmpty() || switched == 0 || moved == 0)
            throw new IllegalStateException("Live brood failed to move and retarget a reachable replacement within 60 ticks");
        log("LIVE_BROOD_RETARGET: OK (replacement acquired within 3 seconds)");
    }

    private static void logCrawlerBrood(ServerLevel level, List<com.alkimor.regnum.mine.CrawlerEntity> minions, String label) {
        for (var minion : minions) {
            var target = minion.getTarget();
            var old = crawlerTestPositionsAtLoss.get(minion.getUUID());
            double moved = old == null ? 0 : old.distanceTo(minion.position());
            log("LIVE_BROOD_MINION " + label + " id=" + minion.getUUID() + " pos=" + minion.position()
                    + " target=" + (target == null ? "none" : target.getUUID())
                    + " targetAlive=" + (target != null && target.isAlive())
                    + " noAI=" + minion.isNoAi()
                    + " backup=" + (target != null && target.getUUID().equals(crawlerTestBackupTarget))
                    + " movedSinceLoss=" + String.format(java.util.Locale.ROOT, "%.2f", moved)
                    + " navDone=" + minion.getNavigation().isDone()
                    + " attackAnim=" + String.format(java.util.Locale.ROOT, "%.2f", minion.getAttackAnim(1.0f)));
        }
    }

    /** Start a real, unscripted Crypt Lord fight in the generated hall with ten live soldier AI units. */
    private static void startCryptBattle(MinecraftServer server) {
        ServerLevel level = level(server);
        BlockPos arena = DT_ARENA;
        if (arena == null) throw new IllegalStateException("Crypt battle has no generated arena");
        com.alkimor.regnum.dungeon.CryptLordEntity boss = findCryptBoss(level);
        if (boss == null) throw new IllegalStateException("Crypt Lord from the preceding arena shot disappeared");

        int[][] slots = {{-6,-4},{-3,-7},{0,-7},{3,-7},{6,-4},{6,0},{6,4},{3,7},{-3,7},{-6,4}};
        SoldierType[] types = {SoldierType.SHIELDMAN, SoldierType.SPEARMAN, SoldierType.SWORDSMAN,
                SoldierType.KNIGHT, SoldierType.ARCHER, SoldierType.GREATSWORD, SoldierType.CROSSBOW,
                SoldierType.KNIGHT, SoldierType.SPEARMAN, SoldierType.SWORDSMAN};
        ServerPlayer viewer = player(server);
        viewer.setGameMode(GameType.CREATIVE);
        viewer.getAbilities().invulnerable = true;
        viewer.getAbilities().flying = true;
        viewer.onUpdateAbilities();

        List<SoldierEntity> fighters = new ArrayList<>();
        for (int i = 0; i < slots.length; i++) {
            SoldierEntity soldier = KingdomModule.SOLDIER.get().create(level);
            if (soldier == null) throw new IllegalStateException("Cannot create Crypt Lord battle soldier " + i);
            soldier.setup(types[i], viewer.getUUID(), null, i % 4 + 1);
            soldier.moveTo(arena.getX() + slots[i][0] + 0.5, arena.getY(), arena.getZ() + slots[i][1] + 0.5,
                    (float) (i * 36), 0f);
            soldier.setNoAi(false);
            soldier.setInvulnerable(true); // keep the showcase readable; AI and attack clips stay live
            soldier.setPersistenceRequired();
            if (!level.addFreshEntity(soldier)) throw new IllegalStateException("Crypt Lord battle soldier was rejected");
            fighters.add(soldier);
            scene.add(soldier);
        }
        boss.setHealth(boss.getMaxHealth() * 0.60f); // transition through the normal boss AI into phase 2
        boss.setInvulnerable(true); // hold the encounter open while recording the actual attack telegraphs
        boss.setTarget(fighters.get(0));
        fighters.forEach(soldier -> soldier.setTarget(boss));
        // The crypt room is sealed underground. Keep the observer inside the perimeter wall,
        // looking diagonally across the arena; the previous outside position was buried in terrain.
        viewer.teleportTo(level, arena.getX() - 4.5, arena.getY() + 2.0, arena.getZ() - 4.5, -45f, 8f);
        cryptBattlePeakPhase = 1;
        cryptBattleActionsSeen.clear();
        log("CRYPT_BATTLE_START boss=" + boss.getUUID() + " arena=" + arena.toShortString()
                + " soldiers=" + fighters.size() + " phaseHp=" + boss.getHealth());
        sampleCryptBattle(server, "start");
    }

    private static void forceCryptPhaseThree(MinecraftServer server) {
        var boss = findCryptBoss(level(server));
        if (boss == null || !boss.isAlive()) throw new IllegalStateException("Crypt Lord died before phase-three battle sample");
        boss.setHealth(boss.getMaxHealth() * 0.30f);
        sampleCryptBattle(server, "phase3-trigger");
    }

    private static void sampleCryptBattle(MinecraftServer server, String label) {
        ServerLevel level = level(server);
        var boss = findCryptBoss(level);
        if (boss == null || !boss.isAlive()) throw new IllegalStateException("Crypt Lord died during live battle showcase");
        int action = boss.getVisualAction();
        int phase = boss.getVisualPhase();
        cryptBattlePeakPhase = Math.max(cryptBattlePeakPhase, phase);
        if (action != com.alkimor.regnum.dungeon.boss.VisualAction.IDLE)
            cryptBattleActionsSeen.add(action);
        boolean attackingAction = action >= com.alkimor.regnum.dungeon.boss.VisualAction.SWEEP
                && action <= com.alkimor.regnum.dungeon.boss.VisualAction.PLATES;
        if (attackingAction) cryptBattleActionsSeen.add(-action); // negative IDs indicate a real attack, not the phase pose
        var arena = DT_ARENA;
        var fighters = level.getEntitiesOfClass(SoldierEntity.class,
                new net.minecraft.world.phys.AABB(arena).inflate(12), e -> e.isAlive());
        long targetBoss = fighters.stream().filter(e -> e.getTarget() == boss).count();
        long activeSwings = fighters.stream().filter(e -> e.getAttackAnim(1.0f) > 0.05f).count();
        long dodging = fighters.stream().filter(SoldierEntity::isDodging).count();
        long guards = fighters.stream().filter(e -> e.getSoldierType() == SoldierType.SHIELDMAN && e.isBlocking()).count();
        log("CRYPT_BATTLE_SAMPLE label=" + label + " t=" + level.getGameTime() + " phase=" + phase
                + " action=" + action + " actionTicks=" + boss.getVisualActionElapsed()
                + " impact=" + boss.getVisualActionImpact() + " hp=" + boss.getHealth()
                + " soldiers=" + fighters.size() + " targeting=" + targetBoss
                + " activeSwings=" + activeSwings + " dodging=" + dodging + " shieldGuard=" + guards
                + " bossTarget=" + (boss.getTarget() == null ? "none" : boss.getTarget().getUUID()));
    }

    private static void finishCryptBattle(MinecraftServer server) {
        sampleCryptBattle(server, "final");
        boolean actualAttackSeen = cryptBattleActionsSeen.stream().anyMatch(action -> action < 0);
        if (cryptBattlePeakPhase < 3 || !actualAttackSeen)
            throw new IllegalStateException("Live Crypt Lord clip did not show phase three and a synced attack action; phases="
                    + cryptBattlePeakPhase + " actions=" + cryptBattleActionsSeen);
        log("CRYPT_BATTLE_VISUAL: OK phases=" + cryptBattlePeakPhase + " syncedAttackActions=" + cryptBattleActionsSeen);
    }

    private static com.alkimor.regnum.dungeon.CryptLordEntity findCryptBoss(ServerLevel level) {
        if (cryptBattleBossId == null) return null;
        for (var boss : level.getEntitiesOfClass(com.alkimor.regnum.dungeon.CryptLordEntity.class,
                new net.minecraft.world.phys.AABB(DT_ARENA).inflate(16, 16, 16),
                e -> e.isAlive() && e.getUUID().equals(cryptBattleBossId))) return boss;
        return null;
    }

    /** Find the crypt altar belonging to the selected located structure; nearby D&T structures may load together. */
    private static BlockPos findCryptArena(ServerLevel level, BlockPos structure) {
        BlockPos best = null;
        long bestDistance = Long.MAX_VALUE;
        int minY = Math.max(level.getMinBuildHeight(), structure.getY() - 64);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, structure.getY() + 64);
        for (int cx = (structure.getX() - 64) >> 4; cx <= (structure.getX() + 64) >> 4; cx++)
            for (int cz = (structure.getZ() - 64) >> 4; cz <= (structure.getZ() + 64) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                var chunk = level.getChunk(cx, cz);
                for (int x = Math.max(structure.getX() - 64, cx << 4); x <= Math.min(structure.getX() + 64, (cx << 4) + 15); x++)
                    for (int z = Math.max(structure.getZ() - 64, cz << 4); z <= Math.min(structure.getZ() + 64, (cz << 4) + 15); z++)
                        for (int y = minY; y <= maxY; y++) {
                            var altar = new BlockPos(x, y, z);
                            if (!chunk.getBlockState(altar).is(DungeonModule.CRYPT_ALTAR.get())) continue;
                            BlockPos arena = altar.offset(0, 0, -6);
                            long dx = arena.getX() - structure.getX(), dy = arena.getY() - structure.getY(), dz = arena.getZ() - structure.getZ();
                            long distance = dx * dx + 2 * dy * dy + dz * dz;
                            if (distance < bestDistance) {
                                best = arena;
                                bestDistance = distance;
                            }
                        }
            }
        return best;
    }

    /** Same inputs must produce the same pose, including after another entity used the model. */
    private static void checkAnimationReset() {
        Minecraft mc = Minecraft.getInstance();
        var layers = com.alkimor.regnum.client.model.ModelIndex.UNITS;
        for (int culture = 0; culture < layers.length; culture++) {
            for (int role = 0; role < layers[culture].length; role++) {
                var root = mc.getEntityModels().bakeLayer(layers[culture][role]);
                var model = new com.alkimor.regnum.client.render.RegnumHumanoidModel<SoldierEntity>(root, layers[culture][role]);
                var soldier = KingdomModule.SOLDIER.get().create(mc.level);
                if (soldier == null) throw new IllegalStateException("Cannot create animation test soldier");
                soldier.setup(SoldierType.values()[role], java.util.UUID.randomUUID(), null, 1);
                soldier.setCulture(com.alkimor.regnum.kingdom.Culture.byId(culture));
                soldier.hurtTime = 8;
                model.prepareMobModel(soldier, 1.0F, 0.4F, 0.5F);
                model.setupAnim(soldier, 1.0F, 0.4F, 20.0F, 10.0F, 5.0F);
                var parts = root.getAllParts().toList();
                float[][] expected = new float[parts.size()][6];
                for (int i = 0; i < parts.size(); i++) {
                    var p = parts.get(i);
                    expected[i] = new float[]{p.x, p.y, p.z, p.xRot, p.yRot, p.zRot};
                }
                for (int frame = 0; frame < 60; frame++) model.setupAnim(soldier, 1.0F, 0.4F, 20.0F, 10.0F, 5.0F);
                for (int i = 0; i < parts.size(); i++) {
                    var p = parts.get(i);
                    float[] actual = {p.x, p.y, p.z, p.xRot, p.yRot, p.zRot};
                    for (int axis = 0; axis < actual.length; axis++)
                        if (!Float.isFinite(actual[axis]) || Math.abs(expected[i][axis] - actual[axis]) > 0.00001F)
                            throw new IllegalStateException("Pose drifts: culture=" + culture + " role=" + role + " part=" + i + " axis=" + axis);
                }
            }
        }
        log("pose reset OK: 84 culture/role models, 60 repeated frames each");
        checkRangedShotClips(mc);
        checkDodgePoses(mc);
        checkCryptLordClips(mc);
    }

    private static final class RangedPoseProbe extends SoldierEntity {
        float swingFraction;
        boolean dodging;
        int dodgeAge;
        float dodgeYaw;
        @Override public boolean isDodging() { return dodging; }
        @Override public int getDodgeAge() { return dodgeAge; }
        @Override public int getDodgeDuration() { return 24; }
        @Override public float getDodgeRelativeYaw() { return dodgeYaw; }

        RangedPoseProbe(net.minecraft.world.level.Level level) { super(KingdomModule.SOLDIER.get(), level); }
        @Override public float getAttackAnim(float partialTick) { return swingFraction; }
    }

    private static final class CryptPoseProbe extends com.alkimor.regnum.dungeon.CryptLordEntity
            implements com.alkimor.regnum.animation.BossAnimationState {
        int action, elapsed, impact = 18, duration = 30;
        CryptPoseProbe(net.minecraft.world.level.Level level) { super(com.alkimor.regnum.dungeon.DungeonModule.CRYPT_LORD.get(), level); }
        public int getVisualAction() { return action; }
        public int getVisualActionTicks() { return elapsed; }
        public int getVisualActionImpact() { return impact; }
        public int getVisualActionDuration() { return duration; }
        public int getVisualPhase() { return 1; }
    }

    private static void checkCryptLordClips(Minecraft mc) {
        var root = mc.getEntityModels().bakeLayer(com.alkimor.regnum.client.model.CryptLordModel.LAYER);
        var model = new com.alkimor.regnum.client.render.CryptLordAnimatedModel(root);
        var boss = new CryptPoseProbe(mc.level);
        for (int action = 1; action <= 7; action++) {
            boss.action = action;
            boss.elapsed = boss.impact - 1;
            model.setupAnim(boss, 0, 0, 0, 0, 0);
            if (model.rightArm.xRot > -1.5F)
                throw new IllegalStateException("Crypt lord windup lacks raised weapon: action=" + action);
            float windupBody = model.body.xRot;
            boss.elapsed = boss.impact + 2;
            model.setupAnim(boss, 0, 0, 0, 0, 0);
            if (action == 2 && model.body.xRot < windupBody + 0.8F)
                throw new IllegalStateException("Crypt lord slam lacks full-body impact");
            var parts = root.getAllParts().toList();
            float[][] expected = new float[parts.size()][];
            for (int i = 0; i < parts.size(); i++) expected[i] = minePose(parts.get(i));
            for (int frame = 0; frame < 60; frame++) model.setupAnim(boss, 0, 0, 0, 0, 0);
            for (int i = 0; i < parts.size(); i++) {
                float[] actual = minePose(parts.get(i));
                for (int axis = 0; axis < actual.length; axis++)
                    if (!Float.isFinite(actual[axis]) || Math.abs(actual[axis] - expected[i][axis]) > 0.00001F)
                        throw new IllegalStateException("Crypt lord pose drift: action=" + action);
            }
        }
        boss.action = 0;
        model.setupAnim(boss, 0, 0, 0, 0, 0);
        if (Math.abs(model.body.yRot) > 0.001F || Math.abs(model.rightArm.xRot + 0.38F) > 0.001F)
            throw new IllegalStateException("Crypt lord does not return to stalking stance");
        log("crypt lord large cast/sweep/slam/reset poses: OK; probe only, server sync still requires live QA");
    }

    private static void checkDodgePoses(Minecraft mc) {
        var layer = com.alkimor.regnum.client.model.ModelIndex.UNITS[0][SoldierType.SHIELDMAN.ordinal()];
        var root = mc.getEntityModels().bakeLayer(layer);
        var model = new com.alkimor.regnum.client.render.RegnumHumanoidModel<SoldierEntity>(root, layer);
        var soldier = new RangedPoseProbe(mc.level);
        soldier.setup(SoldierType.SHIELDMAN, java.util.UUID.randomUUID(), null, 1);
        soldier.dodging = true; soldier.dodgeAge = 6; soldier.dodgeYaw = 90;
        model.prepareMobModel(soldier, 0, 0, 0); model.setupAnim(soldier, 0, 0, 0, 0, 0);
        float rightLean = model.body.zRot;
        soldier.dodgeYaw = -90;
        model.setupAnim(soldier, 0, 0, 0, 0, 0);
        if (rightLean * model.body.zRot >= 0 || Math.abs(rightLean - model.body.zRot) < 0.6F)
            throw new IllegalStateException("Dodge pose does not follow escape direction");
        soldier.dodgeAge = 16;
        model.setupAnim(soldier, 0, 0, 0, 0, 0);
        if (Math.abs(model.body.zRot) > 0.03F || model.leftArm.xRot > -1.2F)
            throw new IllegalStateException("Dodge should recover lean and retain shield while waiting");
        soldier.dodging = false;
        model.setupAnim(soldier, 0, 0, 0, 0, 0);
        if (Math.abs(model.body.xRot) > 0.03F || Math.abs(model.body.zRot) > 0.03F)
            throw new IllegalStateException("Dodge pose remains after server state ends");
        log("dodge directional lean/guard/recovery: OK; pose probe, live path QA still required");
    }

    private static void checkRangedShotClips(Minecraft mc) {
        var layers = com.alkimor.regnum.client.model.ModelIndex.UNITS;
        int probes = 0;
        for (int culture = 0; culture < layers.length; culture++) {
            for (SoldierType type : new SoldierType[]{SoldierType.MUSKETEER, SoldierType.BOMBARDIER, SoldierType.CROSSBOW}) {
                var layer = layers[culture][type.ordinal()];
                var root = mc.getEntityModels().bakeLayer(layer);
                var model = new com.alkimor.regnum.client.render.RegnumHumanoidModel<SoldierEntity>(root, layer);
                var soldier = new RangedPoseProbe(mc.level);
                soldier.setup(type, java.util.UUID.randomUUID(), null, 1);
                soldier.setCulture(com.alkimor.regnum.kingdom.Culture.byId(culture));
                soldier.setAggressive(true);
                model.prepareMobModel(soldier, 0, 0, 0);
                model.attackTime = 0;
                model.setupAnim(soldier, 0, 0, 0, 0, 0);
                float restX = model.body.xRot, restY = model.body.yRot, restArmZ = model.rightArm.z;
                soldier.swingFraction = 1F / 6F;
                model.attackTime = soldier.swingFraction;
                model.setupAnim(soldier, 0, 0, 0, 0, 0);
                if (model.body.xRot >= restX - 0.02F || model.rightArm.z <= restArmZ + 0.2F)
                    throw new IllegalStateException("Ranged shot lacks recoil: " + type + " culture=" + culture);
                if (Math.abs(model.body.yRot - restY) > 0.001F)
                    throw new IllegalStateException("Ranged shot still performs sword twist: " + type);
                float peak = model.rightArm.z;
                soldier.swingFraction = 5F / 6F;
                model.attackTime = soldier.swingFraction;
                model.setupAnim(soldier, 0, 0, 0, 0, 0);
                if (model.rightArm.z >= peak) throw new IllegalStateException("Ranged recoil does not recover: " + type);
                soldier.swingFraction = 0;
                model.attackTime = 0;
                model.setupAnim(soldier, 0, 0, 0, 0, 0);
                if (Math.abs(model.body.xRot - restX) > 0.00001F || Math.abs(model.rightArm.z - restArmZ) > 0.00001F)
                    throw new IllegalStateException("Ranged recoil leaks into rest: " + type);
                probes++;
            }
        }
        log("ranged release/recoil/recovery: OK, no sword twist or retained offsets, probes=" + probes);
    }

    private static BlockPos found;

    private static void dungeon(String tag, String name, Block altar, int camUp, int camBack) {
        STEPS.add(new Step(name + "_outside", s -> {
            clearScene();
            ServerLevel l = level(s);
            found = l.findNearestMapStructure(TagKey.create(Registries.STRUCTURE, Regnum.id(tag)), BlockPos.ZERO, 150, false);
            if (found == null) {
                log("structure not found: " + tag);
                return;
            }
            l.getChunk(found.getX() >> 4, found.getZ() >> 4);
            int top = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, found.getX(), found.getZ());
            log(tag + " at " + found.getX() + " " + top + " " + found.getZ());
            player(s).teleportTo(l, found.getX() + 0.5, top + 22, found.getZ() - 26, 0f, 38f);
        }, null, 220, false));
        if (altar == null) return;
        STEPS.add(new Step(name + "_inside", s -> {
            if (found == null) return;
            ServerLevel l = level(s);
            BlockPos a = null;
            for (int cx = -2; cx <= 4; cx++) for (int cz = -2; cz <= 6; cz++) l.getChunk((found.getX() >> 4) + cx - 1, (found.getZ() >> 4) + cz - 1);
            int top = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, found.getX(), found.getZ());
            outer:
            for (int y = top + 8; y > top - 40; y--)
                for (int dz = -32; dz <= 64; dz++)
                    for (int dx = -32; dx <= 32; dx++) {
                        BlockPos p = new BlockPos(found.getX() + dx, y, found.getZ() + dz);
                        if (l.getBlockState(p).is(altar)) {
                            a = p;
                            break outer;
                        }
                    }
            if (a == null) {
                log("altar not found for " + name);
                return;
            }
            log(name + " altar at " + a.toShortString());
            player(s).teleportTo(l, a.getX() + 0.5, a.getY() + camUp, a.getZ() + camBack + 0.5, 0f, 12f);
        }, null, 140, false));
    }

    // ================================================================== помощники сцены

    private static ServerLevel level(MinecraftServer s) {
        return s.overworld();
    }

    private static ServerPlayer player(MinecraftServer s) {
        return s.getPlayerList().getPlayers().get(0);
    }

    private static void place(Entity e, int x, int z) {
        e.moveTo(x + 0.5, Y + 1, z + 0.5, 180f, 0f);
        if (e instanceof Mob m) {
            m.setNoAi(true);
            m.setYHeadRot(180f);
            m.yBodyRot = 180f;
            m.setPersistenceRequired();
        }
        e.level().addFreshEntity(e);
        scene.add(e);
    }

    private static void clearScene() {
        for (Entity e : scene) e.discard();
        scene.clear();
        // A quick-play test world can load the previous run's persistent subjects
        // after the initial login cleanup. Remove them before each arena shot.
        if (sceneLevel != null) {
            // The copied self-test world may save lingering boss effects on its player.
            // Those can black out the first dungeon frame even though the room is loaded.
            for (ServerPlayer viewer : sceneLevel.players()) viewer.removeAllEffects();
            for (Entity e : sceneLevel.getEntities((Entity) null,
                    new net.minecraft.world.phys.AABB(-25, Y - 2, -25, 25, Y + 15, 25),
                    e -> !(e instanceof net.minecraft.world.entity.player.Player))) e.discard();
        }
    }

    private static void camera(MinecraftServer s, int x, int z, float pitch) {
        player(s).teleportTo(level(s), x + 0.5, Y + 1, z + 0.5, 0f, pitch);
    }

    private static void setupWorld(MinecraftServer s) {
        ServerLevel l = level(s);
        sceneLevel = l;
        ServerPlayer p = player(s);
        p.setGameMode(GameType.CREATIVE);
        l.setDayTime(6000);
        l.setWeatherParameters(999999, 0, false, false);
        l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, s);
        l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, s);
        p.getAbilities().invulnerable = true;
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 60 * 60, 0, false, false));
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        // площадка для съёмки в небе
        for (int x = -20; x <= 20; x++)
            for (int z = -20; z <= 20; z++) {
                l.setBlock(new BlockPos(x, Y, z), (x + z) % 7 == 0 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                for (int y = Y + 1; y <= Y + 12; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            }
        camera(s, 0, -7, 10);
    }

    /** A compact in-game look check for the new flower/understory palette. */
    private static void floraShowcase(MinecraftServer s) {
        clearScene();
        ServerLevel level = level(s);
        ServerPlayer viewer = player(s);
        viewer.getInventory().clearContent();
        net.minecraft.world.level.block.state.BlockState oakLeaves = Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.PERSISTENT, true);
        net.minecraft.world.level.block.state.BlockState birchLeaves = Blocks.BIRCH_LEAVES.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.PERSISTENT, true);

        for (int x = -30; x <= 30; x++) for (int z = -22; z <= 44; z++) {
            int surface = floraSurface(x, z);
            for (int y = Y - 5; y <= surface; y++)
                level.setBlock(new BlockPos(x, y, z), y == surface ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState(), 3);
            for (int y = surface + 1; y <= Y + 80; y++)
                level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }

        floraTree(level, -21, 10, birchLeaves, 8);
        floraTree(level, -13, 15, oakLeaves, 7);
        floraTree(level, -4, 13, birchLeaves, 8);
        floraTree(level, 5, 16, oakLeaves, 7);
        floraTree(level, 15, 13, birchLeaves, 8);
        floraTree(level, 22, 8, oakLeaves, 7);

        floraPatch(level, Blocks.FERN, -17, 1, 5, 0.66, 1841L);
        floraPatch(level, Blocks.ALLIUM, -6, 4, 4, 0.70, 1842L);
        floraPatch(level, Blocks.CORNFLOWER, 6, 2, 5, 0.68, 1843L);
        floraPatch(level, Blocks.OXEYE_DAISY, 17, -1, 5, 0.70, 1844L);

        // A few mossy stones break the flatness and frame the plant clusters.
        for (int[] p : new int[][]{{-9, -8}, {10, -7}, {21, 3}}) {
            int top = floraSurface(p[0], p[1]);
            level.setBlock(new BlockPos(p[0], top + 1, p[1]), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3);
            if ((p[0] & 1) == 0) level.setBlock(new BlockPos(p[0] + 1, top + 1, p[1]), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3);
        }

        viewer.teleportTo(level, 0.5, Y + 19, -25.5, 0f, 54f);
    }

    private static void floraCamera(MinecraftServer s, double x) {
        player(s).teleportTo(level(s), x, Y + 11, -15.5, 0f, 33f);
    }

    private static int floraSurface(int x, int z) {
        return Y + (int) Math.round(0.8 * Math.sin(x * 0.12) + 0.6 * Math.cos(z * 0.15));
    }

    private static void floraTree(ServerLevel level, int x, int z,
                                  net.minecraft.world.level.block.state.BlockState leaves, int height) {
        int ground = floraSurface(x, z);
        net.minecraft.world.level.block.Block log = leaves.is(Blocks.BIRCH_LEAVES) ? Blocks.BIRCH_LOG : Blocks.OAK_LOG;
        for (int y = 1; y <= height; y++)
            level.setBlock(new BlockPos(x, ground + y, z), log.defaultBlockState(), 3);
        for (int dy = height - 3; dy <= height; dy++) {
            int radius = dy == height ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > radius + 1) continue;
                BlockPos leaf = new BlockPos(x + dx, ground + dy, z + dz);
                if (level.isEmptyBlock(leaf)) level.setBlock(leaf, leaves, 3);
            }
        }
    }

    private static void floraPatch(ServerLevel level, net.minecraft.world.level.block.Block plant,
                                   int cx, int cz, int radius, double density, long seed) {
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(seed);
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            if (dx * dx + dz * dz > radius * radius || random.nextDouble() > density) continue;
            int x = cx + dx;
            int z = cz + dz;
            BlockPos pos = new BlockPos(x, floraSurface(x, z) + 1, z);
            level.setBlock(pos, plant.defaultBlockState(), 3);
        }
    }

    /** Inspect actual biome-generated flora in fresh chunks, without painting a showcase over the terrain. */
    private static void naturalFlora(MinecraftServer s) {
        naturalFlora(s, false);
    }

    private static void naturalFlora(MinecraftServer s, boolean forestOnly) {
        clearScene();
        ServerLevel level = level(s);
        ServerPlayer viewer = player(s);
        int centerX = 1024;
        int centerZ = 1024;
        String selectedBiome = "unknown";
        // Sample distant, previously ungenerated chunks so worldgen changes are visible.
        // Prefer an open meadow/flower forest; fall back to a regular forest only if none is nearby.
        for (int pass = forestOnly ? 1 : 0; pass < 2 && selectedBiome.equals("unknown"); pass++) {
            search:
            for (int radius = 10240; radius <= 16384; radius += 128) {
                for (int x = -radius; x <= radius; x += 128) {
                    for (int z = -radius; z <= radius; z += 128) {
                        if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                        BlockPos sample = new BlockPos(x, level.getSeaLevel(), z);
                        String biome = level.getBiome(sample).unwrapKey()
                                .map(key -> key.location().getPath()).orElse("unknown");
                        boolean openFlowerBiome = biome.equals("flower_forest") || biome.equals("meadow");
                        boolean forestFallback = biome.equals("forest") || biome.equals("birch_forest")
                                || biome.equals("old_growth_birch_forest") || biome.equals("dark_forest");
                        if (pass == 0 ? openFlowerBiome : forestFallback) {
                            centerX = x;
                            centerZ = z;
                            selectedBiome = biome;
                            break search;
                        }
                    }
                }
            }
        }

        // Load a modest fresh neighborhood so the screenshot and the counts come from real worldgen.
        int chunkX = centerX >> 4;
        int chunkZ = centerZ >> 4;
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++)
            level.getChunk(chunkX + dx, chunkZ + dz);

        int ferns = 0;
        int oxeye = 0;
        int cornflowers = 0;
        int alliums = 0;
        int azureBluets = 0;
        for (int x = centerX - 48; x <= centerX + 63; x++) {
            for (int z = centerZ - 48; z <= centerZ + 63; z++) {
                int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                for (int y = Math.max(level.getMinBuildHeight(), surface - 20); y <= surface + 6; y++) {
                    var block = level.getBlockState(new BlockPos(x, y, z)).getBlock();
                    if (block == Blocks.FERN) ferns++;
                    else if (block == Blocks.OXEYE_DAISY) oxeye++;
                    else if (block == Blocks.CORNFLOWER) cornflowers++;
                    else if (block == Blocks.ALLIUM) alliums++;
                    else if (block == Blocks.AZURE_BLUET) azureBluets++;
                }
            }
        }
        int cameraZ = centerZ - 35;
        int cameraY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerX, cameraZ) + 8;
        log("natural flora sample biome=" + selectedBiome + " at=" + centerX + "," + centerZ
                + " counts fern=" + ferns + " oxeye=" + oxeye + " cornflower=" + cornflowers
                + " allium=" + alliums + " azure_bluet=" + azureBluets + " cameraY=" + cameraY);
        viewer.teleportTo(level, centerX + 0.5, cameraY, cameraZ + 0.5, 0f, 26f);
    }

    private static void giveItems(MinecraftServer s, int from) {
        clearScene();
        ServerPlayer p = player(s);
        List<Item> items = new ArrayList<>();
        for (var h : ModRegistries.ITEMS.getEntries()) items.add(h.get());
        for (int i = 0; i < 9; i++) {
            int k = from + i;
            p.getInventory().setItem(i, k < items.size() ? new ItemStack(items.get(k)) : ItemStack.EMPTY);
        }
        p.getInventory().selected = 4;
    }

    // ================================================================== цикл

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!enabled()) return;
        Minecraft mc = Minecraft.getInstance();
        // Screen captures and reviewing other windows must not pause the test world.
        // This changes only this explicitly enabled test process, without saving options.
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) {
            mc.setScreen(null);
            return;
        }
        if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen onboarding) {
            log("completing first-launch onboarding in visual test profile");
            onboarding.onClose();
            return;
        }
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        if (mc.player.isDeadOrDying()) {
            mc.player.respawn();
            return;
        }
        if (readyTicks == 0) {
            MinecraftServer server = mc.getSingleplayerServer();
            server.execute(() -> {
                ServerPlayer p = player(server);
                p.setGameMode(GameType.CREATIVE);
                p.getAbilities().invulnerable = true;
                p.onUpdateAbilities();
                List<Entity> all = new ArrayList<>();
                level(server).getAllEntities().forEach(all::add);
                for (Entity e : all) {
                    if (!(e instanceof net.minecraft.world.entity.player.Player)) e.discard();
                }
            });
        }
        if (readyTicks < 100) {
            readyTicks++;
            return;
        }
        if (STEPS.isEmpty()) {
            build();
            String only = System.getProperty("regnum.visualonly");
            try {
                java.nio.file.Path f = mc.gameDirectory.toPath().resolve("visualonly.txt");
                if ((only == null || only.isBlank()) && java.nio.file.Files.exists(f)) only = java.nio.file.Files.readString(f).trim();
            } catch (Exception ignored) {
            }
            if (only != null && !only.isBlank()) {
                String[] pre = only.split(",");
                List<Step> keep = new ArrayList<>();
                REQUIRED_STEPS.clear();
                for (int i = 0; i < STEPS.size(); i++) {
                    Step s = STEPS.get(i);
                    boolean k = i == 0 || s.name() == null;
                    for (String raw : pre) {
                        String p = raw.trim();
                        boolean required = p.endsWith("!");
                        if (required) p = p.substring(0, p.length() - 1);
                        if (s.name() != null && s.name().startsWith(p)) {
                            k = true;
                            if (required) REQUIRED_STEPS.add(s.name());
                        }
                    }
                    if (k) keep.add(s);
                }
                STEPS.clear();
                STEPS.addAll(keep);
            }
        }
        try {
            if (serverFailure != null) {
                Regnum.LOGGER.error("[REGNUM-VISUAL] RESULT: FAIL — server step", serverFailure);
                mc.stop();
                return;
            }
            if (!serverReady) return;
            if (pendingClient != null) {
                Runnable action = pendingClient;
                pendingClient = null;
                action.run();
            }
            if (index < 0 || timer <= 0) {
                if (shotPending) {
                    Step prev = STEPS.get(index);
                    if (!prev.gui() && mc.screen != null)
                        throw new IllegalStateException("World screenshot obstructed by " + mc.screen.getClass().getSimpleName());
                    Screenshot.grab(mc.gameDirectory, prev.name() + ".png", mc.getMainRenderTarget(), msg -> {});
                    log("screenshot " + prev.name());
                    shotPending = false;
                    if (mc.screen != null) mc.setScreen(null);
                }
                index++;
                if (index >= STEPS.size()) {
                    log("RESULT: OK — сценарий завершён");
                    mc.stop();
                    return;
                }
                Step st = STEPS.get(index);
                mc.options.hideGui = !st.gui();
                MinecraftServer server = mc.getSingleplayerServer();
                serverReady = false;
                pendingClient = st.client();
                server.execute(() -> {
                    try {
                        st.server().accept(server);
                    } catch (Throwable t) {
                        Regnum.LOGGER.error("[REGNUM-VISUAL] step failed: " + st.name(), t);
                        serverFailure = t;
                    } finally {
                        serverReady = true;
                    }
                });
                timer = st.delay();
                shotPending = st.name() != null;
            } else {
                timer--;
            }
        } catch (Throwable t) {
            Regnum.LOGGER.error("[REGNUM-VISUAL] RESULT: FAIL", t);
            mc.stop();
        }
    }

    @SuppressWarnings("unused")
    private static ResourceLocation rl(String p) {
        return Regnum.id(p);
    }

    @SuppressWarnings("unused")
    private static TagKey<Structure> unused() {
        return null;
    }
}
