package com.alkimor.regnum.kingdom;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Ресурсы городского склада. Игроки сдают предметы на склад (ПКМ), город тратит их каждый день. */
public enum Resource {
    FOOD("Провиант", "Кормит жителей и армию"),
    WOOD("Дерево", "Развитие города, стены, осадные машины"),
    STONE("Камень", "Развитие города, стены, бастионы"),
    IRON("Железо", "Кузница куёт броню бойцам; осадные орудия"),
    HERBS("Травы", "Лазарет лечит и борется с эпидемиями"),
    GUNPOWDER("Порох", "Мушкетёры, бомбарды (эра пороха)");

    public final String title, use;

    Resource(String title, String use) {
        this.title = title;
        this.use = use;
    }

    public static Resource byName(String n) {
        for (Resource r : values()) if (r.name().equalsIgnoreCase(n)) return r;
        return null;
    }

    /** Сколько единиц ресурса даёт один предмет, и какого. */
    public record Value(Resource res, int units) {}

    public static Value of(ItemStack s) {
        if (s.isEmpty()) return null;
        Item i = s.getItem();
        if (i == Items.WHEAT || i == Items.CARROT || i == Items.POTATO || i == Items.BEETROOT || i == Items.APPLE || i == Items.MELON_SLICE)
            return new Value(FOOD, 1);
        if (i == Items.BREAD || i == Items.BAKED_POTATO || i == Items.PUMPKIN_PIE) return new Value(FOOD, 2);
        if (i == Items.COOKED_BEEF || i == Items.COOKED_PORKCHOP || i == Items.COOKED_MUTTON || i == Items.COOKED_CHICKEN
                || i == Items.COOKED_RABBIT || i == Items.COOKED_COD || i == Items.COOKED_SALMON) return new Value(FOOD, 3);
        if (s.is(ItemTags.LOGS)) return new Value(WOOD, 4);
        if (s.is(ItemTags.PLANKS)) return new Value(WOOD, 1);
        if (i == Items.COBBLESTONE || i == Items.STONE || i == Items.COBBLED_DEEPSLATE || i == Items.STONE_BRICKS || i == Items.ANDESITE
                || i == Items.DIORITE || i == Items.GRANITE) return new Value(STONE, 1);
        if (i == Items.IRON_INGOT) return new Value(IRON, 5);
        if (i == Items.RAW_IRON) return new Value(IRON, 3);
        if (i == Items.IRON_BLOCK) return new Value(IRON, 45);
        if (s.is(ItemTags.FLOWERS) || i == Items.FERN || i == Items.LARGE_FERN || i == Items.SWEET_BERRIES || i == Items.GLOW_BERRIES
                || i == Items.BROWN_MUSHROOM || i == Items.RED_MUSHROOM) return new Value(HERBS, 1);
        if (i == Items.GOLDEN_CARROT || i == Items.HONEY_BOTTLE) return new Value(HERBS, 4);
        if (i == Items.GUNPOWDER) return new Value(GUNPOWDER, 1);
        return null;
    }
}
