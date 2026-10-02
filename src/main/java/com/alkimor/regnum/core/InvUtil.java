package com.alkimor.regnum.core;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Подсчёт и изъятие предметов из инвентаря игрока. */
public final class InvUtil {
    private InvUtil() {}

    public static int count(Player p, Item item) {
        int n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    /** Забирает ровно amount предметов, если их хватает. Возвращает true при успехе. */
    public static boolean take(Player p, Item item, int amount) {
        if (amount <= 0) return true;
        if (p.getAbilities().instabuild) return true;
        if (count(p, item) < amount) return false;
        Inventory inv = p.getInventory();
        int left = amount;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) {
                int t = Math.min(left, s.getCount());
                s.shrink(t);
                left -= t;
            }
        }
        inv.setChanged();
        return true;
    }

    /** Выдать предмет, а если инвентарь полон — выбросить под ноги. */
    public static void give(Player p, ItemStack stack) {
        if (!p.getInventory().add(stack)) {
            p.drop(stack, false);
        }
    }
}
