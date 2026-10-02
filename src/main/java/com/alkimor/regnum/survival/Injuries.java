package com.alkimor.regnum.survival;

import com.alkimor.regnum.core.RegnumConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

/** Наложение и лечение травм. */
public final class Injuries {
    private Injuries() {}

    /** Длительность одной стадии инфекции, после которой она ухудшается. */
    public static final int INFECTION_STAGE_TICKS = 20 * 60 * 6;

    public static boolean enabled() {
        return RegnumConfig.INJURIES_ENABLED.get();
    }

    public static void startBleeding(Player p) {
        MobEffectInstance cur = p.getEffect(SurvivalModule.BLEEDING);
        if (cur == null) {
            // Лёгкое кровотечение остановится само через минуту.
            p.addEffect(new MobEffectInstance(SurvivalModule.BLEEDING, Skills.has(p, Trait.BLEEDER) ? 20 * 120 : 20 * 60, 0));
            msg(p, "У вас кровотечение! Перевяжите рану бинтом.");
        } else if (cur.getAmplifier() < 2) {
            p.removeEffect(SurvivalModule.BLEEDING);
            p.addEffect(new MobEffectInstance(SurvivalModule.BLEEDING, MobEffectInstance.INFINITE_DURATION, cur.getAmplifier() + 1));
            msg(p, "Кровотечение усилилось! Само оно уже не остановится.");
        }
    }

    public static void breakBone(Player p) {
        if (p.hasEffect(SurvivalModule.FRACTURE)) return;
        p.addEffect(new MobEffectInstance(SurvivalModule.FRACTURE, MobEffectInstance.INFINITE_DURATION, 0));
        msg(p, "Хруст... У вас перелом! Наложите шину, иначе он не срастётся.");
    }

    public static void infect(Player p) {
        if (p.hasEffect(SurvivalModule.INFECTION)) return;
        p.addEffect(new MobEffectInstance(SurvivalModule.INFECTION, INFECTION_STAGE_TICKS, 0));
        msg(p, "Вас лихорадит. Похоже на инфекцию — нужен целебный отвар.");
    }

    /** Шина: перелом срастается за время, зависящее от Медицины. */
    public static boolean splint(Player p, int medicineLevel) {
        MobEffectInstance cur = p.getEffect(SurvivalModule.FRACTURE);
        if (cur == null || !cur.isInfiniteDuration()) return false;
        p.removeEffect(SurvivalModule.FRACTURE);
        if (Skills.has(p, Perk.MD_SURGEON)) return true; // хирург: кость сразу на месте
        int ticks = Math.max(20 * 45, 20 * 120 - medicineLevel * 20 * 8 / 10);
        p.addEffect(new MobEffectInstance(SurvivalModule.FRACTURE, ticks, 0));
        return true;
    }

    private static void msg(Player p, String s) {
        p.displayClientMessage(Component.literal(s).withStyle(ChatFormatting.RED), false);
    }
}
