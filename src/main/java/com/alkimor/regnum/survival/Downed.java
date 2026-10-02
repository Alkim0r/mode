package com.alkimor.regnum.survival;

import com.alkimor.regnum.core.Text;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Поднятие раненого: смертельный удар в компании не убивает сразу. Игрок падает без сил на 25 секунд, пока друг рядом
 * (в радиусе 3 блоков, присев) в течение 4 секунд не поднимет его. Не сработает без живого напарника поблизости,
 * от пустоты и команд, а также чаще раза в 5 минут. Не поднят вовремя — погибает как обычно.
 */
public final class Downed {
    private Downed() {}

    public static final int DOWN_TICKS = 500, REVIVE_TICKS = 80, COOLDOWN_TICKS = 6000, MATE_RADIUS = 24;

    private static final class State {
        long until;
        int progress;
    }

    private static final Map<UUID, State> DOWNED = new HashMap<>();
    private static final Map<UUID, Long> LAST = new HashMap<>();

    public static boolean isDowned(ServerPlayer p) {
        return DOWNED.containsKey(p.getUUID());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isCreative() || p.isSpectator()) return;
        if (e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) || e.getSource().is(DamageTypeTags.BYPASSES_RESISTANCE)) return;
        if (isDowned(p)) return;
        if (e.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player) return; // в PvP вторая жизнь не полагается
        long now = p.serverLevel().getGameTime();
        Long last = LAST.get(p.getUUID());
        if (last != null && now - last < COOLDOWN_TICKS) return;
        boolean mate = false;
        for (ServerPlayer o : p.serverLevel().players()) {
            if (o != p && o.isAlive() && !o.isSpectator() && !isDowned(o) && o.distanceToSqr(p) < MATE_RADIUS * MATE_RADIUS) { mate = true; break; }
        }
        if (!mate) return;
        e.setCanceled(true);
        State s = new State();
        s.until = now + DOWN_TICKS;
        DOWNED.put(p.getUUID(), s);
        LAST.put(p.getUUID(), now);
        p.setHealth(2f);
        p.setInvulnerable(true);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DOWN_TICKS + 20, 6, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, DOWN_TICKS + 20, 128, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.GLOWING, DOWN_TICKS + 20, 0, false, false));
        Text.bad(p, "Вы повержены! Друг рядом может поднять вас: присесть рядом на 4 секунды. Иначе через 25 секунд — конец.");
        for (ServerPlayer o : p.serverLevel().players()) {
            if (o != p && o.distanceToSqr(p) < MATE_RADIUS * MATE_RADIUS) Text.gold(o, p.getName().getString() + " повержен! Подойдите и присядьте рядом, чтобы поднять.");
        }
    }

    private static void clear(ServerPlayer p) {
        p.setInvulnerable(false);
        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        p.removeEffect(MobEffects.JUMP);
        p.removeEffect(MobEffects.GLOWING);
    }

    private static void die(ServerPlayer p) {
        clear(p);
        p.hurt(p.damageSources().genericKill(), Float.MAX_VALUE);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (DOWNED.isEmpty() || event.getServer().getTickCount() % 10 != 0) return;
        MinecraftServer server = event.getServer();
        for (Iterator<Map.Entry<UUID, State>> it = DOWNED.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            State s = en.getValue();
            if (p == null || !p.isAlive()) { it.remove(); continue; }
            long now = p.serverLevel().getGameTime();
            if (now >= s.until) {
                it.remove();
                die(p);
                continue;
            }
            boolean helped = false;
            for (ServerPlayer o : p.serverLevel().players()) {
                if (o != p && o.isAlive() && !o.isSpectator() && !isDowned(o) && o.isShiftKeyDown() && o.distanceToSqr(p) < 9) { helped = true; break; }
            }
            s.progress = helped ? s.progress + 10 : Math.max(0, s.progress - 10);
            int left = (int) ((s.until - now) / 20);
            Text.bar(p, helped ? "Вас поднимают: " + s.progress * 100 / REVIVE_TICKS + "%" : "Вы повержены. Осталось " + left + " с", net.minecraft.ChatFormatting.RED);
            if (s.progress >= REVIVE_TICKS) {
                it.remove();
                clear(p);
                p.setHealth(p.getMaxHealth() * 0.4f);
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
                Text.good(p, "Друг поднял вас на ноги!");
                for (ServerPlayer o : p.serverLevel().players()) {
                    if (o != p && o.isShiftKeyDown() && o.distanceToSqr(p) < 9) {
                        Skills.addXp(o, Skill.MEDICINE, 40);
                        Text.good(o, "Вы подняли " + p.getName().getString() + " (+40 опыта Медицины).");
                        break;
                    }
                }
            }
        }
    }

    /** После респавна или входа старое состояние не должно остаться: запись и неуязвимость сбрасываются. */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            DOWNED.remove(p.getUUID());
            if (!p.isCreative()) p.setInvulnerable(false);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && !isDowned(p) && !p.isCreative() && p.isInvulnerable()) {
            p.setInvulnerable(false); // остаток после падения сервера в окне «повержен»
            p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            p.removeEffect(MobEffects.JUMP);
        }
    }

    /** Вышел из игры повержённым — это смерть; иначе остался бы неуязвимым. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && DOWNED.remove(p.getUUID()) != null) die(p);
    }
}
