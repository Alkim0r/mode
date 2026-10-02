package com.alkimor.regnum.survival;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Дорогая смена специализации: можно переиграть один выбранный перк (чтобы выбрать другой),
 * заплатив уровнями опыта; не чаще раза в семь игровых суток. Персонаж не пересоздаётся.
 */
public final class Respec {
    private Respec() {}

    public static final String KEY = "regnum_respec_last";
    public static final long COOLDOWN = 7 * 24000L;
    public static final int COST_LEVELS = 10;

    public static long remaining(ServerPlayer p) {
        var pd = p.getPersistentData();
        if (!pd.contains(KEY)) return 0;
        return Math.max(0, pd.getLong(KEY) + COOLDOWN - p.serverLevel().getGameTime());
    }

    /** Возвращает null при успехе или причину отказа. */
    public static String forget(ServerPlayer p, Perk perk) {
        SurvivorData d = Skills.data(p);
        if (!d.has(perk)) return "У вас нет перка «" + perk.title + "».";
        if (remaining(p) > 0) return "Смена доступна через " + (remaining(p) / 24000 + 1) + " сут.";
        if (!p.isCreative() && p.experienceLevel < COST_LEVELS) return "Нужно " + COST_LEVELS + " уровней опыта (есть " + p.experienceLevel + ").";
        if (!p.isCreative()) p.giveExperienceLevels(-COST_LEVELS);
        d.perks().remove(perk);
        p.getPersistentData().putLong(KEY, p.serverLevel().getGameTime());
        PlayerStats.apply(p);
        Skills.sync(p, false);
        Skills.chronicle(p, "Отказался от перка «" + perk.title + "» ради нового пути");
        return null;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("respec")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Text.gold(p, "══ Смена перка ══");
                    Text.info(p, "Цена: " + COST_LEVELS + " уровней опыта, раз в 7 игровых суток. Забытый перк можно выбрать заново (или взять другой из пары).");
                    if (remaining(p) > 0) Text.info(p, "Доступно через " + (remaining(p) / 24000 + 1) + " сут.");
                    for (Perk pk : Skills.data(p).perks()) Text.info(p, "  " + pk.name().toLowerCase() + " — " + pk.title);
                    Text.info(p, "/regnum respec <перк>");
                    return 1;
                })
                .then(Commands.argument("perk", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Perk pk = null;
                    String n = StringArgumentType.getString(ctx, "perk");
                    for (Perk x : Perk.values()) if (x.name().equalsIgnoreCase(n)) pk = x;
                    if (pk == null) { Text.bad(p, "Нет такого перка. Список: /regnum respec"); return 0; }
                    String err = forget(p, pk);
                    if (err != null) { Text.bad(p, err); return 0; }
                    Text.good(p, "Перк «" + pk.title + "» забыт. Откройте дневник героя и выберите заново.");
                    return 1;
                }))));
    }
}
