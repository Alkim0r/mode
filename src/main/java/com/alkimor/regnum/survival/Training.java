package com.alkimor.regnum.survival;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.kingdom.BuildingType;
import com.alkimor.regnum.kingdom.City;
import com.alkimor.regnum.kingdom.KingdomData;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Наставники и тренировка (идея I007): в своём городе, где есть подходящая постройка, героя учат мастера —
 * не больше двух занятий в игровые сутки, за плату из казны города. Это не бесконечный «безопасный» способ растить навыки.
 */
public final class Training {
    private Training() {}

    public static final int PER_DAY = 2, COST = 15;
    public static final float XP = 30f;

    /** Какая постройка учит какому навыку. */
    public static BuildingType teacher(Skill s) {
        return switch (s) {
            case ONE_HANDED, TWO_HANDED, POLEARM, BOW, CROSSBOW, THROWING, ATHLETICS, TACTICS, LEADERSHIP -> BuildingType.TRAINING_GROUND;
            case RIDING -> BuildingType.STABLE;
            case SMITHING -> BuildingType.SMITHY;
            case MEDICINE -> BuildingType.INFIRMARY;
            case TRADE, CHARM -> BuildingType.MARKET;
            case STEWARD, ENGINEERING, SCOUTING, ROGUERY -> BuildingType.LIBRARY;
        };
    }

    public static int usedToday(ServerPlayer p) {
        var pd = p.getPersistentData();
        long day = p.serverLevel().getGameTime() / 24000L;
        return pd.getLong("regnum_train_day") == day ? pd.getInt("regnum_train_n") : 0;
    }

    /** null — успех, иначе причина отказа. */
    public static String train(ServerPlayer p, Skill s) {
        KingdomData data = KingdomData.get(p.server);
        City c = data.at(p.blockPosition());
        if (c == null || !c.owner.equals(p.getUUID()) && !com.alkimor.regnum.kingdom.Council.isMember(c, p.getUUID()))
            return "Тренироваться можно в своём городе (или городе, где вы в совете).";
        BuildingType need = teacher(s);
        if (c.count(need) == 0) return "Для «" + s.title + "» в городе нужна постройка: " + need.title + ".";
        if (usedToday(p) >= PER_DAY) return "Сегодня вы уже тренировались " + PER_DAY + " раза. Приходите завтра.";
        if (c.treasury < COST) return "Мастерам нечем платить: в казне города нужно " + COST + " монет.";
        c.treasury -= COST;
        data.setDirty();
        var pd = p.getPersistentData();
        long day = p.serverLevel().getGameTime() / 24000L;
        int usedNow = usedToday(p);
        pd.putLong("regnum_train_day", day);
        pd.putInt("regnum_train_n", usedNow + 1);
        Skills.addXp(p, s, XP + 5f * Math.min(4, c.count(need)));
        return null;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("train")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Text.gold(p, "══ Наставники ══");
                    Text.info(p, "Занятий сегодня: " + usedToday(p) + "/" + PER_DAY + ", цена " + COST + " монет из казны города.");
                    List<String> lines = new ArrayList<>();
                    for (BuildingType b : new BuildingType[]{BuildingType.TRAINING_GROUND, BuildingType.STABLE, BuildingType.SMITHY, BuildingType.INFIRMARY, BuildingType.MARKET, BuildingType.LIBRARY}) {
                        StringBuilder sb = new StringBuilder(b.title + ": ");
                        boolean first = true;
                        for (Skill s : Skill.values()) if (teacher(s) == b) { sb.append(first ? "" : ", ").append(s.name().toLowerCase()); first = false; }
                        lines.add(sb.toString());
                    }
                    for (String l : lines) Text.info(p, "  " + l);
                    Text.info(p, "/regnum train <навык>");
                    return 1;
                })
                .then(Commands.argument("skill", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Skill sk = null;
                    String n = StringArgumentType.getString(ctx, "skill");
                    for (Skill x : Skill.values()) if (x.name().equalsIgnoreCase(n)) sk = x;
                    if (sk == null) { Text.bad(p, "Нет такого навыка. Список: /regnum train"); return 0; }
                    String err = train(p, sk);
                    if (err != null) { Text.bad(p, err); return 0; }
                    Text.good(p, "Мастер поправил вашу технику: «" + sk.title + "». Занятий сегодня: " + usedToday(p) + "/" + PER_DAY + ".");
                    return 1;
                }))));
    }
}
