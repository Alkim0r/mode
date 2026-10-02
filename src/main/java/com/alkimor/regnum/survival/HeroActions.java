package com.alkimor.regnum.survival;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.core.network.HeroActionPayload;
import com.alkimor.regnum.core.network.SkillSyncPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Серверная обработка действий героя: вложение очков, выбор перков, создание персонажа. */
public final class HeroActions {
    private HeroActions() {}

    public static final int MAX_TRAITS = 8;
    public static final int CREATION_ATTR_CAP = 7, CREATION_FOCUS_CAP = 3;

    public static void handle(ServerPlayer sp, HeroActionPayload p) {
        SurvivorData d = Skills.data(sp);
        switch (p.action()) {
            case HeroActionPayload.SPEND_ATTR -> {
                if (p.arg() < 0 || p.arg() >= Attr.values().length || d.freeAttr <= 0) return;
                Attr a = Attr.values()[p.arg()];
                if (d.baseAttr(a) >= Attr.MAX) return;
                d.setAttr(a, d.baseAttr(a) + 1);
                d.freeAttr--;
                Text.good(sp, "«" + a.title + "» повышена до " + d.baseAttr(a) + ".");
                PlayerStats.apply(sp);
            }
            case HeroActionPayload.SPEND_FOCUS -> {
                if (p.arg() < 0 || p.arg() >= Skill.values().length || d.freeFocus <= 0) return;
                Skill s = Skill.values()[p.arg()];
                if (d.focus(s) >= Skill.MAX_FOCUS) return;
                d.setFocus(s, d.focus(s) + 1);
                d.freeFocus--;
                Text.good(sp, "Фокус «" + s.title + "»: " + d.focus(s) + ". Предел обучения: "
                        + Skill.learningLimit(d.attr(s.attr), d.focus(s)) + ".");
            }
            case HeroActionPayload.CHOOSE_PERK -> {
                Perk perk = Perk.byName(p.text());
                if (perk == null || !d.choose(perk)) return;
                sp.level().playSound(null, sp.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
                Text.gold(sp, "Перк выбран: «" + perk.title + "» — " + perk.desc);
                Skills.chronicle(sp, "Освоен приём «" + perk.title + "»");
                PlayerStats.apply(sp);
            }
            case HeroActionPayload.CREATE -> create(sp, d, p.text());
            case HeroActionPayload.OPEN -> {
                Skills.sync(sp, d.created() ? SkillSyncPayload.OPEN_JOURNAL : SkillSyncPayload.OPEN_CREATION);
                return;
            }
            default -> {
                return;
            }
        }
        Skills.sync(sp, false);
    }

    /** Формат: CLASS|attr=v,attr=v|skill=v,...|TRAIT,TRAIT */
    private static void create(ServerPlayer sp, SurvivorData d, String text) {
        if (d.created()) {
            Text.bad(sp, "Герой уже создан.");
            return;
        }
        String[] parts = text.split("\\|", -1);
        if (parts.length != 4) return;
        CharClass cls = CharClass.byName(parts[0]);
        if (cls == null) return;
        Map<Attr, Integer> attrs = new EnumMap<>(Attr.class);
        for (Attr a : Attr.values()) attrs.put(a, cls.attr(a));
        int spentA = 0;
        for (String kv : parts[1].split(",")) {
            if (kv.isEmpty()) continue;
            String[] x = kv.split("=");
            for (Attr a : Attr.values()) {
                if (!a.key.equals(x[0])) continue;
                int v = Integer.parseInt(x[1]);
                if (v < cls.attr(a) || v > CREATION_ATTR_CAP) return;
                spentA += v - cls.attr(a);
                attrs.put(a, v);
            }
        }
        Map<Skill, Integer> focus = new EnumMap<>(Skill.class);
        focus.putAll(cls.focus);
        int spentF = 0;
        for (String kv : parts[2].split(",")) {
            if (kv.isEmpty()) continue;
            String[] x = kv.split("=");
            Skill s = Skill.byKey(x[0]);
            if (s == null) return;
            int v = Integer.parseInt(x[1]);
            int base = cls.focus.getOrDefault(s, 0);
            if (v < base || v > CREATION_FOCUS_CAP) return;
            spentF += v - base;
            focus.put(s, v);
        }
        if (spentA > cls.freeAttr || spentF > cls.freeFocus) return;
        Set<Trait> traits = EnumSet.noneOf(Trait.class);
        int balance = cls.traitPoints;
        for (String t : parts[3].split(",")) {
            if (t.isEmpty()) continue;
            Trait tr = Trait.byName(t);
            if (tr == null) return;
            for (Trait o : traits) if (o.conflicts(tr)) return;
            traits.add(tr);
            balance += tr.points;
        }
        if (balance < 0 || traits.size() > MAX_TRAITS) {
            Text.bad(sp, "Черты не сбалансированы.");
            return;
        }
        d.create(cls, attrs, focus, traits);
        // уровни героя, накопленные до создания (переход со старой системы), сохраняют свои очки
        d.freeAttr = d.levelsGranted() / 3;
        d.freeFocus = d.levelsGranted();
        for (String id : cls.kit) {
            Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
            if (it != Items.AIR) {
                ItemStack st = new ItemStack(it);
                if (!sp.getInventory().add(st)) sp.drop(st, false);
            }
        }
        PlayerStats.apply(sp);
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7f, 1.0f);
        sp.sendSystemMessage(Text.of("⚜ Рождён новый герой: " + cls.title + ". ", ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Text.of(cls == CharClass.NOBODY ? "У вас нет ничего — и вся дорога впереди." : "В руках — " + cls.kitText.toLowerCase() + ".",
                        ChatFormatting.GRAY)));
        StringBuilder sb = new StringBuilder();
        for (Trait t : traits) sb.append(sb.isEmpty() ? "" : ", ").append(t.title);
        Skills.chronicle(sp, "Путь героя начат: " + cls.title + (sb.isEmpty() ? "" : " (" + sb + ")"));
    }
}
