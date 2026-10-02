package com.alkimor.regnum.survival;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Грязная броня (идея «Dirty Clothes», переписано под Regnum): сапоги и поножи пачкаются от ходьбы и бега,
 * шлем и кираса — от полученных ударов. Грязная броня хуже защищает (до +15% входящего урона),
 * смывается в воде (быстро) и под дождём (медленно).
 */
public final class ArmorDirt {
    private ArmorDirt() {}

    public static final String KEY = "regnum_armor_dirt";
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static int dirt(ItemStack s) {
        if (s.isEmpty()) return 0;
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        return cd == null ? 0 : cd.copyTag().getInt(KEY);
    }

    public static void add(ItemStack s, int n) {
        if (s.isEmpty()) return;
        int d = Math.max(0, Math.min(100, dirt(s) + n));
        s.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, cd -> cd.update(t -> t.putInt(KEY, d)));
    }

    /** Средняя грязь надетой брони 0..100 (учитываются только занятые слоты). */
    public static int average(Player p) {
        int sum = 0, n = 0;
        for (EquipmentSlot es : SLOTS) {
            ItemStack s = p.getItemBySlot(es);
            if (s.isEmpty()) continue;
            sum += dirt(s); n++;
        }
        return n == 0 ? 0 : sum / n;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isCreative() || p.isSpectator() || p.tickCount % 100 != 0) return;
        if (!Injuries.enabled()) return;
        int before = average(p);
        if (p.isInWater()) {
            for (EquipmentSlot es : SLOTS) add(p.getItemBySlot(es), -12);
        } else if (p.isInWaterOrRain()) {
            for (EquipmentSlot es : SLOTS) add(p.getItemBySlot(es), -2);
        } else if (p.onGround() && p.walkDist != 0) {
            int step = p.isSprinting() ? 2 : 1;
            add(p.getItemBySlot(EquipmentSlot.FEET), step);
            add(p.getItemBySlot(EquipmentSlot.LEGS), step);
        }
        int now = average(p);
        if (before < 60 && now >= 60)
            p.displayClientMessage(Component.literal("Броня в грязи и защищает хуже. Постирайте её в воде.").withStyle(ChatFormatting.GOLD), true);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !Injuries.enabled()) return;
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) return;
        ItemStack hit = p.getItemBySlot(p.getRandom().nextBoolean() ? EquipmentSlot.HEAD : EquipmentSlot.CHEST);
        add(hit, 2);
        int avg = average(p);
        if (avg >= 30) event.setAmount(event.getAmount() * (1f + 0.15f * Math.min(100, avg) / 100f));
    }
}
