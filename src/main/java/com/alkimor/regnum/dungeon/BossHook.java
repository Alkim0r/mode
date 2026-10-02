package com.alkimor.regnum.dungeon;

import com.alkimor.regnum.Regnum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Привязка боссов к готовым данжам (импорт из Dungeons and Taverns, regnum:dt/*): когда структура создана,
 * в ней вырезается небольшая зала с алтарём призыва соответствующего босса и сундуком с наградой.
 */
public final class BossHook {
    private BossHook() {}

    private record Rule(java.util.function.Supplier<Block> altar, ResourceKey<LootTable> loot, int arena) {}

    private static final Map<String, Rule> RULES = new HashMap<>();

    private static void rules() {
        if (!RULES.isEmpty()) return;
        Rule crypt = new Rule(() -> DungeonModule.CRYPT_ALTAR.get(), DungeonModule.CRYPT_TREASURE_LOOT, 8);
        Rule mire = new Rule(() -> RegionsModule.MIRE_ALTAR.get(), RegionsModule.SHRINE_LOOT, 7);
        Rule forge = new Rule(() -> RegionsModule.FORGE_ALTAR.get(), RegionsModule.FORTRESS_LOOT, 10);
        Rule sun = new Rule(() -> RegionsModule.SUN_ALTAR.get(), RegionsModule.TOMB_LOOT, 10);
        RULES.put("dt/undead_crypt", crypt);
        RULES.put("dt/creeping_crypt", crypt);
        RULES.put("dt/toxic_lair", mire);
        RULES.put("dt/witch_villa", mire);
        RULES.put("dt/lone_citadel", forge);
        RULES.put("dt/bunker", forge);
        RULES.put("dt/stray_fort", forge);
        RULES.put("dt/desert_ruins", sun);
    }

    /** Какие стартовые чанки структур уже обработаны (чтобы зала не ставилась при каждой загрузке чанка). */
    public static final class Done extends net.minecraft.world.level.saveddata.SavedData {
        final java.util.Set<Long> set = new java.util.HashSet<>();

        static Done load(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider p) {
            Done d = new Done();
            for (long l : tag.getLongArray("done")) d.set.add(l);
            return d;
        }

        @Override
        public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider p) {
            tag.putLongArray("done", set.stream().mapToLong(Long::longValue).toArray());
            return tag;
        }

        static Done get(ServerLevel l) {
            return l.getDataStorage().computeIfAbsent(new net.minecraft.world.level.saveddata.SavedData.Factory<>(Done::new, Done::load, null), "regnum_bosshook");
        }
    }

    private static final class Pending {
        final ServerLevel level;
        final BoundingBox box;
        final Rule rule;
        int tries = 0;
        long startChunk;

        Pending(ServerLevel l, BoundingBox b, Rule r) {
            level = l;
            box = b;
            rule = r;
        }
    }

    /** Последняя поставленная зала (для проверок/съёмки). */
    public static BlockPos lastArena;

    private static final List<Pending> QUEUE = new ArrayList<>();

    @SubscribeEvent
    public static void onChunk(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel sl) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        if (chunk.getAllStarts().isEmpty()) return;

        rules();
        ChunkPos cp = chunk.getPos();
        for (Map.Entry<net.minecraft.world.level.levelgen.structure.Structure, StructureStart> e : chunk.getAllStarts().entrySet()) {
            StructureStart st = e.getValue();
            if (!st.isValid() || !st.getChunkPos().equals(cp)) continue;
            ResourceLocation id = sl.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(e.getKey());
            if (id == null || !id.getNamespace().equals(Regnum.MODID)) continue;
            Rule r = RULES.get(id.getPath());
            if (r == null || Done.get(sl).set.contains(cp.toLong())) continue;
            Regnum.LOGGER.info("[BossHook] в очереди {} {}", id, st.getBoundingBox());
            Pending pd = new Pending(sl, st.getBoundingBox(), r);
            pd.startChunk = cp.toLong();
            if (QUEUE.stream().noneMatch(q -> q.startChunk == pd.startChunk && q.level == sl)) QUEUE.add(pd);
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (QUEUE.isEmpty() || event.getServer().getTickCount() % 40 != 0) return;
        Iterator<Pending> it = QUEUE.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (++p.tries > 30) {
                Regnum.LOGGER.warn("[BossHook] не удалось поставить залу {}", p.box);
                it.remove();
                continue;
            }
            if (place(p)) {
                Done d = Done.get(p.level);
                d.set.add(p.startChunk);
                d.setDirty();
                it.remove();
            }
        }
    }

    /** Центр залы: под землёй — в середине структуры, на поверхности — рядом с ней (южнее края), чтобы не вырезать саму постройку. */
    private static int[] site(ServerLevel l, BoundingBox b, int r) {
        int cx = (b.minX() + b.maxX()) / 2, cz = (b.minZ() + b.maxZ()) / 2;
        int surface = l.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz);
        boolean underground = b.maxY() < surface - 6;
        if (underground) {
            int[] best = bestHollow(l, b, r);
            if (best != null) return best;
            return new int[]{cx, cz, b.minY() + 2, 1};
        }
        int z2 = b.maxZ() + r + 5;
        int g = l.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, z2);
        return new int[]{cx, z2, g, 0};
    }

    private static final Map<BoundingBox, int[]> HOLLOWS = new HashMap<>();

    /**
     * Под землёй ищем в структуре самую пустую полость под арену (меньше всего вырезать и ломать постройку).
     * Работает только когда все чанки структуры уже загружены; иначе возвращает null (запасной вариант — центр).
     */
    private static int[] bestHollow(ServerLevel l, BoundingBox b, int r) {
        int[] cached = HOLLOWS.get(b);
        if (cached != null) return cached;
        for (int x = b.minX() >> 4; x <= b.maxX() >> 4; x++)
            for (int z = b.minZ() >> 4; z <= b.maxZ() >> 4; z++)
                if (!l.hasChunk(x, z)) return null;
        int bestScore = -1;
        int[] best = null;
        int cxm = (b.minX() + b.maxX()) / 2, czm = (b.minZ() + b.maxZ()) / 2;
        for (int x = b.minX() + r; x <= b.maxX() - r; x += 3)
            for (int z = b.minZ() + r; z <= b.maxZ() - r; z += 3)
                for (int y = b.minY() + 1; y <= b.maxY() - 8; y += 2) {
                    int air = 0, total = 0;
                    for (int dx = -r; dx <= r; dx += 2)
                        for (int dz = -r; dz <= r; dz += 2)
                            for (int dy = 0; dy < 6; dy += 2) {
                                total++;
                                if (l.getBlockState(new BlockPos(x + dx, y + dy, z + dz)).isAir()) air++;
                            }
                    int score = air * 100 / Math.max(1, total) * 10 - (int) Math.hypot(x - cxm, z - czm);
                    if (score > bestScore) {
                        bestScore = score;
                        best = new int[]{x, z, y, 1};
                    }
                }
        if (best != null) HOLLOWS.put(b, best);
        return best;
    }

    private static boolean loaded(ServerLevel l, BoundingBox b, int r) {
        int[] st = site(l, b, r);
        int cx = st[0], cz = st[1];
        for (int x = (cx - r - 1) >> 4; x <= (cx + r + 1) >> 4; x++)
            for (int z = (cz - r - 1) >> 4; z <= (cz + r + 1) >> 4; z++)
                if (!l.hasChunk(x, z)) return false;
        return true;
    }

    /** Для проверок: принудительно загружает залы и ставит всё, что в очереди. Возвращает число поставленных. */
    public static int flush(ServerLevel l) {
        int n = 0;
        Iterator<Pending> it = QUEUE.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (p.level != l) continue;
            int[] st = site(l, p.box, p.rule.arena);
            int cx = st[0], cz = st[1], r = p.rule.arena + 1;
            for (int x = (cx - r) >> 4; x <= (cx + r) >> 4; x++)
                for (int z = (cz - r) >> 4; z <= (cz + r) >> 4; z++) l.getChunk(x, z);
            if (place(p)) {
                Done d = Done.get(l);
                d.set.add(p.startChunk);
                d.setDirty();
                it.remove();
                n++;
            }
        }
        return n;
    }

    private static boolean place(Pending p) {
        ServerLevel l = p.level;
        BoundingBox b = p.box;
        if (!loaded(l, b, p.rule.arena)) return false;
        int r = p.rule.arena;
        int[] st = site(l, b, r);
        int cx = st[0], cz = st[1], floor = st[2];
        boolean underground = st[3] == 1;
        if (!underground) {
            // подсыпка площадки до пола, чтобы зала не висела над обрывом
            for (int x = cx - r; x <= cx + r; x++)
                for (int z = cz - r; z <= cz + r; z++)
                    for (int y = floor - 6; y < floor - 1; y++) {
                        BlockPos q = new BlockPos(x, y, z);
                        if (l.getBlockState(q).canBeReplaced() || !l.getFluidState(q).isEmpty()) l.setBlock(q, Blocks.STONE_BRICKS.defaultBlockState(), 2 | 16);
                    }
        }
        // зала: вырезаем объём, кладём пол и стены-подпорки по краю
        for (int x = cx - r; x <= cx + r; x++)
            for (int z = cz - r; z <= cz + r; z++) {
                l.setBlock(new BlockPos(x, floor - 1, z), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2 | 16);
                for (int y = floor; y <= floor + 7; y++) {
                    boolean edge = Math.abs(x - cx) == r || Math.abs(z - cz) == r;
                    BlockPos q = new BlockPos(x, y, z);
                    boolean gap = !underground && z == cz - r && Math.abs(x - cx) <= 1;
                    if (edge && underground) l.setBlock(q, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2 | 16);
                    else if (edge && !gap && y <= floor + 3) l.setBlock(q, Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState(), 2 | 16);
                    else l.setBlock(q, Blocks.AIR.defaultBlockState(), 2 | 16);
                }
                if (underground) l.setBlock(new BlockPos(x, floor + 8, z), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2 | 16);
            }
        BlockPos altar = new BlockPos(cx, floor, cz + r - 2);
        l.setBlock(altar, p.rule.altar.get().defaultBlockState(), 3);
        BlockPos chest = new BlockPos(cx + r - 2, floor, cz + r - 2);
        l.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
        if (l.getBlockEntity(chest) instanceof ChestBlockEntity ce) ce.setLootTable(p.rule.loot, l.random.nextLong());
        l.setBlock(new BlockPos(cx - r + 2, floor, cz + r - 2), Blocks.LANTERN.defaultBlockState(), 3);
        // факелы по углам
        for (int sx : new int[]{-1, 1})
            for (int sz : new int[]{-1, 1}) l.setBlock(new BlockPos(cx + sx * (r - 1), floor, cz + sz * (r - 1)), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
        lastArena = new BlockPos(cx, floor, cz);
        Regnum.LOGGER.info("[BossHook] зала босса в {} {} {}", cx, floor, cz);
        return true;
    }
}
