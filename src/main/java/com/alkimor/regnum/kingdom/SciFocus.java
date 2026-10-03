package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Научные специализации (I066): три ветви конкурируют за очки знаний. Курс на ветвь даёт +25% очков её технологиям
 * и -15% чужим ветвям. Базовые технологии (земледелие, коневодство) не штрафуются никогда, чтобы выживание нельзя было заблокировать.
 */
public final class SciFocus {
    private SciFocus() {}

    public static final int NONE = 0, WAR = 1, ECON = 2, MED = 3, BASE = 9;
    public static final String[] KEYS = {"нет", "война", "хозяйство", "медицина"};
    private static final Map<UUID, Integer> FOCUS = new HashMap<>();

    public static int focus(UUID owner) { return FOCUS.getOrDefault(owner, NONE); }

    public static int branchOf(Science.Tech t) {
        return switch (t) {
            case AGRICULTURE, HUSBANDRY -> BASE;
            case BRONZE, MASONRY, IRON, RIDING, COMPOSITE, STEEL, CHIVALRY, MACHINERY, ENGINEERING, ESPIONAGE,
                 GUNPOWDER, MUSKETRY, METALLURGY, BASTION, ARTILLERY -> WAR;
            case MEDICINE, CHEMISTRY -> MED;
            default -> ECON;
        };
    }

    public static double mult(int branch, int focus) {
        if (branch == BASE || focus == NONE) return 1.0;
        return branch == focus ? 1.25 : 0.85;
    }

    public static int byKey(String s) {
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equalsIgnoreCase(s)) return i;
        return -1;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("наука")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Text.gold(p, "Курс науки: " + KEYS[focus(p.getUUID())]);
                    Text.info(p, "Ветви: война, хозяйство, медицина. Курс: +25% очков своей ветви, -15% чужим; земледелие и коневодство не страдают.");
                    return 1;
                })
                .then(Commands.argument("ветвь", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    int k = byKey(StringArgumentType.getString(ctx, "ветвь"));
                    if (k < 0) { Text.bad(p, "Есть: война, хозяйство, медицина, нет."); return 0; }
                    if (k == NONE) FOCUS.remove(p.getUUID()); else FOCUS.put(p.getUUID(), k);
                    Text.good(p, k == NONE ? "Курс науки снят." : "Курс науки: " + KEYS[k] + ".");
                    return 1;
                }))));
    }


    public static void setFocus(UUID owner, int f) { if (f == NONE) FOCUS.remove(owner); else FOCUS.put(owner, f); }

    public static net.minecraft.nbt.CompoundTag toTag() {
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        FOCUS.forEach((k, v) -> t.putInt(k.toString(), v));
        return t;
    }

    public static void fromTag(net.minecraft.nbt.CompoundTag t) {
        FOCUS.clear();
        for (String k : t.getAllKeys()) FOCUS.put(UUID.fromString(k), t.getInt(k));
    }
}
