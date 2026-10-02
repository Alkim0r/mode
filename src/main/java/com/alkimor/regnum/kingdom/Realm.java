package com.alkimor.regnum.kingdom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/** Соседнее королевство, которым правит ИИ: столица, правитель, гарнизон, отношения с игроком. */
public class Realm {
    public static final int PEACE = 0, WAR = 1, VASSAL = 2;

    public final UUID id;
    public String name;
    public String ruler;
    public int culture;
    public int x, z;
    public int y = 64;
    public boolean built = false;
    public int state = PEACE;
    /** Отношение к игроку: −100 вражда … +100 дружба. */
    public int relation = 0;
    public boolean trade = false;
    /** Союз: не воюет с вами и присылает подмогу, когда на ваш город идут в поход. */
    public boolean ally = false;
    /** Туман: 0 — неизвестно, 1 — слух (только сторона света), 2 — известно. */
    public int known = 0;
    /** Размер гарнизона и сила армии (растёт с войнами). */
    public int strength = 8;
    public long armyTimer = 0;
    public int armiesSent = 0, armiesLost = 0;
    public UUID warOwner;
    public UUID liege;
    /** Разведка: число агентов (до 3), знание о королевстве 0..100, настороженность 0..100. */
    public int agents = 0, intel = 0, suspicion = 0;
    /** Отложенные диверсии: подкупленные командиры (дух −30 у следующей армии) и испорченная осада. */
    public boolean bribed = false, siegeSabotaged = false;
    public long lastSpyDay = -1;
    /** Дипломатия: воинственность 0..100, перемирие до (игровое время), предложение послов, повод к войне у игрока. */
    public int aggression = -1;
    /** Последние причины отношений (самая новая — последняя). */
    public final java.util.List<String> history = new java.util.ArrayList<>();
    /** Блоки вражеского лагеря осады (BlockPos.asLong), первым идёт костёр. */
    public final java.util.List<Long> camp = new java.util.ArrayList<>();

    public void note(String s) {
        history.add(s + " (отношения " + relation + ")");
        while (history.size() > 6) history.remove(0);
    }

    /** Характер правителя по воинственности. */
    public String temper() {
        int a = aggression();
        return a >= 55 ? "воинственный" : a >= 38 ? "расчётливый" : "осторожный";
    }
    public long truceUntil = 0;
    public int offer = 0; // Diplomacy.OFFER_*
    public int offerAmount = 0;
    public long offerExpires = 0;
    public boolean playerCb = false;
    public UUID offerTo;
    public long spyDay = -1;
    public int spyOps = 0;
    /** День последнего обмена знаниями с этим королевством. */
    public long sciDay = -100;
    public long armyStart = 0;
    /** Цель войны игрока: 0 дань, 1 вассалитет, 2 слава (трофеи). */
    public int warGoal = 0;
    public static final String[] GOALS = {"дань", "вассалитет", "слава"};

    public Realm(UUID id, String name, String ruler, int culture, int x, int z) {
        this.id = id;
        this.name = name;
        this.ruler = ruler;
        this.culture = culture;
        this.x = x;
        this.z = z;
    }

    public BlockPos capital() {
        return new BlockPos(x, y, z);
    }

