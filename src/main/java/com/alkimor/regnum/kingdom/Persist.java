package com.alkimor.regnum.kingdom;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Сохранение «лёгких» состояний систем королевства: путь каравана, курс науки, политики управляющего, сроки договоров,
 * давление и сведения рынка, летопись и объявленные цели. Раз в минуту и при остановке сервера пишется в мир, при запуске читается.
 */
public class Persist extends SavedData {
    private static final String FILE = "regnum_persist";
    CompoundTag data = new CompoundTag();

    public static Persist get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Persist::new, Persist::load), FILE);
    }

    private static Persist load(CompoundTag tag, HolderLookup.Provider p) {
        Persist s = new Persist();
        s.data = tag.getCompound("d");
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider p) {
        tag.put("d", data);
        return tag;
    }

    static void collect(MinecraftServer server) {
        Persist s = get(server);
        s.data.put("caravan", CaravanRoute.toTag());
        s.data.put("scifocus", SciFocus.toTag());
        s.data.put("steward", Steward.toTag());
        s.data.put("treaties", Treaties.toTag());
        s.data.put("exchange", Exchange.toTag());
        s.data.put("chronicle", Chronicle.toTag());
        s.data.put("goals", Goals.toTag());
        s.setDirty();
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) {
        CompoundTag d = get(e.getServer()).data;
        CaravanRoute.fromTag(d.getCompound("caravan"));
        SciFocus.fromTag(d.getCompound("scifocus"));
        Steward.fromTag(d.getCompound("steward"));
        Treaties.fromTag(d.getCompound("treaties"));
        Exchange.fromTag(d.getCompound("exchange"));
        Chronicle.fromTag(d.getCompound("chronicle"));
        Goals.fromTag(d.getCompound("goals"));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 1200 == 600) collect(e.getServer());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        collect(e.getServer());
    }
}
