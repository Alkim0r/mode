package com.alkimor.regnum.kingdom;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Военачальники: король назначает игроков ответственными за род войск.
 * У каждого свой «авторитет» (репутация среди войск): растёт от побед его бойцов,
 * определяет звание, лимит подчинённых и боевой дух отряда.
 */
public class Commissions extends SavedData {
    private static final String FILE = "regnum_commissions";

    public enum Branch {
        INFANTRY("Пехота"), CAVALRY("Конница"), ARCHERS("Стрелки"), AIR("Воздушные силы"), SIEGE("Осада");

        public final String title;

        Branch(String title) {
            this.title = title;
        }

        public static Branch byName(String n) {
            for (Branch b : values()) if (b.name().equalsIgnoreCase(n)) return b;
            return null;
        }

        public boolean accepts(SoldierType t) {
            if (this == AIR) return t.ranged() && !t.mounted();
            return t.branch() == this;
        }
    }

    public static final int[] REP_TIERS = {0, 20, 60, 140, 300, 600, 1000};
    public static final String[] RANKS = {"Десятник", "Сотник", "Хорунжий", "Тысяцкий", "Воевода", "Маршал", "Великий маршал"};

    public static class Commission {
        public UUID commander;
        public UUID king;
        public Branch branch;
        public int rep;
        public int victories;

        public int tier() {
            int t = 0;
            for (int i = 0; i < REP_TIERS.length; i++) if (rep >= REP_TIERS[i]) t = i;
            return t;
        }

        public String rank() {
            return RANKS[tier()];
        }

        /** Сколько бойцов может вести военачальник: 12 + 4 за каждое звание (до 36). */
        public int cap() {
            return 12 + 4 * tier();
        }

        /** Бонус боевого духа подчинённым. */
        public float moraleBonus() {
            return 3f * tier();
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("commander", commander);
            t.putUUID("king", king);
            t.putString("branch", branch.name());
            t.putInt("rep", rep);
            t.putInt("victories", victories);
            return t;
        }

        static Commission load(CompoundTag t) {
            Commission c = new Commission();
            c.commander = t.getUUID("commander");
            c.king = t.getUUID("king");
            c.branch = Branch.byName(t.getString("branch"));
            if (c.branch == null) c.branch = Branch.INFANTRY;
            c.rep = t.getInt("rep");
            c.victories = t.getInt("victories");
            return c;
        }
    }

    private final Map<UUID, Commission> map = new LinkedHashMap<>();

    public static Commissions get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Commissions::new, Commissions::load), FILE);
    }

    private static Commissions load(CompoundTag tag, HolderLookup.Provider p) {
        Commissions d = new Commissions();
        for (Tag t : tag.getList("list", Tag.TAG_COMPOUND)) {
            Commission c = Commission.load((CompoundTag) t);
            d.map.put(c.commander, c);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider p) {
        ListTag l = new ListTag();
        for (Commission c : map.values()) l.add(c.save());
        tag.put("list", l);
        return tag;
    }

    @Nullable
    public Commission of(UUID commander) {
        return map.get(commander);
    }

    public Collection<Commission> all() {
        return map.values();
    }

    public Commission appoint(UUID king, UUID commander, Branch b) {
        Commission c = map.get(commander);
        if (c == null) {
            c = new Commission();
            c.commander = commander;
            map.put(commander, c);
        }
        c.king = king;
        c.branch = b;
        setDirty();
        return c;
    }

    public void dismiss(UUID commander) {
        map.remove(commander);
        setDirty();
    }

    public void addRep(UUID commander, int amount) {
        Commission c = map.get(commander);
        if (c == null) return;
        c.rep += amount;
        setDirty();
    }
}
