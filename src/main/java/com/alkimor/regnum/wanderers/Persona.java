package com.alkimor.regnum.wanderers;

/** Характер странника. */
public enum Persona {
    GUIDE("Проводник", new String[]{"Ратмир", "Добрыня", "Светозар", "Олег", "Велимир"}, 30),
    TRICKSTER("Торговец", new String[]{"Мирон", "Фрол", "Ерёма", "Савва", "Лука"}, 25),
    WOUNDED("Раненый путник", new String[]{"Ивашка", "Степан", "Богдан", "Тимофей", "Гаврила"}, 25),
    HERMIT("Отшельник", new String[]{"Аввакум", "Пимен", "Нестор", "Феофан", "Серафим"}, 20),
    MERCHANT("Купец", new String[]{"Садко", "Афанасий", "Прохор", "Харитон", "Демид"}, 30),
    NOBLE("Боярышня", new String[]{"Злата", "Милана", "Василиса", "Забава", "Любава", "Ярослава", "Агния"}, 12),
    SPOUSE("Супруга", new String[]{"Злата"}, 0),
    HEIR("Наследник", new String[]{"Святослав", "Ярополк", "Всеслава", "Мстислав", "Добромила", "Ростислав", "Владимира", "Изяслав"}, 0);

    public boolean isFamily() {
        return this == SPOUSE || this == HEIR;
    }

    public final String title;
    public final String[] names;
    public final int weight;

    Persona(String title, String[] names, int weight) {
        this.title = title;
        this.names = names;
        this.weight = weight;
    }

    public static Persona byId(int id) {
        Persona[] v = values();
        return id >= 0 && id < v.length ? v[id] : GUIDE;
    }

    public static Persona random(net.minecraft.util.RandomSource r) {
        int total = 0;
        for (Persona p : values()) total += p.weight;
        int x = r.nextInt(total);
        for (Persona p : values()) {
            x -= p.weight;
            if (x < 0) return p;
        }
        return GUIDE;
    }
}
