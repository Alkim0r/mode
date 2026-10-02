package com.alkimor.regnum.story;

import java.util.ArrayList;
import java.util.List;

/** Описание квеста: сюжетная/побочная линия, цели, награды, отношения с фракциями. */
public final class QuestDef {
    public enum Line {
        MAIN("Главная линия"), SIDE("Побочные"), FACTION("Фракции");
        public final String title;
        Line(String t) { title = t; }
    }

    public enum Kind { KILL, COLLECT, VISIT, CITY_LEVEL, BUILD, FLAG, STATE }

    public record Obj(Kind kind, String target, int count, String text) {}

    public final String id, title, giver, intro, outro;
    public final Line line;
    public final String faction;      // фракция, чью репутацию квест двигает (может быть null)
    public final int repReq;          // минимальная репутация этой фракции для получения
    public final List<String> prereq = new ArrayList<>();
    public final List<Obj> objs = new ArrayList<>();
    public final List<String> rewards = new ArrayList<>();
    public String cutsceneHint;       // для будущих катсцен (id сцены)

    QuestDef(String id, Line line, String title, String giver, String faction, int repReq, String intro, String outro) {
        this.id = id;
        this.line = line;
        this.title = title;
        this.giver = giver;
        this.faction = faction;
        this.repReq = repReq;
        this.intro = intro;
        this.outro = outro;
    }

    QuestDef pre(String... ids) {
        prereq.addAll(List.of(ids));
        return this;
    }

    QuestDef kill(String target, int n, String text) {
        objs.add(new Obj(Kind.KILL, target, n, text));
        return this;
    }

    QuestDef collect(String item, int n, String text) {
        objs.add(new Obj(Kind.COLLECT, item, n, text));
        return this;
    }

    QuestDef visit(String structures, String text) {
        objs.add(new Obj(Kind.VISIT, structures, 1, text));
        return this;
    }

    QuestDef city(int level, String text) {
        objs.add(new Obj(Kind.CITY_LEVEL, "", level, text));
        return this;
    }

    QuestDef build(String type, String text) {
        objs.add(new Obj(Kind.BUILD, type, 1, text));
        return this;
    }

    /** Состояние королевства: tech:ИМЯ, techs, vassals, trade, ally, legend, outpost, army. */
    QuestDef state(String target, int count, String text) {
        objs.add(new Obj(Kind.STATE, target, count, text));
        return this;
    }

    QuestDef flag(String name, String text) {
        objs.add(new Obj(Kind.FLAG, name, 1, text));
        return this;
    }

    QuestDef reward(String... r) {
        rewards.addAll(List.of(r));
        return this;
    }
}
