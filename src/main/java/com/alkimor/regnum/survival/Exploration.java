package com.alkimor.regnum.survival;

import com.alkimor.regnum.core.Text;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Исследование мира: первое посещение каждого биома и каждой структуры засчитывается один раз — опыт «Разведки»,
 * очки знаний (через практические открытия) и запись в летописи. Награда за путешествие, а не за стояние на месте.
 */
public final class Exploration {
    private Exploration() {}

    private static final String BIOMES = "regnum_seen_biomes", STRUCTS = "regnum_seen_structs";

    private static boolean seen(ServerPlayer p, String key, String id) {
        for (Tag t : p.getPersistentData().getList(key, Tag.TAG_STRING)) if (t.getAsString().equals(id)) return true;
        return false;
    }

    /** Записывает новое открытие; true — оно новое. */
    public static boolean record(ServerPlayer p, String key, String id) {
        if (seen(p, key, id)) return false;
        ListTag l = p.getPersistentData().getList(key, Tag.TAG_STRING);
        l.add(StringTag.valueOf(id));
        p.getPersistentData().put(key, l);
        return true;
    }

    public static int count(ServerPlayer p, String key) {
        return p.getPersistentData().getList(key, Tag.TAG_STRING).size();
    }

    public static int biomes(ServerPlayer p) { return count(p, BIOMES); }
    public static int structures(ServerPlayer p) { return count(p, STRUCTS); }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isCreative() || p.isSpectator() || p.tickCount % 60 != 0) return;
        if (!(p.level() instanceof ServerLevel sl)) return;
        var pos = p.blockPosition();
        var biome = sl.getBiome(pos).unwrapKey();
        if (biome.isPresent()) {
            String id = biome.get().location().toString();
            if (record(p, BIOMES, id)) {
                Skills.addXp(p, Skill.SCOUTING, 6f);
                com.alkimor.regnum.kingdom.Discoveries.record(p, "biome:" + id, 2);
                p.displayClientMessage(Text.of("Новые земли: " + biome.get().location().getPath().replace('_', ' ') + " (открыто биомов: " + biomes(p) + ")", net.minecraft.ChatFormatting.AQUA), true);
                if (biomes(p) % 5 == 0) Skills.chronicle(p, "Исследовано биомов: " + biomes(p));
            }
        }
        if (p.tickCount % 120 == 0) {
            for (var e : sl.structureManager().getAllStructuresAt(pos).keySet()) {
                var key = sl.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getKey(e);
                if (key == null) continue;
                String id = key.toString();
                if (record(p, STRUCTS, id)) {
                    Skills.addXp(p, Skill.SCOUTING, 10f);
                    com.alkimor.regnum.kingdom.Discoveries.record(p, "struct:" + id, 5);
                    p.displayClientMessage(Text.of("Найдено место: " + key.getPath().replace('_', ' ') + " (открыто мест: " + structures(p) + ")", net.minecraft.ChatFormatting.GOLD), true);
                    Skills.chronicle(p, "Найдено: " + key.getPath().replace('_', ' '));
                }
            }
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("explore").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Text.gold(p, "══ Исследования ══");
            Text.info(p, "Открыто биомов: " + biomes(p) + ", мест (структур): " + structures(p) + ".");
            Text.info(p, "Первое посещение даёт опыт «Разведки» и очки знаний. Путешествуйте — мир большой.");
            return 1;
        })));
    }
}
