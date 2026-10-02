package com.alkimor.regnum.kingdom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/** Деревня-вассал: платит дань городу игрока, пока её защищают. */
public class VassalVillage {
    public final UUID id;
    public String name;
    public int x, y, z;
    public UUID owner;
    /** До этого времени (игровые тики) дань не платится: деревню недавно ограбили. */
    public long unrestUntil = 0;
    public int raidsRepelled = 0, raidsLost = 0;
    /** Преданность жителей 0..100: ниже 20 — смута (нет дани), ниже 8 — деревня уходит. */
    public int loyalty = 55;
    /** Налог: 0 — щадящий, 1 — обычный, 2 — тяжёлый. */
    public int tax = 1;
    /** Постоянный гарнизон ополчения (до 6) — защищает при набеге и идёт в левую на войну. */
    public int garrison = 0;
    /** Частокол/ров: 0..2 — уменьшает налёты, повышает верность. */
    public int palisade = 0;
    /** Тягот в этом сезоне (голод): сколько дней осталось. */
    public int famineDays = 0;
    public boolean levyCalled = false;
    /** Назначение аванпоста: 0 обычная, 1 торговый, 2 разведывательный, 3 военный, 4 медицинский. */
    public int purpose = 0;
    public static final String[] PURPOSES = {"обычная", "торговый пост", "разведпост", "военный форпост", "лазарет-аванпост"};
    /** Число жителей рядом (обновляется сервером, не сохраняется). */
    public transient int villagers = 0;

    public VassalVillage(UUID id, String name, BlockPos pos, UUID owner) {
        this.id = id;
        this.name = name;
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.owner = owner;
    }

    public BlockPos pos() {
        return new BlockPos(x, y, z);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", id);
        t.putString("name", name);
        t.putInt("x", x);
        t.putInt("y", y);
        t.putInt("z", z);
        t.putUUID("owner", owner);
        t.putLong("unrest", unrestUntil);
        t.putInt("won", raidsRepelled);
        t.putInt("lost", raidsLost);
        t.putInt("loyalty", loyalty);
        t.putInt("tax", tax);
        t.putInt("garrison", garrison);
        t.putInt("palisade", palisade);
        t.putInt("famine", famineDays);
        t.putInt("purpose", purpose);
        return t;
    }

    public static VassalVillage load(CompoundTag t) {
        VassalVillage v = new VassalVillage(t.getUUID("id"), t.getString("name"), new BlockPos(t.getInt("x"), t.getInt("y"), t.getInt("z")), t.getUUID("owner"));
        v.unrestUntil = t.getLong("unrest");
        v.raidsRepelled = t.getInt("won");
        v.raidsLost = t.getInt("lost");
        v.loyalty = t.contains("loyalty") ? t.getInt("loyalty") : 55;
        v.tax = t.contains("tax") ? t.getInt("tax") : 1;
        v.garrison = t.getInt("garrison");
        v.palisade = t.getInt("palisade");
        v.famineDays = t.getInt("famine");
        v.purpose = t.getInt("purpose");
        return v;
    }
}
