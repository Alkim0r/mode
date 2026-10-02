package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Преемственность командования. Когда игрок (король или военачальник) выходит с сервера, его отряд не остаётся без головы:
 * <ul>
 *   <li>если у бойцов был конкретный приказ (выдвинуться, атаковать цель, в атаку, отступить) — самый опытный боец становится
 *   полевым командиром и ведёт отряд исполнять приказ до конца (идёт на точку помогать союзникам);</li>
 *   <li>если был обычный «следовать за мной» — отряд возвращается в безопасную зону (к ближайшему своему городу), а если города
 *   поблизости нет — разбивает небольшой лагерь с костром на месте выхода и держит оборону.</li>
 * </ul>
 * Когда игрок возвращается, отряд снова встаёт под его командование.
 */
public final class FieldCommand {
    private FieldCommand() {}

    public static final String ORPHAN = "regnum_orphan", LEADER = "regnum_fieldleader";

    private static boolean specific(Order o) {
        return o == Order.MOVE || o == Order.CHARGE || o == Order.ATTACK_TARGET || o == Order.RETREAT;
    }

    public record Result(int total, int pushing, int returning, int camping, SoldierEntity leader) {}

    /** Решает судьбу отряда игрока, вышедшего из игры. Можно вызывать из тестов. */
    public static Result onLeave(ServerLevel lvl, UUID player, Vec3 at) {
        List<SoldierEntity> mine = new ArrayList<>();
        for (SoldierEntity s : lvl.getEntitiesOfClass(SoldierEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(200, 120, 200))) {
            if (!s.isAlive()) continue;
            boolean com = player.equals(s.getCommander());
            boolean king = player.equals(s.getOwnerId()) && s.getCommander() == null;
            if (com || king) mine.add(s);
        }
        if (mine.isEmpty()) return new Result(0, 0, 0, 0, null);
        List<SoldierEntity> doing = new ArrayList<>(), following = new ArrayList<>();
        for (SoldierEntity s : mine) {
            s.addTag(ORPHAN);
            if (specific(s.getOrder())) doing.add(s);
            else if (s.getOrder() == Order.FOLLOW) following.add(s);
        }
        SoldierEntity leader = null;
        if (!doing.isEmpty()) {
            leader = doing.stream().max(Comparator.comparingInt(SoldierEntity::rank).thenComparingDouble(SoldierEntity::getMaxHealth)).orElse(null);
            if (leader != null) {
                leader.addTag(LEADER);
                leader.refreshLeaderName();
            }
        }
        int returning = 0, camping = 0;
        if (!following.isEmpty()) {
            Vec3 c = Vec3.ZERO;
            for (SoldierEntity s : following) c = c.add(s.position());
            c = c.scale(1.0 / following.size());
            UUID owner = following.get(0).getOwnerId();
            City city = owner == null ? null : KingdomData.get(lvl.getServer()).nearestOwned(owner, BlockPos.containing(c));
            boolean home = city != null && city.hall.distSqr(BlockPos.containing(c)) < 200 * 200;
            SoldierEntity lead2 = following.stream().max(Comparator.comparingInt(SoldierEntity::rank).thenComparingDouble(SoldierEntity::getMaxHealth)).orElse(null);
            if (lead2 != null && leader == null) {
                lead2.addTag(LEADER);
                lead2.refreshLeaderName();
                leader = lead2;
            }
            Vec3 camp = c;
            if (!home) camp = lightCamp(lvl, c);
            int n = following.size(), i = 0;
            for (SoldierEntity s : following) {
                double a = Math.PI * 2 * i / n;
                if (home) {
                    Vec3 p = Vec3.atBottomCenterOf(city.hall).add(Math.cos(a) * 4, 1, Math.sin(a) * 4);
                    s.command(Order.RETREAT, p, 0f, Formation.LOOSE, i, n, null);
                    returning++;
                } else {
                    Vec3 p = camp.add(Math.cos(a) * 3.5, 0, Math.sin(a) * 3.5);
                    s.command(Order.HOLD, p, (float) Math.toDegrees(a), Formation.LOOSE, i, n, null);
                    camping++;
                }
                i++;
            }
        }
        return new Result(mine.size(), doing.size(), returning, camping, leader);
    }

    /** Разводит небольшой костёр на земле у центра (если место свободно) и возвращает точку лагеря. */
    private static Vec3 lightCamp(ServerLevel lvl, Vec3 c) {
        int x = (int) Math.floor(c.x), z = (int) Math.floor(c.z);
        int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        if (lvl.getBlockState(pos).isAir() && lvl.getBlockState(pos.below()).isSolid() && lvl.getFluidState(pos.below()).isEmpty()) {
            lvl.setBlock(pos, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 3);
        }
        return Vec3.atBottomCenterOf(pos);
    }

    /** Возврат игрока: отряд снова слушается его. */
    public static int onReturn(ServerLevel lvl, ServerPlayer p) {
        int n = 0;
        List<SoldierEntity> list = new ArrayList<>();
        for (SoldierEntity s : lvl.getEntitiesOfClass(SoldierEntity.class, p.getBoundingBox().inflate(300, 150, 300))) {
            if (!s.getTags().contains(ORPHAN)) continue;
            if (!p.getUUID().equals(s.getCommander()) && !(p.getUUID().equals(s.getOwnerId()) && s.getCommander() == null)) continue;
            list.add(s);
        }
        int size = list.size(), i = 0;
        for (SoldierEntity s : list) {
            s.removeTag(ORPHAN);
            if (s.getTags().contains(LEADER)) {
                s.removeTag(LEADER);
                s.refreshLeaderName();
            }
            if (s.getOrder() == Order.HOLD || s.getOrder() == Order.RETREAT) {
                s.command(Order.FOLLOW, s.position(), p.getYRot(), Formation.LINE, i, size, null);
            }
            i++;
            n++;
        }
        return n;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel lvl)) return;
        Result r = onLeave(lvl, p.getUUID(), p.position());
        if (r.total() == 0) return;
        var cm = Commissions.get(lvl.getServer()).of(p.getUUID());
        String what = (r.pushing() > 0 ? "полевой командир " + (r.leader() != null ? r.leader().getName().getString() : "") + " ведёт " + r.pushing() + " бойцов исполнять последний приказ; " : "")
                + (r.returning() > 0 ? r.returning() + " бойцов возвращаются в безопасную зону (к ратуше); " : "")
                + (r.camping() > 0 ? r.camping() + " бойцов разбили лагерь с костром на месте выхода; " : "");
        UUID kingId = cm != null ? cm.king : p.getUUID();
        ServerPlayer king = lvl.getServer().getPlayerList().getPlayer(kingId);
        if (king != null && king != p) Text.info(king, (cm != null ? "Военачальник " + p.getGameProfile().getName() + " покинул игру: " : "") + what);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel lvl)) return;
        int n = onReturn(lvl, p);
        if (n > 0) Text.good(p, "Ваш отряд (" + n + ") снова под вашим командованием.");
    }
}
