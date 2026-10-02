package com.alkimor.regnum.kingdom;

import net.minecraft.server.level.ServerLevel;

/**
 * Порча запасов (I054): провиант и травы портятся ежесуточно, склад сильно замедляет порчу,
 * зимой портится меньше, летом больше; порох отсыревает без склада. Дерево, камень и железо не портятся.
 */
public final class Spoilage {
    private Spoilage() {}

    /** Доля суточной порчи ресурса. */
    public static double rate(Resource r, boolean warehouse, int season) {
        double base = switch (r) {
            case FOOD -> warehouse ? 0.01 : 0.04;
            case HERBS -> warehouse ? 0.02 : 0.06;
            case GUNPOWDER -> warehouse ? 0.0 : 0.02;
            default -> 0.0;
        };
        double mul = season == 3 ? 0.5 : season == 1 ? 1.5 : 1.0;
        return base * mul;
    }

    /** Применяет порчу, возвращает текст потерь (пустой — ничего не испортилось). */
    public static String daily(ServerLevel ow, City c) {
        boolean wh = c.count(BuildingType.WAREHOUSE) > 0;
        int season = com.alkimor.regnum.survival.Seasons.index(ow);
        StringBuilder sb = new StringBuilder();
        for (Resource r : Resource.values()) {
            int have = c.stock(r);
            double rt = rate(r, wh, season);
            if (rt <= 0 || have < 10) continue;
            int lost = Math.max(1, (int) Math.round(have * rt));
            c.take(r, lost);
            if (sb.length() > 0) sb.append(", ");
            sb.append(r.title.toLowerCase()).append(" −").append(lost);
        }
        return sb.toString();
    }
}
