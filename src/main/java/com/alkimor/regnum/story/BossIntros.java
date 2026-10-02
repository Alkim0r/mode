package com.alkimor.regnum.story;

import com.alkimor.regnum.story.Cutscene.Shot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Сценарии появления боссов: камера знакомит игроков с хозяином места, пока он «пробуждается». */
public final class BossIntros {
    private BossIntros() {}

    public static List<Shot> script(Mob boss) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).getPath();
        return switch (id) {
            case "crypt_lord" -> List.of(
                    Shot.orbit(14, 12, 9, 7, 150, 190, 1.5, 50).sfx("minecraft:ambient.soul_sand_valley.mood"),
                    Shot.orbit(9, 6, 4, 2.5, 200, 245, 2.0, 70).said("«Здесь спят те, кого не должны были будить…»").sfx("minecraft:entity.wither.ambient"),
                    Shot.orbit(5, 3.5, 1.0, 2.6, 330, 360, 2.4, 55).titled("МОРГРАТ", "Повелитель склепа").sfx("minecraft:entity.wither.spawn"));
            case "mire_mother" -> List.of(
                    Shot.orbit(16, 12, 7, 5, 120, 160, 1.5, 50).sfx("minecraft:ambient.crimson_forest.mood"),
                    Shot.orbit(9, 6, 2.5, 1.5, 180, 230, 2.0, 70).said("«Топь помнит каждого, кто в неё вошёл…»").sfx("minecraft:entity.slime.squish"),
                    Shot.orbit(6, 4, 1.2, 2.8, 340, 372, 2.4, 55).titled("МАТУШКА ТОПЬ", "Хозяйка болот").sfx("minecraft:entity.elder_guardian.curse"));
            case "forgemaster" -> List.of(
                    Shot.orbit(16, 12, 10, 6, 130, 175, 2.0, 50).sfx("minecraft:block.anvil.land"),
                    Shot.orbit(10, 7, 3, 2, 190, 240, 2.5, 70).said("«Железо не прощает слабых рук…»").sfx("minecraft:block.blastfurnace.fire_crackle"),
                    Shot.orbit(7, 5, 1.2, 3.5, 335, 372, 3.0, 55).titled("ГОРНОВОЙ", "Мастер погасшего горна").sfx("minecraft:entity.iron_golem.repair"));
            case "scarab_queen" -> List.of(
                    Shot.orbit(18, 14, 9, 6, 120, 170, 1.5, 50).sfx("minecraft:ambient.basalt_deltas.mood"),
                    Shot.orbit(10, 7, 3, 2, 190, 240, 2.0, 70).said("«Песок хранит имя владычицы…»").sfx("minecraft:block.sand.break"),
                    Shot.orbit(7, 5, 1.2, 3.2, 340, 372, 2.4, 55).titled("СЕХМЕТ-РА", "Царица скарабеев").sfx("minecraft:entity.ender_dragon.growl"));
            case "crawler_queen" -> List.of(
                    Shot.orbit(16, 12, 8, 5, 120, 165, 1.5, 50).sfx("minecraft:ambient.cave"),
                    Shot.orbit(10, 7, 3, 1.5, 190, 240, 1.8, 70).said("«Вы слишком долго копали в моём доме…»").sfx("minecraft:entity.spider.ambient"),
                    Shot.orbit(7, 5, 1.0, 2.8, 340, 372, 2.2, 55).titled("КОРОЛЕВА ПОЛЗУНОВ", "Хозяйка тёмных шахт").sfx("minecraft:entity.ravager.roar"));
            default -> List.of();
        };
    }

    /**
     * Запускает сцену для всех игроков рядом; босс на время «спит» (без ИИ и урона) и просыпается, когда сцена закончилась
     * у всех. Возвращает true, если сцена началась.
     */
    public static boolean intro(ServerLevel sl, Mob boss, Vec3 anchor, float yaw, double radius, ServerPlayer trigger) {
        List<Shot> shots = script(boss);
        if (shots.isEmpty()) return false;
        List<ServerPlayer> viewers = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (p.isSpectator() || p.distanceToSqr(anchor) > radius * radius) continue;
            viewers.add(p);
        }
        if (viewers.isEmpty()) return false;
        boss.setNoAi(true);
        boss.setInvulnerable(true);
        AtomicInteger left = new AtomicInteger(0);
        Runnable wake = () -> {
            if (left.decrementAndGet() > 0) return;
            if (boss.isAlive()) {
                boss.setNoAi(false);
                boss.setInvulnerable(false);
                boss.setTarget(trigger != null && trigger.isAlive() ? trigger : null);
            }
        };
        int started = 0;
        left.set(viewers.size());
        for (ServerPlayer p : viewers) {
            if (Cutscene.play(p, anchor, yaw, shots, wake)) started++;
            else left.decrementAndGet();
        }
        if (started == 0) {
            boss.setNoAi(false);
            boss.setInvulnerable(false);
            return false;
        }
        return true;
    }
}
