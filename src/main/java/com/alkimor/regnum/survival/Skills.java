package com.alkimor.regnum.survival;

import com.alkimor.regnum.core.RegnumConfig;
import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.core.network.SkillSyncPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Точка входа для всех модулей: опыт навыков (со скоростью обучения и пределом, как в Bannerlord),
 * уровень героя, очки атрибутов и фокуса, перки, черты, честь.
 */
public final class Skills {
    private Skills() {}

    // ---------------- клиентская копия (заполняется пакетом SkillSyncPayload)
    public static int[] clientXp = new int[Skill.values().length];
    public static int[] clientAttrs = new int[Attr.values().length];
    public static int[] clientFocus = new int[Skill.values().length];
    public static int clientFreeAttr, clientFreeFocus, clientCharXp;
    public static Set<Perk> clientPerks = EnumSet.noneOf(Perk.class);
    public static Set<Trait> clientTraits = EnumSet.noneOf(Trait.class);
    public static String clientClass = "";
    public static int clientHonor = 0;
    public static List<String> clientChronicle = List.of();
    public static List<String> clientFamily = List.of();

    private static final Map<UUID, float[]> REMAINDER = new HashMap<>();
    private static final Map<UUID, Long> LIMIT_HINT = new HashMap<>();

    public static SurvivorData data(Player p) {
        return p.getData(SurvivalModule.SURVIVOR);
    }

    public static int level(Player p, Skill s) {
        if (p.level().isClientSide) return Skill.levelFor(clientXp.length > s.ordinal() ? clientXp[s.ordinal()] : 0);
        return data(p).level(s);
    }

    public static boolean has(Player p, Perk perk) {
        if (p.level().isClientSide) return clientPerks.contains(perk);
        return data(p).has(perk);
    }

    public static boolean has(Player p, Trait t) {
        if (p.level().isClientSide) return clientTraits.contains(t);
        return data(p).has(t);
    }

    /** Множитель опыта от черт. */
    private static float traitMult(SurvivorData d, Skill s) {
        float m = 1f;
        if (d.has(Trait.FAST_LEARNER)) m *= 1.15f;
        if (d.has(Trait.SLOW_LEARNER)) m *= 0.85f;
        if (d.has(Trait.MARKSMAN) && (s == Skill.BOW || s == Skill.CROSSBOW || s == Skill.THROWING)) m *= 1.2f;
        if (d.has(Trait.ATHLETE) && s == Skill.ATHLETICS) m *= 1.25f;
        if (d.has(Trait.CRAFTSMAN) && s == Skill.SMITHING) m *= 1.25f;
        return m;
    }

