package com.alkimor.regnum.trade;

import net.minecraft.nbt.CompoundTag;

/**
 * Состояние рынка: «давление» по каждому товару.
 * > 0 — рынок завален (игроки много продали), цена ниже; &lt; 0 — товар скуплен, цена выше.
 * Каждый день давление ослабевает на четверть.
 */
public class MarketState {
    public static final int MIN = -12, MAX = 25;
    public final int[] pressure = new int[TradeGood.values().length];
    public long lastDay;

    public MarketState(long day) {
        this.lastDay = day;
    }

    public void decay(long today) {
        long days = today - lastDay;
        if (days <= 0) return;
        for (int d = 0; d < Math.min(days, 30); d++) {
            for (int i = 0; i < pressure.length; i++) {
                int p = pressure[i];
                if (p == 0) continue;
                int step = Math.max(1, (int) Math.ceil(Math.abs(p) * 0.25));
                pressure[i] = p > 0 ? Math.max(0, p - step) : Math.min(0, p + step);
            }
        }
        lastDay = today;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putIntArray("p", pressure);
        t.putLong("day", lastDay);
        return t;
    }

    public static MarketState load(CompoundTag t) {
        MarketState m = new MarketState(t.getLong("day"));
        int[] p = t.getIntArray("p");
        System.arraycopy(p, 0, m.pressure, 0, Math.min(p.length, m.pressure.length));
        return m;
    }
}
