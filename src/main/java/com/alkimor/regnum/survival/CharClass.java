package com.alkimor.regnum.survival;

import java.util.EnumMap;
import java.util.Map;

/**
 * Классы героя: стартовый набор атрибутов и фокуса + свой набор инструментов.
 * Класс — это начало пути, а не клетка: дальше навыки растут от того, чем вы занимаетесь.
 */
public enum CharClass {
    /**
     * «Никто» — как «Нищий» в Dark Souls и «Безработный» в Project Zomboid: ни бонусов класса,
     * ни снаряжения, зато больше свободных очков атрибутов и фокуса и +8 очков на черты.
     */
    NOBODY("Никто", "Без ремесла, без имени, без снаряжения. Ничего не умеет — зато может стать кем угодно: "
            + "больше свободных очков и +8 очков на черты.",
            attrs(), focus(), new String[]{}, "ничего, кроме одежды на плечах", 4, 4, 8),
    WARRIOR("Воин", "Передний край и поединки. Крепок и опасен в ближнем бою.",
            attrs(Attr.VIGOR, 5, Attr.ENDURANCE, 4), focus(Skill.ONE_HANDED, 2, Skill.TWO_HANDED, 1, Skill.ATHLETICS, 1),
            new String[]{"regnum:whetstone"}, "Точильный камень"),
    SCOUT("Разведчик", "Тихо пройти, вскрыть замок, уйти незамеченным. Проникает туда, куда армия не пройдёт.",
            attrs(Attr.CUNNING, 5, Attr.CONTROL, 4), focus(Skill.ROGUERY, 2, Skill.SCOUTING, 1, Skill.BOW, 1),
            new String[]{"regnum:lockpick", "regnum:lockpick", "regnum:lockpick", "regnum:smoke_bomb", "regnum:smoke_bomb", "regnum:grappling_hook"},
            "Отмычки, дымовые шашки, кошка-крюк"),
    HUNTER("Охотник", "Лук, следы и капканы. Бьёт издалека и ставит ловушки на зверя и человека.",
            attrs(Attr.CONTROL, 5, Attr.CUNNING, 3, Attr.ENDURANCE, 3), focus(Skill.BOW, 2, Skill.SCOUTING, 1, Skill.THROWING, 1),
            new String[]{"regnum:bear_trap", "regnum:bear_trap", "regnum:javelin", "regnum:javelin", "regnum:javelin"},
            "Капканы и дротики"),
    HEALER("Лекарь", "Травы, отвары и перевязки. Без него отряд долго не протянет.",
            attrs(Attr.INTELLIGENCE, 5, Attr.SOCIAL, 3, Attr.CUNNING, 3), focus(Skill.MEDICINE, 3, Skill.SCOUTING, 1),
            new String[]{"regnum:herbal_mortar", "regnum:bandage", "regnum:bandage", "regnum:bandage", "regnum:bandage"},
            "Ступка травника и бинты"),
    SMITH("Мастер", "Кузнец и инженер: чинит в походе, куёт лучшее и ставит ловушки.",
            attrs(Attr.ENDURANCE, 4, Attr.INTELLIGENCE, 4, Attr.VIGOR, 3), focus(Skill.SMITHING, 2, Skill.ENGINEERING, 2),
            new String[]{"regnum:field_smith_kit", "regnum:bear_trap"}, "Походный набор кузнеца и капкан"),
    COMMANDER("Полководец", "Ведёт войско и правит городами. Сильнее всех — через своих людей.",
            attrs(Attr.SOCIAL, 5, Attr.CUNNING, 3, Attr.INTELLIGENCE, 3), focus(Skill.LEADERSHIP, 2, Skill.TACTICS, 1, Skill.STEWARD, 1),
            new String[]{"regnum:commander_baton", "regnum:war_banner"}, "Жезл командира и боевое знамя");

    /** Сколько свободных очков атрибутов и фокуса даётся при создании сверх класса (по умолчанию). */
    public static final int FREE_ATTR = 1, FREE_FOCUS = 2;

    public final String title;
    public final String desc;
    public final Map<Attr, Integer> attrBonus;
    public final Map<Skill, Integer> focus;
    public final String[] kit;
    public final String kitText;
    /** Свободные очки при создании и бонус к очкам черт (как профессии в Project Zomboid). */
    public final int freeAttr, freeFocus, traitPoints;

    CharClass(String title, String desc, Map<Attr, Integer> attrBonus, Map<Skill, Integer> focus, String[] kit, String kitText) {
        this(title, desc, attrBonus, focus, kit, kitText, FREE_ATTR, FREE_FOCUS, 0);
    }

    CharClass(String title, String desc, Map<Attr, Integer> attrBonus, Map<Skill, Integer> focus, String[] kit, String kitText,
              int freeAttr, int freeFocus, int traitPoints) {
        this.title = title;
        this.desc = desc;
        this.attrBonus = attrBonus;
        this.focus = focus;
        this.kit = kit;
        this.kitText = kitText;
        this.freeAttr = freeAttr;
        this.freeFocus = freeFocus;
        this.traitPoints = traitPoints;
    }

    /** Итоговое значение атрибута класса (база 2 + бонус). */
    public int attr(Attr a) {
        return Attr.BASE + attrBonus.getOrDefault(a, 0) - (attrBonus.containsKey(a) ? Attr.BASE : 0);
    }

    private static Map<Attr, Integer> attrs(Object... kv) {
        Map<Attr, Integer> m = new EnumMap<>(Attr.class);
        for (int i = 0; i < kv.length; i += 2) m.put((Attr) kv[i], (Integer) kv[i + 1]);
        return m;
    }

    private static Map<Skill, Integer> focus(Object... kv) {
        Map<Skill, Integer> m = new EnumMap<>(Skill.class);
        for (int i = 0; i < kv.length; i += 2) m.put((Skill) kv[i], (Integer) kv[i + 1]);
        return m;
    }

    public static CharClass byName(String n) {
        try {
            return valueOf(n);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
