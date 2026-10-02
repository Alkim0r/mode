package com.alkimor.regnum.kingdom;

/**
 * Типы войск. Порядок (ordinal) сохраняется в мире — новые типы только в конец.
 * Ополченцы вербуются из жителей деревень, остальные — в казарме.
 */
public enum SoldierType {
    //          название          цена жал  hp  ур  роль            лук: дальность перез. скор.
    MILITIA("Ополченец", 0, 0, 20, 1, Role.INFANTRY, "Дешёвое мясо: копьё и кожаная куртка"),
    SWORDSMAN("Мечник", 8, 1, 30, 1, Role.INFANTRY, "Универсальная пехота: меч и щит"),
    ARCHER("Лучник", 10, 1, 22, 1, Role.ARCHER, "Быстрая стрельба на среднюю дистанцию"),
    KNIGHT("Рыцарь", 24, 2, 50, 2, Role.INFANTRY, "Тяжёлая пехота в латах: сильный удар и броня"),
    SHIELDMAN("Щитник", 10, 1, 36, 1, Role.SHIELD, "Держит первый ряд: щит гасит удары спереди и стрелы"),
    SPEARMAN("Копейщик", 9, 1, 28, 1, Role.SPEAR, "Длинное копьё: бьёт дальше, конницу валит вдвое сильнее"),
    GREATSWORD("Двуручник", 18, 2, 34, 2, Role.HEAVY, "Двуручный меч или алебарда: страшный размах, медленный и без щита"),
    LIGHT_CAV("Лёгкая конница", 22, 2, 28, 2, Role.CAVALRY, "Быстрые налётчики: фланги, преследование, добивание стрелков"),
    HEAVY_CAV("Тяжёлая конница", 40, 4, 48, 3, Role.CAVALRY, "Бронированная конница: таранный удар с разгона"),
    HORSE_ARCHER("Конный лучник", 26, 2, 24, 3, Role.HORSE_ARCHER, "Стреляет на скаку и отходит от врага"),
    CROSSBOW("Арбалетчик", 14, 1, 26, 2, Role.ARCHER, "Бьёт дальше и сильнее лучника, но заряжает долго"),
    MUSKETEER("Мушкетёр", 30, 3, 26, 3, Role.GUNNER, "Залп из мушкета: огромный урон, дальний выстрел, долгая зарядка (3 пороха при найме)"),
    BOMBARDIER("Бомбардир", 48, 4, 30, 4, Role.GUNNER, "Ручная бомбарда: разрывает строй навесом, но очень медленная (8 пороха и 10 железа)"),
    SCOUT("Разведчик", 12, 1, 20, 1, Role.HORSE_ARCHER, "Быстрый всадник: открывает неизвестные королевства рядом и копит разведданные");

    public enum Role {
        INFANTRY, SHIELD, SPEAR, HEAVY, CAVALRY, ARCHER, HORSE_ARCHER, GUNNER
    }

    public final String title;
    /** Стоимость найма в изумрудах из казны. */
    public final int cost;
    /** Жалованье в день. */
    public final int upkeep;
    public final double health;
    /** Минимальный уровень города для найма. */
    public final int minCityLevel;
    public final Role role;
    public final String blurb;

    SoldierType(String title, int cost, int upkeep, double health, int minCityLevel, Role role, String blurb) {
        this.title = title;
        this.cost = cost;
        this.upkeep = upkeep;
        this.health = health;
        this.minCityLevel = minCityLevel;
        this.role = role;
        this.blurb = blurb;
    }

    public boolean ranged() {
        return role == Role.ARCHER || role == Role.HORSE_ARCHER || role == Role.GUNNER;
    }

    /** Технология, открывающая найм (null — доступен сразу). */
    public Science.Tech tech() {
        return switch (this) {
            case SPEARMAN, SHIELDMAN -> Science.Tech.BRONZE;
            case KNIGHT -> Science.Tech.IRON;
            case GREATSWORD -> Science.Tech.STEEL;
            case LIGHT_CAV -> Science.Tech.RIDING;
            case HORSE_ARCHER -> Science.Tech.COMPOSITE;
            case SCOUT -> Science.Tech.RIDING;
            case HEAVY_CAV -> Science.Tech.CHIVALRY;
            case CROSSBOW -> Science.Tech.MACHINERY;
            case MUSKETEER -> Science.Tech.MUSKETRY;
            case BOMBARDIER -> Science.Tech.METALLURGY;
            default -> null;
        };
    }

    /** К какому роду войск (для военачальников) относится боец. */
    public Commissions.Branch branch() {
        return switch (this) {
            case LIGHT_CAV, HEAVY_CAV, SCOUT -> Commissions.Branch.CAVALRY;
            case ARCHER, CROSSBOW, HORSE_ARCHER, MUSKETEER -> Commissions.Branch.ARCHERS;
            case BOMBARDIER -> Commissions.Branch.SIEGE;
            default -> Commissions.Branch.INFANTRY;
        };
    }

    /** Порох и железо, которые город тратит на найм (0 — ничего). */
    public int gunpowderCost() {
        return this == MUSKETEER ? 3 : this == BOMBARDIER ? 8 : 0;
    }

    public int ironCost() {
        return this == BOMBARDIER ? 10 : 0;
    }

    public boolean mounted() {
        return this == LIGHT_CAV || this == HEAVY_CAV || this == HORSE_ARCHER || this == SCOUT;
    }

    /** Дальность стрельбы (блоков). */
    public int range() {
        return this == MUSKETEER ? 34 : this == BOMBARDIER ? 30 : this == CROSSBOW ? 28 : this == HORSE_ARCHER || this == SCOUT ? 16 : 18;
    }

    /** Пауза между выстрелами (тиков). */
    public int reload() {
        return this == BOMBARDIER ? 170 : this == MUSKETEER ? 105 : this == CROSSBOW ? 70 : this == HORSE_ARCHER ? 38 : 30;
    }

    /** Сколько «мест» в армии занимает боец (конница дороже в содержании строя). */
    public int slots() {
        return mounted() || this == BOMBARDIER ? 2 : 1;
    }

    /** Базовая скорость пешего бойца. */
    public double speed() {
        return switch (role) {
            case HEAVY -> 0.27;
            case SHIELD -> 0.28;
            default -> this == KNIGHT ? 0.29 : 0.32;
        };
    }

    /** Модель/текстура: у каждого типа своя модель (клиент выбирает её по имени типа); оставлено для совместимости. */
    public int look() {
        return ordinal();
    }

    public static SoldierType byId(int id) {
        SoldierType[] v = values();
        return id >= 0 && id < v.length ? v[id] : SWORDSMAN;
    }
}
