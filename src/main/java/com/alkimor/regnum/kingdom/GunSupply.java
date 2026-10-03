package com.alkimor.regnum.kingdom;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Цена порохового превосходства (I071): огнестрел жрёт порох города, а непогода его портит.
 * Мушкетёры тратят 1 порох на 5 выстрелов, бомбардиры 1 на 2. Нет пороха на складе города: стрелки не стреляют.
 * Под дождём 30% осечек. Бойцы без города (арены, чужие) не ограничены. Счётчики выстрелов в памяти.
 */
public final class GunSupply {
    private GunSupply() {}

    private static final Map<UUID, Integer> SHOTS = new HashMap<>();

    /** Сколько выстрелов на 1 порох. */
    public static int shotsPerPowder(SoldierType t) {
        return switch (t) {
            case MUSKETEER -> 5;
            case BOMBARDIER -> 2;
            default -> 0;
        };
    }

    public static boolean misfires(boolean raining, double roll) { return raining && roll < 0.30; }

    /** Нужно ли списать порох на этом выстреле (после n-го выстрела). */
    public static boolean consumes(int shotsSoFar, int perPowder) { return perPowder > 0 && shotsSoFar % perPowder == 0; }

    /** Разрешён ли выстрел; списывает порох города. */
    public static boolean allow(SoldierEntity s) {
        int per = shotsPerPowder(s.getSoldierType());
        if (per == 0 || s.getCityId() == null || !(s.level() instanceof net.minecraft.server.level.ServerLevel sl)) return true;
        if (misfires(sl.isRaining() && sl.canSeeSky(s.blockPosition()), s.getRandom().nextDouble())) return false;
        City c = KingdomData.get(sl.getServer()).byId(s.getCityId());
        if (c == null) return true;
        int n = SHOTS.merge(c.id, 1, Integer::sum);
        if (consumes(n, per)) {
            if (!c.take(Resource.GUNPOWDER, 1)) { SHOTS.merge(c.id, -1, Integer::sum); return false; }
            KingdomData.get(sl.getServer()).setDirty();
        }
        return true;
    }
}
