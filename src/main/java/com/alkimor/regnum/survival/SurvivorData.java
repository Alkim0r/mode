package com.alkimor.regnum.survival;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Данные героя, прикреплённые к игроку: опыт навыков, атрибуты, фокус, перки, черты, класс,
 * уровень героя, честь и летопись.
 */
public class SurvivorData {
    public static final Codec<SurvivorData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("xp", Map.of()).forGetter(SurvivorData::exportXp),
            Codec.INT.optionalFieldOf("honor", 0).forGetter(SurvivorData::honor),
            Codec.STRING.listOf().optionalFieldOf("chronicle", List.of()).forGetter(SurvivorData::chronicle),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("attrs", Map.of()).forGetter(d -> d.exportEnum(d.attrs)),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("focus", Map.of()).forGetter(d -> d.exportEnum(d.focus)),
            Codec.INT.optionalFieldOf("freeAttr", 0).forGetter(d -> d.freeAttr),
            Codec.INT.optionalFieldOf("freeFocus", 0).forGetter(d -> d.freeFocus),
            Codec.INT.optionalFieldOf("charXp", 0).forGetter(d -> d.charXp),
            Codec.STRING.listOf().optionalFieldOf("perks", List.of()).forGetter(d -> d.perks.stream().map(Enum::name).toList()),
            Codec.STRING.listOf().optionalFieldOf("traits", List.of()).forGetter(d -> d.traits.stream().map(Enum::name).toList()),
            Codec.STRING.optionalFieldOf("cls", "").forGetter(d -> d.cls == null ? "" : d.cls.name()),
            Codec.INT.optionalFieldOf("levelsGranted", 0).forGetter(d -> d.levelsGranted)
    ).apply(i, SurvivorData::fromSaved));

    public static final int CHRONICLE_MAX = 40;
    private final List<String> chronicle = new ArrayList<>();

    private final EnumMap<Skill, Integer> xp = new EnumMap<>(Skill.class);
    private final EnumMap<Attr, Integer> attrs = new EnumMap<>(Attr.class);
    private final EnumMap<Skill, Integer> focus = new EnumMap<>(Skill.class);
    private final EnumSet<Perk> perks = EnumSet.noneOf(Perk.class);
    private final EnumSet<Trait> traits = EnumSet.noneOf(Trait.class);
    private CharClass cls;
    private int honor;
    public int freeAttr, freeFocus;
    private int charXp;
    /** Сколько уровней героя уже «выдано» очками (чтобы очки не начислялись повторно). */
    private int levelsGranted;

    public SurvivorData() {}

    private static SurvivorData fromSaved(Map<String, Integer> xp, Integer honor, List<String> chronicle, Map<String, Integer> attrs,
                                          Map<String, Integer> focus, Integer freeAttr, Integer freeFocus, Integer charXp,
                                          List<String> perks, List<String> traits, String cls, Integer levelsGranted) {
        SurvivorData d = new SurvivorData();
        xp.forEach((k, v) -> {
            Skill s = Skill.byKey(k);
            if (s != null) d.xp.merge(s, v, Integer::sum);
        });
        attrs.forEach((k, v) -> {
            for (Attr a : Attr.values()) if (a.key.equals(k)) d.attrs.put(a, v);
        });
        focus.forEach((k, v) -> {
            Skill s = Skill.byKey(k);
            if (s != null) d.focus.put(s, v);
        });
        d.honor = honor;
        d.chronicle.addAll(chronicle);
        d.freeAttr = freeAttr;
        d.freeFocus = freeFocus;
        d.charXp = charXp;
        for (String p : perks) {
            Perk pk = Perk.byName(p);
            if (pk != null) d.perks.add(pk);
        }
        for (String t : traits) {
            Trait tr = Trait.byName(t);
            if (tr != null) d.traits.add(tr);
        }
        d.cls = cls.isEmpty() ? null : CharClass.byName(cls);
        d.levelsGranted = levelsGranted;
        // переход со старой системы: опыт навыков уже есть, а уровень героя — нет
        if (d.charXp == 0 && !d.xp.isEmpty()) d.charXp = d.xp.values().stream().mapToInt(Integer::intValue).sum();
        return d;
    }

    private <E extends Enum<E>> Map<String, Integer> exportEnum(EnumMap<E, Integer> m) {
        Map<String, Integer> out = new HashMap<>();
        m.forEach((k, v) -> out.put(k instanceof Attr a ? a.key : k instanceof Skill s ? s.key : k.name(), v));
        return out;
    }

    // ------------------------------------------------------------------ наследник: снимок/восстановление

    public Map<String, Integer> snapshot() {
        return exportXp();
    }

    public void restore(Map<String, Integer> snap) {
        xp.clear();
        snap.forEach((k, v) -> {
            Skill s = Skill.byKey(k);
            if (s != null) xp.merge(s, v, Integer::sum);
        });
    }

    public void scaleAll(double factor) {
        xp.replaceAll((k, v) -> (int) Math.round(v * factor));
    }

    private Map<String, Integer> exportXp() {
        Map<String, Integer> m = new HashMap<>();
        xp.forEach((k, v) -> m.put(k.key, v));
        return m;
    }

    // ------------------------------------------------------------------ навыки

    public int xp(Skill s) {
        return xp.getOrDefault(s, 0);
    }

    public int level(Skill s) {
        return Skill.levelFor(xp(s));
    }

    public void addXp(Skill s, int amount) {
        xp.put(s, Math.max(0, Math.min(Skill.xpFor(Skill.MAX_LEVEL), xp(s) + amount)));
    }

    public void setXp(Skill s, int value) {
        xp.put(s, Math.max(0, value));
    }

    // ------------------------------------------------------------------ атрибуты, фокус, уровень героя

    public int attr(Attr a) {
        int v = attrs.getOrDefault(a, Attr.BASE);
        if (a == Attr.VIGOR && traits.contains(Trait.STRONG)) v++;
        if (a == Attr.VIGOR && traits.contains(Trait.FEEBLE)) v--;
        if (a == Attr.ENDURANCE && traits.contains(Trait.ATHLETE)) v++;
        if (a == Attr.SOCIAL && traits.contains(Trait.LEADER)) v++;
        return Math.max(1, Math.min(Attr.MAX + 1, v));
    }

    public int baseAttr(Attr a) {
        return attrs.getOrDefault(a, Attr.BASE);
    }

    public void setAttr(Attr a, int v) {
        attrs.put(a, Math.max(1, Math.min(Attr.MAX, v)));
    }

    public int focus(Skill s) {
        return focus.getOrDefault(s, 0);
    }

    public void setFocus(Skill s, int v) {
        focus.put(s, Math.max(0, Math.min(Skill.MAX_FOCUS, v)));
    }

    public int charXp() {
        return charXp;
    }

    public int charLevel() {
        return Skill.charLevelFor(charXp);
    }

    /** Добавить опыт героя. Возвращает число новых уровней героя. */
    public int addCharXp(int amount) {
        charXp += Math.max(0, amount);
        int lvl = charLevel();
        int gained = 0;
        while (levelsGranted < lvl) {
            levelsGranted++;
            gained++;
            freeFocus++;
            if (levelsGranted % 3 == 0) freeAttr++;
        }
        return gained;
    }

    public int levelsGranted() {
        return levelsGranted;
    }

    // ------------------------------------------------------------------ перки

    public boolean has(Perk p) {
        return perks.contains(p);
    }

    public Set<Perk> perks() {
        return perks;
    }

    public boolean choose(Perk p) {
        if (perks.contains(p) || perks.contains(p.other()) || level(p.skill) < p.level()) return false;
        perks.add(p);
        return true;
    }

    /** Перки, которые уже можно выбрать, но ещё не выбраны. */
    public List<Perk> pendingPerks() {
        List<Perk> out = new ArrayList<>();
        for (Perk p : Perk.values()) {
            if (p.side == 0 && level(p.skill) >= p.level() && !perks.contains(p) && !perks.contains(p.other())) out.add(p);
        }
        return out;
    }

    public void clearPerks() {
        perks.clear();
    }

    // ------------------------------------------------------------------ черты и класс

    public boolean has(Trait t) {
        return traits.contains(t);
    }

    public Set<Trait> traits() {
        return traits;
    }

    public CharClass cls() {
        return cls;
    }

    public boolean created() {
        return cls != null;
    }

    public void create(CharClass c, Map<Attr, Integer> attrValues, Map<Skill, Integer> focusValues, Set<Trait> chosen) {
        this.cls = c;
        attrs.clear();
        attrs.putAll(attrValues);
        focus.clear();
        focus.putAll(focusValues);
        traits.clear();
        traits.addAll(chosen);
        freeAttr = 0;
        freeFocus = 0;
        if (chosen.contains(Trait.INFAMOUS)) honor = Math.min(honor, -15);
    }

    // ------------------------------------------------------------------ честь и летопись

    public int honor() {
        return honor;
    }

    public void addHonor(int delta) {
        honor = Math.max(-100, Math.min(100, honor + delta));
    }

    public List<String> chronicle() {
        return chronicle;
    }

    public void record(String entry) {
        chronicle.add(entry);
        while (chronicle.size() > CHRONICLE_MAX) chronicle.remove(0);
    }

    public static String honorTitle(int honor) {
        if (honor >= 60) return "Легенда";
        if (honor >= 30) return "Благородный";
        if (honor >= 10) return "Честный";
        if (honor > -10) return "Неизвестный";
        if (honor > -30) return "Сомнительный";
        if (honor > -60) return "Разбойник";
        return "Изгой";
    }
}
