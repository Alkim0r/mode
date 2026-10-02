package com.alkimor.regnum.kingdom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Все города мира. Хранится в data/regnum_kingdoms.dat верхнего мира. */
public class KingdomData extends SavedData {
    private static final String FILE = "regnum_kingdoms";
    private final Map<UUID, City> cities = new LinkedHashMap<>();
    private final Map<UUID, Realm> realms = new LinkedHashMap<>();
    private final Map<UUID, VassalVillage> villages = new LinkedHashMap<>();
    /** Событие «Армия ночи»: 0 — спит, 1 — отсчёт, 2 — нашествие, 3 — отбито. */
    public int nightStage = 0;
    public long nightArrival = 0;
    public int nightWave = 0;
    public UUID nightCity;

    public static KingdomData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(KingdomData::new, KingdomData::load), FILE);
    }

    private static KingdomData load(CompoundTag tag, HolderLookup.Provider provider) {
        KingdomData d = new KingdomData();
        for (Tag t : tag.getList("cities", Tag.TAG_COMPOUND)) {
            City c = City.load((CompoundTag) t);
            d.cities.put(c.id, c);
        }
        d.nightStage = tag.getInt("nightStage");
        d.nightArrival = tag.getLong("nightArrival");
        d.nightWave = tag.getInt("nightWave");
        if (tag.hasUUID("nightCity")) d.nightCity = tag.getUUID("nightCity");
        for (Tag t : tag.getList("realms", Tag.TAG_COMPOUND)) {
            Realm r = Realm.load((CompoundTag) t);
            d.realms.put(r.id, r);
        }
        for (Tag t : tag.getList("villages", Tag.TAG_COMPOUND)) {
            VassalVillage v = VassalVillage.load((CompoundTag) t);
            d.villages.put(v.id, v);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (City c : cities.values()) list.add(c.save());
        tag.put("cities", list);
        ListTag rl = new ListTag();
        for (Realm r : realms.values()) rl.add(r.save());
        tag.put("realms", rl);
        ListTag vl = new ListTag();
        for (VassalVillage v : villages.values()) vl.add(v.save());
        tag.put("villages", vl);
        tag.putInt("nightStage", nightStage);
        tag.putLong("nightArrival", nightArrival);
        tag.putInt("nightWave", nightWave);
        if (nightCity != null) tag.putUUID("nightCity", nightCity);
        return tag;
    }

    public Collection<VassalVillage> villages() {
        return villages.values();
    }

    public void addVillage(VassalVillage v) {
        villages.put(v.id, v);
        setDirty();
    }

    public void removeVillage(VassalVillage v) {
        villages.remove(v.id);
        setDirty();
    }

    public Collection<Realm> realms() {
        return realms.values();
    }

    @Nullable
    public Realm realm(@Nullable UUID id) {
        return id == null ? null : realms.get(id);
    }

    public void addRealm(Realm r) {
        realms.put(r.id, r);
        setDirty();
    }

    public Collection<City> all() {
        return cities.values();
    }

    @Nullable
    public City byId(@Nullable UUID id) {
        return id == null ? null : cities.get(id);
    }

    @Nullable
    public City byHall(BlockPos hall) {
        for (City c : cities.values()) if (c.hall.equals(hall)) return c;
        return null;
    }

    /** Город, на территории которого находится точка. */
    @Nullable
    public City at(BlockPos pos) {
        for (City c : cities.values()) if (c.contains(pos)) return c;
        return null;
    }

    public List<City> ownedBy(UUID owner) {
        List<City> res = new ArrayList<>();
        for (City c : cities.values()) if (c.owner.equals(owner)) res.add(c);
        return res;
    }

    @Nullable
    public City nearestOwned(UUID owner, BlockPos pos) {
        City best = null;
        double bd = Double.MAX_VALUE;
        for (City c : ownedBy(owner)) {
            double d = c.hall.distSqr(pos);
            if (d < bd) {
                bd = d;
                best = c;
            }
        }
        return best;
    }

    /** Не слишком ли близко к чужому/своему городу (территории не должны пересекаться). */
    public boolean tooClose(BlockPos pos, int newRadius) {
        for (City c : cities.values()) {
            double dx = c.hall.getX() - pos.getX(), dz = c.hall.getZ() - pos.getZ();
            double min = c.radius() + newRadius + 8;
            if (dx * dx + dz * dz < min * min) return true;
        }
        return false;
    }

    public void add(City c) {
        cities.put(c.id, c);
        setDirty();
    }

    public void remove(City c) {
        cities.remove(c.id);
        setDirty();
    }

    public void onSoldierLost(@Nullable UUID cityId, UUID soldier) {
        City c = byId(cityId);
        if (c != null && c.soldiers.remove(soldier) != null) setDirty();
    }
}
