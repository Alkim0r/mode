package com.alkimor.regnum.story;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Серверные катсцены без клиентского кода: камера — невидимая стойка, к которой «прикрепляется» взгляд игрока
 * (как наблюдатель). Кадры — орбиты вокруг точки с плавным изменением радиуса, высоты и угла; титры, реплики и звуки.
 * Игрок на время сцены неуязвим и неподвижен; Shift (зажать ~1 секунду) пропускает сцену.
 */
public final class Cutscene {
    private Cutscene() {}

    /** Кадр: камера движется по дуге вокруг точки-якоря. Углы — в градусах относительно «лица» якоря. */
    public record Shot(double r0, double r1, double h0, double h1, double a0, double a1, double lookH, int ticks,
                       String title, String sub, String line, String sound) {
        public static Shot orbit(double r0, double r1, double h0, double h1, double a0, double a1, double lookH, int ticks) {
            return new Shot(r0, r1, h0, h1, a0, a1, lookH, ticks, null, null, null, null);
        }

        public Shot titled(String title, String sub) {
            return new Shot(r0, r1, h0, h1, a0, a1, lookH, ticks, title, sub, line, sound);
        }

        public Shot said(String line) {
            return new Shot(r0, r1, h0, h1, a0, a1, lookH, ticks, title, sub, line, sound);
        }

        public Shot sfx(String sound) {
            return new Shot(r0, r1, h0, h1, a0, a1, lookH, ticks, title, sub, line, sound);
        }
    }

    private static final class Run {
        ServerPlayer p;
        ArmorStand cam;
        List<Shot> shots;
        Vec3 anchor;
        float yaw;
        int idx, t, skip, total, age;
        Runnable end;
        ServerLevel level;
    }

    private static final Map<UUID, Run> RUNS = new HashMap<>();

    public static boolean active(ServerPlayer p) {
        return RUNS.containsKey(p.getUUID());
    }

    /** Запустить сцену для игрока. Возвращает false, если нельзя (уже идёт сцена, нет соединения). */
    public static boolean play(ServerPlayer p, Vec3 anchor, float yaw, List<Shot> shots, Runnable onEnd) {
        if (shots.isEmpty() || RUNS.containsKey(p.getUUID()) || p.connection == null || !(p.level() instanceof ServerLevel sl)) return false;
        Run r = new Run();
        r.p = p;
        r.level = sl;
        r.shots = shots;
        r.anchor = anchor;
        r.yaw = yaw;
        r.end = onEnd;
        for (Shot s : shots) r.total += s.ticks();
        ArmorStand cam = net.minecraft.world.entity.EntityType.ARMOR_STAND.create(sl);
        if (cam == null) return false;
        cam.setInvisible(true);
        cam.setNoGravity(true);
        cam.setInvulnerable(true);
        cam.setSilent(true);
        Vec3 start = camPos(r, shots.get(0), 0);
        cam.moveTo(start.x, start.y, start.z, 0f, 0f);
        sl.addFreshEntity(cam);
        r.cam = cam;
        int dur = r.total + 60;
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 6, false, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, dur, 128, false, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, 4, false, false, false));
        RUNS.put(p.getUUID(), r);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new com.alkimor.regnum.core.network.CutscenePayload(true, shots.stream().map(Shot::title).filter(java.util.Objects::nonNull).findFirst().orElse(""), r.total));
        p.connection.send(new ClientboundSetCameraPacket(cam));
        p.displayClientMessage(Component.literal("Shift — пропустить").withStyle(ChatFormatting.DARK_GRAY), true);
        return true;
    }

    private static Vec3 camPos(Run r, Shot s, double f) {
        double rad = s.r0() + (s.r1() - s.r0()) * f;
        double h = s.h0() + (s.h1() - s.h0()) * f;
        double a = Math.toRadians(s.a0() + (s.a1() - s.a0()) * f);
        double face = Math.toRadians(r.yaw);
        // «лицо» якоря: (-sin yaw, cos yaw); поворачиваем на угол a
        double fx = -Math.sin(face), fz = Math.cos(face);
        double cx = fx * Math.cos(a) - fz * Math.sin(a), cz = fx * Math.sin(a) + fz * Math.cos(a);
        return new Vec3(r.anchor.x + cx * rad, r.anchor.y + h - 1.62, r.anchor.z + cz * rad);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (RUNS.isEmpty()) return;
        Iterator<Run> it = RUNS.values().iterator();
        List<Run> done = new ArrayList<>();
        while (it.hasNext()) {
            Run r = it.next();
            if (r.p.isRemoved() || r.p.hasDisconnected() || r.p.level() != r.level || r.age++ > r.total + 100) { done.add(r); continue; }
            if (r.p.isShiftKeyDown()) r.skip++; else r.skip = Math.max(0, r.skip - 1);
            if (r.skip >= 20 || r.idx >= r.shots.size()) { done.add(r); continue; }
            Shot s = r.shots.get(r.idx);
            if (r.t == 0) shotStart(r, s);
            double f = r.t / (double) Math.max(1, s.ticks());
            f = f * f * (3 - 2 * f); // плавный старт/финиш
            Vec3 pos = camPos(r, s, f);
            Vec3 look = r.anchor.add(0, s.lookH(), 0);
            double dx = look.x - pos.x, dy = look.y - (pos.y + 1.62), dz = look.z - pos.z;
            float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
            float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
            r.cam.moveTo(pos.x, pos.y, pos.z, yaw, pitch);
            r.cam.setYHeadRot(yaw);
            r.cam.setYBodyRot(yaw);
            r.p.connection.send(new ClientboundTeleportEntityPacket(r.cam));
            r.p.connection.send(new ClientboundRotateHeadPacket(r.cam, (byte) (yaw * 256f / 360f)));
            if (++r.t >= s.ticks()) {
                r.idx++;
                r.t = 0;
            }
        }
        for (Run r : done) finish(r);
    }

    private static void shotStart(Run r, Shot s) {
        if (s.title() != null) {
            r.p.connection.send(new ClientboundSetTitlesAnimationPacket(10, Math.max(20, s.ticks() - 20), 12));
            if (s.sub() != null) r.p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(s.sub()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
            r.p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(s.title()).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
        }
        if (s.line() != null) r.p.displayClientMessage(Component.literal(s.line()).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
        if (s.sound() != null) {
            var snd = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(s.sound()));
            if (snd != null) r.level.playSound(null, r.anchor.x, r.anchor.y, r.anchor.z, snd, SoundSource.MASTER, 1.4f, 0.8f);
        }
    }

    private static void finish(Run r) {
        RUNS.remove(r.p.getUUID());
        if (r.p.connection != null && !r.p.hasDisconnected()) { r.p.connection.send(new ClientboundSetCameraPacket(r.p)); net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(r.p, new com.alkimor.regnum.core.network.CutscenePayload(false, "", 0)); }
        r.p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        r.p.removeEffect(MobEffects.JUMP);
        r.p.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        if (r.cam != null) r.cam.discard();
        if (r.end != null) r.end.run();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        Run r = RUNS.get(e.getEntity().getUUID());
        if (r != null) finish(r);
    }
}
