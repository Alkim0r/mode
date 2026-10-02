package com.alkimor.regnum.trade;

import java.util.EnumMap;
import java.util.Map;

/**
 * Торговые грузы. У каждого товара есть регионы-производители (дёшево) и регионы-потребители (дорого).
 * Цены — в изумрудах за ящик.
 */
public enum TradeGood {
    SALT("salt", "Ящик соли", 6),
    SPICES("spices", "Мешок пряностей", 14),
    SILK("silk", "Тюк шёлка", 18),
    FURS("furs", "Связка мехов", 12),
    WINE("wine", "Бочонок вина", 10),
    IRON("iron_goods", "Ящик железных изделий", 9),
    HERBS("herb_bundle", "Лекарственный сбор", 8),
    AMBER("amber", "Ларец янтаря", 22);

    public final String id;
    public final String title;
    public final int base;
    private final Map<Region, Double> factors = new EnumMap<>(Region.class);

    TradeGood(String id, String title, int base) {
        this.id = id;
        this.title = title;
        this.base = base;
    }

    static {
        f(SALT, Region.COAST, 0.6, Region.MOUNTAINS, 1.45, Region.HOT_DRY, 1.3, Region.COLD, 1.2);
        f(SPICES, Region.JUNGLE, 0.6, Region.HOT_DRY, 0.7, Region.COLD, 1.6, Region.PLAINS, 1.25);
        f(SILK, Region.JUNGLE, 0.7, Region.COLD, 1.45, Region.MOUNTAINS, 1.3, Region.COAST, 1.15);
        f(FURS, Region.COLD, 0.55, Region.HOT_DRY, 1.6, Region.JUNGLE, 1.4, Region.COAST, 1.1);
        f(WINE, Region.PLAINS, 0.65, Region.COLD, 1.4, Region.MOUNTAINS, 1.3, Region.SWAMP, 1.15);
        f(IRON, Region.MOUNTAINS, 0.6, Region.SWAMP, 1.35, Region.COAST, 1.2, Region.JUNGLE, 1.25);
        f(HERBS, Region.SWAMP, 0.6, Region.JUNGLE, 0.7, Region.COLD, 1.3, Region.HOT_DRY, 1.4);
        f(AMBER, Region.COAST, 0.75, Region.SWAMP, 0.9, Region.HOT_DRY, 1.5, Region.MOUNTAINS, 1.25);
    }

    private static void f(TradeGood g, Object... pairs) {
        for (int i = 0; i < pairs.length; i += 2) g.factors.put((Region) pairs[i], (Double) pairs[i + 1]);
    }

    public double factor(Region r) {
        return factors.getOrDefault(r, 1.0);
    }

    /** Где товар производят (самый низкий множитель). */
    public Region producer() {
        Region best = Region.PLAINS;
        double min = 9;
        for (Region r : Region.values()) {
            if (factor(r) < min) {
                min = factor(r);
                best = r;
            }
        }
        return best;
    }

    public static TradeGood byId(int id) {
        TradeGood[] v = values();
        return id >= 0 && id < v.length ? v[id] : SALT;
    }
}
