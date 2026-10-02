package com.alkimor.regnum.kingdom;

/** Приказы армии. */
public enum Order {
    FOLLOW("Следовать за мной", 14),
    MOVE("Выдвинуться на позицию", 12),
    HOLD("Держать позицию", 12),
    CHARGE("В атаку!", 28),
    RETREAT("Отступить к ратуше", 8),
    ATTACK_TARGET("Атаковать цель", 64),
    PATROL("Патрулировать город", 20),
    GUARD("Охранять город", 0);

    public final String title;
    /** Радиус, в котором отряд сам вступает в бой. */
    public final double engageRadius;

    Order(String title, double engageRadius) {
        this.title = title;
        this.engageRadius = engageRadius;
    }

    public static Order byId(int id) {
        Order[] v = values();
        return id >= 0 && id < v.length ? v[id] : FOLLOW;
    }
}
