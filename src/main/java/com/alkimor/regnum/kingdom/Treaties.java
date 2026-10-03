package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Договор с условиями (I059): торговый договор действует 30 суток, потом истекает и его надо продлить;
 * разрыв договора войной бьёт по репутации у всех держав. Сроки хранятся в памяти.
 */
public final class Treaties {
    private Treaties() {}

    public static final long TERM_TICKS = 30L * 24000L;
    public static final int RENEW_WINDOW_DAYS = 5;
    private static final Map<UUID, Long> EXPIRY = new HashMap<>();

    /** Дней до конца срока (минимум 0). */
    public static int daysLeft(long expiry, long now) { return (int) Math.max(0, (expiry - now + 23999) / 24000L); }

    /** Штраф к отношениям со всеми державами за разрыв: союз тяжелее договора. */
    public static int breachPenalty(boolean trade, boolean ally) { return ally ? 40 : trade ? 20 : 0; }

    public static boolean expired(long expiry, long now) { return now >= expiry; }

    public static void sign(Realm r, long now) { EXPIRY.put(r.id, now + TERM_TICKS); }
    public static Long expiry(Realm r) { return EXPIRY.get(r.id); }

    /** Раз в сутки: договор без записи получает срок, истёкший — расторгается. */
    public static void daily(Realm r, long now, ServerPlayer owner) {
        if (!r.trade) { EXPIRY.remove(r.id); return; }
        Long e = EXPIRY.get(r.id);
        if (e == null) { sign(r, now); return; }
        if (expired(e, now)) {
            r.trade = false;
            r.ally = false;
            EXPIRY.remove(r.id);
            r.note("торговый договор истёк");
            if (owner != null) Text.bad(owner, "Договор с «" + r.name + "» истёк: торговля и союз прекращены. Продлить: /regnum realm treaty рядом с их правителем.");
        } else if (daysLeft(e, now) == RENEW_WINDOW_DAYS && owner != null) {
            Text.info(owner, "Договор с «" + r.name + "» истекает через " + RENEW_WINDOW_DAYS + " сут.: продлите заранее.");
        }
    }

    /** Объявление войны державе, с которой есть договор: репутация падает у всех. */
    public static void breach(KingdomData data, Realm target, ServerPlayer p) {
        int pen = breachPenalty(target.trade, target.ally);
        if (pen <= 0) return;
        target.trade = false;
        target.ally = false;
        EXPIRY.remove(target.id);
        for (Realm o : data.realms()) {
            if (o == target || !o.built) continue;
            o.relation = Math.max(-100, o.relation - pen / 2);
        }
        target.note("вы нарушили договор");
        if (p != null) Text.bad(p, "Вы нарушили договор с «" + target.name + "»: остальные державы запомнили это (отношения −" + (pen / 2) + ").");
    }
}
