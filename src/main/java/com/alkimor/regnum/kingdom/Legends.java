package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.Random;

/**
 * Легендарные клинки: кузнец города (здание «Кузница» + железо на складе + золото) перековывает оружие в руке в именное,
 * с прибавкой урона и скорости и вечной прочностью. Один клинок на мастера-оружейника уровня кузницы.
 */
public final class Legends {
    private Legends() {}

    public static final int IRON = 40, GOLD = 120;
    private static final String[] FIRST = {"Рассвет", "Гром", "Клык", "Заря", "Вихрь", "Жало", "Пламя", "Буран", "Страж", "Ярость"};
    private static final String[] SECOND = {"королей", "предков", "севера", "степи", "империи", "кланов", "пустыни", "падших", "героев", "бури"};

    public static boolean isLegend(ItemStack s) {
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        return cd != null && cd.copyTag().getBoolean("regnum_legend");
    }

    /** Возвращает null при успехе или причину отказа. */
    public static String forge(ServerPlayer p, City c, ItemStack s, Random r) {
        if (s.isEmpty() || s.getMaxDamage() <= 0 && s.getAttributeModifiers().modifiers().isEmpty()) return "Возьмите в руку оружие.";
        if (isLegend(s)) return "Это оружие уже легендарное.";
        if (s.getAttributeModifiers().modifiers().stream().noneMatch(e -> e.attribute().is(Attributes.ATTACK_DAMAGE.unwrapKey().orElseThrow()))) return "Перековывается только оружие, наносящее урон.";
        if (c.count(BuildingType.SMITHY) < 1) return "В городе нет кузницы.";
        int slots = c.count(BuildingType.SMITHY) + (c.level >= 5 ? 1 : 0);
        if (c.legends >= slots) return "Кузница уже выковала " + c.legends + " легенд(ы) — расширьте город или постройте ещё кузницу.";
        if (c.stock(Resource.IRON) < IRON) return "Нужно " + IRON + " железа на складе.";
        if (c.treasury < GOLD) return "Нужно " + GOLD + " монет в казне.";
        c.take(Resource.IRON, IRON);
        c.treasury -= GOLD;
        c.legends++;
        ItemAttributeModifiers cur = s.getAttributeModifiers();
        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
        for (ItemAttributeModifiers.Entry e : cur.modifiers()) b.add(e.attribute(), e.modifier(), e.slot());
        b.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(ResourceLocation.fromNamespaceAndPath("regnum", "legend_damage"), 3.0 + r.nextInt(3), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        b.add(Attributes.ATTACK_SPEED, new AttributeModifier(ResourceLocation.fromNamespaceAndPath("regnum", "legend_speed"), 0.2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        s.set(DataComponents.ATTRIBUTE_MODIFIERS, b.build());
        String name = FIRST[r.nextInt(FIRST.length)] + " " + SECOND[r.nextInt(SECOND.length)];
        s.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        s.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("Легендарное оружие города «" + c.name + "»").withStyle(ChatFormatting.DARK_PURPLE),
                Component.literal("Заказчик: " + p.getGameProfile().getName()).withStyle(ChatFormatting.GRAY))));
        s.set(DataComponents.UNBREAKABLE, new net.minecraft.world.item.component.Unbreakable(true));
        CompoundTag tag = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean("regnum_legend", true);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        Skills.addXp(p, Skill.SMITHING, 6);
        return null;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("legend").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            KingdomData data = KingdomData.get(p.server);
            City c = data.nearestOwned(p.getUUID(), p.blockPosition());
            if (c == null) { Text.bad(p, "Нужен свой город рядом."); return 0; }
            String err = forge(p, c, p.getMainHandItem(), new Random());
            if (err != null) { Text.bad(p, err); return 0; }
            data.setDirty();
            Text.good(p, "⚔ Кузнецы «" + c.name + "» перековали клинок: " + p.getMainHandItem().getHoverName().getString() + ". Урон +3…5, скорость выше, не ломается.");
            return 1;
        })));
    }
}
