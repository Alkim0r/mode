package com.alkimor.regnum.survival;

import com.alkimor.regnum.Regnum;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Постоянные бонусы героя через атрибуты Minecraft: от навыков, перков и черт.
 * Пересчитывается при входе, возрождении, повышении уровня и выборе перка.
 */
public final class PlayerStats {
    private PlayerStats() {}

    private record Mod(Holder<Attribute> attr, String id, double value, AttributeModifier.Operation op) {}

    private static final List<ResourceLocation> USED = new ArrayList<>();

    private static List<Mod> collect(SurvivorData d) {
        List<Mod> m = new ArrayList<>();
        var ADD = AttributeModifier.Operation.ADD_VALUE;
        var MUL = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        // навыки
        int ath = d.level(Skill.ATHLETICS);
        if (ath >= 25) m.add(new Mod(Attributes.MAX_HEALTH, "ath_hp", 2 * (ath / 25), ADD));
        int eng = d.level(Skill.ENGINEERING);
        if (eng > 0) m.add(new Mod(Attributes.BLOCK_BREAK_SPEED, "eng_mine", 0.002 * eng, MUL));
        // черты
        if (d.has(Trait.TOUGH)) m.add(new Mod(Attributes.MAX_HEALTH, "t_tough", 4, ADD));
        if (d.has(Trait.WEAK)) m.add(new Mod(Attributes.MAX_HEALTH, "t_weak", -4, ADD));
        if (d.has(Trait.FAST)) m.add(new Mod(Attributes.MOVEMENT_SPEED, "t_fast", 0.05, MUL));
        if (d.has(Trait.SLOW)) m.add(new Mod(Attributes.MOVEMENT_SPEED, "t_slow", -0.05, MUL));
        if (d.has(Trait.LUCKY)) m.add(new Mod(Attributes.LUCK, "t_lucky", 1, ADD));
        if (d.has(Trait.UNLUCKY)) m.add(new Mod(Attributes.LUCK, "t_unlucky", -1, ADD));
        if (d.has(Trait.CLUMSY)) {
            m.add(new Mod(Attributes.FALL_DAMAGE_MULTIPLIER, "t_clumsy", 0.3, MUL));
            m.add(new Mod(Attributes.SNEAKING_SPEED, "t_clumsy_sneak", -0.1, MUL));
        }
        if (d.has(Trait.BRITTLE)) m.add(new Mod(Attributes.SAFE_FALL_DISTANCE, "t_brittle", -1, ADD));
        // перки
        if (d.has(Perk.OH_QUICK)) m.add(new Mod(Attributes.ATTACK_SPEED, "p_oh_quick", 0.08, MUL));
        if (d.has(Perk.TH_SWEEP)) m.add(new Mod(Attributes.SWEEPING_DAMAGE_RATIO, "p_th_sweep", 0.3, ADD));
        if (d.has(Perk.TH_STEADY)) m.add(new Mod(Attributes.KNOCKBACK_RESISTANCE, "p_th_steady", 0.4, ADD));
        if (d.has(Perk.PA_REACH)) m.add(new Mod(Attributes.ENTITY_INTERACTION_RANGE, "p_pa_reach", 0.5, ADD));
        if (d.has(Perk.PA_DRAGON)) m.add(new Mod(Attributes.ENTITY_INTERACTION_RANGE, "p_pa_dragon", 0.75, ADD));
        if (d.has(Perk.AT_RUNNER)) m.add(new Mod(Attributes.MOVEMENT_SPEED, "p_at_runner", 0.05, MUL));
        if (d.has(Perk.AT_MARATHON)) m.add(new Mod(Attributes.MOVEMENT_SPEED, "p_at_marathon", 0.08, MUL));
        if (d.has(Perk.AT_LEGS)) m.add(new Mod(Attributes.SAFE_FALL_DISTANCE, "p_at_legs", 2, ADD));
        if (d.has(Perk.AT_JUMP)) m.add(new Mod(Attributes.JUMP_STRENGTH, "p_at_jump", 0.08, ADD));
        if (d.has(Perk.AT_SWIM)) {
            m.add(new Mod(Attributes.WATER_MOVEMENT_EFFICIENCY, "p_at_swim", 0.35, ADD));
            m.add(new Mod(Attributes.OXYGEN_BONUS, "p_at_swim_air", 2, ADD));
        }
        if (d.has(Perk.AT_IRON)) m.add(new Mod(Attributes.MAX_HEALTH, "p_at_iron", 4, ADD));
        if (d.has(Perk.EN_MASON)) m.add(new Mod(Attributes.BLOCK_BREAK_SPEED, "p_en_mason", 0.1, MUL));
        if (d.has(Perk.EN_ARCHITECT)) m.add(new Mod(Attributes.BLOCK_BREAK_SPEED, "p_en_arch", 0.2, MUL));
        if (d.has(Perk.EN_REACH)) m.add(new Mod(Attributes.BLOCK_INTERACTION_RANGE, "p_en_reach", 1, ADD));
        if (d.has(Perk.EN_SAPPER)) m.add(new Mod(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, "p_en_sapper", 0.4, ADD));
        return m;
    }

    public static void apply(ServerPlayer sp) {
        SurvivorData d = Skills.data(sp);
        // снять всё, что ставили раньше
        for (Holder<Attribute> a : List.of(Attributes.MAX_HEALTH, Attributes.MOVEMENT_SPEED, Attributes.ATTACK_SPEED, Attributes.LUCK,
                Attributes.FALL_DAMAGE_MULTIPLIER, Attributes.SNEAKING_SPEED, Attributes.SAFE_FALL_DISTANCE, Attributes.SWEEPING_DAMAGE_RATIO,
                Attributes.KNOCKBACK_RESISTANCE, Attributes.ENTITY_INTERACTION_RANGE, Attributes.JUMP_STRENGTH,
                Attributes.WATER_MOVEMENT_EFFICIENCY, Attributes.OXYGEN_BONUS, Attributes.BLOCK_BREAK_SPEED,
                Attributes.BLOCK_INTERACTION_RANGE, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)) {
            AttributeInstance inst = sp.getAttribute(a);
            if (inst == null) continue;
            for (AttributeModifier mod : List.copyOf(inst.getModifiers())) {
                if (mod.id().getNamespace().equals(Regnum.MODID) && mod.id().getPath().startsWith("hero_")) inst.removeModifier(mod.id());
            }
        }
        for (Mod m : collect(d)) {
            AttributeInstance inst = sp.getAttribute(m.attr());
            if (inst == null) continue;
            inst.addTransientModifier(new AttributeModifier(Regnum.id("hero_" + m.id()), m.value(), m.op()));
        }
        if (sp.getHealth() > sp.getMaxHealth()) sp.setHealth(sp.getMaxHealth());
    }
}
