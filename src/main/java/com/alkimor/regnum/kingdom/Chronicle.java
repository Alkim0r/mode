package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Летопись (I093): короткая память династии о войнах, договорах, бунтах и достигнутых целях.
 * Хранит последние 30 записей на правителя, без накопления силы. Записи в памяти.
 */
public final class Chronicle {
    private Chronicle() {}

    public static final int CAP = 30;
    private static final Map<UUID, ArrayDeque<String>> LOG = new HashMap<>();
    private static long today = 0;

    /** Добавляет запись, вытесняя самые старые сверх лимита. */
    public static void push(ArrayDeque<String> q, String line) {
        q.addLast(line);
        while (q.size() > CAP) q.removeFirst();
    }

    public static void add(UUID owner, String text) {
        if (owner == null) return;
        push(LOG.computeIfAbsent(owner, k -> new ArrayDeque<>()), "День " + today + ": " + text);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 100 == 11) today = e.getServer().overworld().getDayTime() / 24000L;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("летопись").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            ArrayDeque<String> q = LOG.get(p.getUUID());
            if (q == null || q.isEmpty()) { Text.info(p, "Летопись пока пуста: войны, договоры, бунты и великие цели попадут сюда."); return 1; }
            Text.gold(p, "══ Летопись ══");
            for (String s : q) Text.info(p, s);
            return 1;
        })));
    }
}
