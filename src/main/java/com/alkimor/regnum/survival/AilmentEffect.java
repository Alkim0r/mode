package com.alkimor.regnum.survival;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.EffectCure;

import java.util.Set;

/**
 * Травмы. Их не снимает молоко — только медицина.
 * <ul>
 *   <li>Кровотечение — периодический урон, может занести инфекцию.</li>
 *   <li>Перелом — замедление и слабый прыжок (модификаторы задаются при регистрации).</li>
 *   <li>Инфекция — голод, тошнота, на 3 стадии — урон. Со временем ухудшается.</li>
 * </ul>
 */
public class AilmentEffect extends MobEffect {
    public enum Kind { BLEEDING, FRACTURE, INFECTION }

    private final Kind kind;

    public AilmentEffect(Kind kind, int color) {
        super(MobEffectCategory.HARMFUL, color);
        this.kind = kind;
    }

    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance instance) {
        // Никаких «бесплатных» лекарств: ни молоко, ни мёд не помогают.
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return switch (kind) {
            case BLEEDING -> duration % Math.max(20, 60 - amplifier * 20) == 0;
            case INFECTION -> duration % 100 == 0;
            case FRACTURE -> false;
        };
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return true;
        switch (kind) {
            case BLEEDING -> {
                DamageSource src = new DamageSource(entity.level().registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SurvivalModule.BLEEDING_DAMAGE));
                entity.hurt(src, 1.0f);
                // Открытая рана может воспалиться.
                if (entity instanceof Player p && !p.hasEffect(SurvivalModule.INFECTION) && entity.getRandom().nextFloat() < 0.02f) {
                    p.addEffect(new MobEffectInstance(SurvivalModule.INFECTION, Injuries.INFECTION_STAGE_TICKS, 0));
                    p.displayClientMessage(net.minecraft.network.chat.Component.literal("Рана воспалилась... Нужен целебный отвар.")
                            .withStyle(net.minecraft.ChatFormatting.DARK_GREEN), false);
                }
            }
            case INFECTION -> {
                if (entity instanceof Player p) p.causeFoodExhaustion(1.5f + amplifier);
                if (amplifier >= 1 && entity.getRandom().nextFloat() < 0.25f) {
                    entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
                }
                if (amplifier >= 1) {
                    entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, amplifier - 1));
                }
                if (amplifier >= 2) {
                    entity.hurt(entity.damageSources().magic(), 1.0f);
                }
            }
            case FRACTURE -> {}
        }
        return true;
    }
}
