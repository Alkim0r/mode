package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Посол (I060): во время войны можно отправить посла с предложением мира, не доходя до правителя. Стоит 40 монет,
 * раз в сутки на державу. Шанс растёт с потерянными врагом армиями и с отношениями; успех заключает мир без убийств,
 * провал оставляет войну и немного портит отношения.
 */
public final class Envoy {
    private Envoy() {}

    public static final int COST = 40;
    private static final Map<UUID, Long> LAST_DAY = new HashMap<>();

    public static int chance(int relation, int armiesLost) {
        return Math.max(5, Math.min(85, 25 + armiesLost * 15 + (relation + 100) / 10));
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("realm").then(Commands.literal("envoy")
                .then(Commands.argument("id", IntegerArgumentType.integer(1, 16)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    Realm r = Realms.byIndex(data, IntegerArgumentType.getInteger(ctx, "id"));
                    if (r == null) return 0;
                    if (r.state != Realm.WAR) { Text.info(p, "Войны с «" + r.name + "» нет: послу нечего предлагать."); return 0; }
                    City c = Realms.cityOf(p, data);
                    if (c == null) return 0;
                    long day = p.level().getDayTime() / 24000L;
                    if (LAST_DAY.getOrDefault(r.id, -1L) == day) { Text.bad(p, "Посол уже ездил к «" + r.name + "» сегодня."); return 0; }
                    if (c.treasury < COST) { Text.bad(p, "Посольство стоит " + COST + " монет в казне."); return 0; }
                    c.treasury -= COST;
                    LAST_DAY.put(r.id, day);
                    int ch = chance(r.relation, r.armiesLost);
                    if (p.getRandom().nextInt(100) < ch) {
                        r.relation = -10;
                        Realms.endWar(p.serverLevel(), data, r, "Посол убедил «" + r.name + "» принять мир (шанс был " + ch + "%).");
                    } else {
                        r.relation = Math.max(-100, r.relation - 5);
                        data.setDirty();
                        Text.bad(p, "Посла «" + r.name + "» выслушали и отослали (шанс был " + ch + "%). Война продолжается.");
                    }
                    return 1;
                })))));
    }
}
