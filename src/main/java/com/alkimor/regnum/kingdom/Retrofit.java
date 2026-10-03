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
 * Переоснащение старых армий (I070): новая технология даёт заказ на модернизацию бойцов города.
 * Ополченец -> копейщик (бронза), мечник -> рыцарь (железо), лучник -> арбалетчик (механика).
 * Победы ветерана сохраняются; за раз не больше 8 бойцов, повтор не раньше чем через 2 минуты.
 */
public final class Retrofit {
    private Retrofit() {}

    public static final int BATCH = 8;
    public static final long COOLDOWN = 2400;
    private static final Map<UUID, Long> LAST = new HashMap<>();

    /** В кого переоснащается тип (null — некуда). */
    public static SoldierType target(SoldierType t) {
        return switch (t) {
            case MILITIA -> SoldierType.SPEARMAN;
            case SWORDSMAN -> SoldierType.KNIGHT;
            case ARCHER -> SoldierType.CROSSBOW;
            default -> null;
        };
    }

    /** Цена одного бойца: половина разницы найма, но не меньше 4. */
    public static int price(SoldierType from, SoldierType to) { return Math.max(4, (to.cost - from.cost) / 2); }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("переоснащение").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            KingdomData data = KingdomData.get(p.server);
            City c = data.nearestOwned(p.getUUID(), p.blockPosition());
            if (c == null) { Text.bad(p, "Рядом нет вашего города."); return 0; }
            if (!Council.can(c, p.getUUID(), Council.Role.COMMAND)) { Text.bad(p, "Нужно право командования."); return 0; }
            long now = p.serverLevel().getGameTime();
            Long last = LAST.get(c.id);
            if (last != null && now - last < COOLDOWN) { Text.bad(p, "Кузнецы ещё заняты: повторите через " + ((COOLDOWN - (now - last)) / 20) + " с."); return 0; }
            int done = 0, spent = 0;
            String why = null;
            for (SoldierEntity s : p.serverLevel().getEntitiesOfClass(SoldierEntity.class, new net.minecraft.world.phys.AABB(c.hall).inflate(70),
                    e -> e.isAlive() && c.id.equals(e.getCityId()))) {
                if (done >= BATCH) break;
                SoldierType from = s.getSoldierType(), to = target(from);
                if (to == null) continue;
                if (to.tech() != null && !Science.has(p.server, c.owner, to.tech())) { why = "нужна технология «" + to.tech().title + "»"; continue; }
                int pr = price(from, to);
                if (c.treasury < pr) { why = "не хватает казны"; break; }
                c.treasury -= pr;
                spent += pr;
                s.setup(to, c.owner, c.id, s.getSquad());
                done++;
            }
            if (done == 0) { Text.bad(p, "Переоснащать некого" + (why == null ? "." : ": " + why + ".")); return 0; }
            LAST.put(c.id, now);
            data.setDirty();
            Text.good(p, "Переоснащено бойцов: " + done + " за " + spent + " монет; победы сохранены." + (why == null ? "" : " Остальных ждёт: " + why + "."));
            return 1;
        })));
    }
}
