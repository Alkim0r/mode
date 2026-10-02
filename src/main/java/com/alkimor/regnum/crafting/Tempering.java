package com.alkimor.regnum.crafting;

import com.alkimor.regnum.core.InvUtil;
import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.dungeon.DungeonModule;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Закалка: каждая ступень даёт +1 к урону оружия или +1 к броне.
 * Шанс успеха зависит от навыка «Кузнечное дело» и текущей ступени.
 */
public final class Tempering {
    private Tempering() {}

    public static final int MAX_TEMPER = 6;
    /** Минимальный уровень кузнеца для перехода на ступень (индекс = целевая ступень). */
    private static final int[] REQUIRED_LEVEL = {0, 0, 0, 10, 30, 50, 90};

    public record Cost(Item item, int count) {}

    public static boolean isTemperable(ItemStack stack) {
        Item i = stack.getItem();
        return i instanceof SwordItem || i instanceof AxeItem || i instanceof TridentItem || i instanceof MaceItem || i instanceof ArmorItem;
    }

    public static int temper(ItemStack stack) {
        return stack.getOrDefault(CraftingModule.TEMPER.get(), 0);
    }

    public static List<Cost> costFor(int target) {
        List<Cost> c = new ArrayList<>();
        switch (target) {
            case 1, 2 -> c.add(new Cost(Items.IRON_INGOT, 3));
            case 3 -> { c.add(new Cost(Items.IRON_INGOT, 3)); c.add(new Cost(Items.GOLD_INGOT, 2)); }
            case 4 -> { c.add(new Cost(Items.DIAMOND, 1)); c.add(new Cost(Items.GOLD_INGOT, 2)); }
            case 5 -> { c.add(new Cost(DungeonModule.CRYPT_HEART.get(), 1)); c.add(new Cost(Items.DIAMOND, 2)); }
            default -> { c.add(new Cost(com.alkimor.regnum.dungeon.RegionsModule.STAR_IRON.get(), 2)); c.add(new Cost(Items.NETHERITE_SCRAP, 1)); }
        }
        return c;
    }

    public static float chance(int target, int smithLevel) {
        float c = 0.95f - 0.15f * (target - 1) + 0.004f * smithLevel;
        return Math.max(0.1f, Math.min(0.98f, c));
    }

    public static String describeCost(List<Cost> cost) {
        StringBuilder sb = new StringBuilder();
        for (Cost c : cost) {
            if (!sb.isEmpty()) sb.append(" + ");
            sb.append(c.count()).append("× ").append(c.item().getDescription().getString());
        }
        return sb.toString();
    }

    public static void attempt(ServerPlayer player, ItemStack stack, ServerLevel level, BlockPos pos) {
        int cur = temper(stack);
        if (cur >= MAX_TEMPER) {
            Text.info(player, "Этот предмет уже закалён до предела.");
            return;
        }
        int target = cur + 1;
        int lvl = Skills.level(player, Skill.SMITHING);
        if (target == 6 && !Skills.has(player, com.alkimor.regnum.survival.Perk.SM_MASTER)) {
            Text.bad(player, "Шестая ступень доступна только мастеру-кузнецу (перк «Мастер-кузнец»).");
            return;
        }
        if (lvl < REQUIRED_LEVEL[target]) {
            Text.bad(player, "Для закалки до ступени " + target + " нужен уровень кузнеца " + REQUIRED_LEVEL[target] + " (у вас " + lvl + ").");
            return;
        }
        List<Cost> cost = costFor(target);
        for (Cost c : cost) {
            if (!player.getAbilities().instabuild && InvUtil.count(player, c.item()) < c.count()) {
                Text.bad(player, "Не хватает материалов: " + describeCost(cost));
                return;
            }
        }
        for (Cost c : cost) InvUtil.take(player, c.item(), c.count());

        float ch = chance(target, lvl) + (Skills.has(player, com.alkimor.regnum.survival.Perk.SM_TEMPER) ? 0.1f : 0f);
        if (player.getRandom().nextFloat() < ch) {
            stack.set(CraftingModule.TEMPER.get(), target);
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1f, 1.1f);
            level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.0);
            Text.good(player, "Удача! «" + stack.getHoverName().getString() + "» закалён до ступени " + target + ".");
            Skills.addXp(player, Skill.SMITHING, 15 * target);
        } else {
            level.playSound(null, pos, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 0.8f, 0.9f);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 10, 0.2, 0.1, 0.2, 0.01);
            if (cur >= 3 && !Skills.has(player, com.alkimor.regnum.survival.Perk.SM_NOCRACK) && player.getRandom().nextFloat() < 0.3f) {
                stack.set(CraftingModule.TEMPER.get(), cur - 1);
                Text.bad(player, "Металл треснул! Закалка упала до ступени " + (cur - 1) + ".");
            } else {
                Text.bad(player, "Неудача — материалы испорчены. (шанс был " + Math.round(ch * 100) + "%)");
            }
            Skills.addXp(player, Skill.SMITHING, 5 + 2 * target);
        }
    }

    /** Ремонт: тратит материал, из которого сделан предмет (или железо), чинит 25%+ прочности. */
    public static void repair(ServerPlayer player, ItemStack stack, ServerLevel level, BlockPos pos) {
        Item material = null;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack cand = inv.getItem(i);
            if (!cand.isEmpty() && cand != stack && stack.getItem().isValidRepairItem(stack, cand)) {
                material = cand.getItem();
                break;
            }
        }
        if (material == null) material = Items.IRON_INGOT;
        boolean thrift = Skills.has(player, com.alkimor.regnum.survival.Perk.SM_THRIFT) && player.getRandom().nextFloat() < 0.25f;
        if (!thrift && !InvUtil.take(player, material, 1)) {
            Text.bad(player, "Для ремонта нужен материал: " + material.getDescription().getString() + ".");
            return;
        }
        int lvl = Skills.level(player, Skill.SMITHING);
        float part = (0.25f + 0.003f * lvl) * (Skills.has(player, com.alkimor.regnum.survival.Perk.SM_REPAIR) ? 1.25f : 1f)
                * (Skills.has(player, com.alkimor.regnum.survival.Trait.CRAFTSMAN) ? 1.1f : 1f);
        if (Skills.has(player, com.alkimor.regnum.survival.Perk.SM_FULL)) part = 1f;
        int fix = Math.max(1, Math.round(stack.getMaxDamage() * part));
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - fix));
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7f, 1.4f);
        level.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.05);
        int pct = Math.round(100f * (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage());
        Text.good(player, "Отремонтировано: «" + stack.getHoverName().getString() + "» — прочность " + pct + "%.");
        Skills.addXp(player, Skill.SMITHING, 4);
    }

    public static void showInfo(ServerPlayer player, ItemStack held) {
        int lvl = Skills.level(player, Skill.SMITHING);
        player.sendSystemMessage(Text.of("⚒ Горн мастера — Кузнечное дело: " + lvl + " ур.", ChatFormatting.GOLD));
        Text.info(player, "Повреждённый предмет в руке + ПКМ — ремонт. Целый меч, топор, трезубец, булава или броня — закалка.");
        for (int t = 1; t <= MAX_TEMPER; t++) {
            String req = REQUIRED_LEVEL[t] > 0 ? " [нужен ур. " + REQUIRED_LEVEL[t] + "]" : "";
            player.sendSystemMessage(Text.of("  Ступень " + t + ": " + describeCost(costFor(t)) + " — шанс " + Math.round(chance(t, lvl) * 100) + "%" + req,
                    lvl >= REQUIRED_LEVEL[t] ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY));
        }
    }
}
