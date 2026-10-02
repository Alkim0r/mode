package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Вылазка гарнизона: на 60 секунд охраняющие город бойцы выходят навстречу врагу дальше обычного (радиус +40 вместо +16),
 * потом сами возвращаются на пост. Стоит 20 монет из казны, повтор не раньше чем через 3 минуты.
 * Состояние не сохраняется: перезапуск сервера просто отменяет вылазку.
 */
public final class Sortie {
    private Sortie() {}

    public static final long DURATION = 1200, COOLDOWN = 3600;
    public static final int COST = 20;
    private static final Map<UUID, Long> until = new HashMap<>();
    private static final Map<UUID, Long> ready = new HashMap<>();

    public static boolean active(UUID cityId, long now) {
        Long u = until.get(cityId);
        return u != null && now < u;
    }

    /** Лишний радиус реагирования для охраны города в данный момент. */
    public static double extraRadius(UUID cityId, long now) { return active(cityId, now) ? 40 : 16; }

    /** null — вылазка началась; иначе причина отказа. */
    public static String start(City c, long now) {
        if (active(c.id, now)) return "Вылазка уже идёт.";
        Long r = ready.get(c.id);
        if (r != null && now < r) return "Отряд ещё не отдохнул: ждать " + ((r - now) / 20) + " с.";
        if (c.soldiers.isEmpty()) return "В гарнизоне никого нет.";
        if (c.treasury < COST) return "В казне не хватает " + COST + " монет на припасы вылазки.";
        c.treasury -= COST;
        until.put(c.id, now + DURATION);
        ready.put(c.id, now + COOLDOWN);
        return null;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("sortie").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            KingdomData data = KingdomData.get(p.server);
            City c = data.nearestOwned(p.getUUID(), p.blockPosition());
            if (c == null) { Text.bad(p, "Рядом нет вашего города."); return 0; }
            if (!Council.can(c, p.getUUID(), Council.Role.COMMAND)) { Text.bad(p, "Нужно право «командование»."); return 0; }
            String err = start(c, p.server.overworld().getGameTime());
            if (err != null) { Text.bad(p, err); return 0; }
            data.setDirty();
            Text.good(p, "Гарнизон «" + c.name + "» выходит на вылазку на 60 секунд (−" + COST + " монет).");
            return 1;
        })));
    }
}
