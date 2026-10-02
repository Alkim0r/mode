package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Map;

/**
 * Практические открытия: первая добыча редкой руды и первая победа над боссом дают разовый вклад в науку.
 * Каждый образец засчитывается игроку один раз, поэтому это поощрение исследования, а не источник бесконечных очков.
 */
public final class Discoveries {
    private Discoveries() {}

    private static final String KEY = "regnum_samples";
    /** Максимум очков, которые можно накопить «про запас», пока нет текущего исследования. */
    public static final int BANK_CAP = 60;

    private static final Map<String, Integer> ORES = Map.ofEntries(
            Map.entry("coal_ore", 2), Map.entry("copper_ore", 3), Map.entry("iron_ore", 4), Map.entry("redstone_ore", 4),
            Map.entry("lapis_ore", 4), Map.entry("gold_ore", 6), Map.entry("emerald_ore", 8), Map.entry("diamond_ore", 10),
            Map.entry("ancient_debris", 15), Map.entry("nether_quartz_ore", 4), Map.entry("nether_gold_ore", 5));

    private static final Map<String, Integer> BOSSES = Map.of(
            "crypt_lord", 25, "mire_mother", 30, "forgemaster", 30, "scarab_queen", 35, "crawler_queen", 40);

    /** Засчитывает образец, если он новый. Возвращает полученные очки (0 — уже был или нет смысла). */
    public static int record(ServerPlayer p, String sample, int pts) {
        CompoundTag data = p.getPersistentData();
        ListTag l = data.getList(KEY, Tag.TAG_STRING);
        for (Tag t : l) if (t.getAsString().equals(sample)) return 0;
        l.add(StringTag.valueOf(sample));
        data.put(KEY, l);
        Science sc = Science.get(p.server);
        Science.Tech done = sc.addPoints(p.getUUID(), pts);
        Science.Kingdom k = sc.of(p.getUUID());
        if (done == null && k.current == null) {
            k.progress = Math.min(BANK_CAP, k.progress + pts);
            sc.setDirty();
        }
        Text.info(p, "✦ Образец «" + sample + "» изучен: +" + pts + " очков знаний.");
        if (done != null) Text.gold(p, "✦ Открыта технология «" + done.title + "»! " + done.desc);
        return pts;
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p) || p.isCreative()) return;
        String id = BuiltInRegistries.BLOCK.getKey(e.getState().getBlock()).getPath();
        String key = id.startsWith("deepslate_") ? id.substring(10) : id;
        Integer pts = ORES.get(key);
        if (pts != null) record(p, key, pts);
    }

    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent e) {
        LivingEntity ent = e.getEntity();
        if (ent.level().isClientSide) return;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(ent.getType()).getPath();
        Integer pts = BOSSES.get(id);
        if (pts == null) return;
        for (ServerPlayer p : ((net.minecraft.server.level.ServerLevel) ent.level()).players()) {
            if (!p.isCreative() && p.distanceToSqr(ent) < 40 * 40) record(p, "босс: " + id, pts);
        }
    }
}
