package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Настроение жителей (0–100): считается из состояния города, не хранится. Сытость, здоровье, мир и слава поднимают его,
 * голод, эпидемия, война, засуха и непосильные поборы — опускают. Довольные платят больше налогов (+10% от 75),
 * недовольные — меньше (−20% ниже 25).
 */
public final class Mood {
    private Mood() {}

    public static int score(City c) {
        int m = 50;
        m += Math.min(15, c.glory / 20);
        m += Math.min(5, c.count(BuildingType.INFIRMARY) * 5);
        m -= Math.min(40, c.hungerDays * 8);
        if (c.plagueDays > 0) m -= c.quarantine ? 10 : 15;
        if (c.war) m -= 10;
        if (c.droughtDays > 0) m -= 5;
        if (c.priority == 1) m -= 5;   // курс «казна»: тяжёлые поборы
        if (c.priority == 2) m += 5;   // курс «провиант»: люди сыты и заняты делом
        if (c.labor > 0) m -= 3;
        return Math.max(0, Math.min(100, m));
    }

    public static final int RIOT_SCORE = 10, RIOT_DAYS = 3;

    /** Сколько суток подряд город на грани бунта после очередных суток. */
    public static int unrestStep(int days, int score) { return score < RIOT_SCORE ? days + 1 : 0; }

    /** Суточный шаг: считает дни недовольства; на третий день — бунт (казна −10%, счётчик сбрасывается). Возвращает строку для сводки. */
    public static String daily(City c) {
        c.unrestDays = unrestStep(c.unrestDays, score(c));
        if (c.unrestDays < RIOT_DAYS) return c.unrestDays > 0 ? "жители ропщут (" + c.unrestDays + "/" + RIOT_DAYS + ")" : "";
        c.unrestDays = 0;
        int loss = Math.max(1, c.treasury / 10);
        c.treasury -= Math.min(c.treasury, loss);
        Chronicle.add(c.owner, "бунт в «" + c.name + "»: потеряно " + loss + " монет");
        return "БУНТ: толпа разграбила казну на " + loss + " монет; накормите и вылечите людей";
    }

    public static double incomeMult(int score) { return score >= 75 ? 1.10 : score < 25 ? 0.80 : 1.0; }

    public static String title(int score) {
        return score >= 75 ? "довольны" : score >= 50 ? "спокойны" : score >= 25 ? "ропщут" : "на грани бунта";
    }

    public static List<String> reasons(City c) {
        List<String> r = new ArrayList<>();
        if (c.hungerDays > 0) r.add("голод " + c.hungerDays + " дн. (−" + Math.min(40, c.hungerDays * 8) + ")");
        if (c.plagueDays > 0) r.add("эпидемия (−" + (c.quarantine ? 10 : 15) + ")");
        if (c.war) r.add("война (−10)");
        if (c.droughtDays > 0) r.add("засуха (−5)");
        if (c.priority == 1) r.add("поборы курса «казна» (−5)");
        if (c.labor > 0) r.add("промысел отвлекает людей (−3)");
        if (c.glory >= 20) r.add("слава города (+" + Math.min(15, c.glory / 20) + ")");
        if (c.count(BuildingType.INFIRMARY) > 0) r.add("лазарет (+5)");
        if (c.priority == 2) r.add("курс «провиант» (+5)");
        return r;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("mood").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
            if (c == null) { Text.bad(p, "Рядом нет вашего города."); return 0; }
            int s = score(c);
            Text.gold(p, "Настроение «" + c.name + "»: " + s + "/100 — жители " + title(s));
            for (String line : reasons(c)) Text.info(p, "  " + line);
            Text.info(p, s >= 75 ? "Налоги +10%." : s < 25 ? "Налоги −20%: накормите людей и вылечите больных." : "Налоги без поправки (нужно 75+ для надбавки).");
            return 1;
        })));
    }
}
