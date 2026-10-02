package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * «Превратности судьбы»: малые случайные события жизни города, которые трогают разные системы — урожай, учёный-странник,
 * дезертиры, купцы, метеорит, ржавчина. Раз в сутки у каждого владельца с шансом 40%. Не при набеге, осаде и эпидемии.
 */
public final class Fortune {
    private Fortune() {}

    public enum Kind {
        HARVEST, SCHOLAR, DESERTERS, MERCHANTS, METEOR, BLIGHT, BOUNTY, PLEA
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 100 != 53 || !DayClock.newDay(server, "fortune")) return;
        KingdomData data = KingdomData.get(server);
        Random r = new Random(server.overworld().getDayTime() / 24000L * 7919L);
        List<UUID> owners = data.all().stream().map(c -> c.owner).distinct().toList();
        for (UUID owner : owners) {
            ServerPlayer p = server.getPlayerList().getPlayer(owner);
            if (p == null || r.nextInt(100) >= 40) continue;
            City c = best(data, owner);
            if (c == null || c.war || c.raidActive || c.plagueDays > 0) continue;
            fire(p, c, Kind.values()[r.nextInt(Kind.values().length)], r);
        }
    }

    private static City best(KingdomData data, UUID owner) {
        City b = null;
        for (City c : data.ownedBy(owner)) if (b == null || c.level > b.level) b = c;
        return b;
    }

    /** Применить событие; возвращает true, если оно произошло. */
    public static boolean fire(ServerPlayer p, City c, Kind k, Random r) {
        ServerLevel ow = p.serverLevel();
        switch (k) {
            case HARVEST -> {
                int n = 50 + r.nextInt(70) + 10 * c.level;
                int got = c.deposit(Resource.FOOD, n);
                Text.good(p, "🌾 Богатый урожай в «" + c.name + "»: на склад поступило " + got + " провианта.");
            }
            case SCHOLAR -> {
                Science sc = Science.get(p.server);
                Science.Kingdom kg = sc.of(p.getUUID());
                if (kg.current == null) return false;
                int pts = 8 + r.nextInt(10) + 2 * c.level;
                Science.Tech done = sc.addPoints(p.getUUID(), pts);
                Text.gold(p, "📜 В «" + c.name + "» остановился учёный-странник: +" + pts + " очков знаний"
                        + (done != null ? " — изучено «" + done.title + "»!" : "."));
            }
            case DESERTERS -> {
                int n = 2 + r.nextInt(3);
                int cap = c.armyCap(Skills.armyBonus(p));
                if (c.armyUsed() + n > cap) return false;
                int added = 0;
                for (int i = 0; i < n; i++) {
                    SoldierEntity s = KingdomModule.SOLDIER.get().create(ow);
                    if (s == null) continue;
                    BlockPos q = c.hall.offset(r.nextInt(9) - 4, 0, r.nextInt(9) - 4);
                    int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, q.getX(), q.getZ());
                    s.moveTo(q.getX() + 0.5, y, q.getZ() + 0.5);
                    s.finalizeSpawn(ow, ow.getCurrentDifficultyAt(q), MobSpawnType.EVENT, null);
                    s.setup(SoldierType.MILITIA, p.getUUID(), c.id, c.recruitSquad);
                    s.command(Order.HOLD, Vec3.atBottomCenterOf(new BlockPos(q.getX(), y, q.getZ())), 0f, Formation.LOOSE, i, n, null);
                    ow.addFreshEntity(s);
                    c.soldiers.put(s.getUUID(), SoldierType.MILITIA.ordinal());
                    added++;
                }
                if (added == 0) return false;
                Text.good(p, "⚔ К «" + c.name + "» пришли дезертиры соседнего владения: ополченцев в отряд — " + added + ".");
            }
            case MERCHANTS -> {
                int gold = 20 + r.nextInt(30) + 5 * c.level;
                c.treasury += gold;
                int iron = c.deposit(Resource.IRON, 8 + r.nextInt(12));
                Text.good(p, "🐪 Проезжие купцы заплатили пошлину: +" + gold + " в казну, +" + iron + " железа на склад.");
            }
            case METEOR -> {
                int iron = c.deposit(Resource.IRON, 20 + r.nextInt(20));
                int stone = c.deposit(Resource.STONE, 30 + r.nextInt(30));
                Text.gold(p, "☄ Рядом с «" + c.name + "» упал метеорит: железо +" + iron + ", камень +" + stone + ".");
            }
            case BLIGHT -> {
                int have = c.stock(Resource.FOOD);
                if (have < 40) return false;
                int loss = have / 4;
                c.take(Resource.FOOD, loss);
                Text.bad(p, "🍂 Ржавчина на полях «" + c.name + "»: потеряно " + loss + " провианта. Склады и лазарет не помогут — копите запас.");
            }
            case PLEA -> {
                if (!Villages.plea(ow, p)) return false;
            }
            case BOUNTY -> {
                boolean ok = VillageAid.start(ow, p, ow.getGameTime());
                if (!ok) return false;
            }
        }
        KingdomData.get(p.server).setDirty();
        Skills.addXp(p, Skill.STEWARD, 2);
        return true;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("fortune").requires(s -> s.hasPermission(2))
                .then(Commands.argument("kind", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null) return 0;
                    String n = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "kind");
                    for (Kind k : Kind.values()) {
                        if (k.name().equalsIgnoreCase(n)) return fire(p, c, k, new Random()) ? 1 : 0;
                    }
                    Text.info(p, "Виды: " + java.util.Arrays.toString(Kind.values()));
                    return 0;
                }))));
    }
}
