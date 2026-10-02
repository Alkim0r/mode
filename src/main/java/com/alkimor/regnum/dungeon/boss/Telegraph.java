package com.alkimor.regnum.dungeon.boss;

import com.alkimor.regnum.core.RegnumConfig;
import com.alkimor.regnum.kingdom.SoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * «Телеграфы» — способности боссов с подготовкой: на земле заранее проступает контур зоны поражения,
 * внутри сжимается кольцо-таймер. Кто не успел выйти — получает удар. Так бой с боссом
 * превращается в танец уклонений, как в соулслайках, а не в обмен ударами в упор.
 */
public final class Telegraph {
    public enum Shape { CIRCLE, RING, CONE, LINE }

    /** Описание одного удара. */
    public static final class Strike {
        final Shape shape;
        Vec3 center;
        Vec3 dir = new Vec3(0, 0, 1);
        double inner, radius, angleDeg = 90, halfWidth = 1.5;
        int windup, total;
        float damage;
        float soldierMult = 2.0f;
        boolean root = false;
        ParticleOptions warn = ParticleTypes.FLAME;
        ParticleOptions burst = ParticleTypes.EXPLOSION;
        SoundEvent fireSound = SoundEvents.GENERIC_EXPLODE.value();
        BiConsumer<ServerLevel, LivingEntity> onHit;
        java.util.function.Consumer<ServerLevel> onFire;
        String warnText;
        Mob owner;

        /** Сколько тиков осталось до удара. */
        public int remaining() { return windup; }

        /** Сколько тиков прошло с начала подготовки. */
        public int elapsed() { return total - windup; }

        public Mob owner() { return owner; }

        public boolean alive() { return owner != null && owner.isAlive() && !owner.isRemoved(); }

        private Strike(Shape shape, Vec3 center, double radius, int windup, float damage) {
            this.shape = shape;
            this.center = center;
            this.radius = radius;
            this.windup = windup;
            this.damage = damage;
        }

        public Strike dir(Vec3 d) {
            Vec3 h = new Vec3(d.x, 0, d.z);
            this.dir = h.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : h.normalize();
            return this;
        }

        public Strike inner(double r) { this.inner = r; return this; }
        public Strike angle(double a) { this.angleDeg = a; return this; }
        public Strike width(double halfWidth) { this.halfWidth = halfWidth; return this; }
        public Strike soldiers(float mult) { this.soldierMult = mult; return this; }
        public Strike root() { this.root = true; return this; }
        public Strike warn(ParticleOptions p) { this.warn = p; return this; }
        public Strike burst(ParticleOptions p) { this.burst = p; return this; }
        public Strike sound(SoundEvent s) { this.fireSound = s; return this; }
        public Strike onHit(BiConsumer<ServerLevel, LivingEntity> f) { this.onHit = f; return this; }
        public Strike onFire(java.util.function.Consumer<ServerLevel> f) { this.onFire = f; return this; }
        public Strike text(String t) { this.warnText = t; return this; }
    }

    public static Strike circle(Vec3 c, double r, int windup, float dmg) { return new Strike(Shape.CIRCLE, c, r, windup, dmg); }
    public static Strike ring(Vec3 c, double inner, double outer, int windup, float dmg) { return new Strike(Shape.RING, c, outer, windup, dmg).inner(inner); }
    public static Strike cone(Vec3 c, Vec3 dir, double r, double angle, int windup, float dmg) { return new Strike(Shape.CONE, c, r, windup, dmg).dir(dir).angle(angle); }
    public static Strike line(Vec3 c, Vec3 dir, double len, double halfWidth, int windup, float dmg) { return new Strike(Shape.LINE, c, len, windup, dmg).dir(dir).width(halfWidth); }

    private final Mob boss;
    private final List<Strike> active = new ArrayList<>();
    /** Все ожидающие удары (для уклонения бойцов). Очищаются при выстреле и при исчезновении босса. */
    private static final List<Strike> LIVE = new ArrayList<>();

    public static List<Strike> live() {
        LIVE.removeIf(s -> !s.alive());
        return LIVE;
    }

    public static boolean isInside(Strike s, Vec3 pos) {
        return inside(s, pos);
    }
    private int unreachable = 0;

    public Telegraph(Mob boss) {
        this.boss = boss;
    }

    /** На «Кошмаре» подготовка короче, на «Обычном» — длиннее. */
    private static int scaleWindup(int w) {
        return switch (RegnumConfig.tier()) {
            case 0 -> Math.round(w * 1.25f);
            case 2 -> Math.round(w * 0.78f);
            default -> w;
        };
    }

    /** Реальная длина подготовки с учётом сложности (для визуальных состояний). */
    public static int windupTicks(int base) {
        return Math.max(6, scaleWindup(base));
    }

