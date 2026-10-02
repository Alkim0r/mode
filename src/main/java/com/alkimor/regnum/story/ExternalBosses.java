package com.alkimor.regnum.story;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.kingdom.City;
import com.alkimor.regnum.kingdom.KingdomData;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Необязательный мост «внешний босс → Regnum». Босс из мода-спутника описывается по registry id в файле
 * config/regnum_external_bosses.json; никаких классов чужого мода не нужно, мод может быть и не установлен.
 * Первое убийство даёт награду каждому игроку в радиусе 48 блоков (не только нанёсшему удар): запись в хронике,
 * опыт, изумруды в казну ближайшего города, репутация фракции и засчитывание в KILL-квестах. Повторные убийства
 * награды не дают.
 */
public final class ExternalBosses {
    private ExternalBosses() {}

    public record Entry(String id, String title, int emeralds, int steward, String faction, int rep) {}

    private static Map<String, Entry> table = null;

    /** Разбор JSON-массива вида [{"id":"mod:boss","title":"..","emeralds":40,"steward":30,"faction":"x","rep":10}]. */
    public static Map<String, Entry> parse(String json) {
        Map<String, Entry> m = new HashMap<>();
        JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            if (!o.has("id")) continue;
            String id = o.get("id").getAsString();
            m.put(id, new Entry(id, o.has("title") ? o.get("title").getAsString() : id,
                    o.has("emeralds") ? o.get("emeralds").getAsInt() : 0, o.has("steward") ? o.get("steward").getAsInt() : 0,
                    o.has("faction") ? o.get("faction").getAsString() : "", o.has("rep") ? o.get("rep").getAsInt() : 0));
        }
        return m;
    }

    private static Map<String, Entry> table() {
        if (table != null) return table;
        table = new HashMap<>();
        try {
            Path f = FMLPaths.CONFIGDIR.get().resolve("regnum_external_bosses.json");
            if (Files.isRegularFile(f)) table = parse(Files.readString(f));
        } catch (Exception e) {
            Regnum.LOGGER.warn("[Regnum] regnum_external_bosses.json не прочитан: {}", e.toString());
        }
        return table;
    }

    /** Награда игроку за первое убийство; false — уже получал или записи нет. */
    public static boolean reward(ServerPlayer p, Entry e) {
        String flag = "ext_" + e.id();
        if (Quests.flag(p, flag)) return false;
        Quests.setFlag(p, flag);
        Text.gold(p, "★ Побеждён «" + e.title() + "»!");
        Skills.chronicle(p, "Побеждён: " + e.title());
        if (e.steward() > 0) Skills.addXp(p, Skill.STEWARD, e.steward());
        if (e.emeralds() > 0) {
            City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
            if (c != null) {
                c.treasury += e.emeralds();
                KingdomData.get(p.server).setDirty();
                Text.good(p, "В казну «" + c.name + "»: +" + e.emeralds() + " изумр.");
            }
        }
        if (!e.faction().isEmpty() && e.rep() != 0) Quests.addRep(p, e.faction(), e.rep());
        return true;
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent ev) {
        LivingEntity v = ev.getEntity();
        if (v.level().isClientSide || table().isEmpty()) return;
        Entry e = table().get(BuiltInRegistries.ENTITY_TYPE.getKey(v.getType()).toString());
        if (e == null) return;
        List<ServerPlayer> group = new ArrayList<>(((net.minecraft.server.level.ServerLevel) v.level()).getPlayers(sp -> sp.distanceToSqr(v) <= 48 * 48));
        if (ev.getSource().getEntity() instanceof ServerPlayer k && !group.contains(k)) group.add(k);
        for (ServerPlayer p : group) {
            if (ev.getSource().getEntity() != p) Quests.creditKill(p, v); // ударивший уже засчитан в Quests.onKill
            reward(p, e);
        }
    }
}
