package com.alkimor.regnum.kingdom;

import net.minecraft.world.phys.Vec3;

/** Построения отряда. Смещения считаются в локальных координатах (вправо, назад) от точки сбора. */
public enum Formation {
    LINE("Линия"),
    WEDGE("Клин"),
    SQUARE("Каре"),
    LOOSE("Рассыпной строй"),
    SHIELD_WALL("Стена щитов"),
    PHALANX("Фаланга");

    public final String title;

    Formation(String title) {
        this.title = title;
    }

    public static Formation byId(int id) {
        Formation[] v = values();
        return id >= 0 && id < v.length ? v[id] : LINE;
    }

    /** @return x = вправо, z = назад */
    public Vec3 localOffset(int slot, int size) {
        size = Math.max(1, size);
        if (slot < 0) {
            // конница: фланги строя (чётные слоты — левый фланг, нечётные — правый)
            int k = (-slot - 1) / 2;
            double side = ((-slot - 1) % 2 == 0) ? -1 : 1;
            int w = this == PHALANX ? 6 : this == SHIELD_WALL ? 10 : Math.min(size, 8);
            double half = w * (this == SHIELD_WALL ? 0.6 : 0.8);
            return new Vec3(side * (half + 3.5 + (k % 3) * 1.9), 0, -1.0 + (k / 3) * 2.2);
        }
        switch (this) {
            case SHIELD_WALL -> {
                // тесная стена: ряд до 10 бойцов впритык (щитники и копья в первом ряду, стрелки сзади)
                int w = Math.min(size, 10);
                int row = slot / w, col = slot % w;
                return new Vec3((col - (w - 1) / 2.0) * 1.15, 0, row * 1.6);
            }
            case PHALANX -> {
                int w = Math.min(size, 6);
                int row = slot / w, col = slot % w;
                return new Vec3((col - (w - 1) / 2.0) * 1.3, 0, row * 1.4);
            }
            case LINE -> {
                int w = Math.min(size, 8);
                int row = slot / w, col = slot % w;
                return new Vec3((col - (w - 1) / 2.0) * 1.6, 0, row * 1.8);
            }
            case WEDGE -> {
                if (slot == 0) return Vec3.ZERO;
                int k = (slot + 1) / 2;
                double side = (slot % 2 == 1) ? -1 : 1;
                return new Vec3(side * k * 1.4, 0, k * 1.4);
            }
            case SQUARE -> {
                double r = Math.max(2.0, size * 0.45);
                double a = 2 * Math.PI * slot / size;
                return new Vec3(Math.cos(a) * r, 0, Math.sin(a) * r);
            }
            default -> {
                int w = (int) Math.ceil(Math.sqrt(size));
                int row = slot / w, col = slot % w;
                return new Vec3((col - (w - 1) / 2.0) * 3.0, 0, row * 3.0);
            }
        }
    }

    /** Мировая позиция бойца: anchor + поворот на yaw (градусы Minecraft). */
    public Vec3 worldPos(Vec3 anchor, float yaw, int slot, int size, double extraBack) {
        Vec3 o = localOffset(slot, size);
        double rad = Math.toRadians(yaw);
        double fx = -Math.sin(rad), fz = Math.cos(rad);      // вперёд
        double rx = -Math.cos(rad), rz = -Math.sin(rad);     // вправо
        double back = o.z + extraBack;
        return new Vec3(anchor.x + rx * o.x - fx * back, anchor.y, anchor.z + rz * o.x - fz * back);
    }
}
