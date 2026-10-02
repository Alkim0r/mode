package com.alkimor.regnum.combat;

import com.alkimor.regnum.core.RegnumConfig;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Дефицит: в сундуках мира меньше ценностей. Алмазы, изумруды, книги чар и золотые яблоки
 * встречаются реже, железо и золото — меньшими стопками. Лут Regnum (данжи, боссы) не трогается.
 */
public class ScarcityModifier extends LootModifier {
    public static final MapCodec<ScarcityModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, ScarcityModifier::new));

    public ScarcityModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
        if (!RegnumConfig.LOOT_SCARCITY.get()) return loot;
        var id = ctx.getQueriedLootTableId();
        // Мародёр: в сундуках данжей и разбойников больше ценностей
        if ("regnum".equals(id.getNamespace()) && ctx.getParamOrNull(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY)
                instanceof net.minecraft.world.entity.player.Player p && com.alkimor.regnum.survival.Skills.has(p, com.alkimor.regnum.survival.Perk.RG_LOOT)
                && !loot.isEmpty()) {
            ItemStack pick = loot.get(ctx.getRandom().nextInt(loot.size()));
            pick.grow(Math.max(1, pick.getCount() / 2));
            com.alkimor.regnum.survival.Skills.addXp(p, com.alkimor.regnum.survival.Skill.ROGUERY, 3);
            return loot;
        }
        if (!"minecraft".equals(id.getNamespace()) || !id.getPath().startsWith("chests/")) return loot;
        int tier = RegnumConfig.tier();
        float dropRare = new float[]{0.25f, 0.5f, 0.7f}[tier];
        float halveCommon = new float[]{0.0f, 0.4f, 0.65f}[tier];
        ObjectArrayList<ItemStack> out = new ObjectArrayList<>();
        for (ItemStack s : loot) {
            if (s.is(Items.DIAMOND) || s.is(Items.EMERALD) || s.is(Items.ENCHANTED_BOOK) || s.is(Items.GOLDEN_APPLE)
                    || s.is(Items.ENCHANTED_GOLDEN_APPLE) || s.is(Items.NETHERITE_SCRAP) || s.is(Items.DIAMOND_HORSE_ARMOR)
                    || s.is(Items.EXPERIENCE_BOTTLE) || s.isEnchanted()) {
                if (ctx.getRandom().nextFloat() < dropRare) continue;
            } else if (s.is(Items.IRON_INGOT) || s.is(Items.GOLD_INGOT) || s.is(Items.IRON_NUGGET) || s.is(Items.GOLD_NUGGET)
                    || s.is(Items.BREAD) || s.is(Items.ARROW)) {
                if (s.getCount() > 1 && ctx.getRandom().nextFloat() < halveCommon) s.setCount(Math.max(1, s.getCount() / 2));
            }
            out.add(s);
        }
        return out;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
