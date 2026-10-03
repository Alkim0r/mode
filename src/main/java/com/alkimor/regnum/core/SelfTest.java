package com.alkimor.regnum.core;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.dungeon.DungeonModule;
import com.alkimor.regnum.wanderers.WandererDialogs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Автотест при запуске сервера с -Dregnum.selftest=true (задача gradle runSelfTest).
 * Проверяет создание сущностей, генерацию структур, лут и рецепты, затем останавливает сервер.
 */
public final class SelfTest {
    private SelfTest() {}

    public static boolean enabled() {
        return Boolean.getBoolean("regnum.selftest");
    }

    public static void onServerStarted(ServerStartedEvent event) {
        if (!enabled()) return;
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        List<String> fails = new ArrayList<>();
        BlockPos spawn = level.getSharedSpawnPos();

        // Чистый старт: мир самотеста сохраняется между запусками, и бойцы прошлых прогонов копились (тысячи), искажая замеры.
        {
            int purged = 0;
            for (Entity old : new ArrayList<>(java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(), false).toList())) {
                if (old instanceof net.minecraft.world.entity.player.Player) continue;
                if (old instanceof com.alkimor.regnum.kingdom.SoldierEntity || old instanceof com.alkimor.regnum.kingdom.BanditEntity
                        || old instanceof net.minecraft.world.entity.animal.horse.AbstractHorse || old instanceof com.alkimor.regnum.dungeon.CryptLordEntity
                        || old instanceof net.minecraft.world.entity.monster.Monster) {
                    old.discard();
                    purged++;
                }
            }
            log("selftest: очищено сущностей прошлых прогонов: " + purged);
        }

