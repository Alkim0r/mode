package com.alkimor.regnum.story;

import com.alkimor.regnum.core.Text;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Защита от тупика: потеряв всё, герой не остаётся без шанса. Дар странника — скромный набор,
 * выдаётся после смерти с пустым инвентарём и по команде, не чаще раза в игровые сутки.
 */
public final class Recovery {
    private Recovery() {}

    public static final String KEY = "regnum_gift_last";
    public static final long COOLDOWN = 24000L;

    public static int itemCount(Player p) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (!s.isEmpty()) n++;
        for (ItemStack s : p.getInventory().armor) if (!s.isEmpty()) n++;
        return n;
    }

    public static boolean destitute(Player p) {
        return itemCount(p) <= 2;
    }

    public static long remaining(ServerPlayer p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(KEY)) return 0;
        return Math.max(0, pd.getLong(KEY) + COOLDOWN - p.serverLevel().getGameTime());
    }

    /** Выдать набор, если герой нуждается и сутки прошли. */
    public static boolean give(ServerPlayer p) {
        if (!destitute(p) || remaining(p) > 0) return false;
        p.getPersistentData().putLong(KEY, p.serverLevel().getGameTime());
        ItemStack[] kit = {
                new ItemStack(Items.BREAD, 6), new ItemStack(Items.STONE_SWORD), new ItemStack(Items.STONE_PICKAXE),
                new ItemStack(Items.STONE_AXE), new ItemStack(Items.TORCH, 8), new ItemStack(Items.LEATHER_CHESTPLATE)};
        for (ItemStack s : kit) if (!p.getInventory().add(s)) p.drop(s, false);
        Text.gold(p, "✦ Дар странника: добрые люди собрали вам немного хлеба и простой инструмент.");
        Text.info(p, "Поручения города (/regnum contracts) и задания (/regnum story) помогут встать на ноги.");
        return true;
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && !e.isEndConquered()) {
            if (!give(p) && destitute(p) && remaining(p) > 0)
                Text.info(p, "Следующий дар странника — через " + (remaining(p) / 1200 + 1) + " мин.");
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("gift").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            if (!destitute(p)) {
                Text.bad(p, "Дар странника — только тем, у кого почти ничего не осталось.");
                return 0;
            }
            if (remaining(p) > 0) {
                Text.bad(p, "Подождите ещё " + (remaining(p) / 1200 + 1) + " мин.");
                return 0;
            }
            return give(p) ? 1 : 0;
        })));
    }
}
