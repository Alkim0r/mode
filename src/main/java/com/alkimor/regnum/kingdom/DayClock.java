package com.alkimor.regnum.kingdom;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Игровые сутки для ежедневных систем: не зависят от времени работы сервера, переживают перезапуск. */
public class DayClock extends SavedData {
    private final Map<String, Long> last = new HashMap<>();

    public static DayClock get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(DayClock::new, DayClock::load), "regnum_clock");
    }

    private static DayClock load(CompoundTag tag, HolderLookup.Provider p) {
        DayClock c = new DayClock();
        CompoundTag m = tag.getCompound("last");
        for (String k : m.getAllKeys()) c.last.put(k, m.getLong(k));
        return c;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider p) {
        CompoundTag m = new CompoundTag();
        last.forEach(m::putLong);
        tag.put("last", m);
        return tag;
    }

    /** true один раз за игровые сутки для ключа (при первом обращении только запоминает день). */
    public static boolean newDay(MinecraftServer server, String key) {
        long day = server.overworld().getDayTime() / 24000L;
        DayClock c = get(server);
        Long l = c.last.get(key);
        if (l == null) {
            c.last.put(key, day);
            c.setDirty();
            return false;
        }
        if (day <= l) return false;
        c.last.put(key, day);
        c.setDirty();
        return true;
    }
}
