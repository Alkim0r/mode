package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Специализация города: выбор определяет, чем он силён и чем платит. Смена стоит денег и не чаще раза в 5 дней.
 */
public final class Specialization {
    private Specialization() {}

    public static final String[] NAMES = {"без специализации", "кузнечный", "торговый", "учёный", "конный"};
    public static final String[] KEYS = {"нет", "кузница", "рынок", "наука", "кони"};
    public static final String[] EFFECT = {
            "",
            "Кузница тратит на 30% меньше железа, броня бойцов на ступень лучше.",
            "Доход города +15%, но найм дороже на 10%.",
            "Очки знаний +4 и +уровень города в сутки, но доход −5%.",
            "Конница на 15% дешевле."};
    public static final int COST = 150, COOLDOWN_DAYS = 5;

    public static int byKey(String k) {
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equalsIgnoreCase(k)) return i;
        return -1;
    }

    /** Меняет специализацию города: возвращает сообщение об ошибке или null. */
    public static String choose(City c, int spec, long day) {
        if (spec < 0 || spec >= NAMES.length) return "Неизвестная специализация.";
        if (c.spec == spec) return "Город уже так специализирован.";
        if (day - c.specDay < COOLDOWN_DAYS) return "Менять специализацию можно раз в " + COOLDOWN_DAYS + " дней.";
        int cost = c.spec == 0 ? 0 : COST;
        if (c.treasury < cost) return "Смена стоит " + cost + " монет, в казне " + c.treasury + ".";
        c.treasury -= cost;
        c.spec = spec;
        c.specDay = day;
        return null;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("spec")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null) { Text.bad(p, "Рядом нет вашего города."); return 0; }
                    Text.gold(p, "«" + c.name + "»: " + NAMES[c.spec]);
                    if (c.spec > 0) Text.info(p, "  " + EFFECT[c.spec]);
                    Text.info(p, "Выбрать: /regnum spec <кузница|рынок|наука|кони>  (первый выбор бесплатный, смена — " + COST + " монет)");
                    for (int i = 1; i < NAMES.length; i++) Text.info(p, "  " + KEYS[i] + " — " + EFFECT[i]);
                    return 1;
                })
                .then(Commands.argument("тип", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    var data = KingdomData.get(p.server);
                    City c = data.nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null || !c.owner.equals(p.getUUID())) { Text.bad(p, "Рядом нет вашего города."); return 0; }
                    int spec = byKey(StringArgumentType.getString(ctx, "тип"));
                    String err = choose(c, spec, p.serverLevel().getDayTime() / 24000L);
                    if (err != null) { Text.bad(p, err); return 0; }
                    data.setDirty();
                    Text.good(p, "«" + c.name + "» теперь " + NAMES[spec] + " город. " + EFFECT[spec]);
                    return 1;
                }))));
    }
}
