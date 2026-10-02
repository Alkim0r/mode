package com.alkimor.regnum.kingdom;

import static com.alkimor.regnum.kingdom.SoldierType.*;

/**
 * Военная доктрина культур: из каких родов войск ИИ-государства собирает гарнизон и полевую армию.
 * Строй идёт слоями: первый ряд (щитники/копья), второй (мечники/двуручники), стрелки, конница на флангах.
 */
public final class Doctrine {
    private Doctrine() {}

    private static final SoldierType[][] FIELD = {
            // NORTH: копья, щиты, топоры дружины, лучники
            {SPEARMAN, SHIELDMAN, SWORDSMAN, ARCHER, SPEARMAN, KNIGHT, ARCHER, SHIELDMAN, GREATSWORD, LIGHT_CAV},
            // EMPIRE: легионный строй — щиты и копья, арбалеты, тяжёлая конница
            {SHIELDMAN, SHIELDMAN, SPEARMAN, CROSSBOW, SWORDSMAN, SPEARMAN, CROSSBOW, KNIGHT, HEAVY_CAV, SHIELDMAN},
            // WEST: рыцари, арбалетчики, оруженосцы
            {KNIGHT, SHIELDMAN, CROSSBOW, SWORDSMAN, HEAVY_CAV, CROSSBOW, SPEARMAN, KNIGHT, ARCHER, SHIELDMAN},
            // STEPPE: конные лучники и лёгкая конница
            {HORSE_ARCHER, LIGHT_CAV, HORSE_ARCHER, SPEARMAN, HORSE_ARCHER, LIGHT_CAV, ARCHER, HORSE_ARCHER, SWORDSMAN, HEAVY_CAV},
            // SULTANATE: гвардия, лёгкая конница, лучники
            {SWORDSMAN, LIGHT_CAV, ARCHER, SPEARMAN, LIGHT_CAV, ARCHER, SHIELDMAN, HORSE_ARCHER, KNIGHT, ARCHER},
            // CLANS: двуручники, копья, лучники
            {GREATSWORD, SPEARMAN, GREATSWORD, ARCHER, SWORDSMAN, GREATSWORD, ARCHER, SPEARMAN, SHIELDMAN, GREATSWORD},
    };

    /** Состав полевой армии: i-й боец. */
    public static SoldierType field(int culture, int i) {
        SoldierType[][] f = FIELD;
        return f[Math.floorMod(culture, f.length)][i % 10];
    }

    /** Гарнизон: больше пехоты и стрелков, конницы нет. */
    public static SoldierType garrison(int culture, int i) {
        SoldierType t = field(culture, i);
        return t.mounted() ? (t == HORSE_ARCHER ? ARCHER : SPEARMAN) : t;
    }
}
