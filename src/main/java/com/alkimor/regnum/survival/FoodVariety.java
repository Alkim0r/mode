package com.alkimor.regnum.survival;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * Разнообразие питания (идея «Spice of Life», переписано под Regnum): одна и та же еда подряд приедается
 * (питает вдвое хуже), разнообразный стол даёт крепость: 6 разных блюд из последних 8 — «Сытость» (+2 сердца поглощения).
 */
public final class FoodVariety {
    private FoodVariety() {}

    public static final String KEY = "regnum_foods";
    public static final int MEMORY = 8, BONUS_KINDS = 6;

    /** Записывает съеденное, возвращает число разных блюд в памяти и признак «приелось». */
    public static int[] record(ServerPlayer p, String id) {
        var pd = p.getPersistentData();
        ListTag l = pd.getList(KEY, net.minecraft.nbt.Tag.TAG_STRING);
        boolean repeat = false;
        for (int i = Math.max(0, l.size() - 3); i < l.size(); i++) if (l.getString(i).equals(id)) repeat = true;
        l.add(StringTag.valueOf(id));
        while (l.size() > MEMORY) l.remove(0);
        pd.put(KEY, l);
        java.util.Set<String> kinds = new java.util.HashSet<>();
        for (int i = 0; i < l.size(); i++) kinds.add(l.getString(i));
        return new int[]{kinds.size(), repeat ? 1 : 0};
    }

    @SubscribeEvent
    public static void onUse(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !Injuries.enabled()) return;
        ItemStack s = event.getItem();
        var food = s.get(DataComponents.FOOD);
        if (food == null || food.nutrition() < 2) return;
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
        int[] r = record(p, id);
        if (r[1] == 1) {
            int back = Math.max(1, food.nutrition() / 2);
            p.getFoodData().setFoodLevel(Math.max(0, p.getFoodData().getFoodLevel() - back));
            p.displayClientMessage(Component.literal("Одно и то же приелось — разнообразьте стол.").withStyle(ChatFormatting.GRAY), true);
        } else if (r[0] >= BONUS_KINDS) {
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 180, 0, false, true, true));
            p.displayClientMessage(Component.literal("Разнообразный стол придаёт сил.").withStyle(ChatFormatting.GREEN), true);
        }
    }
}
