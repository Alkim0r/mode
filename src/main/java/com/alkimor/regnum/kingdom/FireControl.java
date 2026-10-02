package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Огневая дисциплина (I021): правитель задаёт всем своим стрелкам режим, без микроконтроля.
 * свободный — стреляют как обычно; засада — ждут врага ближе 12 блоков (стрелы и порох экономятся, враг не видит расстановку);
 * осада — бьют только осадные машины и атаманов, остальным занимаются мечники.
 */
public final class FireControl {
    private FireControl() {}

    public static final String KEY = "regnum_fire";
    public static final int FREE = 0, AMBUSH = 1, SIEGE = 2;
    public static final String[] NAMES = {"свободный", "засада", "осада"};

    public static int mode(Player p) {
        return p == null ? FREE : Math.max(0, Math.min(2, p.getPersistentData().getInt(KEY)));
    }

    public static void set(Player p, int m) {
        p.getPersistentData().putInt(KEY, m);
    }

    public static int byName(String s) {
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equalsIgnoreCase(s)) return i;
        return -1;
    }

    /** Режим по цели и расстоянию (чистая функция для проверки). */
    public static boolean allows(int mode, boolean importantTarget, double distSqr) {
        return switch (mode) {
            case AMBUSH -> distSqr <= 12 * 12;
            case SIEGE -> importantTarget;
            default -> true;
        };
    }

    /** Разрешён ли выстрел бойца по цели. */
    public static boolean canFire(SoldierEntity s, LivingEntity t) {
        if (!(s.getOwnerPlayer() instanceof ServerPlayer owner)) return true;
        int m = mode(owner);
        if (m == FREE) return true;
        boolean important = t instanceof SiegeTowerEntity || t instanceof CatapultEntity
                || (t instanceof BanditEntity b && b.getVariant() == BanditEntity.CAPTAIN);
        // под огнём сами (враг вплотную) стрелки всё равно отвечают
        if (s.getLastHurtByMob() == t && s.tickCount - s.getLastHurtByMobTimestamp() < 60) return true;
        return allows(m, important, s.distanceToSqr(t));
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("fire")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Text.gold(p, "Огневая дисциплина: " + NAMES[mode(p)]);
                    Text.info(p, "/regnum fire <свободный|засада|осада>. Засада — стрелять только по врагу ближе 12 блоков; осада — только по осадным машинам и атаманам.");
                    return 1;
                })
                .then(Commands.argument("режим", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    int m = byName(StringArgumentType.getString(ctx, "режим"));
                    if (m < 0) { Text.bad(p, "Есть: свободный, засада, осада."); return 0; }
                    set(p, m);
                    Text.good(p, "Огневая дисциплина: " + NAMES[m] + ".");
                    return 1;
                }))));
    }
}