    public static void addXp(Player p, Skill s, float base) {
        if (!(p instanceof ServerPlayer sp) || !RegnumConfig.SURVIVAL_ENABLED.get() || base <= 0) return;
        if (sp.isCreative() || sp.isSpectator()) return;
        SurvivorData d = data(sp);
        int before = d.level(s);
        float rate = Skill.learningRate(d.attr(s.attr), d.focus(s), before);
        if (rate <= 0f) {
            long now = sp.serverLevel().getGameTime();
            if (now - LIMIT_HINT.getOrDefault(sp.getUUID(), -100000L) > 20 * 120) {
                LIMIT_HINT.put(sp.getUUID(), now);
                Text.bar(sp, "«" + s.title + "» упёрся в предел обучения — вложите фокус или поднимите «" + s.attr.title + "»",
                        ChatFormatting.GRAY);
            }
            return;
        }
        float amount = (float) (base * RegnumConfig.SKILL_XP_MULTIPLIER.get() * rate * traitMult(d, s));
        float[] rem = REMAINDER.computeIfAbsent(sp.getUUID(), k -> new float[Skill.values().length]);
        rem[s.ordinal()] += amount;
        int whole = (int) rem[s.ordinal()];
        if (whole <= 0) return;
        rem[s.ordinal()] -= whole;
        d.addXp(s, whole);
        int heroLevels = d.addCharXp(whole);
        int after = d.level(s);
        boolean dirty = heroLevels > 0;
        if (after > before) {
            dirty = true;
            if (after % 5 == 0 || after - before > 1) {
                sp.sendSystemMessage(Text.of("✦ «" + s.title + "»: " + after, s.color(), ChatFormatting.BOLD)
                        .append(Text.of("  " + s.bonus, ChatFormatting.GRAY)));
            } else {
                Text.bar(sp, "✦ «" + s.title + "» " + after, s.color());
            }
            for (int pl : Skill.PERK_LEVELS) {
                if (before < pl && after >= pl) {
                    Perk a = Perk.get(s, java.util.Arrays.binarySearch(Skill.PERK_LEVELS, pl), 0);
                    sp.sendSystemMessage(Text.of("  ★ Выбор перка «" + s.title + "» " + pl + ": ", ChatFormatting.LIGHT_PURPLE)
                            .append(Text.of(a.title + " или " + a.other().title + " — откройте дневник героя", ChatFormatting.GRAY)));
                    sp.level().playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.3f);
                    chronicle(sp, "«" + s.title + "» достиг " + pl);
                }
            }
            if (s == Skill.ATHLETICS || s == Skill.ENGINEERING || s == Skill.RIDING) PlayerStats.apply(sp);
        } else if (whole >= 4) {
            Text.bar(sp, "+" + whole + " " + s.title, s.color());
        }
        if (heroLevels > 0) {
            int lvl = d.charLevel();
            sp.sendSystemMessage(Text.of("⚜ Уровень героя " + lvl + "! ", ChatFormatting.GOLD, ChatFormatting.BOLD)
                    .append(Text.of("Свободно: фокус " + d.freeFocus + ", атрибуты " + d.freeAttr + " — дневник героя.", ChatFormatting.GRAY)));
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9f, 0.9f);
        }
        if (dirty) sync(sp, false);
    }

    public static void addHonor(ServerPlayer sp, int delta, String reason) {
        SurvivorData d = data(sp);
        if (delta > 0 && d.has(Perk.CH_PEACE)) delta = Math.round(delta * 1.25f);
        String before = SurvivorData.honorTitle(d.honor());
        d.addHonor(delta);
        String after = SurvivorData.honorTitle(d.honor());
        Text.bar(sp, (delta >= 0 ? "Честь +" : "Честь ") + delta + " — " + reason, delta >= 0 ? ChatFormatting.YELLOW : ChatFormatting.DARK_RED);
        if (!before.equals(after)) Text.gold(sp, "Молва о вас изменилась. Теперь вас знают как: «" + after + "»");
    }

    public static void sync(ServerPlayer sp, boolean openJournal) {
        sync(sp, openJournal ? SkillSyncPayload.OPEN_JOURNAL : 0);
    }

    public static void sync(ServerPlayer sp, int open) {
        SurvivorData d = data(sp);
        int[] xp = new int[Skill.values().length];
        int[] focus = new int[Skill.values().length];
        for (Skill s : Skill.values()) {
            xp[s.ordinal()] = d.xp(s);
            focus[s.ordinal()] = d.focus(s);
        }
        int[] attrs = new int[Attr.values().length];
        for (Attr a : Attr.values()) attrs[a.ordinal()] = d.attr(a);
        PacketDistributor.sendToPlayer(sp, new SkillSyncPayload(xp, attrs, focus, d.freeAttr, d.freeFocus, d.charXp(),
                d.perks().stream().map(Enum::name).toList(), d.traits().stream().map(Enum::name).toList(),
                d.cls() == null ? "" : d.cls().name(), d.honor(), List.copyOf(d.chronicle()),
                com.alkimor.regnum.dynasty.Dynasty.familyLines(sp), open));
    }

    /** Записать событие в летопись игрока. */
    public static void chronicle(ServerPlayer sp, String entry) {
        long day = sp.serverLevel().getDayTime() / 24000L + 1;
        data(sp).record("День " + day + ": " + entry);
    }

    /** Множитель силы лечения от Медицины. */
    public static float medicineFactor(Player p) {
        return 1.0f + 0.01f * level(p, Skill.MEDICINE);
    }

    /** Лимит армии от лидерства, тактики и черт. */
    public static int armyBonus(Player p) {
        int b = level(p, Skill.LEADERSHIP) / 10;
        if (has(p, Perk.LD_SERGEANT)) b += 2;
        if (has(p, Perk.TC_RESERVE)) b += 3;
        if (has(p, Trait.LEADER)) b += 2;
        return b;
    }
}
