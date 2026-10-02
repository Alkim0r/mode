package com.alkimor.regnum.kingdom;

/** Постройки города. Каждая ставится блоком внутри территории своего города. */
public enum BuildingType {
    BARRACKS("Казарма", "Позволяет нанимать войска, +5 к лимиту армии"),
    MARKET("Рынок", "+3 изумруда в казну ежедневно, торговля грузами и пошлина 5% со сделок"),
    WATCHTOWER("Сторожевая башня", "Раннее предупреждение о набегах, подсветка врагов"),
    TRAINING_GROUND("Учебный плац", "Солдаты рядом (приказ «Держать позицию») тренируют боевые приёмы"),
    BUILDER_HUT("Мастерская строителя", "Строитель возводит стены по «Плану строителя» за казну; каждая следующая мастерская ускоряет работу"),
    LIBRARY("Библиотека", "+4 очка знаний в сутки: исследуйте технологии (/regnum science)", Science.Tech.WRITING),
    UNIVERSITY("Университет", "+10 очков знаний в сутки", Science.Tech.EDUCATION),
    INFIRMARY("Лазарет", "Лечит раненых бойцов в городе, снижает риск эпидемий", Science.Tech.MEDICINE),
    SMITHY("Кузница", "Выдаёт бойцам города лучшую броню и оружие (за железо в казне)", Science.Tech.IRON),
    WAREHOUSE("Склад", "Хранит запасы города, защищает от порчи и пожаров", Science.Tech.AGRICULTURE),
    STABLE("Конюшня", "Нужна для найма конницы; лучшие кони у ветеранов", Science.Tech.HUSBANDRY);

    public final String title;
    public final String effect;
    /** Технология, без которой постройку не поставить (null — доступна сразу). */
    public final Science.Tech tech;

    BuildingType(String title, String effect) {
        this(title, effect, null);
    }

    BuildingType(String title, String effect, Science.Tech tech) {
        this.title = title;
        this.effect = effect;
        this.tech = tech;
    }
}
