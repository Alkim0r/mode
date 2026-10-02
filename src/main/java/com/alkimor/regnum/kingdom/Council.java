package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Map;
import java.util.UUID;

/** Совет королевства: король выдаёт друзьям отдельные права (казна, стройка, склад, командование, дипломатия). Сервер проверяет каждое действие. */
public final class Council {
    private Council() {}

    public enum Role {
        TREASURY(1, "казна", "вносить и снимать монеты, улучшать город, вкладывать в облигации"),
        BUILD(2, "стройка", "ставить здания и размечать стены"),
        DIPLOMACY(4, "дипломатия", "принимать и отклонять предложения послов"),
        STOCK(8, "склад", "сдавать на склад и распоряжаться запасами"),
        COMMAND(16, "командование", "нанимать бойцов и менять отряд найма");

        public final int bit;
        public final String title, desc;

        Role(int bit, String title, String desc) {
            this.bit = bit;
            this.title = title;
            this.desc = desc;
        }

        public static Role byName(String n) {
            for (Role r : values()) if (r.name().equalsIgnoreCase(n) || r.title.equalsIgnoreCase(n)) return r;
            return null;
        }
    }

    /** Есть ли у игрока право в городе: правитель — всегда; доверенный — стройка и склад; остальным — выданные роли. */
    public static boolean can(City c, UUID player, Role r) {
        if (c.owner.equals(player)) return true;
        if (c.trusted.contains(player) && (r == Role.BUILD || r == Role.STOCK)) return true;
        return (c.roles.getOrDefault(player, 0) & r.bit) != 0;
    }

    public static boolean isMember(City c, UUID player) {
        return c.owner.equals(player) || c.trusted.contains(player) || c.roles.getOrDefault(player, 0) != 0;
    }

    public static String describe(City c, UUID player) {
        if (c.owner.equals(player)) return "правитель";
        StringBuilder sb = new StringBuilder();
        for (Role r : Role.values()) if (can(c, player, r)) sb.append(sb.isEmpty() ? "" : ", ").append(r.title);
        return sb.isEmpty() ? "нет прав" : sb.toString();
    }

    private static City ownCity(ServerPlayer p) {
        City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
        if (c == null) Text.bad(p, "Нужен свой город рядом.");
        return c;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("council")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = ownCity(p);
                    if (c == null) return 0;
                    Text.gold(p, "══ Совет «" + c.name + "» ══");
                    Text.info(p, "Правитель — все права.");
                    for (Map.Entry<UUID, Integer> e : c.roles.entrySet()) {
                        ServerPlayer o = p.server.getPlayerList().getPlayer(e.getKey());
                        Text.info(p, (o != null ? o.getName().getString() : e.getKey().toString().substring(0, 8)) + ": " + describe(c, e.getKey()));
                    }
                    var top = new java.util.ArrayList<>(c.contrib.entrySet());
                    top.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
                    int shown = 0;
                    for (var e : top) {
                        if (shown++ >= 5) break;
                        ServerPlayer o = p.server.getPlayerList().getPlayer(e.getKey());
                        Text.info(p, "Вклад: " + (o != null ? o.getName().getString() : e.getKey().toString().substring(0, 8)) + " — " + e.getValue() + " (" + City.rankTitle(e.getValue()) + ")");
                    }
                    Text.info(p, "Роли: казна, стройка, дипломатия, склад, командование. /regnum council grant|revoke <игрок> <роль>");
                    return 1;
                })
                .then(Commands.literal("grant").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("role", StringArgumentType.word()).executes(ctx -> change(ctx, true)))))
                .then(Commands.literal("revoke").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("role", StringArgumentType.word()).executes(ctx -> change(ctx, false)))))));
    }

    private static int change(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> ctx, boolean grant) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
        City c = ownCity(p);
        if (c == null) return 0;
        Role r = Role.byName(StringArgumentType.getString(ctx, "role"));
        if (r == null) { Text.bad(p, "Неизвестная роль. Есть: казна, стройка, дипломатия, склад, командование."); return 0; }
        if (t.getUUID().equals(p.getUUID())) { Text.info(p, "Правитель и так может всё."); return 0; }
        int cur = c.roles.getOrDefault(t.getUUID(), 0);
        cur = grant ? cur | r.bit : cur & ~r.bit;
        if (cur == 0) c.roles.remove(t.getUUID()); else c.roles.put(t.getUUID(), cur);
        KingdomData.get(p.server).setDirty();
        Text.good(p, t.getName().getString() + (grant ? " получает право: " : " теряет право: ") + r.title + " (" + r.desc + ").");
        Text.info(t, p.getName().getString() + (grant ? " доверил вам: " : " отозвал у вас: ") + r.title + " в «" + c.name + "».");
        return 1;
    }
}