    public void cast(ServerLevel sl, Strike s) {
        s.windup = Math.max(6, scaleWindup(s.windup));
        s.total = s.windup;
        s.center = new Vec3(s.center.x, groundY(sl, s.center), s.center.z);
        s.owner = boss;
        if (boss instanceof VisualActor va) va.visualStart(VisualAction.forShape(s.shape), s.windup, s.windup + 10);
        active.add(s);
        LIVE.add(s);
        sl.playSound(null, BlockPos.containing(s.center), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1.2f, 0.7f);
        if (s.warnText != null) {
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(s.center) < 40 * 40) p.displayClientMessage(Component.literal("⚠ " + s.warnText).withStyle(net.minecraft.ChatFormatting.RED), true);
            }
        }
    }

    public boolean busy() {
        for (Strike s : active) if (s.root) return true;
        return false;
    }

    public boolean idle() {
        return active.isEmpty();
    }

    /** Вызывать каждый тик из customServerAiStep. */
    public void tick(ServerLevel sl) {
        if (busy()) {
            boss.getNavigation().stop();
            Vec3 v = boss.getDeltaMovement();
            boss.setDeltaMovement(0, v.y, 0);
            if (boss.getTarget() != null) boss.getLookControl().setLookAt(boss.getTarget(), 30f, 30f);
        }
        Iterator<Strike> it = active.iterator();
        while (it.hasNext()) {
            Strike s = it.next();
            s.windup--;
            if (s.windup % 3 == 0) draw(sl, s);
            if (s.windup <= 0) {
                fire(sl, s);
                it.remove();
                LIVE.remove(s);
            }
        }
    }

    // ------------------------------------------------------------------ геометрия

    private static double groundY(ServerLevel sl, Vec3 c) {
        BlockPos p = BlockPos.containing(c);
        for (int i = 0; i < 6; i++) {
            if (!sl.getBlockState(p.below()).isAir()) return p.getY();
            p = p.below();
        }
        return c.y;
    }

    static boolean inside(Strike s, Vec3 pos) {
        double dx = pos.x - s.center.x, dz = pos.z - s.center.z;
        if (Math.abs(pos.y - s.center.y) > 3.5) return false;
        double d2 = dx * dx + dz * dz;
        switch (s.shape) {
            case CIRCLE:
                return d2 <= s.radius * s.radius;
            case RING:
                return d2 <= s.radius * s.radius && d2 >= s.inner * s.inner;
            case CONE: {
                if (d2 > s.radius * s.radius) return false;
                if (d2 < 1.0) return true;
                double d = Math.sqrt(d2);
                double cos = (dx * s.dir.x + dz * s.dir.z) / d;
                return cos >= Math.cos(Math.toRadians(s.angleDeg / 2));
            }
            case LINE: {
                double along = dx * s.dir.x + dz * s.dir.z;
                double side = Math.abs(dx * s.dir.z - dz * s.dir.x);
                return along >= -0.5 && along <= s.radius && side <= s.halfWidth;
            }
        }
        return false;
    }

    private void draw(ServerLevel sl, Strike s) {
        double y = s.center.y + 0.15;
        float progress = 1f - (float) s.windup / s.total;
        int density = RegnumConfig.tier() >= 0 ? 1 : 1;
        switch (s.shape) {
            case CIRCLE, RING -> {
                ringPts(sl, s.warn, s.center, s.radius, y, density);
                if (s.shape == Shape.RING) ringPts(sl, ParticleTypes.HAPPY_VILLAGER, s.center, s.inner, y, density);
                // сжимающееся кольцо-таймер: когда сойдётся к краю безопасной зоны — удар
                double r = s.shape == Shape.RING ? s.inner + (s.radius - s.inner) * (1 - progress) : s.radius * (1 - progress);
                if (r > 0.3) ringPts(sl, ParticleTypes.SMALL_FLAME, s.center, r, y, density);
            }
            case CONE -> {
                double half = Math.toRadians(s.angleDeg / 2);
                double base = Math.atan2(s.dir.z, s.dir.x);
                int n = (int) Math.max(6, s.radius * s.angleDeg / 25);
                for (int i = 0; i <= n; i++) {
                    double a = base - half + 2 * half * i / n;
                    p(sl, s.warn, s.center.x + Math.cos(a) * s.radius, y, s.center.z + Math.sin(a) * s.radius);
                }
                for (double r = 1; r < s.radius; r += 1.0) {
                    p(sl, s.warn, s.center.x + Math.cos(base - half) * r, y, s.center.z + Math.sin(base - half) * r);
                    p(sl, s.warn, s.center.x + Math.cos(base + half) * r, y, s.center.z + Math.sin(base + half) * r);
                }
                double rr = s.radius * progress;
                for (int i = 0; i <= n / 2; i++) {
                    double a = base - half + 2 * half * i / (n / 2.0);
                    p(sl, ParticleTypes.SMALL_FLAME, s.center.x + Math.cos(a) * rr, y, s.center.z + Math.sin(a) * rr);
                }
            }
            case LINE -> {
                Vec3 side = new Vec3(-s.dir.z, 0, s.dir.x).scale(s.halfWidth);
                for (double t = 0; t <= s.radius; t += 0.8) {
                    Vec3 c = s.center.add(s.dir.scale(t));
                    p(sl, s.warn, c.x + side.x, y, c.z + side.z);
                    p(sl, s.warn, c.x - side.x, y, c.z - side.z);
                }
                Vec3 front = s.center.add(s.dir.scale(s.radius * progress));
                for (double w = -s.halfWidth; w <= s.halfWidth; w += 0.6) {
                    p(sl, ParticleTypes.SMALL_FLAME, front.x - s.dir.z * w, y, front.z + s.dir.x * w);
                }
            }
        }
    }

    private static void ringPts(ServerLevel sl, ParticleOptions part, Vec3 c, double r, double y, int density) {
        int n = (int) Math.max(8, r * 5 * density);
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            p(sl, part, c.x + Math.cos(a) * r, y, c.z + Math.sin(a) * r);
        }
    }

    private static void p(ServerLevel sl, ParticleOptions part, double x, double y, double z) {
        sl.sendParticles(part, x, y, z, 1, 0, 0, 0, 0);
    }

    private void fire(ServerLevel sl, Strike s) {
        sl.playSound(null, BlockPos.containing(s.center), s.fireSound, SoundSource.HOSTILE, 1.4f, 0.8f);
        // вспышка по форме
        switch (s.shape) {
            case CIRCLE, RING -> {
                int n = (int) Math.max(4, s.radius * 2);
                for (int i = 0; i < n; i++) {
                    double a = Math.PI * 2 * i / n;
                    double r = s.shape == Shape.RING ? (s.inner + s.radius) / 2 : s.radius * 0.6;
                    sl.sendParticles(s.burst, s.center.x + Math.cos(a) * r, s.center.y + 0.3, s.center.z + Math.sin(a) * r, 1, 0.3, 0.2, 0.3, 0);
                }
            }
            case CONE, LINE -> {
                for (double t = 1; t <= s.radius; t += 1.5) {
                    Vec3 c = s.center.add(s.dir.scale(t));
                    sl.sendParticles(s.burst, c.x, c.y + 0.3, c.z, 1, 0.4, 0.2, 0.4, 0);
                }
            }
        }
        float scale = boss.getPersistentData().contains("regnum_dmg_scale") ? boss.getPersistentData().getFloat("regnum_dmg_scale") : 1f;
        AABB box = new AABB(s.center, s.center).inflate(s.radius + 1, 4, s.radius + 1);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, e -> e != boss && e.isAlive() && foe(e))) {
            if (!inside(s, e.position())) continue;
            float dmg = s.damage * scale * (e instanceof SoldierEntity ? s.soldierMult : 1f);
            e.hurt(boss.damageSources().mobAttack(boss), dmg);
            if (s.onHit != null) s.onHit.accept(sl, e);
        }
        if (s.onFire != null) s.onFire.accept(sl);
    }

    public static boolean foe(LivingEntity e) {
        return e instanceof Player pl && !pl.isCreative() && !pl.isSpectator() || e instanceof SoldierEntity;
    }

    // ------------------------------------------------------------------ против «сыра»

    /**
     * Против уловок: столб из блоков, лодка, недоступная ниша. Если цель долго недосягаема —
     * босс прыгает/переносится к ней. Вызывать раз в 20 тиков.
     */
    public boolean antiCheese(ServerLevel sl, LivingEntity t, Runnable onJump) {
        if (t.getVehicle() != null && t instanceof Player) {
            t.stopRiding();
            if (t instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal("Босс сбивает вас с места!").withStyle(net.minecraft.ChatFormatting.RED), true);
        }
        boolean high = t.getY() - boss.getY() > 3.5;
        Path path = boss.getNavigation().createPath(t, 1);
        boolean stuck = path == null || !path.canReach();
        if ((high || stuck) && boss.distanceToSqr(t) > 4) unreachable++;
        else unreachable = Math.max(0, unreachable - 1);
        if (unreachable < 4) return false;
        unreachable = 0;
        for (int i = 0; i < 12; i++) {
            double a = boss.getRandom().nextDouble() * Math.PI * 2;
            double x = t.getX() + Math.cos(a) * 2.5, z = t.getZ() + Math.sin(a) * 2.5;
            if (boss.randomTeleport(x, t.getY() + 1, z, true)) {
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, boss.getX(), boss.getY() + 1, boss.getZ(), 40, 0.5, 1, 0.5, 0.05);
                if (onJump != null) onJump.run();
                return true;
            }
        }
        return false;
    }

    /** Направление от босса к цели (горизонтально). */
    public static Vec3 toward(Mob from, LivingEntity to) {
        Vec3 d = to.position().subtract(from.position());
        return new Vec3(d.x, 0, d.z);
    }

    /** Точка чуть впереди цели (упреждение по скорости). */
    public static Vec3 lead(LivingEntity t, float ticks) {
        Vec3 v = t.getDeltaMovement();
        return t.position().add(v.x * ticks, 0, v.z * ticks);
    }

    public static float baseDamage(Mob m) {
        return (float) m.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
    }

    static float clampYaw(float v) {
        return Mth.wrapDegrees(v);
    }
}
