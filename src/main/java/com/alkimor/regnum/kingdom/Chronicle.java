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


    public static net.minecraft.nbt.CompoundTag toTag() {
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        LOG.forEach((k, q) -> {
            net.minecraft.nbt.ListTag l = new net.minecraft.nbt.ListTag();
            for (String s : q) l.add(net.minecraft.nbt.StringTag.valueOf(s));
            t.put(k.toString(), l);
        });
        return t;
    }

    public static void fromTag(net.minecraft.nbt.CompoundTag t) {
        LOG.clear();
        for (String k : t.getAllKeys()) {
            ArrayDeque<String> q = new ArrayDeque<>();
            net.minecraft.nbt.ListTag l = t.getList(k, 8);
            for (int i = 0; i < l.size(); i++) push(q, l.getString(i));
            LOG.put(UUID.fromString(k), q);
        }
    }
}
