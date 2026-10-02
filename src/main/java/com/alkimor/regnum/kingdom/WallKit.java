package com.alkimor.regnum.kingdom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.List;

/**
 * Набор готовых кусков стены для культуры: сегменты, башня, ворота (схемы из MineColonies).
 * Каждый кусок описан длиной вдоль стены и осью схемы, вдоль которой она тянется.
 */
public final class WallKit {
    public record Piece(String id, int len, char axis) {}

    /** Одна операция планировщика: поставить схему id с центром (x,z) и поворотом. */
    public record Op(String id, int x, int z, int quarter) {}

    public final Piece[] segs;   // по убыванию длины
    public final Piece tower;
    public final Piece gate;
    public final Block foundation;
    /** Доп. поворот (в четвертях) всех кусков: какой стороной «наружу» смотрит схема. */
    public final int flip;

    private WallKit(Piece[] segs, Piece tower, Piece gate, Block foundation, int flip) {
        this.segs = segs;
        this.tower = tower;
        this.gate = gate;
        this.foundation = foundation;
        this.flip = flip;
    }

    private static Piece p(String id, int len, char axis) {
        return new Piece(id, len, axis);
    }

    /** Порядок как в {@link Culture}: север, империя, запад, степь, султанат, кланы. */
    private static final WallKit[] KITS = {
            new WallKit(new Piece[]{p("north/walls/fancy_fancy_segment_large", 13, 'x'), p("north/walls/fancy_fancy_segment_small", 7, 'x')},
                    p("north/walls/fancy_fancy_square_tower", 8, 'x'), p("north/walls/fancy_fancy_gate", 11, 'x'), Blocks.STONE_BRICKS, 0),
            new WallKit(new Piece[]{p("empire/walls/tall_straight", 8, 'z')},
                    p("empire/military/walltower3", 7, 'x'), p("empire/military/gatehouse3", 18, 'x'), Blocks.STONE_BRICKS, 0),
            new WallKit(new Piece[]{p("west/walls/wall_wall3", 8, 'z')},
                    p("west/walls/corner_corner3", 16, 'x'), p("west/walls/gate_gatelarge", 16, 'z'), Blocks.STONE_BRICKS, 0),
            new WallKit(new Piece[]{p("steppe/walls/big_walls_long", 21, 'z'), p("steppe/walls/big_walls_short", 7, 'z')},
                    p("steppe/walls/big_walls_corner_tower", 9, 'z'), p("steppe/walls/gates_gatehouse1", 13, 'x'), Blocks.COBBLESTONE, 0),
            new WallKit(new Piece[]{p("sultanate/walls/basic_section_long", 16, 'x'), p("sultanate/walls/basic_section_medium", 8, 'x'), p("sultanate/walls/basic_section_short", 4, 'x')},
                    p("sultanate/walls/basic_tower", 8, 'x'), p("sultanate/walls/basic_gatehouse", 16, 'x'), Blocks.SANDSTONE, 0),
            new WallKit(new Piece[]{p("clans/walls/walls_long", 19, 'x'), p("clans/walls/walls_medium", 13, 'x'), p("clans/walls/walls_short", 5, 'x')},
                    p("clans/walls/corners_tower", 9, 'x'), p("clans/walls/gates_single1", 19, 'x'), Blocks.COBBLESTONE, 0),
    };

    public static WallKit of(int culture) {
        return KITS[Math.floorMod(culture, KITS.length)];
    }

    /** Поворот схемы так, чтобы её ось axis шла в направлении (dx,dz). */
    public int quarter(char axis, int dx, int dz) {
        int q;
        if (axis == 'x') q = dx > 0 ? 0 : dz > 0 ? 1 : dx < 0 ? 2 : 3;
        else q = dz > 0 ? 0 : dx < 0 ? 1 : dz < 0 ? 2 : 3;
        return Math.floorMod(q + flip, 4);
    }

    private int half(Piece pc) {
        return pc.len() / 2;
    }

    private Piece node(boolean gate) {
        return gate ? this.gate : tower;
    }

