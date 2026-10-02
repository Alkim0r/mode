package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Разбор сражения: пока идёт бой, считаем потери, победы и лучшего бойца; через 30 секунд тишины владелец получает краткий отчёт.
 */
public final class BattleReport {
    private BattleReport() {}

    public static final class Tally {
        int kills, losses, bossKills;
        long last;
        final EnumMap<SoldierType, Integer> lost = new EnumMap<>(SoldierType.class);
        final Map<UUID, Integer> byUnit = new HashMap<>();
        final Map<UUID, String> unitName = new HashMap<>();
        UUID topPlayer;
        int playerKills;
    }

    private static final Map<UUID, Tally> TALLIES = new HashMap<>();

    private static Tally tally(UUID owner, long now) {
        Tally t = TALLIES.computeIfAbsent(owner, k -> new Tally());
        t.last = now;
        return t;
    }

    private static boolean isBoss(LivingEntity e) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
        return id.equals("crypt_lord") || id.equals("mire_mother") || id.equals("forgemaster") || id.equals("scarab_queen");
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        long now = dead.level().getGameTime();
        if (dead instanceof SoldierEntity s && s.getOwnerId() != null) {
            Tally t = tally(s.getOwnerId(), now);
            t.losses++;
            t.lost.merge(s.getSoldierType(), 1, Integer::sum);
            return;
        }
        if (!(dead instanceof Enemy) && !(dead instanceof BanditEntity) && !(isBoss(dead))) return;
        var src = e.getSource().getEntity();
        if (src instanceof SoldierEntity s && s.getOwnerId() != null) {
            Tally t = tally(s.getOwnerId(), now);
            t.kills++;
            t.byUnit.merge(s.getUUID(), 1, Integer::sum);
            t.unitName.put(s.getUUID(), s.getName().getString());
            if (isBoss(dead)) t.bossKills++;
        } else if (src instanceof ServerPlayer p) {
            Tally t = tally(p.getUUID(), now);
            t.kills++;
            t.playerKills++;
            if (isBoss(dead)) t.bossKills++;
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (TALLIES.isEmpty() || e.getServer().getTickCount() % 40 != 17) return;
        MinecraftServer server = e.getServer();
        long now = server.overworld().getGameTime();
        Iterator<Map.Entry<UUID, Tally>> it = TALLIES.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            Tally t = en.getValue();
            if (now - t.last < 600) continue;
            it.remove();
            if (t.kills + t.losses < 3) continue;
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            if (p != null) send(p, t);
        }
    }

    public static String summary(Tally t) {
        StringBuilder sb = new StringBuilder();
        sb.append("Побед: ").append(t.kills);
        if (t.bossKills > 0) sb.append(" (боссов: ").append(t.bossKills).append(")");
        sb.append(", потерь: ").append(t.losses);
        if (!t.lost.isEmpty()) {
            sb.append(" [");
            boolean first = true;
            for (var l : t.lost.entrySet()) {
                sb.append(first ? "" : ", ").append(l.getKey().title.toLowerCase()).append(" ×").append(l.getValue());
                first = false;
            }
            sb.append("]");
        }
        UUID best = null;
        int bk = 0;
        for (var b : t.byUnit.entrySet()) if (b.getValue() > bk) { bk = b.getValue(); best = b.getKey(); }
        if (best != null && bk >= 2) sb.append(". Лучший боец: ").append(t.unitName.get(best)).append(" (").append(bk).append(" побед)");
        if (t.playerKills > 0) sb.append(". Лично вами повержено: ").append(t.playerKills);
        return sb.toString();
    }

    private static void send(ServerPlayer p, Tally t) {
        Text.gold(p, "⚔ Разбор сражения");
        Text.info(p, summary(t));
        String hint = t.losses > t.kills ? "Потерь больше побед: усилите строй, возьмите щитников и лекарей, не лезьте без разведки." :
                t.losses == 0 ? "Победа без потерь — строй держится отлично." : "Победа достигнута; раненых отведите в лазарет.";
        Text.info(p, hint);
    }

    /** Для тестов: принудительно завершить сбор и вернуть текст. */
    public static String flushFor(UUID owner) {
        Tally t = TALLIES.remove(owner);
        return t == null ? null : summary(t);
    }

    public static void record(UUID owner, boolean loss, SoldierType type, long now) {
        Tally t = tally(owner, now);
        if (loss) {
            t.losses++;
            t.lost.merge(type, 1, Integer::sum);
        } else t.kills++;
    }
}