        // 1. Сущности
        for (var holder : ModRegistries.ENTITIES.getEntries()) {
            EntityType<?> type = holder.get();
            try {
                Entity e = type.create(level);
                if (e == null) {
                    fails.add("entity null: " + holder.getId());
                    continue;
                }
                e.moveTo(spawn.getX() + 0.5, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ()), spawn.getZ() + 0.5);
                if (e instanceof Mob m) m.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn), MobSpawnType.COMMAND, null);
                level.addFreshEntity(e);
                for (int i = 0; i < 40; i++) e.tick();
                e.discard();
                log("entity OK: " + holder.getId());
            } catch (Throwable t) {
                fails.add("entity " + holder.getId() + ": " + t);
                Regnum.LOGGER.error("[SELFTEST] entity failure", t);
            }
        }

        // 2. Структуры: найти и сгенерировать
        locateAndBuild(level, spawn, DungeonModule.CRYPTS_TAG, "crypt", fails);
        locateAndBuild(level, spawn, DungeonModule.CAMPS_TAG, "bandit_camp", fails);
        for (String r : new String[]{"sunken_shrine", "forge_fortress", "sand_tomb"}) {
            locateAndBuild(level, spawn, TagKey.create(Registries.STRUCTURE, Regnum.id(r)), r, fails);
        }

        // 2b. Импортированные данжи (regnum:dt/*): ищем и генерируем; отсутствие в тестовом мире — только предупреждение
        for (String n : new String[]{"undead_crypt", "creeping_crypt", "toxic_lair", "lone_citadel", "desert_ruins", "bunker", "stray_fort", "witch_villa", "illager_camp"}) {
            try {
                var reg = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
                var h = reg.getHolder(ResourceKey.create(Registries.STRUCTURE, Regnum.id("dt/" + n)));
                if (h.isEmpty()) {
                    fails.add("dt structure not registered: " + n);
                    continue;
                }
                var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, net.minecraft.core.HolderSet.direct(h.get()), spawn, 120, false);
                if (found == null) {
                    log("WARN: dt " + n + " not found near spawn (biome-dependent)");
                    continue;
                }
                BlockPos at = found.getFirst();
                for (int dx = -4; dx <= 4; dx++)
                    for (int dz = -4; dz <= 4; dz++)
                        level.getChunk((at.getX() >> 4) + dx, (at.getZ() >> 4) + dz, ChunkStatus.FULL, true);
                log("dt " + n + " generated OK at " + at.getX() + " " + at.getZ());
            } catch (Throwable t) {
                fails.add("dt " + n + ": " + t);
            }
        }

        try {
            int placed = com.alkimor.regnum.dungeon.BossHook.flush(level);
            log("BossHook: залы боссов поставлены: " + placed);
        } catch (Throwable t) {
            fails.add("bosshook: " + t);
        }

        // 3. Лут-таблицы
        String[] tables = {"chests/crypt", "chests/crypt_treasure", "chests/bandit_stash", "gameplay/wounded_reward",
                "gameplay/hermit_reward", "entities/crypt_lord", "entities/bandit", "entities/mire_mother", "entities/forgemaster",
                "entities/scarab_queen", "chests/sunken_shrine", "chests/forge_fortress", "chests/sand_tomb"};
        for (String t : tables) {
            LootTable lt = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Regnum.id(t)));
            if (lt == LootTable.EMPTY) fails.add("loot table missing: " + t);
            else log("loot OK: " + t);
        }
        lootCheck(server, WandererDialogs.BANDIT_STASH, fails);

        // 4. Рецепты
        int recipes = 0;
        for (var r : server.getRecipeManager().getRecipes()) {
            if (r.id().getNamespace().equals(Regnum.MODID)) recipes++;
        }
        log("recipes loaded: " + recipes);
        if (recipes < 10) fails.add("too few recipes: " + recipes);

        // 5. Территория: защита земли и прочность построек
        try {
            var data = com.alkimor.regnum.kingdom.KingdomData.get(server);
            var city = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "Тест", spawn);
            data.add(city);
            if (com.alkimor.regnum.kingdom.Territory.cityAt(level, spawn.east(5)) != city) fails.add("territory: cityAt");
            BlockPos wall = spawn.above(60);
            level.setBlock(wall, net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState(), 3);
            int hits = 0;
            while (!com.alkimor.regnum.kingdom.Territory.damage(level, wall, 1f) && hits < 200) hits++;
            if (hits < 5 || hits >= 200) fails.add("territory: wall durability hits=" + hits);
            level.setBlock(wall, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            data.remove(city);
            log("territory OK: wall breaks after " + (hits + 1) + " hits");
        } catch (Throwable t) {
            fails.add("territory: " + t);
        }

        // 6. Стена рабочих: план, фундамент, ворота, расход казны
        try {
            var wc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "Стена", spawn);
            wc.treasury = 1000;
            wc.culture = 1;
            BlockPos wa = ground(level, spawn.getX() + 60, spawn.getZ() + 60), wb = ground(level, spawn.getX() + 74, spawn.getZ() + 60);
            var job = com.alkimor.regnum.kingdom.WallJob.plan(List.of(wa, wb), List.of(false, true), com.alkimor.regnum.kingdom.WallStyle.STONE);
            int guard = 0, placed = 0;
            while (guard++ < 40000) {
                var st = com.alkimor.regnum.kingdom.Walls.step(level, wc, job);
                if (st == com.alkimor.regnum.kingdom.Walls.Step.PLACED) placed++;
                if (st == com.alkimor.regnum.kingdom.Walls.Step.DONE || st == com.alkimor.regnum.kingdom.Walls.Step.WAIT) break;
            }
            int x0 = wa.getX(), z0 = wa.getZ();
            BlockPos topBlock = new BlockPos(x0, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x0, z0) - 1, z0);
            if (!job.done()) fails.add("wall: not finished");
            if (placed < 50) fails.add("wall: placed only " + placed);
            if (wc.treasury >= 1000) fails.add("wall: treasury unchanged");
            if (!level.getBlockState(topBlock).is(net.minecraft.world.level.block.Blocks.BRICKS)) fails.add("wall: wrong block " + level.getBlockState(topBlock));
            log("wall OK: " + placed + " blocks, cells=" + job.total() + ", treasury " + wc.treasury);
        } catch (Throwable t) {
            fails.add("wall: " + t);
        }

        // 6b. Здания города из схем культуры
        try {
            var kd2 = com.alkimor.regnum.kingdom.KingdomData.get(server);
            BlockPos hall = ground(level, spawn.getX() - 200, spawn.getZ() + 200);
            var bc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "Зодчие", hall);
            bc.treasury = 5000;
            bc.culture = 2;
            bc.level = 3;
            kd2.add(bc);
            for (var bt : com.alkimor.regnum.kingdom.BuildingType.values()) {
                BlockPos at = new BlockPos(hall.getX() - 12, hall.getY(), hall.getZ() - 6 + bt.ordinal() * 0);
                int before = bc.treasury;
                String why = com.alkimor.regnum.kingdom.CityBuildings.raise(level, bc, bt, at, net.minecraft.core.Direction.EAST);
                log("building " + bt + " -> " + (why == null ? "OK cost " + (before - bc.treasury) : why));
                if (why != null) fails.add("building " + bt + ": " + why);
            }
            kd2.remove(bc);
        } catch (Throwable t) {
            fails.add("buildings: " + t);
        }

        // 6c. Цена стен CASTLE: квадрат 60x60 с двумя воротами для каждой культуры
        try {
            for (int cu = 0; cu < 6; cu++) {
                var kit = com.alkimor.regnum.kingdom.WallKit.of(cu);
                int h = 30;
                List<int[]> pts = new java.util.ArrayList<>(List.of(new int[]{-h, -h}, new int[]{0, -h}, new int[]{h, -h}, new int[]{h, h}, new int[]{0, h}, new int[]{-h, h}));
                var ops = kit.plan(pts, List.of(false, true, false, false, true, false), true);
                int blocks = 0, missing = 0;
                for (var o : ops) {
                    if (!com.alkimor.regnum.kingdom.Prefab.exists(level, o.id())) missing++;
                    else blocks += com.alkimor.regnum.kingdom.Prefab.blocks(level, o.id());
                }
                log("castle wall " + com.alkimor.regnum.kingdom.Culture.byId(cu).id + ": " + ops.size() + " pieces, " + blocks + " blocks, cost " + blocks / com.alkimor.regnum.kingdom.WallStyle.CASTLE.blocksPerCoin + ", missing " + missing);
                if (missing > 0) fails.add("castle wall pieces missing: " + cu);
            }
        } catch (Throwable t) {
            fails.add("castle wall: " + t);
        }

        // 6d. Катапульта: баллистика, создание сущности, разрушение стены ядром в городе на войне
        try {
            for (double rg : new double[]{25, 40, 60}) {
                double v = com.alkimor.regnum.kingdom.SiegeBoulderEntity.speedFor(rg, 3);
                double x = 0, y = 0, vx = Math.cos(Math.toRadians(50)) * v, vy = Math.sin(Math.toRadians(50)) * v;
                for (int t = 0; t < 400; t++) {
                    x += vx; y += vy; vx *= 0.99; vy *= 0.99; vy -= 0.03;
                    if (vy < 0 && y <= 3) break;
                }
                log("catapult ballistics range " + rg + " -> lands " + String.format("%.1f", x) + " speed " + String.format("%.2f", v));
                if (Math.abs(x - rg) > 2.5) fails.add("ballistics off for " + rg);
            }
            BlockPos hall = ground(level, spawn.getX() + 260, spawn.getZ() - 260);
            var kd3 = com.alkimor.regnum.kingdom.KingdomData.get(server);
            var sc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "Осаждённый", hall);
            sc.war = true;
            kd3.add(sc);
            BlockPos wp = new BlockPos(hall.getX() + 6, hall.getY(), hall.getZ());
            for (int dy = 0; dy < 4; dy++)
                for (int dx = -2; dx <= 2; dx++)
                    for (int dz = -2; dz <= 2; dz++) level.setBlock(wp.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState(), 3);
            var cat = com.alkimor.regnum.kingdom.KingdomModule.CATAPULT.get().create(level);
            if (cat == null) fails.add("catapult create failed");
            var boulder = new com.alkimor.regnum.kingdom.SiegeBoulderEntity(level, cat);
            boulder.explode(level, wp.offset(0, 1, 0));
            int left1 = countLeft(level, wp);
            for (int i = 0; i < 8; i++) boulder.explode(level, wp.offset(0, 1, 0));
            int left = countLeft(level, wp);
            log("catapult boulder: after 1 hit " + left1 + " of 100 left, after 9 hits " + left);
            if (left1 < 85) fails.add("one boulder is too strong: left " + left1);
            if (left >= left1) fails.add("boulders do not accumulate damage: " + left1 + " -> " + left);
            kd3.remove(sc);
        } catch (Throwable t) {
            fails.add("catapult: " + t);
        }

        // 6e. Осадная башня: создание, стыковка у стены, выход воинов
        try {
            BlockPos th = ground(level, spawn.getX() + 300, spawn.getZ() + 300);
            var kd4 = com.alkimor.regnum.kingdom.KingdomData.get(server);
            var rl = new com.alkimor.regnum.kingdom.Realm(java.util.UUID.randomUUID(), "Осаждающие", "правитель", 1, th.getX(), th.getZ());
            kd4.addRealm(rl);
            var tc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "Башенный", th);
            tc.war = true;
            kd4.add(tc);
            level.setChunkForced(th.getX() >> 4, th.getZ() >> 4, true);
            for (int dy = 0; dy < 7; dy++)
                for (int dz = -5; dz <= 5; dz++)
                    level.setBlock(new BlockPos(th.getX() + 8, th.getY() + dy, th.getZ() + dz), net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState(), 3);
            var tw = com.alkimor.regnum.kingdom.KingdomModule.SIEGE_TOWER.get().create(level);
            if (tw == null) {
                fails.add("tower create failed");
            } else {
                tw.setRealm(rl.id);
                tw.setCarried(4);
                tw.moveTo(th.getX() + 3.5, th.getY(), th.getZ() + 0.5, 270f, 0f);
                level.addFreshEntity(tw);
                tw.forceDock(level, tc, net.minecraft.core.Direction.EAST);
                int n = 0;
                double wallX = th.getX() + 8;
                for (var s2 : tw.released) {
                    if (!s2.isRemoved() && s2.getX() >= wallX - 1 && s2.getY() >= th.getY() + 6) n++;
                    log("siege tower soldier at " + s2.blockPosition() + " removed " + s2.isRemoved());
                }
                log("siege tower: soldiers out " + n);
                if (n < 4) fails.add("tower released only " + n);
                tw.discard();
            }
            kd4.remove(tc);
        } catch (Throwable t) {
            fails.add("siege tower: " + t);
        }

        // 6g. Кампания «Хроника»
        try {
            var fpq = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "RegnumQuester"));
            var tag = new net.minecraft.nbt.CompoundTag();
            for (var st : com.alkimor.regnum.kingdom.Campaign.STEPS) st.done().test(fpq, tag);
            if (com.alkimor.regnum.kingdom.Campaign.STEPS.size() < 12) fails.add("campaign: мало шагов");
            if (com.alkimor.regnum.kingdom.Campaign.status(fpq).isEmpty()) fails.add("campaign: пустой статус");
            for (var st : com.alkimor.regnum.kingdom.Campaign.STEPS) {
                if (st.reward() == null) continue;
                String n = st.reward().split(":")[0];
                var it = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(com.alkimor.regnum.Regnum.id(n));
                if (it == net.minecraft.world.item.Items.AIR)
                    it = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(n));
                if (it == net.minecraft.world.item.Items.AIR) fails.add("campaign: нет предмета награды " + n);
            }
            Regnum.LOGGER.info("[REGNUM-SELFTEST] campaign steps=" + com.alkimor.regnum.kingdom.Campaign.STEPS.size());
        } catch (Throwable t) {
            fails.add("campaign: " + t);
        }

        // 6h. Шпионаж
        try {
            var kd6 = com.alkimor.regnum.kingdom.KingdomData.get(server);
            var spy = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "RegnumSpy"));
            com.alkimor.regnum.kingdom.Science.get(server).of(spy.getUUID()).done.add(com.alkimor.regnum.kingdom.Science.Tech.ESPIONAGE);
            BlockPos hh = ground(level, spawn.getX() - 400, spawn.getZ() - 400);
            var cs = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), spy.getUUID(), "Шпионград", hh);
            cs.treasury = 500;
            kd6.add(cs);
            var rs = new com.alkimor.regnum.kingdom.Realm(java.util.UUID.randomUUID(), "Цель", "тень", 3, hh.getX() + 300, hh.getZ());
            rs.known = 2;
            rs.strength = 10;
            kd6.addRealm(rs);
            String e0 = com.alkimor.regnum.kingdom.Espionage.send(spy, kd6, rs, cs);
            boolean ok = e0 == null && rs.agents == 1 && cs.treasury == 470;
            String r1 = com.alkimor.regnum.kingdom.Espionage.perform(spy, kd6, rs, cs, com.alkimor.regnum.kingdom.Espionage.Op.SABOTAGE, 0);
            ok &= r1.startsWith("Успех") && rs.strength == 8 && rs.relation == 0;
            com.alkimor.regnum.kingdom.Espionage.perform(spy, kd6, rs, cs, com.alkimor.regnum.kingdom.Espionage.Op.BRIBE, 0);
            ok &= rs.bribed;
            String r2 = com.alkimor.regnum.kingdom.Espionage.perform(spy, kd6, rs, cs, com.alkimor.regnum.kingdom.Espionage.Op.STEAL, 99);
            ok &= r2.startsWith("ПРОВАЛ") && rs.agents == 0 && rs.relation == -25;
            String r3 = com.alkimor.regnum.kingdom.Espionage.perform(spy, kd6, rs, cs, com.alkimor.regnum.kingdom.Espionage.Op.SCOUT, 0);
            ok &= r3.contains("нет ваших агентов");
            log("spy: " + (ok ? "OK" : "FAIL " + e0 + " | " + r1 + " | " + r2 + " | " + r3));
            if (!ok) fails.add("espionage");
            // наука
            var scn = com.alkimor.regnum.kingdom.Science.get(server);
            var sk = scn.of(spy.getUUID());
            boolean sok = com.alkimor.regnum.kingdom.Science.canResearch(sk, com.alkimor.regnum.kingdom.Science.Tech.WRITING)
                    && !com.alkimor.regnum.kingdom.Science.canResearch(sk, com.alkimor.regnum.kingdom.Science.Tech.MATHEMATICS)
                    && !com.alkimor.regnum.kingdom.Science.has(server, spy.getUUID(), com.alkimor.regnum.kingdom.SoldierType.SPEARMAN.tech());
            sk.current = com.alkimor.regnum.kingdom.Science.Tech.WRITING;
            var fin = scn.addPoints(spy.getUUID(), 35);
            sok &= fin == com.alkimor.regnum.kingdom.Science.Tech.WRITING && sk.progress == 5
                    && com.alkimor.regnum.kingdom.Science.canResearch(sk, com.alkimor.regnum.kingdom.Science.Tech.MATHEMATICS)
                    && com.alkimor.regnum.kingdom.Science.has(server, spy.getUUID(), com.alkimor.regnum.kingdom.BuildingType.LIBRARY.tech);
            // каждая технология достижима из начала: нет циклов
            java.util.Set<com.alkimor.regnum.kingdom.Science.Tech> reach = java.util.EnumSet.noneOf(com.alkimor.regnum.kingdom.Science.Tech.class);
            boolean grew = true;
            while (grew) {
                grew = false;
                for (var t : com.alkimor.regnum.kingdom.Science.Tech.values()) {
                    if (reach.contains(t)) continue;
                    if (java.util.Arrays.stream(t.prereq).allMatch(reach::contains)) { reach.add(t); grew = true; }
                }
            }
            sok &= reach.size() == com.alkimor.regnum.kingdom.Science.Tech.values().length;
            log("science: " + (sok ? "OK" : "FAIL") + " techs=" + reach.size());
            if (!sok) fails.add("science");
            scn.of(spy.getUUID()).done.clear();
            // деревни: верность, налог, бунт
            var vv = new com.alkimor.regnum.kingdom.VassalVillage(java.util.UUID.randomUUID(), "Тест", hh.east(50), spy.getUUID());
            vv.garrison = 2;
            vv.tax = 0;
            kd6.addVillage(vv);
            com.alkimor.regnum.kingdom.Villages.daily(server, level, kd6);
            boolean vok = vv.loyalty >= 59 && vv.loyalty <= 61;
            vv.garrison = 0;
            vv.tax = 2;
            vv.loyalty = 10;
            vv.famineDays = 0;
            com.alkimor.regnum.kingdom.Villages.daily(server, level, kd6);
            vok &= !kd6.villages().contains(vv);
            log("village: " + (vok ? "OK" : "FAIL loyalty=" + vv.loyalty));
            if (!vok) fails.add("village loyalty");
            // аванпосты по назначению
            var vo = new com.alkimor.regnum.kingdom.VassalVillage(java.util.UUID.randomUUID(), "Пост", hh.east(60), spy.getUUID());
            vo.loyalty = 60;
            vo.purpose = 4;
            kd6.addVillage(vo);
            cs.plagueDays = 5;
            int g0 = cs.glory, h0 = cs.stock(com.alkimor.regnum.kingdom.Resource.HERBS);
            com.alkimor.regnum.kingdom.Villages.daily(server, level, kd6);
            boolean pok = cs.plagueDays <= 4 && cs.stock(com.alkimor.regnum.kingdom.Resource.HERBS) > h0;
            vo.purpose = 2;
            com.alkimor.regnum.kingdom.Villages.daily(server, level, kd6);
            pok &= cs.glory >= g0 + 2;
            kd6.removeVillage(vo);
            cs.plagueDays = 0;
            // легендарный клинок
            cs.buildings.put(hh.east(5).asLong(), com.alkimor.regnum.kingdom.BuildingType.SMITHY);
            cs.deposit(com.alkimor.regnum.kingdom.Resource.IRON, 100);
            cs.treasury = 500;
            var blade = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
            String lerr = com.alkimor.regnum.kingdom.Legends.forge(spy, cs, blade, new java.util.Random(3));
            boolean lok = lerr == null && com.alkimor.regnum.kingdom.Legends.isLegend(blade) && cs.legends == 1 && cs.stock(com.alkimor.regnum.kingdom.Resource.IRON) == 60;
            lok &= com.alkimor.regnum.kingdom.Legends.forge(spy, cs, blade, new java.util.Random(3)) != null;
            lok &= com.alkimor.regnum.kingdom.Legends.forge(spy, cs, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK), new java.util.Random(3)) != null;
            cs.buildings.remove(hh.east(5).asLong());
            cs.take(com.alkimor.regnum.kingdom.Resource.IRON, cs.stock(com.alkimor.regnum.kingdom.Resource.IRON));
            // полевое командование при выходе игрока
            var fo = java.util.UUID.randomUUID();
            BlockPos fb = ground(level, spawn.getX() + 12, spawn.getZ() + 12);
            java.util.List<com.alkimor.regnum.kingdom.SoldierEntity> grp = new java.util.ArrayList<>();
            for (int i = 0; i < 4; i++) {
                var fs = com.alkimor.regnum.kingdom.KingdomModule.SOLDIER.get().create(level);
                fs.moveTo(fb.getX() + 0.5 + i, fb.getY(), fb.getZ() + 0.5);
                fs.setup(com.alkimor.regnum.kingdom.SoldierType.SWORDSMAN, fo, null, 1);
                fs.command(i < 2 ? com.alkimor.regnum.kingdom.Order.FOLLOW : com.alkimor.regnum.kingdom.Order.MOVE, fs.position().add(30, 0, 0), 0f, com.alkimor.regnum.kingdom.Formation.LINE, i, 4, null);
                level.addFreshEntity(fs);
                grp.add(fs);
            }
            var fr = com.alkimor.regnum.kingdom.FieldCommand.onLeave(level, fo, grp.get(0).position());
            boolean fcok = fr.total() == 4 && fr.pushing() == 2 && fr.camping() == 2 && fr.leader() != null
                    && grp.get(0).getOrder() == com.alkimor.regnum.kingdom.Order.HOLD && grp.get(2).getOrder() == com.alkimor.regnum.kingdom.Order.MOVE;
            log("fieldcommand: " + (fcok ? "OK" : "FAIL " + fr));
            if (!fcok) fails.add("fieldcommand");
            for (var fs : grp) fs.discard();
            // квесты: целостность данных и прохождение
            {
                var sreg = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
                java.util.List<String> bad = new java.util.ArrayList<>();
                for (var qd : com.alkimor.regnum.story.Quests.DEFS.values()) {
                    for (String pr : qd.prereq) if (!com.alkimor.regnum.story.Quests.DEFS.containsKey(pr)) bad.add(qd.id + " prereq " + pr);
                    for (var ob : qd.objs) {
                        switch (ob.kind()) {
                            case VISIT -> { for (String t : ob.target().split("\\|")) if (sreg.get(net.minecraft.resources.ResourceLocation.parse(t)) == null) bad.add(qd.id + " structure " + t); }
                            case KILL -> { if (!ob.target().startsWith("#") && !net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(net.minecraft.resources.ResourceLocation.parse(ob.target()))) bad.add(qd.id + " entity " + ob.target()); }
                            case COLLECT -> { var rl = ob.target().contains(":") ? net.minecraft.resources.ResourceLocation.parse(ob.target()) : com.alkimor.regnum.Regnum.id(ob.target()); if (net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl) == net.minecraft.world.item.Items.AIR) bad.add(qd.id + " item " + ob.target()); }
                            case BUILD -> { try { com.alkimor.regnum.kingdom.BuildingType.valueOf(ob.target()); } catch (Exception ex) { bad.add(qd.id + " building " + ob.target()); } }
                            default -> { }
                        }
                    }
                    for (String rw : qd.rewards) if (rw.startsWith("item:")) {
                        String[] ra = rw.split(":");
                        var rl = ra.length == 4 ? net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ra[1], ra[2]) : com.alkimor.regnum.Regnum.id(ra[1]);
                        var ri = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl);
                        if (ri == net.minecraft.world.item.Items.AIR) { ri = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(ra[1])); if (ri == net.minecraft.world.item.Items.AIR) bad.add(qd.id + " reward " + rw); }
                    }
                }
                var herb = com.alkimor.regnum.Regnum.id("healing_herb");
                spy.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(herb), 8));
                boolean qok = bad.isEmpty();
                qok &= !com.alkimor.regnum.story.Quests.available(spy, com.alkimor.regnum.story.Quests.DEFS.get("m2"));
                qok &= com.alkimor.regnum.story.Quests.accept(spy, "s1");
                qok &= com.alkimor.regnum.story.Quests.done(spy, "s1");
                qok &= com.alkimor.regnum.story.Quests.rep(spy, "keep") == 5;
                log("quests: " + (qok ? "OK" : "FAIL " + bad) + " defs=" + com.alkimor.regnum.story.Quests.DEFS.size());
                if (!qok) fails.add("quests " + bad);
                {
                    var rcv = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "RegnumRecover"));
                    boolean rok = com.alkimor.regnum.story.Recovery.destitute(rcv);
                    rcv.getPersistentData().putLong(com.alkimor.regnum.story.Recovery.KEY, rcv.serverLevel().getGameTime());
                    rok &= !com.alkimor.regnum.story.Recovery.give(rcv);
                    rcv.getPersistentData().remove(com.alkimor.regnum.story.Recovery.KEY);
                    rok &= com.alkimor.regnum.story.Recovery.give(rcv) && !com.alkimor.regnum.story.Recovery.destitute(rcv);
                    com.alkimor.regnum.kingdom.Marks.place(rcv, com.alkimor.regnum.kingdom.Marks.Kind.FLANK, rcv.blockPosition(), 5);
                    com.alkimor.regnum.kingdom.Marks.place(rcv, com.alkimor.regnum.kingdom.Marks.Kind.FLANK, rcv.blockPosition().above(), 5);
                    rok &= com.alkimor.regnum.kingdom.Marks.MARKS.stream().filter(m -> m.author.equals(rcv.getUUID())).count() == 1;
                    com.alkimor.regnum.kingdom.Marks.MARKS.removeIf(m -> m.author.equals(rcv.getUUID()));
                    {
                        var pcs = com.alkimor.regnum.kingdom.KingdomData.get(server).all().stream().findFirst().orElse(null);
                        if (pcs != null) {
                            var rep = com.alkimor.regnum.kingdom.Industry.balanceReport(level, pcs);
                            rok &= rep.size() >= 5 && rep.get(0).startsWith("Провиант");
                        }
                    }
                    {
                        var cr = com.alkimor.regnum.mine.MineModule.CRAWLER.get().create(level);
                        rok &= cr != null && cr.getMaxHealth() >= 20 && com.alkimor.regnum.mine.MineModule.CRAWLER_QUEEN.get().create(level) != null;
                        rok &= com.alkimor.regnum.mine.MineModule.CHITIN_PLATE.get() != null;
                    }
                    rok &= com.alkimor.regnum.survival.Respec.forget(rcv, com.alkimor.regnum.survival.Perk.values()[0]) != null;
                    {
                        var wd = com.alkimor.regnum.kingdom.KingdomData.get(server);
                        var wr = wd.realms().stream().findFirst().orElse(null);
                        if (wr != null) {
                            var rr = com.alkimor.regnum.kingdom.Realm.load(wr.save());
                            rok &= rr.warGoal == wr.warGoal && com.alkimor.regnum.kingdom.Realm.GOALS.length == 3;
                        }
                    }
                    rok &= !com.alkimor.regnum.trade.TradeNews.rumors().isEmpty();
                    for (var tg : com.alkimor.regnum.trade.TradeGood.values()) { double tf = com.alkimor.regnum.trade.TradeNews.factor(tg); rok &= tf > 0.7 && tf < 1.8; }
                    log("recovery+marks: " + (rok ? "OK" : "FAIL"));
                    if (!rok) fails.add("recovery");
                }
                try {
                    boolean cok = com.alkimor.regnum.kingdom.Contracts.rewardFactor(100) == 1.2 && com.alkimor.regnum.kingdom.Contracts.rewardFactor(-100) == 0.9
                            && com.alkimor.regnum.kingdom.Contracts.slotsFor(-8) == 2 && com.alkimor.regnum.kingdom.Contracts.slotsFor(0) == 3;
                    log("contractrep: " + (cok ? "OK" : "FAIL"));
                    if (!cok) fails.add("contractrep");
                } catch (Exception cex) { fails.add("contractrep " + cex); }
                try {
                    int sgn = com.alkimor.regnum.core.SaveGuard.backup(event.getServer());
                    log("saveguard: OK files=" + sgn);
                } catch (Exception sge) { fails.add("saveguard " + sge); log("saveguard EX " + sge); }
                try {
                    int sgf1 = SaveGuard.stamp(server);
                    int sgf2 = SaveGuard.stamp(server);
                    boolean sgfok = (sgf1 == 0 || sgf1 == SaveGuard.FORMAT) && sgf2 == SaveGuard.FORMAT;
                    log("saveformat: prev=" + sgf1 + " now=" + sgf2 + " " + (sgfok ? "OK" : "FAIL"));
                    if (!sgfok) fails.add("saveformat");
                } catch (Exception sgfe) { fails.add("saveformat " + sgfe); log("saveformat EX " + sgfe); }
                try {
                    boolean pyok = !com.alkimor.regnum.combat.PlayerParry.window(spy) && com.alkimor.regnum.combat.PlayerParry.WINDOW >= 9;
                    log("playerparry: " + (pyok ? "OK" : "FAIL"));
                    if (!pyok) fails.add("playerparry");
                } catch (Exception pye) { fails.add("playerparry " + pye); log("playerparry EX " + pye); }
                try {
                    boolean vaok = true;
                    var vabs = new Object[]{com.alkimor.regnum.dungeon.DungeonModule.CRYPT_LORD.get().create(level),
                            com.alkimor.regnum.dungeon.RegionsModule.MIRE_MOTHER.get().create(level),
                            com.alkimor.regnum.dungeon.RegionsModule.SCARAB_QUEEN.get().create(level),
                            com.alkimor.regnum.dungeon.RegionsModule.FORGEMASTER.get().create(level)};
                    for (Object vaoo : vabs) {
                        var vaa = (com.alkimor.regnum.dungeon.boss.VisualActor) vaoo;
                        vaok &= vaa.getVisualAction() == 0 && vaa.getVisualPhase() == 1;
                        vaa.visualStart(2, 10, 20);
                        vaok &= vaa.getVisualAction() == 2 && vaa.getVisualActionImpact() == 10 && vaa.getVisualActionDuration() == 20 && vaa.getVisualActionElapsed() >= 0;
                        vaa.visualStart(5, 6, 16); // короткий снаряд не перебивает подготовку удара
                        vaok &= vaa.getVisualAction() == 2;
                        vaa.visualStart(7, 0, 40); // смена фазы приоритетна
                        vaok &= vaa.getVisualAction() == 7;
                    }
                    var vads = com.alkimor.regnum.kingdom.KingdomModule.SOLDIER.get().create(level);
                    vads.setup(com.alkimor.regnum.kingdom.SoldierType.SPEARMAN, spy.getUUID(), null, 1);
                    vaok &= !vads.isDodging();
                    vads.startDodge(new net.minecraft.world.phys.Vec3(1, 0, 0), 9);
                    vaok &= vads.isDodging() && vads.getDodgeDuration() == 9;
                    vads.endDodge();
                    vaok &= !vads.isDodging();
                    log("visualaction: " + (vaok ? "OK" : "FAIL"));
                    if (!vaok) fails.add("visualaction");
                } catch (Exception vae) { fails.add("visualaction " + vae); log("visualaction EX " + vae); }
                try {
                    boolean dok = com.alkimor.regnum.kingdom.Discoveries.record(spy, "selftest_sample", 3) == 3
                            && com.alkimor.regnum.kingdom.Discoveries.record(spy, "selftest_sample", 3) == 0;
                    log("discoveries: " + (dok ? "OK" : "FAIL"));
                    if (!dok) fails.add("discoveries");
                } catch (Exception dex) { fails.add("discoveries " + dex); log("discoveries EX " + dex); }
                try {
                    var rr = new com.alkimor.regnum.kingdom.Realm(java.util.UUID.randomUUID(), "Тест", "Правитель", 0, 0, 0);
                    for (int ri = 0; ri < 8; ri++) rr.note("событие " + ri);
                    var rrb = com.alkimor.regnum.kingdom.Realm.load(rr.save());
                    boolean rrok = rrb.history.size() == 6 && rrb.history.get(5).startsWith("событие 7") && !rrb.temper().isEmpty();
                    log("realmhist: " + (rrok ? "OK" : "FAIL"));
                    if (!rrok) fails.add("realmhist");
                } catch (Exception rrx) { fails.add("realmhist " + rrx); log("realmhist EX " + rrx); }
                try {
                    var cprm = new com.alkimor.regnum.kingdom.Realm(java.util.UUID.randomUUID(), "Лагерный", "Правитель", 0, 10, 10);
                    cprm.camp.add(net.minecraft.core.BlockPos.asLong(5, 70, 6));
                    cprm.camp.add(net.minecraft.core.BlockPos.asLong(7, 70, 6));
                    var cprl = com.alkimor.regnum.kingdom.Realm.load(cprm.save());
                    boolean cpok = cprl.camp.size() == 2 && cprl.camp.get(0) == net.minecraft.core.BlockPos.asLong(5, 70, 6);
                    log("campsave: " + (cpok ? "OK" : "FAIL"));
                    if (!cpok) fails.add("campsave");
                } catch (Exception cpe) { fails.add("campsave " + cpe); log("campsave EX " + cpe); }
                try {
                    var exc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), spy.getUUID(), "Торг", new net.minecraft.core.BlockPos(5200, 70, 5200));
                    exc.treasury = 100;
                    String exe0 = com.alkimor.regnum.kingdom.Exchange.trade(exc, com.alkimor.regnum.kingdom.Resource.WOOD, 10, true, 0);
                    boolean exok = exe0 != null; // без рынка нельзя
                    exc.buildings.put(1L, com.alkimor.regnum.kingdom.BuildingType.MARKET);
                    exok &= com.alkimor.regnum.kingdom.Exchange.trade(exc, com.alkimor.regnum.kingdom.Resource.WOOD, 10, true, 0) == null && exc.treasury == 80 && exc.stock(com.alkimor.regnum.kingdom.Resource.WOOD) == 10;
                    exok &= com.alkimor.regnum.kingdom.Exchange.trade(exc, com.alkimor.regnum.kingdom.Resource.WOOD, 10, false, 0) == null && exc.treasury == 92;
                    exok &= com.alkimor.regnum.kingdom.Exchange.trade(exc, com.alkimor.regnum.kingdom.Resource.IRON, 50, true, 0) != null;
                    exok &= com.alkimor.regnum.kingdom.Exchange.price(com.alkimor.regnum.kingdom.Resource.FOOD, 3) > com.alkimor.regnum.kingdom.Exchange.price(com.alkimor.regnum.kingdom.Resource.FOOD, 1);
                    log("exchange: " + (exok ? "OK" : "FAIL") + " treasury=" + exc.treasury);
                    if (!exok) fails.add("exchange");
                } catch (Exception exe) { fails.add("exchange " + exe); log("exchange EX " + exe); }
                try {
                    boolean exok2 = com.alkimor.regnum.survival.Exploration.record(spy, "regnum_seen_biomes", "minecraft:plains")
                            && !com.alkimor.regnum.survival.Exploration.record(spy, "regnum_seen_biomes", "minecraft:plains")
                            && com.alkimor.regnum.survival.Exploration.record(spy, "regnum_seen_biomes", "minecraft:desert")
                            && com.alkimor.regnum.survival.Exploration.biomes(spy) == 2;
                    log("exploration: " + (exok2 ? "OK" : "FAIL"));
                    if (!exok2) fails.add("exploration");
                } catch (Exception exe2) { fails.add("exploration " + exe2); log("exploration EX " + exe2); }
                try {
                    var rgc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), spy.getUUID(), "Регентство", new net.minecraft.core.BlockPos(5300, 70, 5300));
                    rgc.population = 20;
                    int rgFull = rgc.dailyIncome(2);
                    rgc.absentDays = 6;
                    int rgLow = rgc.dailyIncome(2);
                    rgc.roles.put(java.util.UUID.randomUUID(), com.alkimor.regnum.kingdom.Council.Role.TREASURY.bit);
                    boolean rgok = rgLow < rgFull && rgc.hasRegent() && rgc.dailyIncome(2) == rgFull
                            && com.alkimor.regnum.kingdom.City.load(rgc.save()).absentDays == 6;
                    log("regency: " + (rgok ? "OK" : "FAIL") + " " + rgFull + "/" + rgLow);
                    if (!rgok) fails.add("regency");
                } catch (Exception rge) { fails.add("regency " + rge); log("regency EX " + rge); }
                try {
                    var ctc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), spy.getUUID(), "Вклад", new net.minecraft.core.BlockPos(5400, 70, 5400));
                    ctc.addContrib(spy.getUUID(), 300);
                    ctc.addContrib(spy.getUUID(), 600);
                    var ctl = com.alkimor.regnum.kingdom.City.load(ctc.save());
                    boolean ctok = ctl.contrib.getOrDefault(spy.getUUID(), 0) == 900 && com.alkimor.regnum.kingdom.City.rankTitle(900).equals("Опора города");
                    log("contrib: " + (ctok ? "OK" : "FAIL"));
                    if (!ctok) fails.add("contrib");
                } catch (Exception cte) { fails.add("contrib " + cte); log("contrib EX " + cte); }
                try {
                    var lbc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), spy.getUUID(), "Промысел", new net.minecraft.core.BlockPos(5500, 70, 5500));
                    lbc.population = 20;
                    int lbIncome = lbc.dailyIncome(2);
                    lbc.labor = com.alkimor.regnum.kingdom.Labor.byKey("дерево");
                    String lbMsg = com.alkimor.regnum.kingdom.Labor.daily(lbc);
                    boolean lbok = lbc.dailyIncome(2) < lbIncome && lbc.stock(com.alkimor.regnum.kingdom.Resource.WOOD) == 8 && lbMsg.contains("+8")
                            && com.alkimor.regnum.kingdom.City.load(lbc.save()).labor == lbc.labor;
                    log("labor: " + (lbok ? "OK" : "FAIL") + " " + lbMsg);
                    if (!lbok) fails.add("labor");
                } catch (Exception lbe) { fails.add("labor " + lbe); log("labor EX " + lbe); }
                try {
                    var pyc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), spy.getUUID(), "Курс", new net.minecraft.core.BlockPos(5600, 70, 5600));
                    pyc.population = 40;
                    int pyBase = pyc.dailyIncome(2);
                    pyc.priority = com.alkimor.regnum.kingdom.Priority.byKey("казна");
                    int pyTreas = pyc.dailyIncome(2);
                    pyc.priority = com.alkimor.regnum.kingdom.Priority.byKey("оборона");
                    int pyDef = pyc.dailyIncome(2);
                    boolean pyok = pyTreas > pyBase && pyDef < pyBase && com.alkimor.regnum.kingdom.Priority.foodMult(pyc) == 1.0
                            && com.alkimor.regnum.kingdom.Priority.upkeepMult(pyc) < 1.0 && com.alkimor.regnum.kingdom.City.load(pyc.save()).priority == 3;
                    log("priority: " + (pyok ? "OK" : "FAIL") + " " + pyBase + "/" + pyTreas + "/" + pyDef);
                    if (!pyok) fails.add("priority");
                } catch (Exception pye) { fails.add("priority " + pye); log("priority EX " + pye); }
                try {
                    var soc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), spy.getUUID(), "Вылазка", new net.minecraft.core.BlockPos(5700, 70, 5700));
                    soc.treasury = 100;
                    String soNo = com.alkimor.regnum.kingdom.Sortie.start(soc, 1000);
                    soc.soldiers.put(java.util.UUID.randomUUID(), 0);
                    String soOk = com.alkimor.regnum.kingdom.Sortie.start(soc, 1000);
                    String soAgain = com.alkimor.regnum.kingdom.Sortie.start(soc, 1100);
                    boolean sook = soNo != null && soOk == null && soc.treasury == 80 && soAgain != null
                            && com.alkimor.regnum.kingdom.Sortie.extraRadius(soc.id, 1100) == 40 && com.alkimor.regnum.kingdom.Sortie.extraRadius(soc.id, 2300) == 16
                            && com.alkimor.regnum.kingdom.Sortie.start(soc, 1000 + com.alkimor.regnum.kingdom.Sortie.COOLDOWN + 1) == null;
                    log("sortie: " + (sook ? "OK" : "FAIL") + " " + soNo + " | " + soAgain);
                    if (!sook) fails.add("sortie");
                } catch (Exception soe) { fails.add("sortie " + soe); log("sortie EX " + soe); }
                try {
                    boolean dfok = com.alkimor.regnum.kingdom.Diplomacy.driftStep(80, false, false) == 79
                            && com.alkimor.regnum.kingdom.Diplomacy.driftStep(30, true, false) == 30
                            && com.alkimor.regnum.kingdom.Diplomacy.driftStep(65, true, true) == 64
                            && com.alkimor.regnum.kingdom.Diplomacy.driftStep(-10, false, false) == -9
                            && com.alkimor.regnum.kingdom.Diplomacy.driftStep(-60, false, false) == -60;
                    log("drift: " + (dfok ? "OK" : "FAIL"));
                    if (!dfok) fails.add("drift");
                } catch (Exception dfe) { fails.add("drift " + dfe); log("drift EX " + dfe); }
                try {
                    var exr = com.alkimor.regnum.kingdom.Resource.IRON;
                    com.alkimor.regnum.kingdom.Exchange.resetPressure();
                    int exp0 = com.alkimor.regnum.kingdom.Exchange.priceNow(exr, 0);
                    com.alkimor.regnum.kingdom.Exchange.addPressure(exr, 500);
                    int exp1 = com.alkimor.regnum.kingdom.Exchange.priceNow(exr, 0);
                    com.alkimor.regnum.kingdom.Exchange.coolDown();
                    int exCool = com.alkimor.regnum.kingdom.Exchange.pressure(exr);
                    com.alkimor.regnum.kingdom.Exchange.addPressure(exr, -1000);
                    int exp2 = com.alkimor.regnum.kingdom.Exchange.priceNow(exr, 0);
                    com.alkimor.regnum.kingdom.Exchange.resetPressure();
                    boolean expok = exp0 == 6 && exp1 == 9 && exCool == 180 && exp2 == 3;
                    log("exchangepressure: " + (expok ? "OK" : "FAIL") + " " + exp0 + "/" + exp1 + "/" + exCool + "/" + exp2);
                    if (!expok) fails.add("exchangepressure");
                } catch (Exception exe2) { fails.add("exchangepressure " + exe2); log("exchangepressure EX " + exe2); }
                try {
                    boolean rtok = com.alkimor.regnum.kingdom.Villages.routeMul(100.0 * 100.0) == 1.2 && com.alkimor.regnum.kingdom.Villages.routeMul(250.0 * 250.0) == 1.0
                            && com.alkimor.regnum.kingdom.Villages.routeMul(500.0 * 500.0) == 0.8;
                    log("villageroute: " + (rtok ? "OK" : "FAIL"));
                    if (!rtok) fails.add("villageroute");
                } catch (Exception rte) { fails.add("villageroute " + rte); log("villageroute EX " + rte); }
                try {
                    var fk = com.alkimor.regnum.kingdom.Fortune.Kind.class;
                    boolean fcnok = com.alkimor.regnum.kingdom.Fortune.nextInChain(com.alkimor.regnum.kingdom.Fortune.Kind.HARVEST) == com.alkimor.regnum.kingdom.Fortune.Kind.MERCHANTS
                            && com.alkimor.regnum.kingdom.Fortune.nextInChain(com.alkimor.regnum.kingdom.Fortune.Kind.DROUGHT) == com.alkimor.regnum.kingdom.Fortune.Kind.BLIGHT
                            && com.alkimor.regnum.kingdom.Fortune.nextInChain(com.alkimor.regnum.kingdom.Fortune.Kind.METEOR) == com.alkimor.regnum.kingdom.Fortune.Kind.SCHOLAR
                            && com.alkimor.regnum.kingdom.Fortune.nextInChain(com.alkimor.regnum.kingdom.Fortune.Kind.PLEA) == null && fk != null;
                    log("fortunechain: " + (fcnok ? "OK" : "FAIL"));
                    if (!fcnok) fails.add("fortunechain");
                } catch (Exception fce) { fails.add("fortunechain " + fce); log("fortunechain EX " + fce); }
                try {
                    boolean psok = com.alkimor.regnum.combat.Posture.threshold(0) == 30f && com.alkimor.regnum.combat.Posture.threshold(3) == 60f
                            && com.alkimor.regnum.combat.Posture.gain(7f) == 14f
                            && com.alkimor.regnum.combat.Posture.decayed(20f, 50) == 20f
                            && Math.abs(com.alkimor.regnum.combat.Posture.decayed(20f, 60 + 100) - 12.5f) < 0.01f
                            && com.alkimor.regnum.combat.Posture.decayed(5f, 1000) == 0f;
                    log("posture: " + (psok ? "OK" : "FAIL"));
                    if (!psok) fails.add("posture");
                } catch (Exception pse) { fails.add("posture " + pse); log("posture EX " + pse); }
                try {
                    var mdc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), spy.getUUID(), "Дух", new net.minecraft.core.BlockPos(5800, 70, 5800));
                    mdc.population = 40;
                    int mdBase = com.alkimor.regnum.kingdom.Mood.score(mdc);
                    int mdInc0 = mdc.dailyIncome(2);
                    mdc.hungerDays = 5;
                    mdc.plagueDays = 3;
                    mdc.war = true;
                    int mdBad = com.alkimor.regnum.kingdom.Mood.score(mdc);
                    int mdInc1 = mdc.dailyIncome(2);
                    boolean mdok = mdBase == 50 && mdBad == 0 && mdInc1 < mdInc0
                            && com.alkimor.regnum.kingdom.Mood.incomeMult(80) == 1.10 && com.alkimor.regnum.kingdom.Mood.incomeMult(50) == 1.0
                            && com.alkimor.regnum.kingdom.Mood.title(10).equals("на грани бунта");
                    log("mood: " + (mdok ? "OK" : "FAIL") + " " + mdBase + "/" + mdBad + " " + mdInc0 + "->" + mdInc1);
                    if (!mdok) fails.add("mood");
                } catch (Exception mde) { fails.add("mood " + mde); log("mood EX " + mde); }
                try {
                    boolean cbok = com.alkimor.regnum.combat.Combo.next(0, 0, 100) == 1 && com.alkimor.regnum.combat.Combo.next(2, 100, 130) == 3
                            && com.alkimor.regnum.combat.Combo.next(4, 100, 141) == 1
                            && com.alkimor.regnum.combat.Combo.multiplier(1) == 1f && Math.abs(com.alkimor.regnum.combat.Combo.multiplier(6) - 1.15f) < 0.001f
                            && Math.abs(com.alkimor.regnum.combat.Combo.multiplier(20) - 1.15f) < 0.001f;
                    log("combo: " + (cbok ? "OK" : "FAIL"));
                    if (!cbok) fails.add("combo");
                } catch (Exception cbe) { fails.add("combo " + cbe); log("combo EX " + cbe); }
                try {
                    var rtc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), spy.getUUID(), "Бунт", new net.minecraft.core.BlockPos(5900, 70, 5900));
                    rtc.treasury = 200;
                    rtc.hungerDays = 10;
                    rtc.plagueDays = 3; // настроение 0
                    String rt1 = com.alkimor.regnum.kingdom.Mood.daily(rtc);
                    String rt2 = com.alkimor.regnum.kingdom.Mood.daily(rtc);
                    String rt3 = com.alkimor.regnum.kingdom.Mood.daily(rtc);
                    boolean rtok = rt1.contains("1/3") && rt2.contains("2/3") && rt3.startsWith("БУНТ") && rtc.treasury == 180 && rtc.unrestDays == 0
                            && com.alkimor.regnum.kingdom.Mood.unrestStep(2, 50) == 0
                            && com.alkimor.regnum.kingdom.City.load(rtc.save()).unrestDays == 0;
                    log("riot: " + (rtok ? "OK" : "FAIL") + " " + rt3 + " treasury=" + rtc.treasury);
                    if (!rtok) fails.add("riot");
                } catch (Exception rte2) { fails.add("riot " + rte2); log("riot EX " + rte2); }
                try {
                    boolean fsok = "b".equals(com.alkimor.regnum.kingdom.ai.FoeScanGoal.mostCommon(java.util.List.of("a", "b", "b", "c")))
                            && "a".equals(com.alkimor.regnum.kingdom.ai.FoeScanGoal.mostCommon(java.util.List.of("a", "b")))
                            && com.alkimor.regnum.kingdom.ai.FoeScanGoal.mostCommon(java.util.List.<String>of()) == null;
                    log("focusfire: " + (fsok ? "OK" : "FAIL"));
                    if (!fsok) fails.add("focusfire");
                } catch (Exception fse) { fails.add("focusfire " + fse); log("focusfire EX " + fse); }
                try {
                    boolean crok = com.alkimor.regnum.kingdom.CaravanRoute.payMult(1, 0.1) == 0.0
                            && com.alkimor.regnum.kingdom.CaravanRoute.payMult(1, 0.9) == 1.35
                            && com.alkimor.regnum.kingdom.CaravanRoute.payMult(2, 0.0) == 0.85
                            && com.alkimor.regnum.kingdom.CaravanRoute.payMult(3, 0.0) == 1.0
                            && com.alkimor.regnum.kingdom.CaravanRoute.toll(3) == 4
                            && com.alkimor.regnum.kingdom.CaravanRoute.byKey("платный") == 3;
                    log("caravanroute: " + (crok ? "OK" : "FAIL"));
                    if (!crok) fails.add("caravanroute");
                } catch (Exception cre) { fails.add("caravanroute " + cre); log("caravanroute EX " + cre); }
                try {
                    boolean mi = com.alkimor.regnum.kingdom.Exchange.staleDays(10, 9) == 0
                            && com.alkimor.regnum.kingdom.Exchange.staleDays(10, 7) == 1
                            && com.alkimor.regnum.kingdom.Exchange.staleDays(3, 9) == 0
                            && com.alkimor.regnum.kingdom.Exchange.shownPrice(8, 5, 0) == 8
                            && com.alkimor.regnum.kingdom.Exchange.shownPrice(8, 5, 2) == 5;
                    log("marketintel: " + (mi ? "OK" : "FAIL"));
                    if (!mi) fails.add("marketintel");
                } catch (Exception mie) { fails.add("marketintel " + mie); log("marketintel EX " + mie); }
                try {
                    boolean tr = com.alkimor.regnum.kingdom.Treaties.daysLeft(48000, 0) == 2
                            && com.alkimor.regnum.kingdom.Treaties.daysLeft(0, 5000) == 0
                            && com.alkimor.regnum.kingdom.Treaties.breachPenalty(true, true) == 40
                            && com.alkimor.regnum.kingdom.Treaties.breachPenalty(true, false) == 20
                            && com.alkimor.regnum.kingdom.Treaties.breachPenalty(false, false) == 0
                            && com.alkimor.regnum.kingdom.Treaties.expired(100, 100)
                            && !com.alkimor.regnum.kingdom.Treaties.expired(100, 99);
                    log("treaties: " + (tr ? "OK" : "FAIL"));
                    if (!tr) fails.add("treaties");
                } catch (Exception tre) { fails.add("treaties " + tre); log("treaties EX " + tre); }
                try {
                    boolean encok = Math.abs(com.alkimor.regnum.combat.Tactics.slotAngle(1, 4) - Math.PI / 2) < 1e-9
                            && com.alkimor.regnum.combat.Tactics.slotAngle(0, 3) == 0
                            && Math.abs(com.alkimor.regnum.combat.Tactics.slotAngle(5, 4) - Math.PI / 2) < 1e-9
                            && com.alkimor.regnum.combat.Tactics.slotAngle(2, 0) == 0;
                    log("encircle: " + (encok ? "OK" : "FAIL"));
                    if (!encok) fails.add("encircle");
                } catch (Exception ence) { fails.add("encircle " + ence); log("encircle EX " + ence); }
                try {
                    var adm = new java.util.EnumMap<com.alkimor.regnum.kingdom.SoldierType, Integer>(com.alkimor.regnum.kingdom.SoldierType.class);
                    boolean adok = com.alkimor.regnum.kingdom.Espionage.assaultAdvice(adm).contains("не нужен");
                    adm.put(com.alkimor.regnum.kingdom.SoldierType.ARCHER, 6);
                    adm.put(com.alkimor.regnum.kingdom.SoldierType.SPEARMAN, 2);
                    adok &= com.alkimor.regnum.kingdom.Espionage.assaultAdvice(adm).contains("лучников");
                    log("assaultadvice: " + (adok ? "OK" : "FAIL"));
                    if (!adok) fails.add("assaultadvice");
                } catch (Exception ade) { fails.add("assaultadvice " + ade); log("assaultadvice EX " + ade); }
                try {
                    var drc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), spy.getUUID(), "Засуха", new net.minecraft.core.BlockPos(5600, 70, 5600));
                    drc.droughtDays = 1;
                    drc.treasury = 100;
                    String drMsg = com.alkimor.regnum.kingdom.Industry.daily(level, com.alkimor.regnum.kingdom.KingdomData.get(server), drc);
                    boolean drok = drc.droughtDays == 0 && drMsg.contains("бунт") && drc.treasury == 90
                            && com.alkimor.regnum.kingdom.City.load(drc.save()).droughtDays == 0;
                    log("drought: " + (drok ? "OK" : "FAIL") + " " + drMsg);
                    if (!drok) fails.add("drought");
                } catch (Exception dre) { fails.add("drought " + dre); log("drought EX " + dre); }
                try {
                    boolean rpok = com.alkimor.regnum.kingdom.Espionage.reported(20, 100, new java.util.Random(1)) == 20;
                    for (int rpi = 0; rpi < 50; rpi++) {
                        int rpv = com.alkimor.regnum.kingdom.Espionage.reported(20, 0, new java.util.Random(rpi));
                        rpok &= rpv >= 15 && rpv <= 25;
                    }
                    log("reported: " + (rpok ? "OK" : "FAIL"));
                    if (!rpok) fails.add("reported");
                } catch (Exception rpe) { fails.add("reported " + rpe); log("reported EX " + rpe); }
                try {
                    var kdx = com.alkimor.regnum.kingdom.KingdomData.get(server);
                    var scx = com.alkimor.regnum.kingdom.Science.get(server);
                    var sxo = java.util.UUID.randomUUID();
                    var sxc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), sxo, "Знание", new net.minecraft.core.BlockPos(5700, 70, 5700));
                    sxc.treasury = 100;
                    var sxr = new com.alkimor.regnum.kingdom.Realm(java.util.UUID.randomUUID(), "Учёные", "Правитель", 0, 5800, 5800);
                    boolean sxok = com.alkimor.regnum.kingdom.Science.exchange(server, sxo, kdx, sxr, sxc, 10) != null; // не союзник
                    sxr.trade = true;
                    sxok &= com.alkimor.regnum.kingdom.Science.exchange(server, sxo, kdx, sxr, sxc, 10) != null; // нет исследования
                    scx.of(sxo).current = com.alkimor.regnum.kingdom.Science.Tech.WRITING;
                    sxok &= com.alkimor.regnum.kingdom.Science.exchange(server, sxo, kdx, sxr, sxc, 10) == null && sxc.treasury == 70 && scx.of(sxo).progress > 0;
                    sxok &= com.alkimor.regnum.kingdom.Science.exchange(server, sxo, kdx, sxr, sxc, 11) != null; // кулдаун
                    log("sciexchange: " + (sxok ? "OK" : "FAIL"));
                    if (!sxok) fails.add("sciexchange");
                } catch (Exception sxe) { fails.add("sciexchange " + sxe); log("sciexchange EX " + sxe); }
                try {
                    var ebm = com.alkimor.regnum.story.ExternalBosses.parse("[{\"id\":\"othermod:titan\",\"title\":\"Титан\",\"emeralds\":40,\"steward\":10}]");
                    var ebe = ebm.get("othermod:titan");
                    boolean ebok = ebe != null && ebe.emeralds() == 40 && ebe.title().equals("Титан");
                    ebok &= com.alkimor.regnum.story.ExternalBosses.parse(com.alkimor.regnum.story.ExternalBosses.DEFAULTS).containsKey("cataclysm:ender_guardian");
                    ebok &= com.alkimor.regnum.story.ExternalBosses.reward(spy, ebe);
                    ebok &= !com.alkimor.regnum.story.ExternalBosses.reward(spy, ebe); // повторно награды нет
                    log("externalboss: " + (ebok ? "OK" : "FAIL"));
                    if (!ebok) fails.add("externalboss");
                } catch (Exception ebx) { fails.add("externalboss " + ebx); log("externalboss EX " + ebx); }
                try {
                                        boolean frcok = com.alkimor.regnum.kingdom.FireControl.allows(0, false, 900)
                            && !com.alkimor.regnum.kingdom.FireControl.allows(1, false, 20 * 20) && com.alkimor.regnum.kingdom.FireControl.allows(1, false, 8 * 8)
                            && !com.alkimor.regnum.kingdom.FireControl.allows(2, false, 4) && com.alkimor.regnum.kingdom.FireControl.allows(2, true, 900)
                            && com.alkimor.regnum.kingdom.FireControl.byName("засада") == 1;
                    com.alkimor.regnum.kingdom.FireControl.set(spy, 2);
                    frcok &= com.alkimor.regnum.kingdom.FireControl.mode(spy) == 2;
                    com.alkimor.regnum.kingdom.FireControl.set(spy, 0);
                    log("firecontrol: " + (frcok ? "OK" : "FAIL"));
                    if (!frcok) fails.add("firecontrol");
                } catch (Exception frce) { fails.add("firecontrol " + frce); log("firecontrol EX " + frce); }
                try {
                    var flv = com.alkimor.regnum.kingdom.KingdomModule.SOLDIER.get().create(level);
                    var fla = com.alkimor.regnum.kingdom.KingdomModule.SOLDIER.get().create(level);
                    flv.setPos(0, 80, 0);
                    flv.yBodyRot = 0f; // смотрит на +Z
                    fla.setPos(0, 80, 3);
                    boolean flok = !com.alkimor.regnum.kingdom.KingdomEvents.isBehind(flv, fla);
                    fla.setPos(0, 80, -3);
                    flok &= com.alkimor.regnum.kingdom.KingdomEvents.isBehind(flv, fla);
                    fla.setPos(3, 80, 0);
                    flok &= !com.alkimor.regnum.kingdom.KingdomEvents.isBehind(flv, fla);
                    log("flank: " + (flok ? "OK" : "FAIL"));
                    if (!flok) fails.add("flank");
                } catch (Exception fle) { fails.add("flank " + fle); log("flank EX " + fle); }
                try {
                    boolean rtok = com.alkimor.regnum.kingdom.KingdomEvents.retreatFactor(com.alkimor.regnum.kingdom.SoldierType.Role.SHIELD, 0) == 0.6f
                            && com.alkimor.regnum.kingdom.KingdomEvents.retreatFactor(com.alkimor.regnum.kingdom.SoldierType.Role.ARCHER, 0) == 1.0f
                            && Math.abs(com.alkimor.regnum.kingdom.KingdomEvents.retreatFactor(com.alkimor.regnum.kingdom.SoldierType.Role.HEAVY, 5) - 0.48f) < 0.001f;
                    log("retreat: " + (rtok ? "OK" : "FAIL"));
                    if (!rtok) fails.add("retreat");
                } catch (Exception rte) { fails.add("retreat " + rte); log("retreat EX " + rte); }
                try {
                    var spc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "Спец", spy.blockPosition());
                    spc.treasury = 100;
                    boolean spk = com.alkimor.regnum.kingdom.Specialization.choose(spc, 2, 10) == null && spc.spec == 2 && spc.treasury == 100
                            && com.alkimor.regnum.kingdom.Specialization.choose(spc, 3, 12) != null
                            && com.alkimor.regnum.kingdom.Specialization.choose(spc, 3, 16) != null
                            && com.alkimor.regnum.kingdom.City.load(spc.save()).spec == 2;
                    spc.treasury = 200;
                    spk &= com.alkimor.regnum.kingdom.Specialization.choose(spc, 3, 16) == null && spc.treasury == 50;
                    log("spec: " + (spk ? "OK" : "FAIL"));
                    if (!spk) fails.add("spec");
                } catch (Exception sx) { fails.add("spec " + sx); log("spec EX " + sx); }
                try {
                    var rc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "Руины", spy.blockPosition());
                    rc.buildings.put(1L, com.alkimor.regnum.kingdom.BuildingType.MARKET);
                    rc.buildings.put(2L, com.alkimor.regnum.kingdom.BuildingType.SMITHY);
                    boolean rok = rc.ruinSome(net.minecraft.util.RandomSource.create(1), 2).size() == 2 && rc.buildings.isEmpty() && rc.ruined.size() == 2;
                    rok &= com.alkimor.regnum.kingdom.City.load(rc.save()).ruined.size() == 2;
                    rc.treasury = 200;
                    rok &= com.alkimor.regnum.kingdom.Ruins.repairOne(rc).startsWith("Починено") && rc.buildings.size() == 1 && rc.treasury == 110;
                    log("ruins: " + (rok ? "OK" : "FAIL"));
                    if (!rok) fails.add("ruins");
                } catch (Exception rex) { fails.add("ruins " + rex); log("ruins EX " + rex); }
                try {
                    spy.getPersistentData().putInt(com.alkimor.regnum.survival.Needs.THIRST, 100);
                    com.alkimor.regnum.survival.Needs.drink(spy, 300);
                    boolean nok = com.alkimor.regnum.survival.Needs.thirst(spy) == 400;
                    int amb = com.alkimor.regnum.survival.Needs.ambient(spy);
                    nok &= amb >= -150 && amb <= 150;
                    var plate = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE);
                    com.alkimor.regnum.survival.ArmorDirt.add(plate, 40);
                    com.alkimor.regnum.survival.ArmorDirt.add(plate, -10);
                    nok &= com.alkimor.regnum.survival.ArmorDirt.dirt(plate) == 30;
                    log("needs: thirst=" + com.alkimor.regnum.survival.Needs.thirst(spy) + " ambient=" + amb + " dirt=" + com.alkimor.regnum.survival.ArmorDirt.dirt(plate) + " " + (nok ? "OK" : "FAIL"));
                    if (!nok) fails.add("needs");
                } catch (Exception nex) { fails.add("needs " + nex); log("needs EX " + nex); }
                try {
                    var mq = com.alkimor.regnum.mine.MineModule.CRAWLER.get().create(level);
                    var mqs1 = com.alkimor.regnum.kingdom.KingdomModule.SOLDIER.get().create(level);
                    var mqs2 = com.alkimor.regnum.kingdom.KingdomModule.SOLDIER.get().create(level);
                    mqs1.setup(com.alkimor.regnum.kingdom.SoldierType.SPEARMAN, spy.getUUID(), null, 1);
                    mqs2.setup(com.alkimor.regnum.kingdom.SoldierType.SPEARMAN, spy.getUUID(), null, 1);
                    mqs1.moveTo(0.5, 100, 0.5); mqs2.moveTo(2.5, 100, 0.5); mq.moveTo(1.5, 100, 4.5);
                    level.addFreshEntity(mqs1); level.addFreshEntity(mqs2); level.addFreshEntity(mq);
                    // обычный ползун теряет цель на свету, выводок Королевы — нет, но цель-мертвец сбрасывается
                    mq.setTarget(mqs1); mq.setTarget(null);
                    boolean normalOk = mq.getTarget() == null;
                    mq.addTag("regnum_minion");
                    mq.setTarget(mqs1); mq.setTarget(null);
                    boolean keeps = mq.getTarget() == mqs1;
                    mqs1.kill(); mq.setTarget(null);
                    boolean dropsDead = mq.getTarget() == null;
                    mq.setTarget(mqs2);
                    boolean again = mq.getTarget() == mqs2;
                    log("minion: normal=" + normalOk + " keeps=" + keeps + " dropsDead=" + dropsDead + " retarget=" + again + " " + (normalOk && keeps && dropsDead && again ? "OK" : "FAIL"));
                    if (!(normalOk && keeps && dropsDead && again)) fails.add("minion");
                    mq.discard(); mqs1.discard(); mqs2.discard();
                } catch (Exception mex) { fails.add("minion " + mex); log("minion EX " + mex); }
                try {
                    var fv1 = com.alkimor.regnum.survival.FoodVariety.record(spy, "minecraft:bread");
                    var fv2 = com.alkimor.regnum.survival.FoodVariety.record(spy, "minecraft:bread");
                    var fv3 = com.alkimor.regnum.survival.FoodVariety.record(spy, "minecraft:apple");
                    boolean fvok = fv1[1] == 0 && fv2[1] == 1 && fv3[1] == 0 && fv3[0] == 2;
                    log("foodvariety: kinds=" + fv3[0] + " " + (fvok ? "OK" : "FAIL"));
                    if (!fvok) fails.add("foodvariety");
                } catch (Exception fex) { fails.add("foodvariety " + fex); log("foodvariety EX " + fex); }
                try {
                    var qls = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
                    int qlm = qls.getMaxDamage();
                    com.alkimor.regnum.survival.Quality.apply(qls, 4);
                    var qla = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE);
                    com.alkimor.regnum.survival.Quality.apply(qla, 3);
                    boolean qlok = qls.getMaxDamage() > qlm && com.alkimor.regnum.survival.Quality.of(qls) == 4
                            && com.alkimor.regnum.survival.Quality.tier(0, 10) == 0 && com.alkimor.regnum.survival.Quality.tier(100, 90) == 4
                            && com.alkimor.regnum.survival.Quality.of(qla) == 3;
                    log("quality: sword max " + qlm + "->" + qls.getMaxDamage() + " " + (qlok ? "OK" : "FAIL"));
                    if (!qlok) fails.add("quality");
                } catch (Exception qlex) { fails.add("quality " + qlex); log("quality EX " + qlex); }
                try {
                    var prsc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), spy.getUUID(), "Острог", new net.minecraft.core.BlockPos(5000, 70, 5000));
                    prsc.prisoners = 3; prsc.treasury = 0;
                    prsc.ruined.put(1L, com.alkimor.regnum.kingdom.BuildingType.MARKET);
                    boolean prsok = com.alkimor.regnum.kingdom.Prisoners.act(spy, prsc, "ransom") == null && prsc.treasury == com.alkimor.regnum.kingdom.Prisoners.RANSOM && prsc.prisoners == 2;
                    prsok &= com.alkimor.regnum.kingdom.Prisoners.act(spy, prsc, "labor") == null && prsc.ruined.isEmpty() && prsc.prisoners == 1;
                    prsok &= com.alkimor.regnum.kingdom.Prisoners.act(spy, prsc, "labor") != null && prsc.prisoners == 1;
                    prsok &= com.alkimor.regnum.kingdom.City.load(prsc.save()).prisoners == 1;
                    log("prisoners: " + (prsok ? "OK" : "FAIL"));
                    if (!prsok) fails.add("prisoners");
                } catch (Exception prsex) { fails.add("prisoners " + prsex); log("prisoners EX " + prsex); }
                try {
                    var spc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), spy.getUUID(), "Амбар", new net.minecraft.core.BlockPos(5100, 70, 5100));
                    spc.deposit(com.alkimor.regnum.kingdom.Resource.FOOD, 100);
                    spc.deposit(com.alkimor.regnum.kingdom.Resource.WOOD, 100);
                    String spmsg = com.alkimor.regnum.kingdom.Spoilage.daily(level, spc);
                    boolean spok = spc.stock(com.alkimor.regnum.kingdom.Resource.FOOD) < 100 && spc.stock(com.alkimor.regnum.kingdom.Resource.FOOD) >= 90
                            && spc.stock(com.alkimor.regnum.kingdom.Resource.WOOD) == 100 && !spmsg.isEmpty()
                            && com.alkimor.regnum.kingdom.Spoilage.rate(com.alkimor.regnum.kingdom.Resource.FOOD, true, 0) < com.alkimor.regnum.kingdom.Spoilage.rate(com.alkimor.regnum.kingdom.Resource.FOOD, false, 0);
                    log("spoilage: " + spmsg + " " + (spok ? "OK" : "FAIL"));
                    if (!spok) fails.add("spoilage");
                } catch (Exception spex) { fails.add("spoilage " + spex); log("spoilage EX " + spex); }
                try {
                    var hc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "Мор", spy.blockPosition());
                    boolean hok = !com.alkimor.regnum.kingdom.Health.diagnose(event.getServer(), hc).isEmpty();
                    hc.plagueDays = 3; hc.plagueLevel = 2; hc.plagueSource = 1;
                    hok &= com.alkimor.regnum.kingdom.Health.diagnose(event.getServer(), hc).size() >= 4;
                    hc.plagueDays = 0; hc.postPlagueDays = 2;
                    hok &= com.alkimor.regnum.kingdom.Health.startChance(event.getServer(), hc) == 0f;
                    var hcn = hc.save();
                    hok &= com.alkimor.regnum.kingdom.City.load(hcn).postPlagueDays == 2;
                    log("plaguediag: " + (hok ? "OK" : "FAIL"));
                    if (!hok) fails.add("plaguediag");
                } catch (Exception hex) { fails.add("plaguediag " + hex); log("plaguediag EX " + hex); }
                try {
                    var snapP = com.alkimor.regnum.story.Quests.snapshot(spy, true);
                    var qsfb = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                    com.alkimor.regnum.core.network.QuestSnapshotPayload.CODEC.encode(qsfb, snapP);
                    var qsback = com.alkimor.regnum.core.network.QuestSnapshotPayload.CODEC.decode(qsfb);
                    boolean qssok = qsback.quests().size() == snapP.quests().size() && qsback.repFactions().size() == 4 && !qsback.quests().isEmpty();
                    var qscb = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                    com.alkimor.regnum.core.network.CutscenePayload.CODEC.encode(qscb, new com.alkimor.regnum.core.network.CutscenePayload(true, "Босс", 120));
                    qssok &= com.alkimor.regnum.core.network.CutscenePayload.CODEC.decode(qscb).ticks() == 120;
                    log("questsnap: " + (qssok ? "OK" : "FAIL") + " n=" + qsback.quests().size());
                    if (!qssok) fails.add("questsnap");
                } catch (Exception qsex) { fails.add("questsnap " + qsex); log("questsnap EX " + qsex); }
            }
            // совет: роли
            var friend = java.util.UUID.randomUUID();
            boolean ccok = com.alkimor.regnum.kingdom.Council.can(cs, spy.getUUID(), com.alkimor.regnum.kingdom.Council.Role.TREASURY)
                    && !com.alkimor.regnum.kingdom.Council.can(cs, friend, com.alkimor.regnum.kingdom.Council.Role.TREASURY);
            cs.roles.put(friend, com.alkimor.regnum.kingdom.Council.Role.COMMAND.bit);
            ccok &= com.alkimor.regnum.kingdom.Council.can(cs, friend, com.alkimor.regnum.kingdom.Council.Role.COMMAND)
                    && !com.alkimor.regnum.kingdom.Council.can(cs, friend, com.alkimor.regnum.kingdom.Council.Role.BUILD)
                    && com.alkimor.regnum.kingdom.Council.isMember(cs, friend);
            cs.roles.remove(friend);
            cs.trusted.add(friend);
            ccok &= com.alkimor.regnum.kingdom.Council.can(cs, friend, com.alkimor.regnum.kingdom.Council.Role.BUILD)
                    && !com.alkimor.regnum.kingdom.Council.can(cs, friend, com.alkimor.regnum.kingdom.Council.Role.TREASURY);
            cs.trusted.remove(friend);
            log("council: " + (ccok ? "OK" : "FAIL"));
            if (!ccok) fails.add("council");
            // разбор сражения
            var bo = java.util.UUID.randomUUID();
            for (int i = 0; i < 5; i++) com.alkimor.regnum.kingdom.BattleReport.record(bo, false, null, 0);
            com.alkimor.regnum.kingdom.BattleReport.record(bo, true, com.alkimor.regnum.kingdom.SoldierType.ARCHER, 0);
            String brs = com.alkimor.regnum.kingdom.BattleReport.flushFor(bo);
            boolean brok = brs != null && brs.contains("Побед: 5") && brs.contains("потерь: 1");
            log("battlereport: " + (brok ? "OK" : "FAIL " + brs));
            if (!brok) fails.add("battlereport");
            log("legend: " + (lok ? "OK" : "FAIL " + lerr));
            if (!lok) fails.add("legend");
            log("outpost: " + (pok ? "OK" : "FAIL"));
            if (!pok) fails.add("outpost");
            // склад, кузница, голод, эпидемия
            cs.population = 10;
            cs.stock.clear();
            int dep = cs.deposit(com.alkimor.regnum.kingdom.Resource.FOOD, 50);
            cs.buildings.put(hh.asLong(), com.alkimor.regnum.kingdom.BuildingType.SMITHY);
            cs.deposit(com.alkimor.regnum.kingdom.Resource.IRON, 10);
            com.alkimor.regnum.kingdom.Industry.daily(level, kd6, cs);
            boolean iok = dep == 50 && cs.forged >= 1 && cs.stock(com.alkimor.regnum.kingdom.Resource.IRON) < 10 && cs.hungerDays == 0;
            cs.buildings.remove(hh.asLong());
            for (int i = 0; i < 30; i++) cs.soldiers.put(java.util.UUID.randomUUID(), 0);
            cs.stock.put(com.alkimor.regnum.kingdom.Resource.FOOD, 0);
            com.alkimor.regnum.kingdom.Industry.daily(level, kd6, cs);
            iok &= cs.hungerDays == 1;
            cs.soldiers.clear();
            cs.hungerDays = 0;
            com.alkimor.regnum.kingdom.Health.start(level, cs, 2);
            iok &= cs.plagueDays > 0 && cs.plagueLevel == 2;
            int before = cs.plagueDays;
            com.alkimor.regnum.kingdom.Health.daily(level, kd6, cs);
            iok &= cs.plagueDays < before || cs.plagueDays == 0;
            cs.plagueDays = 0;
            iok &= com.alkimor.regnum.kingdom.Health.startChance(server, cs) > 0f;
            log("industry: " + (iok ? "OK" : "FAIL forged=" + cs.forged + " hunger=" + cs.hungerDays + " plague=" + cs.plagueDays));
            if (!iok) fails.add("industry");
            // дипломатия: предложение, перемирие, вероломство
            var dr = new com.alkimor.regnum.kingdom.Realm(java.util.UUID.randomUUID(), "Дипломаты", "князь", 1, hh.getX() + 400, hh.getZ());
            dr.built = true;
            var dr2 = new com.alkimor.regnum.kingdom.Realm(java.util.UUID.randomUUID(), "Соседи", "хан", 3, hh.getX() - 400, hh.getZ());
            dr2.built = true;
            dr2.relation = 30;
            kd6.addRealm(dr);
            kd6.addRealm(dr2);
            dr.offer = com.alkimor.regnum.kingdom.Diplomacy.OFFER_TRADE;
            dr.offerExpires = level.getGameTime() + 100000;
            com.alkimor.regnum.kingdom.Diplomacy.accept(spy, dr);
            boolean dok = dr.trade && dr.offer == 0;
            dr.state = com.alkimor.regnum.kingdom.Realm.PEACE;
            dr.relation = 50;
            com.alkimor.regnum.kingdom.Diplomacy.setTruce(level, dr, 3);
            dok &= com.alkimor.regnum.kingdom.Diplomacy.truce(level, dr) && !com.alkimor.regnum.kingdom.Diplomacy.hasCb(dr);
            com.alkimor.regnum.kingdom.Diplomacy.declare(level, kd6, dr, spy);
            dok &= dr.state == com.alkimor.regnum.kingdom.Realm.WAR && dr2.relation < 30;
            dr.offer = com.alkimor.regnum.kingdom.Diplomacy.OFFER_TRIBUTE;
            dr.state = com.alkimor.regnum.kingdom.Realm.PEACE;
            com.alkimor.regnum.kingdom.Diplomacy.refuse(spy, dr);
            dok &= dr.playerCb && dr.relation <= 0;
            log("diplomacy: " + (dok ? "OK" : "FAIL trade=" + dr.trade + " war=" + dr.state + " r2=" + dr2.relation));
            if (!dok) fails.add("diplomacy");
            boolean cok = com.alkimor.regnum.kingdom.Commissions.Branch.AIR.accepts(com.alkimor.regnum.kingdom.SoldierType.MUSKETEER)
                    && !com.alkimor.regnum.kingdom.Commissions.Branch.AIR.accepts(com.alkimor.regnum.kingdom.SoldierType.LIGHT_CAV)
                    && com.alkimor.regnum.kingdom.Compat.techBlocks(level, cs) == 0 && cs.mech == 0;
            log("compat: " + (cok ? "OK" : "FAIL") + " create=" + com.alkimor.regnum.kingdom.Compat.create());
            if (!cok) fails.add("compat");
            // контракты
            long day0 = 100;
            var cons = com.alkimor.regnum.kingdom.Contracts.get(server);
            cons.of(spy.getUUID()).clear();
            com.alkimor.regnum.kingdom.Contracts.refresh(server, spy.getUUID(), day0);
            var cl = cons.of(spy.getUUID());
            boolean kok = cl.size() == 3;
            var sup = new com.alkimor.regnum.kingdom.Contracts.Contract();
            sup.kind = com.alkimor.regnum.kingdom.Contracts.Kind.SUPPLY;
            sup.arg = com.alkimor.regnum.kingdom.Resource.WOOD.ordinal();
            sup.amount = 20;
            sup.gold = 33;
            sup.glory = 2;
            sup.expires = day0 + 3;
            cl.add(0, sup);
            cs.stock.put(com.alkimor.regnum.kingdom.Resource.WOOD, 25);
            int tr0 = cs.treasury;
            kd6.add(cs);
            kok &= com.alkimor.regnum.kingdom.Contracts.complete(spy, 0) && cs.treasury == tr0 + 33 && cs.stock(com.alkimor.regnum.kingdom.Resource.WOOD) == 5;
            cons.of(spy.getUUID()).clear();
            log("contracts: " + (kok ? "OK" : "FAIL size=" + cl.size() + " tr=" + cs.treasury));
            if (!kok) fails.add("contracts");
            // превратности судьбы
            cs.stock.put(com.alkimor.regnum.kingdom.Resource.FOOD, 100);
            boolean fok = com.alkimor.regnum.kingdom.Fortune.fire(spy, cs, com.alkimor.regnum.kingdom.Fortune.Kind.HARVEST, new java.util.Random(1))
                    && cs.stock(com.alkimor.regnum.kingdom.Resource.FOOD) > 100
                    && com.alkimor.regnum.kingdom.Fortune.fire(spy, cs, com.alkimor.regnum.kingdom.Fortune.Kind.BLIGHT, new java.util.Random(1))
                    && com.alkimor.regnum.kingdom.Fortune.fire(spy, cs, com.alkimor.regnum.kingdom.Fortune.Kind.METEOR, new java.util.Random(1));
            log("fortune: " + (fok ? "OK" : "FAIL"));
            if (!fok) fails.add("fortune");
            kd6.realms().remove(dr);
            kd6.realms().remove(dr2);
            kd6.remove(cs);
        } catch (Throwable t) {
            fails.add("espionage: " + t);
        }

        // 6f. Караван и союз
        try {
            var kd5 = com.alkimor.regnum.kingdom.KingdomData.get(server);
            BlockPos ch = ground(level, spawn.getX() - 300, spawn.getZ() - 300);
            var cc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "Торговый", ch);
            kd5.add(cc);
            var al = new com.alkimor.regnum.kingdom.Realm(java.util.UUID.randomUUID(), "Партнёры", "купец", 1, ch.getX() + 200, ch.getZ());
            al.trade = true;
            al.relation = 80;
            kd5.addRealm(al);
            var cv = com.alkimor.regnum.kingdom.KingdomModule.CARAVAN.get().create(level);
            if (cv == null) fails.add("caravan create failed");
            else {
                cv.setRoute(al.id, cc.id);
                cv.moveTo(ch.getX() + 0.5, ch.getY(), ch.getZ() + 0.5, 0f, 0f);
                level.addFreshEntity(cv);
                int before = cc.treasury;
                cv.arrive(level, kd5, cc);
                log("caravan paid " + (cc.treasury - before) + ", relation " + al.relation);
                if (cc.treasury - before < 12) fails.add("caravan paid nothing");
            }
            al.ally = true;
            cc.war = true;
            log("ally dbg: loaded " + level.isLoaded(cc.hall) + " aided " + cc.aided + " hall " + cc.hall + " warCities " + kd5.all().stream().filter(x -> x.war).count());
            int helpers = com.alkimor.regnum.kingdom.Realms.allyAidForTest(level, kd5, al, cc);
            log("ally aid: " + helpers + " soldiers");
            if (helpers < 6) fails.add("ally sent " + helpers);
            kd5.remove(cc);
        } catch (Throwable t) {
            fails.add("caravan/ally: " + t);
        }

        // 7. Соседние королевства: столица, правитель, гарнизон, поход армии
        try {
            var kd = com.alkimor.regnum.kingdom.KingdomData.get(server);
            BlockPos rp = ground(level, spawn.getX() - 90, spawn.getZ() - 90);
            var realm = new com.alkimor.regnum.kingdom.Realm(java.util.UUID.randomUUID(), "Тестовое княжество", "князь Тест", 1, rp.getX(), rp.getZ());
            kd.addRealm(realm);
            boolean builtOk = com.alkimor.regnum.kingdom.Realms.build(level, kd, realm);
            if (!builtOk) { com.alkimor.regnum.kingdom.Realms.buildAt(level, realm, rp.getX(), rp.getY() - 1, rp.getZ()); builtOk = realm.built; }
            log("realm build -> " + builtOk + " at " + realm.x + "," + realm.z + " y " + realm.y);
            int garrison = 0;
            boolean ruler = false;
            for (var e : level.getEntitiesOfClass(com.alkimor.regnum.kingdom.SoldierEntity.class, new net.minecraft.world.phys.AABB(realm.capital()).inflate(30))) {
                if (realm.id.equals(e.realmId())) {
                    garrison++;
                    if (e.getTags().contains(com.alkimor.regnum.kingdom.Realms.RULER_TAG)) ruler = true;
                }
            }
            if (garrison < 5) fails.add("realm: garrison " + garrison);
            if (!ruler) fails.add("realm: no ruler");
            realm.state = com.alkimor.regnum.kingdom.Realm.WAR;
            var fakeCity = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "Цель", ground(level, spawn.getX() + 10, spawn.getZ() + 10));
            kd.add(fakeCity);
            var fakeKing = server.getPlayerList().getPlayers().isEmpty() ? null : server.getPlayerList().getPlayers().get(0);
            if (fakeKing != null) {
                int sent = com.alkimor.regnum.kingdom.Realms.sendArmy(level, kd, realm, fakeCity, fakeKing);
                if (sent < 5) fails.add("realm: army sent " + sent);
            }
            log("realm OK: garrison " + garrison + ", ruler " + ruler + ", capital " + realm.capital());
            kd.remove(fakeCity);
        } catch (Throwable t) {
            fails.add("realm: " + t);
        }

        // 8. Осада: сапёр ломает стену чужого города на войне
        try {
            var kd2 = com.alkimor.regnum.kingdom.KingdomData.get(server);
            BlockPos sg = ground(level, spawn.getX() + 120, spawn.getZ() - 120);
            var sc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "Осаждённый", sg);
            sc.war = true;
            kd2.add(sc);
            BlockPos wp = sg.offset(3, 0, 0);
            for (int dy = 0; dy < 3; dy++) level.setBlock(wp.above(dy), net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState(), 3);
            var sap = com.alkimor.regnum.kingdom.KingdomModule.SOLDIER.get().create(level);
            sap.setup(com.alkimor.regnum.kingdom.SoldierType.SWORDSMAN, java.util.UUID.randomUUID(), null, 2);
            sap.setRealm(java.util.UUID.randomUUID());
            sap.addTag(com.alkimor.regnum.kingdom.Realms.SAPPER_TAG);
            sap.moveTo(sg.getX() + 2.5, sg.getY(), sg.getZ() + 0.5);
            level.addFreshEntity(sap);
            int tries = 0;
            boolean broke = false;
            while (tries++ < 60 && !broke) broke = com.alkimor.regnum.kingdom.Realms.breach(sap, new net.minecraft.world.phys.Vec3(wp.getX() + 5, sg.getY(), wp.getZ()));
            sap.discard();
            kd2.remove(sc);
            if (!broke) fails.add("siege: sapper did not break the wall");
            log("siege OK: wall breached in " + tries + " swings");
        } catch (Throwable t) {
            fails.add("siege: " + t);
        }

        // 9. События: Ходоки Ночи (урон), беженцы
        try {
            var walker = com.alkimor.regnum.kingdom.Events.spawnWalker(level, ground(level, spawn.getX() + 30, spawn.getZ() + 140), false);
            if (walker == null) fails.add("night: no walker");
            else {
                float steel = com.alkimor.regnum.kingdom.Events.nightDamage(walker, level.damageSources().generic(), 10f);
                float fire = com.alkimor.regnum.kingdom.Events.nightDamage(walker, level.damageSources().inFire(), 10f);
                if (steel > 2f || fire < 15f) fails.add("night: damage rule steel=" + steel + " fire=" + fire);
                walker.discard();
                log("night OK: steel 10 -> " + steel + ", fire 10 -> " + fire);
            }
            var rc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), "Беженцы", ground(level, spawn.getX() + 20, spawn.getZ() + 160));
            var kd3 = com.alkimor.regnum.kingdom.KingdomData.get(server);
            kd3.add(rc);
            if (!server.getPlayerList().getPlayers().isEmpty()) com.alkimor.regnum.kingdom.Events.spawnRefugees(level, rc, server.getPlayerList().getPlayers().get(0), level.getGameTime());
            kd3.remove(rc);
        } catch (Throwable t) {
            fails.add("events: " + t);
        }

        // 10. Броня культур, вассальные деревни, туман королевств
        try {
            var fp = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "RegnumArmorTester"));
            var slots = new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                    net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
            int okSets = 0;
            for (var cult : com.alkimor.regnum.kingdom.Culture.values()) {
                var pieces = com.alkimor.regnum.armor.ArmorModule.PIECES.get(cult);
                for (int i = 0; i < 4; i++) fp.setItemSlot(slots[i], new net.minecraft.world.item.ItemStack(pieces.get(i).get()));
                if (com.alkimor.regnum.armor.ArmorModule.fullSet(fp) == cult) okSets++;
                else fails.add("armor: full set not detected for " + cult);
            }
            fp.setItemSlot(slots[3], net.minecraft.world.item.ItemStack.EMPTY);
            if (com.alkimor.regnum.armor.ArmorModule.fullSet(fp) != null) fails.add("armor: partial set counted");
            log("armor OK: " + okSets + " full sets detected");
            var kd4 = com.alkimor.regnum.kingdom.KingdomData.get(server);
            var vc = new com.alkimor.regnum.kingdom.City(java.util.UUID.randomUUID(), fp.getUUID(), "Вассалы", ground(level, spawn.getX() + 40, spawn.getZ() + 180));
            kd4.add(vc);
            var vv = new com.alkimor.regnum.kingdom.VassalVillage(java.util.UUID.randomUUID(), "Тестовка", vc.hall, fp.getUUID());
            kd4.addVillage(vv);
            int tribute = com.alkimor.regnum.kingdom.Villages.dailyBonus(kd4, vc);
            kd4.removeVillage(vv);
            kd4.remove(vc);
            if (tribute < 3) fails.add("village: tribute " + tribute);
            else log("village OK: tribute " + tribute);
        } catch (Throwable t) {
            fails.add("armor/village: " + t);
        }

        if (!fails.isEmpty()) {
            for (String f : fails) log("FAIL: " + f);
            log("RESULT: FAIL (" + fails.size() + ")");
            server.halt(false);
            return;
        }
        log("static checks OK, starting live scenarios");
        startScenarios(level, spawn);
    }

    // ------------------------------------------------------------------ живые сценарии

    private static int scenarioTicks = -1;
    private static final List<Entity> bandits = new ArrayList<>();
    private static final List<Entity> bosses = new ArrayList<>();
    private static int startBandits;

    private static final List<Entity> soldiers = new ArrayList<>();

    private static BlockPos ground(ServerLevel level, int x, int z) {
        level.setChunkForced(x >> 4, z >> 4, true);
        return new BlockPos(x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    private static void startScenarios(ServerLevel level, BlockPos spawn) {
        java.util.UUID fakeKing = java.util.UUID.randomUUID();
        // 1. Бой: 4 солдата против 4 разбойников
        for (int i = 0; i < 4; i++) {
            BlockPos p = ground(level, spawn.getX() + i * 2, spawn.getZ());
            var s = com.alkimor.regnum.kingdom.KingdomModule.SOLDIER.get().create(level);
            s.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            s.setup(i % 2 == 0 ? com.alkimor.regnum.kingdom.SoldierType.SWORDSMAN : com.alkimor.regnum.kingdom.SoldierType.ARCHER, fakeKing, null, 1);
            s.command(com.alkimor.regnum.kingdom.Order.HOLD, s.position(), 0f, com.alkimor.regnum.kingdom.Formation.LINE, i, 4, null);
            s.trainTech(com.alkimor.regnum.kingdom.Technique.FOCUS_FIRE, 20);
            level.addFreshEntity(s);
            soldiers.add(s);
            BlockPos q = ground(level, spawn.getX() + i * 2, spawn.getZ() + 8);
            var b = com.alkimor.regnum.kingdom.KingdomModule.BANDIT.get().create(level);
            b.moveTo(q.getX() + 0.5, q.getY(), q.getZ() + 0.5);
            b.setup(i % 2);
            b.setPersistenceRequired();
            level.addFreshEntity(b);
            bandits.add(b);
        }
        startBandits = bandits.size();
        // 2. Каждый босс против отряда
        int dx = 40;
        for (var type : new EntityType<?>[]{DungeonModule.CRYPT_LORD.get(), com.alkimor.regnum.dungeon.RegionsModule.MIRE_MOTHER.get(),
                com.alkimor.regnum.dungeon.RegionsModule.FORGEMASTER.get(), com.alkimor.regnum.dungeon.RegionsModule.SCARAB_QUEEN.get(),
                com.alkimor.regnum.mine.MineModule.CRAWLER_QUEEN.get()}) {
            BlockPos p = ground(level, spawn.getX() - dx, spawn.getZ());
            Entity boss = type.create(level);
            if (boss instanceof Mob m) {
                m.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                m.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.COMMAND, null);
                m.setHealth(m.getMaxHealth() * 0.3f); // сразу 3-я фаза, чтобы проверить все способности
                level.addFreshEntity(m);
                bosses.add(m);
                for (int i = 0; i < 3; i++) {
                    var s = com.alkimor.regnum.kingdom.KingdomModule.SOLDIER.get().create(level);
                    BlockPos q = ground(level, p.getX() + 4 + i, p.getZ() + 3);
                    s.moveTo(q.getX() + 0.5, q.getY(), q.getZ() + 0.5);
                    s.setup(com.alkimor.regnum.kingdom.SoldierType.KNIGHT, fakeKing, null, 2);
                    s.command(com.alkimor.regnum.kingdom.Order.HOLD, s.position(), 0f, com.alkimor.regnum.kingdom.Formation.LINE, i, 3, null);
                    level.addFreshEntity(s);
                    soldiers.add(s);
                    m.setTarget(s);
                }
            }
            dx += 40;
        }
        // 3. Все новые рода войск: отряд из 11 типов против разбойников (проверка, что кони/стрельба/приёмы не падают)
        {
            for (int fcx = (spawn.getX() + 56) >> 4; fcx <= (spawn.getX() + 96) >> 4; fcx++)
                for (int fcz = (spawn.getZ() + 32) >> 4; fcz <= (spawn.getZ() + 70) >> 4; fcz++) level.setChunkForced(fcx, fcz, true); // чтобы бойцы тикали и в свежем мире
            var types = com.alkimor.regnum.kingdom.SoldierType.values();
            for (int i = 0; i < types.length; i++) {
                if (types[i] == com.alkimor.regnum.kingdom.SoldierType.MILITIA) continue;
                BlockPos p = ground(level, spawn.getX() + 60 + i * 2, spawn.getZ() + 40);
                var s = com.alkimor.regnum.kingdom.KingdomModule.SOLDIER.get().create(level);
                s.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                s.setup(types[i], fakeKing, null, 3);
                s.command(com.alkimor.regnum.kingdom.Order.HOLD, s.position(), 0f, com.alkimor.regnum.kingdom.Formation.LINE, i, types.length, null);
                level.addFreshEntity(s);
                roster.add(s);
            }
            for (int i = 0; i < 6; i++) {
                BlockPos q = ground(level, spawn.getX() + 60 + i * 2, spawn.getZ() + 62);
                var b = com.alkimor.regnum.kingdom.KingdomModule.BANDIT.get().create(level);
                b.moveTo(q.getX() + 0.5, q.getY(), q.getZ() + 0.5);
                b.setup(i % 2);
                b.setPersistenceRequired();
                level.addFreshEntity(b);
                rosterEnemies.add(b);
            }
        }
        scenarioTicks = 0;
    }

    private static long perfLast, perfSum, perfMax;
    private static int perfN, perfCount = 240;
    private static volatile boolean perfStop;
    private static final java.util.Map<String, Integer> perfIter = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, Integer> perfTop = new java.util.concurrent.ConcurrentHashMap<>(), perfRegnum = new java.util.concurrent.ConcurrentHashMap<>();
    private static int failures = 0;
    private static final java.util.List<Entity> roster = new java.util.ArrayList<>();
    private static final java.util.List<Entity> rosterEnemies = new java.util.ArrayList<>();

    /** Бой в лоб: щитник держит удар спереди и теряет больше с тыла; копейщик бьёт конницу сильнее. */
    private static void roleDamageTests(ServerLevel lvl) {
        BlockPos p = ground(lvl, 300, 300);
        java.util.UUID k = java.util.UUID.randomUUID();
        var K = com.alkimor.regnum.kingdom.KingdomModule.SOLDIER.get();
        var shield = K.create(lvl);
        shield.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0f, 0f);
        shield.setup(com.alkimor.regnum.kingdom.SoldierType.SHIELDMAN, k, null, 1);
        shield.setNoAi(true);
        shield.yBodyRot = 0f;
        lvl.addFreshEntity(shield);
        var foe = com.alkimor.regnum.kingdom.KingdomModule.BANDIT.get().create(lvl);
        foe.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 2.5);
        foe.setNoAi(true);
        lvl.addFreshEntity(foe);
        float h0 = shield.getHealth();
        shield.invulnerableTime = 0;
        shield.hurt(lvl.damageSources().mobAttack(foe), 8f);
        float front = h0 - shield.getHealth();
        shield.setHealth(shield.getMaxHealth());
        foe.moveTo(p.getX() + 0.5, p.getY(), p.getZ() - 1.5);
        shield.invulnerableTime = 0;
        h0 = shield.getHealth();
        shield.hurt(lvl.damageSources().mobAttack(foe), 8f);
        float back = h0 - shield.getHealth();
        log("role: shieldman front dmg " + front + " back dmg " + back + (front < back * 0.7f ? " OK" : " FAIL"));
        if (!(front < back * 0.7f)) failures++;
        // копейщик против конного
        var spear = K.create(lvl);
        spear.moveTo(p.getX() + 5.5, p.getY(), p.getZ() + 0.5);
        spear.setup(com.alkimor.regnum.kingdom.SoldierType.SPEARMAN, k, null, 1);
        spear.setNoAi(true);
        lvl.addFreshEntity(spear);
        net.minecraft.world.entity.animal.horse.Horse hs = net.minecraft.world.entity.EntityType.HORSE.create(lvl);
        hs.moveTo(p.getX() + 5.5, p.getY(), p.getZ() + 2.0);
        hs.setNoAi(true);
        lvl.addFreshEntity(hs);
        var sword = K.create(lvl);
        sword.moveTo(p.getX() + 9.5, p.getY(), p.getZ() + 0.5);
        sword.setup(com.alkimor.regnum.kingdom.SoldierType.SWORDSMAN, k, null, 1);
        sword.setNoAi(true);
        lvl.addFreshEntity(sword);
        float hh = hs.getHealth();
        hs.invulnerableTime = 0;
        hs.hurt(lvl.damageSources().mobAttack(sword), 4f);
        float swordLoss = hh - hs.getHealth();
        hs.setHealth(hs.getMaxHealth());
        hh = hs.getHealth();
        hs.invulnerableTime = 0;
        hs.hurt(lvl.damageSources().mobAttack(spear), 4f);
        float spearLoss = hh - hs.getHealth();
        log("role: horse hit by sword " + swordLoss + " by spear " + spearLoss + (spearLoss > swordLoss * 1.8f ? " OK" : " FAIL"));
        if (!(spearLoss > swordLoss * 1.8f)) failures++;
        shield.discard(); foe.discard(); spear.discard(); hs.discard(); sword.discard();
        // военачальники
        var cm = com.alkimor.regnum.kingdom.Commissions.get(lvl.getServer());
        var cmdr = net.neoforged.neoforge.common.util.FakePlayerFactory.get(lvl, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "RegnumCommander"));
        var c = cm.appoint(k, cmdr.getUUID(), com.alkimor.regnum.kingdom.Commissions.Branch.CAVALRY);
        int cap0 = c.cap();
        cm.addRep(cmdr.getUUID(), 65);
        var vs = K.create(lvl);
        vs.moveTo(p.getX() + 12.5, p.getY(), p.getZ() + 0.5);
        vs.setup(com.alkimor.regnum.kingdom.SoldierType.LIGHT_CAV, k, null, 1);
        vs.setNoAi(true);
        lvl.addFreshEntity(vs);
        boolean before = vs.isOwnedBy(cmdr);
        vs.setCommander(cmdr.getUUID());
        boolean ok = !before && vs.isOwnedBy(cmdr) && c.tier() == 2 && c.cap() == cap0 + 8
                && com.alkimor.regnum.kingdom.Commissions.Branch.CAVALRY.accepts(vs.getSoldierType())
                && !com.alkimor.regnum.kingdom.Commissions.Branch.CAVALRY.accepts(com.alkimor.regnum.kingdom.SoldierType.SPEARMAN);
        log("commander: tier=" + c.tier() + " rank=" + c.rank() + " cap " + cap0 + "->" + c.cap() + (ok ? " OK" : " FAIL"));
        if (!ok) failures++;
        vs.discard();
        cm.dismiss(cmdr.getUUID());
    }

    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (scenarioTicks < 0) return;
        scenarioTicks++;
        if (scenarioTicks == 80) {
            try { roleDamageTests(event.getServer().overworld()); } catch (Throwable t) { log("FAIL role tests: " + t); failures++; }
        }
        if (scenarioTicks == 100) {
            ServerLevel lvl = event.getServer().overworld();
            var fake = net.neoforged.neoforge.common.util.FakePlayerFactory.get(lvl, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "RegnumTester"));
            fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
            for (Entity b : bosses) {
                if (!(b instanceof Mob m) || !m.isAlive()) continue;
                float before = m.getHealth();
                for (int i = 0; i < 3; i++) {
                    m.invulnerableTime = 0;
                    fake.moveTo(m.getX() + 1.5, m.getY(), m.getZ());
                    m.hurt(fake.damageSources().playerAttack(fake), 8f);
                }
                log("player-hit test " + EntityType.getKey(m.getType()) + ": hp " + Math.round(before) + " -> " + Math.round(m.getHealth())
                        + (m.getHealth() < before - 1f ? " OK" : " NO DAMAGE"));
            }
        }
        if (scenarioTicks == 101) {
            try {
                ServerLevel lvl = event.getServer().overworld();
                var tb = com.alkimor.regnum.dungeon.RegionsModule.SCARAB_QUEEN.get().create(lvl);
                tb.moveTo(bosses.get(0).getX(), bosses.get(0).getY(), bosses.get(0).getZ() + 90);
                float base = tb.getMaxHealth();
                com.alkimor.regnum.dungeon.boss.BossTactics.empower(tb);
                float wm = com.alkimor.regnum.dungeon.boss.BossTactics.multiplier(lvl, tb);
                float nm = com.alkimor.regnum.dungeon.boss.BossTactics.multiplier(lvl, (Mob) bosses.get(0));
                boolean tok = tb.getMaxHealth() > base * 1.5f && wm < 0.15f && nm >= 0.64f && nm <= 1.26f;
                log("bosstactics: " + (tok ? "OK" : "FAIL") + " warHp=" + Math.round(tb.getMaxHealth()) + " warMult=" + wm + " normalMult=" + nm);
                if (!tok) failures++;
            } catch (Throwable t) { log("FAIL bosstactics: " + t); failures++; }
        }
        if (scenarioTicks == 300) {
            try {
                ServerLevel lvl = event.getServer().overworld();
                var kk = java.util.UUID.randomUUID();
                BlockPos base = bosses.get(0) == null ? lvl.getSharedSpawnPos() : bosses.get(0).blockPosition().offset(0, 0, -200);
                int perfSide = 120;
                try { var pf = java.nio.file.Path.of("_perf_n.txt"); if (!java.nio.file.Files.exists(pf)) pf = java.nio.file.Path.of("..", "_perf_n.txt"); if (java.nio.file.Files.exists(pf)) perfSide = Math.max(10, Math.min(300, Integer.parseInt(java.nio.file.Files.readString(pf).trim()))); } catch (Exception ignored) { }
                perfCount = perfSide * 2;
                for (int i = 0; i < perfSide; i++) {
                    BlockPos p = ground(lvl, base.getX() + (i % 20) * 2, base.getZ() + (i / 20) * 2);
                    var s = com.alkimor.regnum.kingdom.KingdomModule.SOLDIER.get().create(lvl);
                    s.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                    s.setup(i % 3 == 0 ? com.alkimor.regnum.kingdom.SoldierType.ARCHER : i % 3 == 1 ? com.alkimor.regnum.kingdom.SoldierType.SPEARMAN : com.alkimor.regnum.kingdom.SoldierType.SHIELDMAN, kk, null, 1);
                    s.command(com.alkimor.regnum.kingdom.Order.HOLD, s.position(), 0f, com.alkimor.regnum.kingdom.Formation.LINE, i % 20, 20, null);
                    lvl.addFreshEntity(s);
                    BlockPos q = ground(lvl, base.getX() + (i % 20) * 2, base.getZ() + 30 + (i / 20) * 2);
                    var b = com.alkimor.regnum.kingdom.KingdomModule.BANDIT.get().create(lvl);
                    b.moveTo(q.getX() + 0.5, q.getY(), q.getZ() + 0.5);
                    b.setup(i % 2);
                    b.setPersistenceRequired();
                    lvl.addFreshEntity(b);
                }
                perfLast = System.nanoTime();
                perfSum = 0;
                perfMax = 0;
                perfN = 0;
                {
                    final Thread srv = Thread.currentThread();
                    perfStop = false;
                    perfTop.clear();
                    perfRegnum.clear();
                    Thread sampler = new Thread(() -> {
                        while (!perfStop) {
                            try { Thread.sleep(5); } catch (InterruptedException ie) { return; }
                            StackTraceElement[] st = srv.getStackTrace();
                            if (st.length == 0) continue;
                            perfTop.merge(st[0].getClassName() + "." + st[0].getMethodName(), 1, Integer::sum);
                            if (st[0].getClassName().contains("Iterators")) {
                                StringBuilder sb = new StringBuilder();
                                int shown = 0;
                                for (int i = 1; i < st.length && shown < 9; i++) {
                                    String cn = st[i].getClassName();
                                    if (cn.startsWith("com.google") || cn.startsWith("java.util")) continue;
                                    sb.append(cn.substring(cn.lastIndexOf('.') + 1)).append('.').append(st[i].getMethodName()).append(':').append(st[i].getLineNumber()).append(" < ");
                                    shown++;
                                }
                                perfIter.merge(sb.toString(), 1, Integer::sum);
                            }
                            for (StackTraceElement f : st) {
                                if (f.getClassName().startsWith("com.alkimor")) { perfRegnum.merge(f.getClassName().substring(f.getClassName().lastIndexOf('.') + 1) + "." + f.getMethodName() + ":" + f.getLineNumber(), 1, Integer::sum); break; }
                            }
                        }
                    }, "regnum-perf-sampler");
                    sampler.setDaemon(true);
                    sampler.start();
                }
            } catch (Throwable t) { log("FAIL perf spawn: " + t); failures++; }
        }
        if (scenarioTicks > 300 && scenarioTicks <= 800 && perfLast != 0) {
            long now = System.nanoTime();
            long d = now - perfLast;
            perfLast = now;
            perfSum += d;
            perfMax = Math.max(perfMax, d);
            perfN++;
            if (scenarioTicks == 800) {
                double avg = perfSum / 1e6 / perfN;
                perfStop = true;
                log("perf-top (leaf): " + perfTop.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(14).map(e -> e.getKey() + "=" + e.getValue()).collect(java.util.stream.Collectors.joining("; ")));
                log("perf-iter: " + perfIter.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(4).map(e -> e.getValue() + " " + e.getKey()).collect(java.util.stream.Collectors.joining(" || ")));
                log("perf-top (regnum frame): " + perfRegnum.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(18).map(e -> e.getKey() + "=" + e.getValue()).collect(java.util.stream.Collectors.joining("; ")));
                {
                    int ents = 0, sold = 0, ban = 0;
                    ServerLevel pl = event.getServer().overworld();
                    for (Entity pe : pl.getAllEntities()) { ents++; if (pe instanceof com.alkimor.regnum.kingdom.SoldierEntity) sold++; else if (pe instanceof com.alkimor.regnum.kingdom.BanditEntity) ban++; }
                    java.util.Map<String, Integer> byTag = new java.util.TreeMap<>(), byArea = new java.util.HashMap<>();
                    for (Entity pe : pl.getAllEntities()) {
                        if (!(pe instanceof com.alkimor.regnum.kingdom.SoldierEntity ps)) continue;
                        byTag.merge(pe.getTags().toString() + " " + ps.getSoldierType() + (ps.getOwnerPlayer() == null ? " noOwnerOnline" : ""), 1, Integer::sum);
                        byArea.merge(((int) pe.getX() >> 6) * 64 + "," + (((int) pe.getZ()) >> 6) * 64, 1, Integer::sum);
                    }
                    log("perf-soldier-tags: " + byTag.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(8).map(e -> e.getValue() + " " + e.getKey()).collect(java.util.stream.Collectors.joining(" | ")));
                    log("perf-soldier-areas: " + byArea.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(6).map(e -> e.getValue() + "@" + e.getKey()).collect(java.util.stream.Collectors.joining(" | ")));
                    log("perf-entities: total=" + ents + " soldiers=" + sold + " bandits=" + ban);
                }
                log("perf: " + perfCount + " бойцов в бою, средний тик " + String.format("%.1f", avg) + " мс, худший " + String.format("%.1f", perfMax / 1e6) + " мс" + (avg > 60 ? " WARN: медленно" : " OK"));
            }
        }
        if (scenarioTicks == 200 || scenarioTicks == 600 || scenarioTicks == 900) {
            long alive = bandits.stream().filter(Entity::isAlive).count();
            log("t=" + scenarioTicks + ": bandits alive " + alive + "/" + startBandits + ", soldiers alive "
                    + soldiers.stream().filter(Entity::isAlive).count() + "/" + soldiers.size());
            for (Entity e : soldiers) {
                if (e instanceof com.alkimor.regnum.kingdom.SoldierEntity s && s.isAlive() && scenarioTicks == 200) {
                    log("   soldier " + s.getSoldierType() + " hp=" + Math.round(s.getHealth()) + " target=" + (s.getTarget() == null ? "-" : EntityType.getKey(s.getTarget().getType())));
                }
            }
            for (Entity b : bosses) {
                if (b instanceof Mob m) {
                    double nearest = soldiers.stream().filter(Entity::isAlive).mapToDouble(e -> e.distanceTo(m)).min().orElse(-1);
                    log("t=" + scenarioTicks + ": boss " + EntityType.getKey(m.getType()) + " hp=" + Math.round(m.getHealth()) + " alive=" + m.isAlive()
                            + " pos=" + m.blockPosition().toShortString() + " nearestSoldier=" + Math.round(nearest)
                            + " target=" + (m.getTarget() == null ? "-" : EntityType.getKey(m.getTarget().getType()))
                            + " inWater=" + m.isInWater() + " invul=" + m.isInvulnerable() + " hurtTime=" + m.hurtTime);
                }
            }
        }
        if (scenarioTicks == 150) {
            int rd = 0, mt = 0;
            for (Entity e : roster) if (e instanceof com.alkimor.regnum.kingdom.SoldierEntity s && s.isAlive() && s.getSoldierType().mounted()) { mt++; if (s.isPassenger()) rd++; }
            log("roster: mounted at t=150 " + rd + "/" + mt);
            if (mt > 0 && rd == 0) { log("FAIL: ни один конник не сел в седло"); failures++; }
        }
        if (scenarioTicks == 600) {
            int riding = 0, mountedTypes = 0;
            for (Entity e : roster) {
                if (e instanceof com.alkimor.regnum.kingdom.SoldierEntity s && s.isAlive()) {
                    if (s.getSoldierType().mounted()) { mountedTypes++; if (s.isPassenger()) riding++; }
                    log("   roster " + s.getSoldierType() + " hp=" + Math.round(s.getHealth()) + " riding=" + s.isPassenger() + " morale=" + Math.round(s.morale()) + " routing=" + s.routing());
                }
            }
            {
                int horses = 0, horsesAlive = 0;
                for (Entity he : event.getServer().overworld().getAllEntities()) if (he instanceof net.minecraft.world.entity.animal.horse.Horse hh && hh.getTags().contains(com.alkimor.regnum.kingdom.SoldierEntity.MOUNT_TAG)) { horses++; if (hh.isAlive()) horsesAlive++; }
                log("roster-diag: mount horses=" + horses + " alive=" + horsesAlive + " rosterCavTick=" + roster.stream().filter(r -> r instanceof com.alkimor.regnum.kingdom.SoldierEntity rs && rs.getSoldierType().mounted()).map(r -> r.tickCount + "@" + r.blockPosition().toShortString()).toList());
            }
            long enemiesLeft = rosterEnemies.stream().filter(Entity::isAlive).count();
            log("roster: mounted riding " + riding + "/" + mountedTypes + ", enemies left " + enemiesLeft + "/" + rosterEnemies.size());
            // кони могут погибнуть в бою к 600-му тику — посадку проверяем раньше (tick 150)
        }
        if (scenarioTicks == 900) {
            long alive = bandits.stream().filter(Entity::isAlive).count();
            if (alive < startBandits) log("combat scenario OK (soldiers fought bandits)");
            else log("WARN: combat scenario — no bandit died in 30s");
            log("dodge: attempts=" + com.alkimor.regnum.kingdom.ai.SoldierDodgeGoal.attempts + " escaped=" + com.alkimor.regnum.kingdom.ai.SoldierDodgeGoal.escaped + " noRoute=" + com.alkimor.regnum.kingdom.ai.SoldierDodgeGoal.noRoute);
            log(failures == 0 ? "RESULT: OK" : "RESULT: FAIL (" + failures + " role/army checks)");
            scenarioTicks = -1;
            event.getServer().halt(false);
        }
    }

    private static int countLeft(ServerLevel level, BlockPos wp) {
        int left = 0;
        for (int dy = 0; dy < 4; dy++)
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++)
                    if (!level.getBlockState(wp.offset(dx, dy, dz)).isAir()) left++;
        return left;
    }

    private static void lootCheck(MinecraftServer server, ResourceKey<LootTable> key, List<String> fails) {
        if (server.reloadableRegistries().getLootTable(key) == LootTable.EMPTY) fails.add("loot " + key.location());
    }

    private static void locateAndBuild(ServerLevel level, BlockPos from, TagKey<Structure> tag, String name, List<String> fails) {
        try {
            BlockPos at = level.findNearestMapStructure(tag, from, 150, false);
            if (at == null) {
                fails.add("structure not found: " + name);
                return;
            }
            log("structure " + name + " at " + at.getX() + " " + at.getZ());
            for (int dx = -3; dx <= 3; dx++)
                for (int dz = -3; dz <= 3; dz++)
                    level.getChunk((at.getX() >> 4) + dx, (at.getZ() >> 4) + dz, ChunkStatus.FULL, true);
            log("structure " + name + " generated OK");
        } catch (Throwable t) {
            fails.add("structure " + name + ": " + t);
            Regnum.LOGGER.error("[SELFTEST] structure failure", t);
        }
    }

    private static void log(String s) {
        Regnum.LOGGER.info("[REGNUM-SELFTEST] {}", s);
    }

    @SuppressWarnings("unused")
    private static ResourceLocation rl(String s) {
        return Regnum.id(s);
    }
}