    public String stateTitle() {
        return switch (state) {
            case WAR -> "война";
            case VASSAL -> "ваш вассал";
            default -> relation >= 50 ? "дружба" : relation >= 15 ? "добрососедство" : relation > -30 ? "нейтралитет" : "неприязнь";
        };
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", id);
        t.putString("name", name);
        t.putString("ruler", ruler);
        t.putInt("culture", culture);
        t.putInt("x", x);
        t.putInt("y", y);
        t.putInt("z", z);
        t.putBoolean("built", built);
        t.putInt("state", state);
        t.putInt("relation", relation);
        t.putBoolean("trade", trade);
        t.putBoolean("ally", ally);
        t.putInt("known", known);
        t.putInt("strength", strength);
        t.putLong("armyTimer", armyTimer);
        t.putInt("sent", armiesSent);
        t.putInt("lost", armiesLost);
        if (warOwner != null) t.putUUID("warOwner", warOwner);
        if (liege != null) t.putUUID("liege", liege);
        t.putInt("agents", agents);
        t.putInt("intel", intel);
        t.putInt("suspicion", suspicion);
        t.putBoolean("bribed", bribed);
        t.putBoolean("siegeSab", siegeSabotaged);
        t.putLong("lastSpyDay", lastSpyDay);
        t.putInt("aggression", aggression);
        net.minecraft.nbt.ListTag hl = new net.minecraft.nbt.ListTag();
        for (String h : history) hl.add(net.minecraft.nbt.StringTag.valueOf(h));
        t.put("hist", hl);
        t.putLongArray("camp", camp.stream().mapToLong(Long::longValue).toArray());
        t.putLong("truceUntil", truceUntil);
        t.putInt("offer", offer);
        t.putInt("offerAmount", offerAmount);
        t.putLong("offerExpires", offerExpires);
        t.putBoolean("playerCb", playerCb);
        if (offerTo != null) t.putUUID("offerTo", offerTo);
        t.putLong("spyDay", spyDay);
        t.putInt("spyOps", spyOps);
        t.putLong("sciDay", sciDay);
        t.putLong("armyStart", armyStart);
        t.putInt("warGoal", warGoal);
        return t;
    }

    public static Realm load(CompoundTag t) {
        Realm r = new Realm(t.getUUID("id"), t.getString("name"), t.getString("ruler"), t.getInt("culture"), t.getInt("x"), t.getInt("z"));
        r.y = t.getInt("y");
        r.built = t.getBoolean("built");
        r.state = t.getInt("state");
        r.relation = t.getInt("relation");
        r.trade = t.getBoolean("trade");
        r.ally = t.getBoolean("ally");
        r.known = t.getInt("known");
        r.strength = Math.max(4, t.getInt("strength"));
        r.armyTimer = t.getLong("armyTimer");
        r.armiesSent = t.getInt("sent");
        r.armiesLost = t.getInt("lost");
        if (t.hasUUID("warOwner")) r.warOwner = t.getUUID("warOwner");
        if (t.hasUUID("liege")) r.liege = t.getUUID("liege");
        r.agents = t.getInt("agents");
        r.intel = t.getInt("intel");
        r.suspicion = t.getInt("suspicion");
        r.bribed = t.getBoolean("bribed");
        r.siegeSabotaged = t.getBoolean("siegeSab");
        r.lastSpyDay = t.contains("lastSpyDay") ? t.getLong("lastSpyDay") : -1;
        r.aggression = t.contains("aggression") ? t.getInt("aggression") : -1;
        r.truceUntil = t.getLong("truceUntil");
        for (net.minecraft.nbt.Tag h : t.getList("hist", net.minecraft.nbt.Tag.TAG_STRING)) r.history.add(h.getAsString());
        for (long l : t.getLongArray("camp")) r.camp.add(l);
        r.offer = t.getInt("offer");
        r.offerAmount = t.getInt("offerAmount");
        r.offerExpires = t.getLong("offerExpires");
        r.playerCb = t.getBoolean("playerCb");
        if (t.hasUUID("offerTo")) r.offerTo = t.getUUID("offerTo");
        r.spyDay = t.contains("spyDay") ? t.getLong("spyDay") : -1;
        r.spyOps = t.getInt("spyOps");
        r.sciDay = t.contains("sciDay") ? t.getLong("sciDay") : -100;
        r.warGoal = t.getInt("warGoal");
        r.armyStart = t.getLong("armyStart");
        return r;
    }

    /** Воинственность правителя (инициализируется по культуре и имени). */
    public int aggression() {
        if (aggression < 0) aggression = 20 + Math.floorMod(id.hashCode(), 50) + (culture == 3 || culture == 5 ? 15 : 0);
        return aggression;
    }
}
