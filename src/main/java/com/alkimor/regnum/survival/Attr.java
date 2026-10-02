package com.alkimor.regnum.survival;

import net.minecraft.ChatFormatting;

/** Атрибуты персонажа (как в Mount &amp; Blade II: Bannerlord). Каждый ведёт три навыка. */
public enum Attr {
    VIGOR("vigor", "Сила", ChatFormatting.RED, "Ближний бой: одноручное, двуручное, древковое"),
    CONTROL("control", "Ловкость", ChatFormatting.GREEN, "Стрельба: луки, арбалеты, метательное"),
    ENDURANCE("endurance", "Выносливость", ChatFormatting.GOLD, "Атлетика, верховая езда, кузнечное дело"),
    CUNNING("cunning", "Хитрость", ChatFormatting.DARK_GREEN, "Разведка, тактика, плутовство"),
    SOCIAL("social", "Обаяние", ChatFormatting.YELLOW, "Харизма, лидерство, торговля"),
    INTELLIGENCE("intelligence", "Ум", ChatFormatting.AQUA, "Управление, медицина, инженерия");

    public static final int MAX = 10;
    public static final int BASE = 2;

    public final String key;
    public final String title;
    public final ChatFormatting color;
    public final String desc;

    Attr(String key, String title, ChatFormatting color, String desc) {
        this.key = key;
        this.title = title;
        this.color = color;
        this.desc = desc;
    }

    public Skill[] skills() {
        return java.util.Arrays.stream(Skill.values()).filter(s -> s.attr == this).toArray(Skill[]::new);
    }
}
