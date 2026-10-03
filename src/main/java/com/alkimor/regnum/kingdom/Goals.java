package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Финальные цели кампании (I096): технологическое превосходство, союз держав, военное господство.
 * Достижение объявляется один раз; игра после финала продолжается (мир остаётся открытым). Отметки в памяти.
 */
public final class Goals {
    private Goals() {}

    public static final String[] NAMES = {"Технологическое превосходство", "Союз держав", "Военное господство"};
    public static final int ALLIES_NEEDED = 3, VASSALS_NEEDED = 3, TECHS_NEEDED = 20;
    private static final Set<String> ANNOUNCED = new HashSet<>();

    public static boolean techDone(int techs, boolean topTech) { return topTech && techs >= TECHS_NEEDED; }
    public static boolean allianceDone(int allies) { return allies >= ALLIES_NEEDED; }
    public static boolean conquestDone(int vassals) { return vassals >= VASSALS_NEEDED; }

    /** Прогресс цели 0..100 для вывода игроку. */
    public static int percent(int have, int need) { return Math.max(0, Math.min(100, have * 100 / Math.max(1, need))); }

    static int[] counts(net.minecraft.server.MinecraftServer server, UUID owner) {
        var k = Science.get(server).of(owner);
        int allies = 0, vassals = 0;
        for (Realm r : KingdomData.get(server).realms()) {
            if (!r.built) continue;
            if (r.ally && r.state == Realm.PEACE) allies++;
            if (r.state == Realm.VASSAL && owner.equals(r.liege)) vassals++;
        }
        return new int[]{k.done.size(), k.done.contains(Science.Tech.ARTILLERY) ? 1 : 0, allies, vassals};
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 100 != 83 || !DayClock.newDay(e.getServer(), "goals")) return;
        for (UUID owner : KingdomData.get(e.getServer()).all().stream().map(c -> c.owner).distinct().toList()) {
            int[] n = counts(e.getServer(), owner);
            boolean[] ok = {techDone(n[0], n[1] == 1), allianceDone(n[2]), conquestDone(n[3])};
            for (int i = 0; i < ok.length; i++) {
                if (!ok[i] || !ANNOUNCED.add(owner + ":" + i)) continue;
                ServerPlayer p = e.getServer().getPlayerList().getPlayer(owner);
                if (p != null) {
                    Text.gold(p, "══ ЦЕЛЬ КАМПАНИИ ДОСТИГНУТА: " + NAMES[i] + " ══");
                    Chronicle.add(owner, "достигнута цель кампании: " + NAMES[i]);
                    Text.info(p, "Мир остаётся открытым: можно продолжать играть и ставить новые цели.");
                }
            }
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("цели").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            int[] n = counts(p.server, p.getUUID());
            Text.gold(p, "══ Цели кампании ══");
            Text.info(p, NAMES[0] + ": технологий " + n[0] + "/" + TECHS_NEEDED + ", артиллерия " + (n[1] == 1 ? "есть" : "нет") + " (" + (techDone(n[0], n[1] == 1) ? "ДОСТИГНУТО" : percent(n[0], TECHS_NEEDED) + "%") + ")");
            Text.info(p, NAMES[1] + ": союзников " + n[2] + "/" + ALLIES_NEEDED + " (" + (allianceDone(n[2]) ? "ДОСТИГНУТО" : percent(n[2], ALLIES_NEEDED) + "%") + ")");
            Text.info(p, NAMES[2] + ": вассалов " + n[3] + "/" + VASSALS_NEEDED + " (" + (conquestDone(n[3]) ? "ДОСТИГНУТО" : percent(n[3], VASSALS_NEEDED) + "%") + ")");
            return 1;
        })));
    }
}
