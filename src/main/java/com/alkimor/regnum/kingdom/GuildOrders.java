package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Гильдейские поручения (I048): кузнецы, лекари и торговцы просят ресурсы, которых сейчас не хватает на рынке (давление спроса
 * от 20). Выполнить можно со склада города один раз в сутки на гильдию: гильдия платит полную базовую цену (рынок скупает за 60%), а нехватка на рынке ослабевает.
 */
public final class GuildOrders {
    private GuildOrders() {}

    public static final int MIN_PRESSURE = 20;
    public static final String[] GUILDS = {"кузнецы", "лекари", "торговцы"};
    private static final Map<String, Long> DONE = new HashMap<>();

    /** Ресурсы, которые интересуют гильдию (null — любые). */
    public static Resource[] wants(int guild) {
        return switch (guild) {
            case 0 -> new Resource[]{Resource.IRON, Resource.STONE};
            case 1 -> new Resource[]{Resource.HERBS, Resource.FOOD};
            default -> Resource.values();
        };
    }

    /** Сколько единиц просят при данном давлении: 5..30. */
    public static int qty(int pressure) { return Math.max(5, Math.min(30, pressure / 5)); }

    /** Гильдия платит полную базовую цену (рынок скупает за 60%), но не больше: перепродажа купленного на рынке не даёт прибыли. */
    public static int reward(int price, int qty) { return price * qty; }

    public static int byKey(String s) {
        for (int i = 0; i < GUILDS.length; i++) if (GUILDS[i].equalsIgnoreCase(s)) return i;
        return -1;
    }

    /** Ресурс с наибольшим давлением среди интересов гильдии или null. */
    public static Resource pick(int guild) {
        Resource best = null;
        for (Resource r : wants(guild)) if (Exchange.pressure(r) >= MIN_PRESSURE && (best == null || Exchange.pressure(r) > Exchange.pressure(best))) best = r;
        return best;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("поручения")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null) { Text.bad(p, "Рядом нет вашего города."); return 0; }
                    int s = com.alkimor.regnum.survival.Seasons.index(p.level());
                    long day = p.level().getDayTime() / 24000L;
                    Text.gold(p, "══ Поручения гильдий ══");
                    for (int g = 0; g < GUILDS.length; g++) {
                        Resource r = pick(g);
                        boolean done = DONE.getOrDefault(c.id + ":" + g, -1L) == day;
                        if (done) Text.info(p, GUILDS[g] + ": сегодня уже выполнено.");
                        else if (r == null) Text.info(p, GUILDS[g] + ": поручений нет, рынок спокоен.");
                        else {
                            int q = qty(Exchange.pressure(r));
                            Text.info(p, GUILDS[g] + ": нужно " + q + " ед. «" + r.title + "», заплатят " + reward(Exchange.price(r, s), q) + " (на складе " + c.stock(r) + ").");
                        }
                    }
                    Text.info(p, "Сдать: /regnum поручения <кузнецы|лекари|торговцы>");
                    return 1;
                })
                .then(Commands.argument("гильдия", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null) { Text.bad(p, "Рядом нет вашего города."); return 0; }
                    if (!Council.can(c, p.getUUID(), Council.Role.TREASURY)) { Text.bad(p, "Нужно право «казна»."); return 0; }
                    int g = byKey(StringArgumentType.getString(ctx, "гильдия"));
                    if (g < 0) { Text.bad(p, "Есть: кузнецы, лекари, торговцы."); return 0; }
                    long day = p.level().getDayTime() / 24000L;
                    String key = c.id + ":" + g;
                    if (DONE.getOrDefault(key, -1L) == day) { Text.bad(p, "Эта гильдия сегодня уже получила своё."); return 0; }
                    Resource r = pick(g);
                    if (r == null) { Text.bad(p, "У гильдии сейчас нет поручений."); return 0; }
                    int q = qty(Exchange.pressure(r));
                    if (c.stock(r) < q) { Text.bad(p, "На складе не хватает: нужно " + q + " ед. «" + r.title + "», есть " + c.stock(r) + "."); return 0; }
                    int pay = reward(Exchange.price(r, com.alkimor.regnum.survival.Seasons.index(p.level())), q);
                    c.take(r, q);
                    c.treasury += pay;
                    Exchange.addPressure(r, -q * 3);
                    DONE.put(key, day);
                    KingdomData.get(p.server).setDirty();
                    Text.good(p, "Гильдия «" + GUILDS[g] + "» получила " + q + " ед. «" + r.title + "» и заплатила " + pay + ". Нехватка на рынке ослабла.");
                    return 1;
                }))));
    }
}
