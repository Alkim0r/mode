package com.alkimor.regnum.kingdom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.registries.DeferredItem;

import static com.alkimor.regnum.core.ModRegistries.ITEMS;

/** Сгенерировано tools/gen_weapons.py — оружие культур. */
public final class CultureWeapons {
    private CultureWeapons() {}

    public static final DeferredItem<CultureWeaponItem> NORTH_SPEAR = ITEMS.register("north_spear", () -> new CultureWeaponItem(Tiers.STONE, CultureWeaponItem.Kind.SPEAR, Culture.NORTH, "Широкое копьё на медведя и на ворога", new Item.Properties().rarity(Rarity.COMMON)));
    public static final DeferredItem<CultureWeaponItem> NORTH_SWORD = ITEMS.register("north_sword", () -> new CultureWeaponItem(Tiers.IRON, CultureWeaponItem.Kind.BROAD, Culture.NORTH, "Широкий клинок с долом — меч дружины", new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<CultureWeaponItem> NORTH_NOBLE = ITEMS.register("north_noble", () -> new CultureWeaponItem(Tiers.DIAMOND, CultureWeaponItem.Kind.BROAD, Culture.NORTH, "Узорчатый булат и золотая крестовина", new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<CultureWeaponItem> EMPIRE_SPEAR = ITEMS.register("empire_spear", () -> new CultureWeaponItem(Tiers.STONE, CultureWeaponItem.Kind.PILUM, Culture.EMPIRE, "Длинное острие гнётся в щите врага", new Item.Properties().rarity(Rarity.COMMON)));
    public static final DeferredItem<CultureWeaponItem> EMPIRE_SWORD = ITEMS.register("empire_sword", () -> new CultureWeaponItem(Tiers.IRON, CultureWeaponItem.Kind.SHORT, Culture.EMPIRE, "Короткий и быстрый — колоть из-за щита", new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<CultureWeaponItem> EMPIRE_NOBLE = ITEMS.register("empire_noble", () -> new CultureWeaponItem(Tiers.DIAMOND, CultureWeaponItem.Kind.STRAIGHT, Culture.EMPIRE, "Длинный меч всадников империи", new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<CultureWeaponItem> WEST_MACE = ITEMS.register("west_mace", () -> new CultureWeaponItem(Tiers.STONE, CultureWeaponItem.Kind.MACE, Culture.WEST, "Шипастый шар против кольчуги", new Item.Properties().rarity(Rarity.COMMON)));
    public static final DeferredItem<CultureWeaponItem> WEST_SWORD = ITEMS.register("west_sword", () -> new CultureWeaponItem(Tiers.IRON, CultureWeaponItem.Kind.STRAIGHT, Culture.WEST, "Прямой обоюдоострый клинок", new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<CultureWeaponItem> WEST_NOBLE = ITEMS.register("west_noble", () -> new CultureWeaponItem(Tiers.DIAMOND, CultureWeaponItem.Kind.LONG, Culture.WEST, "Длинная рукоять — бей хоть одной, хоть двумя", new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<CultureWeaponItem> STEPPE_SPEAR = ITEMS.register("steppe_spear", () -> new CultureWeaponItem(Tiers.STONE, CultureWeaponItem.Kind.SPEAR, Culture.STEPPE, "Лёгкое копьё всадника с бунчуком", new Item.Properties().rarity(Rarity.COMMON)));
    public static final DeferredItem<CultureWeaponItem> STEPPE_SWORD = ITEMS.register("steppe_sword", () -> new CultureWeaponItem(Tiers.IRON, CultureWeaponItem.Kind.SABRE, Culture.STEPPE, "Изогнутый клинок рубит с оттяжкой — раны кровоточат", new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<CultureWeaponItem> STEPPE_NOBLE = ITEMS.register("steppe_noble", () -> new CultureWeaponItem(Tiers.DIAMOND, CultureWeaponItem.Kind.SABRE, Culture.STEPPE, "Сабля нойонов, ножны в бирюзе", new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<CultureWeaponItem> SULTANATE_SPEAR = ITEMS.register("sultanate_spear", () -> new CultureWeaponItem(Tiers.STONE, CultureWeaponItem.Kind.SPEAR, Culture.SULTANATE, "Тонкое длинное острие", new Item.Properties().rarity(Rarity.COMMON)));
    public static final DeferredItem<CultureWeaponItem> SULTANATE_SWORD = ITEMS.register("sultanate_sword", () -> new CultureWeaponItem(Tiers.IRON, CultureWeaponItem.Kind.SCIMITAR, Culture.SULTANATE, "Широкий изогнутый клинок — глубокие раны", new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<CultureWeaponItem> SULTANATE_NOBLE = ITEMS.register("sultanate_noble", () -> new CultureWeaponItem(Tiers.DIAMOND, CultureWeaponItem.Kind.SHAMSHIR, Culture.SULTANATE, "Клинок-коготь султанской гвардии", new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<CultureWeaponItem> CLANS_AXE = ITEMS.register("clans_axe", () -> new CultureWeaponItem(Tiers.STONE, CultureWeaponItem.Kind.AXE, Culture.CLANS, "Бородовидный топор — пробивает щиты", new Item.Properties().rarity(Rarity.COMMON)));
    public static final DeferredItem<CultureWeaponItem> CLANS_SWORD = ITEMS.register("clans_sword", () -> new CultureWeaponItem(Tiers.IRON, CultureWeaponItem.Kind.FALCATA, Culture.CLANS, "Клинок с обратным изгибом, тяжёлый удар", new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<CultureWeaponItem> CLANS_NOBLE = ITEMS.register("clans_noble", () -> new CultureWeaponItem(Tiers.DIAMOND, CultureWeaponItem.Kind.CLAYMORE, Culture.CLANS, "Огромный двуручник вождей — широкий размах", new Item.Properties().rarity(Rarity.RARE)));

    /** Оружие солдата по культуре и роду войск (лучники — с луком). */
    public static Item forSoldier(Culture c, SoldierType t) {
        if (t == SoldierType.ARCHER) return Items.BOW;
        return switch (c) {
            case NORTH -> t == SoldierType.MILITIA ? NORTH_SPEAR.get() : t == SoldierType.KNIGHT ? NORTH_NOBLE.get() : NORTH_SWORD.get();
            case EMPIRE -> t == SoldierType.MILITIA ? EMPIRE_SPEAR.get() : t == SoldierType.KNIGHT ? EMPIRE_NOBLE.get() : EMPIRE_SWORD.get();
            case WEST -> t == SoldierType.MILITIA ? WEST_MACE.get() : t == SoldierType.KNIGHT ? WEST_NOBLE.get() : WEST_SWORD.get();
            case STEPPE -> t == SoldierType.MILITIA ? STEPPE_SPEAR.get() : t == SoldierType.KNIGHT ? STEPPE_NOBLE.get() : STEPPE_SWORD.get();
            case SULTANATE -> t == SoldierType.MILITIA ? SULTANATE_SPEAR.get() : t == SoldierType.KNIGHT ? SULTANATE_NOBLE.get() : SULTANATE_SWORD.get();
            case CLANS -> t == SoldierType.MILITIA ? CLANS_AXE.get() : t == SoldierType.KNIGHT ? CLANS_NOBLE.get() : CLANS_SWORD.get();
        };
    }

    public static void init() {}
}
