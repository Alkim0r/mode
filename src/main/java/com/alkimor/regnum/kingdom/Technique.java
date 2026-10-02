package com.alkimor.regnum.kingdom;

/**
 * Боевые приёмы солдат (в духе PvP-техник). Уровни 0..5.
 * Растут на Учебном плацу и в спарринге с королём — солдат перенимает то, что делает игрок.
 */
public enum Technique {
    SHIELD_BLOCK("Блок щитом", "Шанс отразить удар щитом (8%/ур.), с 3 ур. — контратака"),
    JUMP_CRIT("Удар в прыжке", "+6% урона в ближнем бою/ур. и шанс крита ×1.5"),
    W_TAP("Отбрасывание", "Удары сильнее отбрасывают врага"),
    STRAFE("Уклонение", "Шанс увернуться от удара (5%/ур.)"),
    KITING("Стрельба с отходом", "Лучник держит дистанцию и стреляет чаще"),
    FOCUS_FIRE("Фокус цели", "Соседи по отряду атакуют ту же цель"),
    RETREAT_HEAL("Отход на перевязку", "Раненый боец отступает и быстро лечится");

    public static final int MAX = 5;
    public static final int[] XP = {0, 5, 15, 30, 50, 80};

    public final String title;
    public final String desc;

    Technique(String title, String desc) {
        this.title = title;
        this.desc = desc;
    }

    public static int levelFor(int xp) {
        int l = 0;
        for (int i = 1; i <= MAX; i++) if (xp >= XP[i]) l = i;
        return l;
    }
}
