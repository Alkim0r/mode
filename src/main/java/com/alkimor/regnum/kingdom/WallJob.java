package com.alkimor.regnum.kingdom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Заказ на стену: клетки в плане (x,z), проёмы ворот и ход работы. Хранится в {@link City}. */
public class WallJob {
    public int style;
    /** Клетки стены в порядке строительства: BlockPos.asLong(x, 0, z). */
    public long[] cells = new long[0];
    /** Индексы клеток, оставленных проёмом (ворота). */
    public boolean[] gate = new boolean[0];
    public int cell = 0;
    /** Ярус внутри клетки: от -глубина фундамента до высоты стены. */
    public int layer = Integer.MIN_VALUE;
    public int base = Integer.MIN_VALUE;
    public int coinCounter = 0;
    public boolean paused = false;
    public UUID builder;
    public long lastMsg = 0;

    // ---- режим готовых кусков (стиль CASTLE)
    public String[] oid = null;
    public int[] ox = new int[0], oz = new int[0], oq = new int[0], ogy = new int[0];
    public int op = 0, opLayer = 0;
    public boolean repair = false;

    public boolean pieces() {
        return oid != null;
    }

    public boolean done() {
        return pieces() ? op >= oid.length : cell >= cells.length;
    }

    public int total() {
        return pieces() ? oid.length : cells.length;
    }

    public int cur() {
        return pieces() ? op : cell;
    }

    public void resetProgress() {
        cell = 0;
        op = 0;
        opLayer = 0;
        if (ogy != null) java.util.Arrays.fill(ogy, Integer.MIN_VALUE);
    }

    public static WallJob planPieces(WallKit kit, int style, List<WallKit.Op> ops) {
        WallJob j = new WallJob();
        j.style = style;
        int n = ops.size();
        j.oid = new String[n];
        j.ox = new int[n];
        j.oz = new int[n];
        j.oq = new int[n];
        j.ogy = new int[n];
        for (int i = 0; i < n; i++) {
            WallKit.Op o = ops.get(i);
            j.oid[i] = o.id();
            j.ox[i] = o.x();
            j.oz[i] = o.z();
            j.oq[i] = o.quarter();
            j.ogy[i] = Integer.MIN_VALUE;
        }
        return j;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("style", style);
        t.putLongArray("cells", cells);
        int[] g = new int[gate.length];
        int n = 0;
        for (int i = 0; i < gate.length; i++) if (gate[i]) g[n++] = i;
        t.putIntArray("gates", java.util.Arrays.copyOf(g, n));
        t.putInt("cell", cell);
        t.putInt("coin", coinCounter);
        if (pieces()) {
            net.minecraft.nbt.ListTag ids = new net.minecraft.nbt.ListTag();
            for (String s : oid) ids.add(net.minecraft.nbt.StringTag.valueOf(s));
            t.put("oid", ids);
            t.putIntArray("ox", ox);
            t.putIntArray("oz", oz);
            t.putIntArray("oq", oq);
            t.putIntArray("ogy", ogy);
            t.putInt("op", op);
            t.putInt("opLayer", opLayer);
            t.putBoolean("repair", repair);
        }
        if (builder != null) t.putUUID("builder", builder);
        return t;
    }

    public static WallJob load(CompoundTag t) {
        WallJob j = new WallJob();
        j.style = t.getInt("style");
        j.cells = t.getLongArray("cells");
        j.gate = new boolean[j.cells.length];
        for (int i : t.getIntArray("gates")) if (i >= 0 && i < j.gate.length) j.gate[i] = true;
        j.cell = t.getInt("cell");
        j.coinCounter = t.getInt("coin");
        if (t.contains("oid")) {
            net.minecraft.nbt.ListTag ids = t.getList("oid", 8);
            j.oid = new String[ids.size()];
            for (int i = 0; i < ids.size(); i++) j.oid[i] = ids.getString(i);
            j.ox = t.getIntArray("ox");
            j.oz = t.getIntArray("oz");
            j.oq = t.getIntArray("oq");
            j.ogy = t.getIntArray("ogy");
            j.op = t.getInt("op");
            j.opLayer = t.getInt("opLayer");
            j.repair = t.getBoolean("repair");
        }
        if (t.hasUUID("builder")) j.builder = t.getUUID("builder");
        return j;
    }

    // ------------------------------------------------------------------ планирование

    /** Строит клетки по ломаной из точек; клетки у ворот помечаются проёмом. */
    public static WallJob plan(List<BlockPos> points, List<Boolean> gates, WallStyle style) {
        WallJob j = new WallJob();
        j.style = style.ordinal();
        List<long[]> centers = new ArrayList<>();
        List<Boolean> centerGate = new ArrayList<>();
        for (int i = 0; i + 1 < points.size(); i++) {
            BlockPos a = points.get(i), b = points.get(i + 1);
            int n = Math.max(Math.abs(b.getX() - a.getX()), Math.abs(b.getZ() - a.getZ()));
            for (int k = (i == 0 ? 0 : 1); k <= n; k++) {
                int x = a.getX() + Math.round((float) (b.getX() - a.getX()) * k / Math.max(1, n));
                int z = a.getZ() + Math.round((float) (b.getZ() - a.getZ()) * k / Math.max(1, n));
                centers.add(new long[]{x, z});
                centerGate.add(false);
            }
        }
        // ворота: ближайший центр к точке-воротам
        for (int i = 0; i < points.size(); i++) {
            if (!gates.get(i)) continue;
            int best = -1;
            long bd = Long.MAX_VALUE;
            for (int k = 0; k < centers.size(); k++) {
                long dx = centers.get(k)[0] - points.get(i).getX(), dz = centers.get(k)[1] - points.get(i).getZ();
                long d = dx * dx + dz * dz;
                if (d < bd) {
                    bd = d;
                    best = k;
                }
            }
            if (best >= 0) centerGate.set(best, true);
        }
        int r = style.radius;
        List<Long> cells = new ArrayList<>();
        List<Boolean> g = new ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (int k = 0; k < centers.size(); k++) {
            int cx = (int) centers.get(k)[0], cz = (int) centers.get(k)[1];
            // ворота — проём шире стены: 3 клетки по линии
            boolean gate = false;
            for (int d = -1; d <= 1 && !gate; d++) {
                int kk = k + d;
                if (kk >= 0 && kk < centers.size() && centerGate.get(kk)) gate = true;
            }
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    long key = BlockPos.asLong(cx + dx, 0, cz + dz);
                    if (!seen.add(key)) continue;
                    cells.add(key);
                    g.add(gate);
                }
            }
        }
        j.cells = new long[cells.size()];
        j.gate = new boolean[cells.size()];
        for (int i = 0; i < cells.size(); i++) {
            j.cells[i] = cells.get(i);
            j.gate[i] = g.get(i);
        }
        return j;
    }
}
