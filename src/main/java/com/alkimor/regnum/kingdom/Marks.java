package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Совместное планирование: тактические метки «сбор», «обход», «цель стрелков», «путь отхода».
 * Видны автору и участникам совета его (или своего) города; живут ограниченное время; у каждого автора по одной метке каждого вида.
 */
public final class Marks {
    private Marks() {}

    public enum Kind {
        RALLY("сбор", ChatFormatting.GREEN, 0x33DD33),
        FLANK("обход", ChatFormatting.YELLOW, 0xEEDD22),
        TARGET("цель стрелков", ChatFormatting.RED, 0xEE2222),
        RETREAT("путь отхода", ChatFormatting.AQUA, 0x33CCEE);

        public final String title;
        public final ChatFormatting color;
        public final int rgb;

        Kind(String t, ChatFormatting c, int rgb) {
            this.title = t;
            this.color = c;
            this.rgb = rgb;
        }

        public static Kind byName(String n) {
            for (Kind k : values()) if (k.name().equalsIgnoreCase(n) || k.title.equalsIgnoreCase(n)) return k;
            return null;
        }
    }

    public static final class Mark {
        public UUID author;
        public String authorName;
        public Kind kind;
        public ServerLevel level;
        public BlockPos pos;
        public long expires;
    }

    public static final List<Mark> MARKS = new ArrayList<>();
    public static final int MAX_MINUTES = 15;

    /** Участвуют ли двое в одном совете (или один из них правитель города другого). */
    public static boolean sameCouncil(KingdomData data, UUID a, UUID b) {
        if (a.equals(b)) return true;
        for (City c : data.all()) {
            boolean ma = Council.isMember(c, a), mb = Council.isMember(c, b);
            if (ma && mb) return true;
        }
        return false;
    }

    public static Mark place(ServerPlayer p, Kind k, BlockPos pos, int minutes) {
        MARKS.removeIf(m -> m.author.equals(p.getUUID()) && m.kind == k);
        Mark m = new Mark();
        m.author = p.getUUID();
        m.authorName = p.getName().getString();
        m.kind = k;
        m.level = p.serverLevel();
        m.pos = pos;
        m.expires = p.serverLevel().getGameTime() + minutes * 1200L;
        MARKS.add(m);
        KingdomData data = KingdomData.get(p.server);
        String where = pos.getX() + " " + pos.getY() + " " + pos.getZ();
        for (ServerPlayer q : p.server.getPlayerList().getPlayers()) {
            if (!sameCouncil(data, p.getUUID(), q.getUUID())) continue;
            q.sendSystemMessage(Text.of("◆ " + m.authorName + " отметил: " + k.title + " (" + where + ", " + minutes + " мин.)", k.color, ChatFormatting.BOLD));
        }
        return m;
    }

    public static int visibleCount(ServerPlayer p) {
        KingdomData data = KingdomData.get(p.server);
        int n = 0;
        for (Mark m : MARKS) if (sameCouncil(data, m.author, p.getUUID())) n++;
        return n;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (MARKS.isEmpty()) return;
        var server = e.getServer();
        long now = server.overworld().getGameTime();
        MARKS.removeIf(m -> m.expires <= now);
        if (now % 20 != 0 || MARKS.isEmpty()) return;
        KingdomData data = KingdomData.get(server);
        for (Mark m : MARKS) {
            DustParticleOptions dust = new DustParticleOptions(new org.joml.Vector3f(((m.kind.rgb >> 16) & 255) / 255f, ((m.kind.rgb >> 8) & 255) / 255f, (m.kind.rgb & 255) / 255f), 2.0f);
            for (ServerPlayer q : server.getPlayerList().getPlayers()) {
                if (q.level() != m.level || q.distanceToSqr(m.pos.getX(), m.pos.getY(), m.pos.getZ()) > 160 * 160) continue;
                if (!sameCouncil(data, m.author, q.getUUID())) continue;
                for (int i = 0; i < 12; i++) {
                    m.level.sendParticles(q, dust, true, m.pos.getX() + 0.5, m.pos.getY() + 1 + i * 0.5, m.pos.getZ() + 0.5, 1, 0.05, 0, 0.05, 0);
                }
            }
        }
    }

    private static BlockPos target(ServerPlayer p) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(120));
        BlockHitResult r = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return r.getType() == HitResult.Type.MISS ? null : r.getBlockPos();
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        var root = Commands.literal("mark");
        for (Kind k : Kind.values()) {
            root.then(Commands.literal(k.name().toLowerCase())
                    .executes(ctx -> mark(ctx.getSource().getPlayerOrException(), k, 5))
                    .then(Commands.argument("min", IntegerArgumentType.integer(1, MAX_MINUTES))
                            .executes(ctx -> mark(ctx.getSource().getPlayerOrException(), k, IntegerArgumentType.getInteger(ctx, "min")))));
        }
        root.then(Commands.literal("clear").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            MARKS.removeIf(m -> m.author.equals(p.getUUID()));
            Text.info(p, "Ваши метки сняты.");
            return 1;
        }));
        root.then(Commands.literal("list").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            KingdomData data = KingdomData.get(p.server);
            long now = p.serverLevel().getGameTime();
            int n = 0;
            for (Mark m : MARKS) {
                if (!sameCouncil(data, m.author, p.getUUID())) continue;
                n++;
                p.sendSystemMessage(Text.of(m.kind.title + " — " + m.authorName + ": " + m.pos.getX() + " " + m.pos.getY() + " " + m.pos.getZ()
                        + " (ещё " + Math.max(0, (m.expires - now) / 20) + " с)", m.kind.color));
            }
            if (n == 0) Text.info(p, "Меток нет. /regnum mark rally|flank|target|retreat [минуты] — отметить блок, на который смотрите.");
            return n;
        }));
        event.getDispatcher().register(Commands.literal("regnum").then(root));
    }

    private static int mark(ServerPlayer p, Kind k, int minutes) {
        BlockPos pos = target(p);
        if (pos == null) {
            Text.bad(p, "Посмотрите на блок не дальше 120 шагов.");
            return 0;
        }
        place(p, k, pos, minutes);
        return 1;
    }
}
