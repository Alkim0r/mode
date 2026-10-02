package com.alkimor.regnum.survival;

/**
 * Черты характера (как в Project Zomboid): выбираются при создании героя за очки.
 * Положительные стоят очков, отрицательные — дают. Итог не может уйти в минус.
 */
public enum Trait {
    // ---------------- положительные
    EAGLE_EYE(-3, "Орлиный глаз", "Видит дальше: туман отступает, +12% урона стрелами по дальним целям"),
    MARKSMAN(-3, "Меткий", "+10% урона луками, арбалетами и метательным; +20% опыта стрельбы"),
    FAST(-4, "Быстрый", "+5% к скорости передвижения"),
    TOUGH(-4, "Крепкий", "+4 здоровья"),
    STRONG(-4, "Силач", "+1 Сила, +25% к переносимому весу"),
    ATHLETE(-3, "Атлет", "+1 Выносливость, атлетика растёт на 25% быстрее"),
    THICK_SKIN(-3, "Толстокожий", "−10% получаемого физического урона"),
    FAST_LEARNER(-5, "Быстро учится", "+15% ко всему опыту навыков"),
    CAT_EYES(-2, "Кошачье зрение", "В темноте под землёй вы видите лучше"),
    QUIET(-3, "Тихоня", "Враги замечают вас на 20% позже"),
    LEADER(-4, "Вожак", "+1 Обаяние, +2 к лимиту армии"),
    HAGGLER(-2, "Торгаш", "+5% к цене продажи грузов"),
    CRAFTSMAN(-2, "Ремесленник", "+25% опыта кузнечного дела, ремонт сильнее на 10%"),
    LUCKY(-3, "Удачливый", "+1 к удаче: добыча из сундуков и рыбалки лучше"),
    IRON_GUT(-2, "Крепкий желудок", "Инфекции и яды вдвое слабее"),

    // ---------------- отрицательные
    SHORT_SIGHTED(3, "Близорукий", "Даль скрыта туманом, −20% урона стрелами по целям дальше 16 блоков"),
    NOISY(3, "Шумный", "Враги замечают вас на 30% раньше"),
    CLUMSY(2, "Неуклюжий", "Урон от падения ×1.3, медленнее крадётесь"),
    WEAK(4, "Хилый", "−4 здоровья"),
    SLOW(3, "Медлительный", "−5% к скорости передвижения"),
    FEEBLE(3, "Слабак", "−1 Сила, −20% к переносимому весу"),
    SLOW_LEARNER(4, "Медленно учится", "−15% ко всему опыту навыков"),
    NIGHT_BLIND(3, "Куриная слепота", "В темноте видно хуже, −10% урона стрелами ночью"),
    BRITTLE(2, "Хрупкие кости", "Переломы даже от небольшого падения"),
    BLEEDER(2, "Слабая кровь", "Кровотечения чаще и опаснее"),
    GLUTTON(2, "Обжора", "Голод наступает быстрее"),
    COWARD(2, "Трус", "При здоровье ниже 30% — слабость"),
    UNLUCKY(2, "Неудачник", "−1 к удаче"),
    INFAMOUS(2, "Дурная слава", "Начинаете с честью −15");

    /** Отрицательное значение — стоимость черты; положительное — сколько очков она даёт. */
    public final int points;
    public final String title;
    public final String desc;

    Trait(int points, String title, String desc) {
        this.points = points;
        this.title = title;
        this.desc = desc;
    }

    public boolean positive() {
        return points < 0;
    }

    /** Взаимоисключающие пары. */
    public boolean conflicts(Trait o) {
        return pair(this, o, EAGLE_EYE, SHORT_SIGHTED) || pair(this, o, FAST, SLOW) || pair(this, o, TOUGH, WEAK)
                || pair(this, o, STRONG, FEEBLE) || pair(this, o, FAST_LEARNER, SLOW_LEARNER) || pair(this, o, QUIET, NOISY)
                || pair(this, o, LUCKY, UNLUCKY) || pair(this, o, CAT_EYES, NIGHT_BLIND) || pair(this, o, ATHLETE, CLUMSY);
    }

    private static boolean pair(Trait a, Trait b, Trait x, Trait y) {
        return a == x && b == y || a == y && b == x;
    }

    public static Trait byName(String n) {
        try {
            return valueOf(n);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
