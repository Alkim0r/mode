package com.alkimor.regnum.kingdom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Готовые постройки (из схем MineColonies, сконвертированных в ванильный structure NBT):
 * {@code data/regnum/structure/<культура>/<категория>/<имя>.nbt}. Слой 0 схемы — уровень земли.
 */
public final class Prefab {
    private Prefab() {}

    private static final Map<String, Integer> COUNT = new HashMap<>();

    public static StructureTemplate get(ServerLevel l, String id) {
        Optional<StructureTemplate> t = l.getServer().getStructureManager()
                .get(ResourceLocation.fromNamespaceAndPath("regnum", id));
        return t.orElse(null);
    }

    public static boolean exists(ServerLevel l, String id) {
        return get(l, id) != null;
    }

    /** Размер схемы с учётом поворота (x, высота, z). */
    public static BlockPos size(ServerLevel l, String id, Rotation rot) {
        StructureTemplate t = get(l, id);
        if (t == null) return BlockPos.ZERO;
        var s = t.getSize(rot);
        return new BlockPos(s.getX(), s.getY(), s.getZ());
    }

    /** Сколько «настоящих» блоков в схеме (для стоимости). */
    public static int blocks(ServerLevel l, String id) {
        Integer c = COUNT.get(id);
        if (c != null) return c;
        StructureTemplate t = get(l, id);
        int n = 0;
        if (t != null) {
            CompoundTag tag = t.save(new CompoundTag());
            ListTag list = tag.getList("blocks", 10);
            n = list.size();
        }
        COUNT.put(id, n);
        return n;
    }

    /** Высота земли под схемой: медиана высот по углам и центру. */
    public static int groundAt(ServerLevel l, int cx, int cz, int hx, int hz) {
        int[] h = new int[5];
        int[][] p = {{0, 0}, {-hx, -hz}, {hx, -hz}, {-hx, hz}, {hx, hz}};
        for (int i = 0; i < 5; i++) {
            int x = cx + p[i][0], z = cz + p[i][1];
            l.getChunk(x >> 4, z >> 4);
            h[i] = l.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
        }
        java.util.Arrays.sort(h);
        return h[2];
    }

    private static boolean soft(BlockState s) {
        return s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty();
    }

    /** Ставит схему целиком. */
    public static boolean place(ServerLevel l, String id, int cx, int groundY, int cz, Rotation rot, Block foundation, int layers) {
        return place(l, id, cx, groundY, cz, rot, foundation, 0, layers);
    }

    /**
     * Ставит слои [from, to) схемы: центр основания в (cx, cz), слой 0 на высоте groundY.
     * Для постепенной стройки вызывается подряд с растущими from/to; фундамент — при from == 0.
     */
    public static boolean place(ServerLevel l, String id, int cx, int groundY, int cz, Rotation rot, Block foundation, int from, int to) {
        StructureTemplate t = get(l, id);
        if (t == null) return false;
        var sz = t.getSize();
        BlockPos pivot = new BlockPos(sz.getX() / 2, 0, sz.getZ() / 2);
        BlockPos origin = new BlockPos(cx - pivot.getX(), groundY, cz - pivot.getZ());
        StructurePlaceSettings st = new StructurePlaceSettings().setRotation(rot).setRotationPivot(pivot).setIgnoreEntities(true);
        BoundingBox bb = t.getBoundingBox(st, origin);
        for (int x = bb.minX() >> 4; x <= bb.maxX() >> 4; x++)
            for (int z = bb.minZ() >> 4; z <= bb.maxZ() >> 4; z++) l.getChunk(x, z);
        int lo = groundY + Math.max(0, from);
        int top = Math.min(bb.maxY(), groundY + Math.min(512, Math.max(0, to)) - 1);
        if (top < lo) return true;
        // очистка объёма над землёй (деревья, холм, камни)
        for (int x = bb.minX(); x <= bb.maxX(); x++)
            for (int z = bb.minZ(); z <= bb.maxZ(); z++)
                for (int y = Math.max(lo, groundY + 1); y <= top; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!l.getBlockState(p).isAir()) l.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
                }
        st.setBoundingBox(new BoundingBox(bb.minX(), lo, bb.minZ(), bb.maxX(), top, bb.maxZ()));
        boolean ok = t.placeInWorld(l, origin, origin, st, l.random, 2 | 16);
        if (from <= 0) {
            Block f = foundation == null ? Blocks.STONE_BRICKS : foundation;
            for (int x = bb.minX(); x <= bb.maxX(); x++)
                for (int z = bb.minZ(); z <= bb.maxZ(); z++) {
                    if (l.getBlockState(new BlockPos(x, groundY, z)).isAir()) continue;
                    for (int y = groundY - 1; y >= groundY - 24; y--) {
                        BlockPos p = new BlockPos(x, y, z);
                        if (!soft(l.getBlockState(p))) break;
                        l.setBlock(p, f.defaultBlockState(), 2 | 16);
                    }
                }
        }
        return ok;
    }

    private static final Map<String, java.util.List<String>> LISTS = new HashMap<>();

    /** Все схемы культуры в категории: id вида "north/fundamentals/townhall5". */
    public static java.util.List<String> list(ServerLevel l, String culture, String cat) {
        String key = culture + "/" + cat;
        java.util.List<String> r = LISTS.get(key);
        if (r != null) return r;
        r = new java.util.ArrayList<>();
        String pre = "structure/" + key + "/";
        for (var e : l.getServer().getResourceManager().listResources("structure", rl -> rl.getNamespace().equals("regnum")
                && rl.getPath().startsWith(pre) && rl.getPath().endsWith(".nbt")).keySet()) {
            String p = e.getPath();
            r.add(p.substring("structure/".length(), p.length() - 4));
        }
        java.util.Collections.sort(r);
        LISTS.put(key, r);
        return r;
    }

    /**
     * Подбирает схему культуры по ключевому слову в имени; maxSide — предел по длине стороны основания.
     * Из подходящих берётся самый высокий уровень (число в конце имени).
     */
    public static String find(ServerLevel l, String culture, String cat, String keyword, int maxSide) {
        String best = null;
        int bestLvl = -1;
        for (String id : list(l, culture, cat)) {
            String name = id.substring(id.lastIndexOf('/') + 1);
            if (keyword != null && !name.contains(keyword)) continue;
            var sz = size(l, id, Rotation.NONE);
            if (Math.max(sz.getX(), sz.getZ()) > maxSide) continue;
            int lvl = 0;
            int i = name.length();
            while (i > 0 && Character.isDigit(name.charAt(i - 1))) i--;
            if (i < name.length()) lvl = Integer.parseInt(name.substring(i));
            if (lvl > bestLvl) {
                bestLvl = lvl;
                best = id;
            }
        }
        return best;
    }

    /** Высота схемы в слоях. */
    public static int height(ServerLevel l, String id) {
        StructureTemplate t = get(l, id);
        return t == null ? 0 : t.getSize().getY();
    }

    public static Rotation rotFor(int quarter) {
        return Rotation.values()[Math.floorMod(quarter, 4)];
    }
}
