package com.alkimor.regnum.trade;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.kingdom.City;
import com.alkimor.regnum.kingdom.KingdomData;
import com.alkimor.regnum.kingdom.Realm;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Сезонные и событийные цены (идея I042): война поднимает спрос на железные изделия, эпидемия — на травы,
 * а каждый товар раз в несколько суток переживает «дефицит» или «изобилие». Всё выводится из состояния мира и дня,
 * поэтому не требует хранения; купцы пересказывают причины слухами (/regnum trade news).
 */
public final class TradeNews {
    private TradeNews() {}

    private static volatile boolean war, plague;
    private static volatile long day;

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 200 != 7) return;
        KingdomData data = KingdomData.get(e.getServer());
        boolean w = false, p = false;
        for (City c : data.all()) {
            if (c.war) w = true;
            if (c.plagueDays > 0) p = true;
        }
        for (Realm r : data.realms()) if (r.state == Realm.WAR) w = true;
        war = w;
        plague = p;
        day = e.getServer().overworld().getDayTime() / 24000L;
    }

    /** 0 — обычно, 1 — дефицит (дороже), 2 — изобилие (дешевле): сменяется каждые 12 суток по-своему для каждого товара. */
    public static int season(TradeGood g, long day) {
        long phase = Math.floorMod(day / 4 + g.ordinal() * 3L, 12);
        return phase == 0 ? 1 : phase == 6 ? 2 : 0;
    }

    public static double factor(TradeGood g) {
        double f = 1.0;
        if (g == TradeGood.IRON && war) f *= 1.35;
        if (g == TradeGood.HERBS && plague) f *= 1.4;
        int s = season(g, day);
        if (s == 1) f *= 1.2;
        else if (s == 2) f *= 0.85;
        return f;
    }

    public static List<String> rumors() {
        List<String> out = new ArrayList<>();
        if (war) out.add("Идёт война — оружейники скупают железо: железные изделия дорожают.");
        if (plague) out.add("Где-то хворь — лекарственные сборы нарасхват.");
        for (TradeGood g : TradeGood.values()) {
            int s = season(g, day);
            if (s == 1) out.add(g.title + ": дефицит, цены выросли.");
            else if (s == 2) out.add(g.title + ": изобилие, цены упали — время закупаться.");
        }
        if (out.isEmpty()) out.add("Рынки спокойны: купцы не жалуются.");
        return out;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("trade").then(Commands.literal("news").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Text.gold(p, "══ Вести с рынков ══");
            for (String r : rumors()) p.sendSystemMessage(Text.of("  " + r, ChatFormatting.GRAY));
            return 1;
        }))));
    }
}
