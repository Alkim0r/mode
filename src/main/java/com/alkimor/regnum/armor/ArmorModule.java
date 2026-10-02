package com.alkimor.regnum.armor;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.core.RegnumModule;
import com.alkimor.regnum.kingdom.Culture;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.alkimor.regnum.core.ModRegistries.ITEMS;

/** Комплекты брони шести культур с бонусами за полный комплект. Ресурсы — tools/gen_armor.py. */
public final class ArmorModule implements RegnumModule {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Regnum.MODID);

    /** Защита (шлем, нагрудник, поножи, сапоги), стойкость, сопротивление отбрасыванию. */
    private record Spec(int h, int c, int l, int b, float tough, float kb, int durMul) {}

    private static final Map<Culture, Spec> SPEC = new EnumMap<>(Culture.class);

    static {
        SPEC.put(Culture.NORTH, new Spec(3, 7, 5, 3, 1f, 0.05f, 30));
        SPEC.put(Culture.EMPIRE, new Spec(3, 8, 6, 3, 2f, 0.05f, 34));
        SPEC.put(Culture.WEST, new Spec(3, 8, 6, 3, 2f, 0.10f, 36));
        SPEC.put(Culture.STEPPE, new Spec(2, 6, 4, 2, 0f, 0f, 26));
        SPEC.put(Culture.SULTANATE, new Spec(2, 5, 4, 2, 0f, 0f, 24));
        SPEC.put(Culture.CLANS, new Spec(2, 5, 4, 2, 0f, 0.05f, 26));
    }

    public static final Map<Culture, Holder<ArmorMaterial>> MAT = new EnumMap<>(Culture.class);
    public static final Map<Culture, List<DeferredItem<CultureArmorItem>>> PIECES = new EnumMap<>(Culture.class);

    static {
        for (Culture c : Culture.values()) {
            Spec s = SPEC.get(c);
            DeferredHolder<ArmorMaterial, ArmorMaterial> m = MATERIALS.register(c.id, () -> new ArmorMaterial(
                    Map.of(ArmorItem.Type.HELMET, s.h, ArmorItem.Type.CHESTPLATE, s.c, ArmorItem.Type.LEGGINGS, s.l, ArmorItem.Type.BOOTS, s.b),
                    13, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(net.minecraft.world.item.Items.IRON_INGOT),
                    List.of(new ArmorMaterial.Layer(Regnum.id(c.id))), s.tough, s.kb));
            MAT.put(c, m);
            String[] names = {"helmet", "chestplate", "leggings", "boots"};
            ArmorItem.Type[] types = {ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS};
            java.util.ArrayList<DeferredItem<CultureArmorItem>> list = new java.util.ArrayList<>();
            for (int i = 0; i < 4; i++) {
                ArmorItem.Type t = types[i];
                list.add(ITEMS.register(c.id + "_" + names[i], () -> new CultureArmorItem(c, m, t,
                        new Item.Properties().durability(t.getDurability(s.durMul)).rarity(Rarity.UNCOMMON))));
            }
            PIECES.put(c, list);
        }
    }

    public static String setName(Culture c) {
        return switch (c) {
            case NORTH -> "Дружинная чешуя";
            case EMPIRE -> "Сегментата легионера";
            case WEST -> "Латы рыцаря";
            case STEPPE -> "Ламеллярный панцирь";
            case SULTANATE -> "Бармица гулама";
            case CLANS -> "Кожа и килт клана";
        };
    }

    public static String setBonusText(Culture c) {
        return switch (c) {
            case NORTH -> "+4 к здоровью (два сердца)";
            case EMPIRE -> "+2 стойкости брони и +20% сопротивления отбрасыванию";
            case WEST -> "+2 брони и +30% сопротивления отбрасыванию, но вы медленнее";
            case STEPPE -> "+10% скорости и +0.3 к скорости атаки";
            case SULTANATE -> "+8% скорости и +2 к здоровью";
            case CLANS -> "+2 к урону и +2 к здоровью";
        };
    }

    private static final Map<Culture, Object[][]> BONUS = new EnumMap<>(Culture.class);

    private static void b(Culture c, Object... flat) {
        Object[][] arr = new Object[flat.length / 3][];
        for (int i = 0; i < arr.length; i++) arr[i] = new Object[]{flat[i * 3], flat[i * 3 + 1], flat[i * 3 + 2]};
        BONUS.put(c, arr);
    }

    static {
        AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE, MUL = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        b(Culture.NORTH, Attributes.MAX_HEALTH, 4.0, ADD);
        b(Culture.EMPIRE, Attributes.ARMOR_TOUGHNESS, 2.0, ADD, Attributes.KNOCKBACK_RESISTANCE, 0.2, ADD);
        b(Culture.WEST, Attributes.ARMOR, 2.0, ADD, Attributes.KNOCKBACK_RESISTANCE, 0.3, ADD, Attributes.MOVEMENT_SPEED, -0.06, MUL);
        b(Culture.STEPPE, Attributes.MOVEMENT_SPEED, 0.10, MUL, Attributes.ATTACK_SPEED, 0.3, ADD);
        b(Culture.SULTANATE, Attributes.MOVEMENT_SPEED, 0.08, MUL, Attributes.MAX_HEALTH, 2.0, ADD);
        b(Culture.CLANS, Attributes.ATTACK_DAMAGE, 2.0, ADD, Attributes.MAX_HEALTH, 2.0, ADD);
    }

    /** Культура, полный комплект которой надет, иначе null. */
    public static Culture fullSet(Player p) {
        Culture found = null;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = p.getItemBySlot(s);
            if (!(st.getItem() instanceof CultureArmorItem a)) return null;
            if (found == null) found = a.culture;
            else if (found != a.culture) return null;
        }
        return found;
    }

    private static ResourceLocation modId(Culture c, int i) {
        return Regnum.id("set_" + c.id + "_" + i);
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 7) return;
        Culture full = fullSet(p);
        for (Culture c : Culture.values()) {
            Object[][] bonus = BONUS.get(c);
            for (int i = 0; i < bonus.length; i++) {
                AttributeInstance inst = p.getAttribute((Holder<Attribute>) bonus[i][0]);
                if (inst == null) continue;
                ResourceLocation id = modId(c, i);
                boolean has = inst.getModifier(id) != null;
                if (c == full && !has) {
                    inst.addTransientModifier(new AttributeModifier(id, (Double) bonus[i][1], (AttributeModifier.Operation) bonus[i][2]));
                    if (bonus[i][0] == Attributes.MAX_HEALTH) p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 1f));
                } else if (c != full && has) {
                    inst.removeModifier(id);
                    if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
                }
            }
        }
    }

    @Override
    public String id() {
        return "armor";
    }

    @Override
    public String title() {
        return "Броня культур";
    }

    @Override
    public void init(IEventBus modBus) {
        MATERIALS.register(modBus);
        NeoForge.EVENT_BUS.register(ArmorModule.class);
    }
}
