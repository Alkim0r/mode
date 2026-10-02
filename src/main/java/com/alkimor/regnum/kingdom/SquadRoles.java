package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Особые бойцы отряда: знаменосец поднимает боевой дух бойцов рядом, лекарь лечит раненых. Они занимают место в строю,
 * поэтому число ограничено (по одному на каждые 10 бойцов). Назначение стоит 30 монет из казны города.
 */
public final class SquadRoles {
    private SquadRoles() {}

    public static final String BEARER_TAG = "regnum_bearer", MEDIC_TAG = "regnum_medic";
    public static final int COST = 30;

    private static final Map<UUID, SoldierEntity> BEARERS = new HashMap<>(), MEDICS = new HashMap<>();

    /** Прибавка к духу в секунду от ближайшего знаменосца того же владельца (до +1.5). */
    public static float bannerBonus(SoldierEntity s) {
        if (BEARERS.isEmpty()) return 0f;
        for (SoldierEntity b : BEARERS.values()) {
            if (b != s && b.isAlive() && b.level() == s.level() && s.getOwnerId() != null && s.getOwnerId().equals(b.getOwnerId())
                    && b.distanceToSqr(s) < 14 * 14) return 1.5f;
        }
        return 0f;
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide || !(e.getEntity() instanceof SoldierEntity s)) return;
        if (s.getTags().contains(BEARER_TAG)) BEARERS.put(s.getUUID(), s);
        if (s.getTags().contains(MEDIC_TAG)) MEDICS.put(s.getUUID(), s);
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent e) {
        if (e.getLevel().isClientSide || !(e.getEntity() instanceof SoldierEntity s)) return;
        BEARERS.remove(s.getUUID());
        MEDICS.remove(s.getUUID());
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (MEDICS.isEmpty() || e.getServer().getTickCount() % 40 != 11) return;
        for (SoldierEntity m : new ArrayList<>(MEDICS.values())) {
            if (!m.isAlive() || !(m.level() instanceof ServerLevel sl)) { MEDICS.remove(m.getUUID()); continue; }
            for (SoldierEntity a : sl.getEntitiesOfClass(SoldierEntity.class, m.getBoundingBox().inflate(8),
                    x -> x != m && x.isAlive() && x.getHealth() < x.getMaxHealth() && m.getOwnerId() != null && m.getOwnerId().equals(x.getOwnerId()))) {
                a.heal(2f);
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, a.getX(), a.getY() + 1.6, a.getZ(), 1, 0.2, 0.2, 0.2, 0);
            }
        }
    }

    /** Назначает ближайшего свободного бойца ролью. Возвращает сообщение об ошибке или null. */
    public static String assign(ServerPlayer p, boolean bearer) {
        KingdomData data = KingdomData.get(p.server);
        City c = data.nearestOwned(p.getUUID(), p.blockPosition());
        if (c == null) return "Рядом нет вашего города: казне нечем платить.";
        List<SoldierEntity> mine = p.serverLevel().getEntitiesOfClass(SoldierEntity.class, p.getBoundingBox().inflate(40),
                s -> s.isAlive() && s.isOwnedBy(p));
        int have = 0;
        for (SoldierEntity s : mine) if (s.getTags().contains(bearer ? BEARER_TAG : MEDIC_TAG)) have++;
        if (have >= Math.max(1, mine.size() / 10)) return "Таких бойцов уже " + have + " — по одному на каждые 10 бойцов.";
        if (c.treasury < COST) return "В казне нужно " + COST + " монет.";
        SoldierEntity pick = null;
        double best = 36;
        for (SoldierEntity s : mine) {
            if (s.getTags().contains(BEARER_TAG) || s.getTags().contains(MEDIC_TAG) || s.getSoldierType().ranged()) continue;
            double d = s.distanceToSqr(p);
            if (d < best) { best = d; pick = s; }
        }
        if (pick == null) return "В 6 блоках нет подходящего бойца (не стрелка и не со второй ролью).";
        c.treasury -= COST;
        data.setDirty();
        pick.addTag(bearer ? BEARER_TAG : MEDIC_TAG);
        pick.setCustomName(Text.of(bearer ? "Знаменосец" : "Лекарь", bearer ? ChatFormatting.GOLD : ChatFormatting.GREEN));
        if (bearer) BEARERS.put(pick.getUUID(), pick); else MEDICS.put(pick.getUUID(), pick);
        return null;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("squadrole")
                .then(Commands.literal("знаменосец").executes(ctx -> run(ctx.getSource().getPlayerOrException(), true)))
                .then(Commands.literal("лекарь").executes(ctx -> run(ctx.getSource().getPlayerOrException(), false)))));
    }

    private static int run(ServerPlayer p, boolean bearer) {
        String err = assign(p, bearer);
        if (err != null) { Text.bad(p, err); return 0; }
        Text.good(p, bearer ? "Знаменосец назначен: бойцы рядом держатся стойче." : "Лекарь назначен: раненые рядом поправляются.");
        return 1;
    }
}