    /**
     * Планирует стену по точкам: углы и ворота — отдельные куски, между ними сегменты.
     * Диагонали ломаются на два прямых участка (с башней в изломе). closed — замкнуть контур.
     */
    public List<Op> plan(List<int[]> pts, List<Boolean> gates, boolean closed) {
        List<int[]> nodes = new ArrayList<>();
        List<Boolean> kinds = new ArrayList<>();
        int n = pts.size();
        for (int i = 0; i < n; i++) {
            int[] a = pts.get(i);
            if (i > 0) bend(nodes, kinds, nodes.get(nodes.size() - 1), a);
            nodes.add(a);
            kinds.add(gates.get(i));
        }
        if (closed && n > 2) {
            bend(nodes, kinds, nodes.get(nodes.size() - 1), pts.get(0));
            // замыкающий участок: первая точка уже есть, повторяем как конец
            nodes.add(pts.get(0));
            kinds.add(gates.get(0));
        }
        List<Op> ops = new ArrayList<>();
        int last = nodes.size() - 1;
        for (int k = 0; k <= last; k++) {
            int[] a = nodes.get(k);
            int[] d;
            if (k < last) d = dir(a, nodes.get(k + 1));
            else d = k > 0 ? dir(nodes.get(k - 1), a) : new int[]{1, 0};
            if (!(closed && k == last)) {
                Piece pc = node(kinds.get(k));
                ops.add(new Op(pc.id(), a[0], a[1], quarter(pc.axis(), d[0], d[1])));
            }
            if (k < last) {
                int[] b = nodes.get(k + 1);
                int len = Math.abs(b[0] - a[0]) + Math.abs(b[1] - a[1]);
                fill(ops, a[0], a[1], d[0], d[1], half(node(kinds.get(k))), len - half(node(kinds.get(k + 1))));
            }
        }
        return ops;
    }

    private static int[] dir(int[] a, int[] b) {
        return new int[]{Integer.signum(b[0] - a[0]), Integer.signum(b[1] - a[1])};
    }

    /** Диагональный переход a→b превращаем в «Г»: добавляем угловую башню. */
    private void bend(List<int[]> nodes, List<Boolean> kinds, int[] a, int[] b) {
        if (a[0] != b[0] && a[1] != b[1]) {
            nodes.add(new int[]{b[0], a[1]});
            kinds.add(false);
        }
    }

    private void fill(List<Op> ops, int x0, int z0, int dx, int dz, int from, int to) {
        if (dx == 0 && dz == 0) return;
        int pos = from;
        while (pos < to) {
            int remain = to - pos;
            Piece pick = null;
            for (Piece sg : segs) if (sg.len() <= remain) {
                pick = sg;
                break;
            }
            if (pick == null) {
                Piece sm = segs[segs.length - 1];
                if (remain >= 2) ops.add(at(sm, x0, z0, dx, dz, to - sm.len() + sm.len() / 2));
                return;
            }
            ops.add(at(pick, x0, z0, dx, dz, pos + pick.len() / 2));
            pos += pick.len();
        }
    }

    private Op at(Piece pc, int x0, int z0, int dx, int dz, int along) {
        return new Op(pc.id(), x0 + dx * along, z0 + dz * along, quarter(pc.axis(), dx, dz));
    }

    /** Простой образец для проверки: прямая стена на восток от точки. */
    public int showcase(ServerLevel l, int x0, int z0, int len, boolean gate) {
        List<Op> ops = plan(List.of(new int[]{x0, z0}, new int[]{x0 + len / 2, z0}, new int[]{x0 + len, z0}), List.of(false, gate, false), false);
        int placed = 0;
        for (Op o : ops) {
            BlockPosHelper.placeOp(l, o, foundation);
            placed++;
        }
        return placed;
    }

    /** Помощник постановки операции с подбором высоты земли. */
    public static final class BlockPosHelper {
        private BlockPosHelper() {}

        public static void placeOp(ServerLevel l, Op o, Block foundation) {
            var size = Prefab.size(l, o.id(), Rotation.NONE);
            int hx = Math.max(1, size.getX() / 2), hz = Math.max(1, size.getZ() / 2);
            Rotation rot = Prefab.rotFor(o.quarter());
            if (o.quarter() % 2 != 0) {
                int t = hx;
                hx = hz;
                hz = t;
            }
            int g = Prefab.groundAt(l, o.x(), o.z(), hx, hz);
            Prefab.place(l, o.id(), o.x(), g, o.z(), rot, foundation, Integer.MAX_VALUE);
        }
    }
}
