package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.UUID;

/**
 * Пленные (I032): разбойничьи главари, павшие у стен города от рук его бойцов или героя, берутся живыми:
 * с шансом 60% главаря берут в плен вместо смерти. Пленных можно выкупить у семьи (золото в казну),
 * отпустить (слава и честь), либо отдать на работы (бесплатно подновляют разрушенное).
 */
public final class Prisoners {
    private Prisoners() {}

    public static final int RANSOM = 90;

    /** Берёт в плен главаря, если он пал рядом с городом; возвращает город или null. */
    public static City capture(ServerLevel sl, BanditEntity b, UUID byOwner) {
        if (b.getVariant() != BanditEntity.CAPTAIN) return null;
        City c = KingdomData.get(sl.getServer()).at(b.blockPosition());
        if (c == null || byOwner == null || !c.owner.equals(byOwner)) return null;
        if (sl.random.nextFloat() >= 0.6f) return null;
        c.prisoners++;
        KingdomData.get(sl.getServer()).setDirty();
        return c;
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof BanditEntity b) || !(b.level() instanceof ServerLevel sl)) return;
        var src = e.getSource().getEntity();
        UUID owner = src instanceof ServerPlayer sp ? sp.getUUID() : src instanceof SoldierEntity se ? se.getOwnerId() : null;
        City c = capture(sl, b, owner);
        if (c == null) return;
        ServerPlayer p = sl.getServer().getPlayerList().getPlayer(c.owner);
        if (p != null) Text.good(p, "Главарь разбойников взят в плен! Пленных: " + c.prisoners + " (/regnum prisoners).");
    }

    /** null — успех, иначе причина отказа; mode: ransom | release | labor. */
    public static String act(ServerPlayer p, City c, String mode) {
        if (c.prisoners <= 0) return "В городе нет пленных.";
        switch (mode) {
            case "ransom" -> { c.prisoners--; c.treasury += RANSOM; }
            case "release" -> {
                c.prisoners--; c.glory += 3;
                com.alkimor.regnum.survival.Skills.addHonor(p, 2, "милость к пленному");
            }
            case "labor" -> {
                c.prisoners--;
                String r = Ruins.repairFree(c);
                if (r == null) { c.prisoners++; return "Чинить нечего — разрушенных построек нет."; }
            }
            default -> { return "Режимы: ransom, release, labor."; }
        }
        KingdomData.get(p.server).setDirty();
        return null;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("prisoners")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(p.server).at(p.blockPosition());
                    if (c == null || !c.owner.equals(p.getUUID())) { Text.bad(p, "Встаньте на земле своего города."); return 0; }
                    Text.gold(p, "══ Пленные ══");
                    Text.info(p, "В «" + c.name + "»: " + c.prisoners + ". Выкуп — " + RANSOM + " монет в казну за каждого.");
                    Text.info(p, "/regnum prisoners ransom — выкуп; release — отпустить (слава, честь); labor — на починку руин.");
                    return 1;
                })
                .then(Commands.argument("mode", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(p.server).at(p.blockPosition());
                    if (c == null || !c.owner.equals(p.getUUID())) { Text.bad(p, "Встаньте на земле своего города."); return 0; }
                    String err = act(p, c, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "mode"));
                    if (err != null) { Text.bad(p, err); return 0; }
                    Text.good(p, "Готово. Пленных осталось: " + c.prisoners + ", казна: " + c.treasury + ".");
                    return 1;
                }))));
    }
}
