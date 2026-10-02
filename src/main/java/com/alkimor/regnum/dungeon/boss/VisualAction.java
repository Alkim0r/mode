package com.alkimor.regnum.dungeon.boss;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;

/**
 * Синхронизированное «визуальное действие» босса для клиентских анимаций. Сервер хранит только номер действия,
 * тик начала, тик удара (impact) и полную длину; клиент считает прошедшее время сам. Общие номера действий:
 * 0 покой, 1 удар по дуге/взмах, 2 удар по земле, 3 призыв, 4 кольцо/проклятие, 5 снаряд/рывок, 6 пластины, 7 смена фазы.
 * Каждая сущность создаёт свой набор полей через {@link #of(Class)}.
 */
public final class VisualAction {
    public static final int IDLE = 0, SWEEP = 1, SLAM = 2, SUMMON = 3, RING = 4, PROJECTILE = 5, PLATES = 6, PHASE = 7;

    private final EntityDataAccessor<Integer> action, start, len, impact, phase;

    private VisualAction(Class<? extends Entity> c) {
        action = SynchedEntityData.defineId(c, EntityDataSerializers.INT);
        start = SynchedEntityData.defineId(c, EntityDataSerializers.INT);
        len = SynchedEntityData.defineId(c, EntityDataSerializers.INT);
        impact = SynchedEntityData.defineId(c, EntityDataSerializers.INT);
        phase = SynchedEntityData.defineId(c, EntityDataSerializers.INT);
    }

    public static VisualAction of(Class<? extends Entity> c) { return new VisualAction(c); }

    public void define(SynchedEntityData.Builder b) {
        b.define(action, 0).define(start, 0).define(len, 0).define(impact, 0).define(phase, 1);
    }

    /**
     * Не даёт короткому действию (снаряд, зелье, призыв) перебить ещё не доигранную подготовку другого действия:
     * пока активное не дошло до удара, новое принимается только при смене фазы или если оно длиннее активного.
     */
    public void start(Entity e, int a, int impactTick, int total) {
        var d = e.getEntityData();
        if (a != PHASE && current(e) != IDLE && elapsedRaw(e) < d.get(impact) && total <= d.get(len)) return;
        d.set(action, a); d.set(impact, impactTick); d.set(start, (int) e.level().getGameTime()); d.set(len, total);
    }

    public void setPhase(Entity e, int p) {
        if (e.getEntityData().get(phase) != p) e.getEntityData().set(phase, p);
    }

    public int phase(Entity e) { return e.getEntityData().get(phase); }

    public int elapsedRaw(Entity e) { return (int) e.level().getGameTime() - e.getEntityData().get(start); }

    public int current(Entity e) {
        int a = e.getEntityData().get(action);
        return a != IDLE && elapsedRaw(e) <= e.getEntityData().get(len) ? a : IDLE;
    }

    public int elapsed(Entity e) { return current(e) == IDLE ? 0 : Math.max(0, elapsedRaw(e)); }
    public int duration(Entity e) { return current(e) == IDLE ? 0 : e.getEntityData().get(len); }
    public int impact(Entity e) { return current(e) == IDLE ? 0 : e.getEntityData().get(impact); }

    /** Действие по форме телеграфа. */
    public static int forShape(Telegraph.Shape s) {
        return switch (s) {
            case CONE -> SWEEP;
            case CIRCLE -> SLAM;
            case RING -> RING;
            case LINE -> PROJECTILE;
        };
    }
}
