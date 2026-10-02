package com.alkimor.regnum.survival;

import net.minecraft.ChatFormatting;

/**
 * 18 навыков в духе Bannerlord, растут только от действий (как в Project Zomboid).
 * Уровни 0..100. Скорость роста зависит от атрибута и фокуса; у каждого навыка есть
 * «предел обучения» — без вложенного фокуса выше него не подняться.
 * На уровнях 25/50/75/100 открывается выбор одного из двух перков.
 */
public enum Skill {
    ONE_HANDED("one_handed", Attr.VIGOR, "Одноручное", "мечи, сабли, булавы, топорики", "+0.2% урона одноручным за уровень"),
    TWO_HANDED("two_handed", Attr.VIGOR, "Двуручное", "топоры, клейморы, полуторные мечи", "+0.2% урона двуручным за уровень"),
    POLEARM("polearm", Attr.VIGOR, "Древковое", "копья, пилумы, трезубцы", "+0.2% урона древковым за уровень"),
    BOW("bow", Attr.CONTROL, "Луки", "стрельба из лука", "+0.2% урона луком за уровень"),
    CROSSBOW("crossbow", Attr.CONTROL, "Арбалеты", "стрельба из арбалета", "+0.2% урона арбалетом за уровень"),
    THROWING("throwing", Attr.CONTROL, "Метательное", "дротики, топорики, трезубцы", "+0.25% урона метательным за уровень"),
    ATHLETICS("athletics", Attr.ENDURANCE, "Атлетика", "бег, плавание, переноска тяжестей", "+1 сердце за 25 ур., больше переносимый вес"),
    RIDING("riding", Attr.ENDURANCE, "Верховая езда", "езда верхом", "+0.15% скорости коня за уровень"),
    SMITHING("smithing", Attr.ENDURANCE, "Кузнечное дело", "закалка, ремонт, ковка", "Выше шанс закалки и сильнее ремонт"),
    SCOUTING("scouting", Attr.CUNNING, "Разведка", "собирательство, следы, находки", "Чаще травы и полезные находки"),
    TACTICS("tactics", Attr.CUNNING, "Тактика", "победы армии, приказы в бою", "Ваши солдаты +0.1% урона за уровень"),
    ROGUERY("roguery", Attr.CUNNING, "Плутовство", "скрытность, взлом, грабёж", "Врагам труднее заметить вас"),
    CHARM("charm", Attr.SOCIAL, "Харизма", "беседы, подарки, сватовство", "Странники и знать относятся лучше"),
    LEADERSHIP("leadership", Attr.SOCIAL, "Лидерство", "найм, приказы, оборона городов", "+1 к лимиту армии за 10 уровней"),
    TRADE("trade", Attr.SOCIAL, "Торговля", "купля-продажа грузов", "−0.2% к цене покупки и +0.2% к продаже за уровень"),
    STEWARD("steward", Attr.INTELLIGENCE, "Управление", "развитие городов, казна", "+0.2% к доходу городов за уровень"),
    MEDICINE("medicine", Attr.INTELLIGENCE, "Медицина", "лечение себя и других", "Сильнее лечение, выше шанс вылечить инфекцию"),
    ENGINEERING("engineering", Attr.INTELLIGENCE, "Инженерия", "механизмы, ловушки, стройка", "+0.2% к скорости добычи за уровень");

    public static final int MAX_LEVEL = 100;
    public static final int[] PERK_LEVELS = {25, 50, 75, 100};
    public static final int MAX_FOCUS = 5;

    public final String key;
    public final Attr attr;
    public final String title;
    public final String grows;
    public final String bonus;

    Skill(String key, Attr attr, String title, String grows, String bonus) {
        this.key = key;
        this.attr = attr;
        this.title = title;
        this.grows = grows;
        this.bonus = bonus;
    }

    public ChatFormatting color() {
        return attr.color;
    }

    /** Суммарный опыт, нужный для уровня n. */
    public static int xpFor(int n) {
        n = Math.max(0, Math.min(MAX_LEVEL, n));
        return Math.round(8f * n + 0.9f * n * n);
    }

    public static int levelFor(int xp) {
        int lo = 0, hi = MAX_LEVEL;
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (xpFor(mid) <= xp) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }

    /** Предел обучения: выше него опыт почти не идёт (как в Bannerlord). */
    public static int learningLimit(int attr, int focus) {
        return 10 + 8 * attr + 18 * focus;
    }

    /** Скорость обучения (множитель опыта) на текущем уровне. */
    public static float learningRate(int attr, int focus, int level) {
        float rate = (0.4f + 0.12f * attr) * (1f + 0.5f * focus);
        int limit = learningLimit(attr, focus);
        if (level >= limit) return 0f;
        if (level > limit - 10) rate *= (limit - level) / 10f;
        return rate;
    }

    public static Skill byKey(String key) {
        for (Skill s : values()) if (s.key.equals(key)) return s;
        return switch (key) { // старые навыки (до перехода на 18 навыков)
            case "melee" -> ONE_HANDED;
            case "archery" -> BOW;
            case "foraging" -> SCOUTING;
            case "fitness" -> ATHLETICS;
            default -> null;
        };
    }

    /** Уровень героя: суммарный опыт для уровня n. */
    public static int charXpFor(int n) {
        return 150 * n + 40 * n * n;
    }

    public static int charLevelFor(int xp) {
        int n = 0;
        while (n < 200 && charXpFor(n + 1) <= xp) n++;
        return n;
    }
}
