package com.alkimor.regnum.client.screen;

import com.alkimor.regnum.core.network.HeroActionPayload;
import com.alkimor.regnum.survival.Attr;
import com.alkimor.regnum.survival.CharClass;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Trait;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Создание героя в два шага:
 * 1) класс и черты (положительные стоят очков, отрицательные — дают, как в Project Zomboid);
 * 2) свободные очки атрибутов и фокуса (как в Bannerlord).
 */
public class CreationScreen extends RegnumScreen {
    private static final int GOLD = JournalScreen.GOLD, TEXT = JournalScreen.TEXT, DIM = JournalScreen.DIM,
            RED = JournalScreen.RED, GREEN = JournalScreen.GREEN;
    private static final int MAX_TRAITS = 8;

    private int step = 0;
    private CharClass cls = CharClass.WARRIOR;
    private final Set<Trait> traits = EnumSet.noneOf(Trait.class);
    private final Map<Attr, Integer> extraAttr = new EnumMap<>(Attr.class);
    private final Map<Skill, Integer> extraFocus = new EnumMap<>(Skill.class);

    public CreationScreen() {
        super(Component.literal("Рождение героя"), 534, 300);
    }

    private int balance() {
        int b = cls.traitPoints;
        for (Trait t : traits) b += t.points;
        return b;
    }

    private int spentAttr() {
        return extraAttr.values().stream().mapToInt(Integer::intValue).sum();
    }

    private int spentFocus() {
        return extraFocus.values().stream().mapToInt(Integer::intValue).sum();
    }

    private int attrValue(Attr a) {
        return cls.attr(a) + extraAttr.getOrDefault(a, 0);
    }

    private int focusValue(Skill s) {
        return cls.focus.getOrDefault(s, 0) + extraFocus.getOrDefault(s, 0);
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        if (step == 0) initStep0();
        else initStep1();
    }

    // ================================================================== шаг 1: класс и черты

    private void initStep0() {
        int y = top + 40;
        for (CharClass c : CharClass.values()) {
            String lbl = c.title + (c.traitPoints > 0 ? " (+" + c.traitPoints + ")" : "");
            Button b = Button.builder(Component.literal((c == cls ? "▶ " : "") + lbl), x -> {
                cls = c;
                extraAttr.clear();
                extraFocus.clear();
                init();
            }).bounds(left + 10, y, 110, 18).build();
            b.setTooltip(Tooltip.create(Component.literal(c.desc + "\nНабор: " + c.kitText
                    + (c.traitPoints > 0 ? "\nОчки черт: +" + c.traitPoints : ""))));
            addRenderableWidget(b);
            y += 21;
        }
        // черты: две колонки
        List<Trait> pos = java.util.Arrays.stream(Trait.values()).filter(Trait::positive).toList();
        List<Trait> neg = java.util.Arrays.stream(Trait.values()).filter(t -> !t.positive()).toList();
        addTraitColumn(pos, left + 282, GREEN);
        addTraitColumn(neg, left + 406, RED);
        Button next = Button.builder(Component.literal("Далее →"), b -> {
            step = 1;
            init();
        }).bounds(left + panelW - 90, top + panelH - 24, 80, 18).build();
        next.active = balance() >= 0;
        addRenderableWidget(next);
    }

    private void addTraitColumn(List<Trait> list, int x, int color) {
        int y = top + 40;
        for (Trait t : list) {
            boolean on = traits.contains(t);
            boolean blocked = !on && (traits.size() >= MAX_TRAITS || traits.stream().anyMatch(o -> o.conflicts(t)));
            String label = (on ? "✔ " : "") + t.title + " " + (t.points < 0 ? "−" + (-t.points) : "+" + t.points);
            Button b = Button.builder(Component.literal(label), x2 -> {
                if (traits.contains(t)) traits.remove(t);
                else traits.add(t);
                init();
            }).bounds(x, y, 122, 14).build();
            b.active = !blocked;
            b.setTooltip(Tooltip.create(Component.literal(t.title + "\n" + t.desc)));
            addRenderableWidget(b);
            y += 15;
        }
    }

    private void drawStep0(GuiGraphics g) {
        g.drawString(font, "Шаг 1 из 2 — кто вы?", left + 10, top + 26, GOLD);
        // описание класса
        int x = left + 128, y = top + 40;
        g.drawString(font, cls.title, x, y, GOLD);
        y += 12;
        for (var line : font.split(Component.literal(cls.desc), 146)) {
            g.drawString(font, line, x, y, TEXT);
            y += 10;
        }
        y += 6;
        for (Attr a : Attr.values()) {
            int v = cls.attr(a);
            g.drawString(font, a.title + " " + v, x, y, v > Attr.BASE ? JournalScreen.colorOf(a) : DIM);
            y += 10;
        }
        y += 4;
        g.drawString(font, "Фокус:", x, y, GOLD);
        y += 10;
        for (var e : cls.focus.entrySet()) {
            g.drawString(font, e.getKey().title + " " + "●".repeat(e.getValue()), x, y, TEXT);
            y += 10;
        }
        y += 4;
        String bonus = "Свободно: атрибуты " + cls.freeAttr + ", фокус " + cls.freeFocus
                + (cls.traitPoints > 0 ? ", черты +" + cls.traitPoints : "");
        for (var line : font.split(Component.literal(bonus), 146)) {
            g.drawString(font, line, x, y, cls.traitPoints > 0 ? GREEN : TEXT);
            y += 10;
        }
        for (var line : font.split(Component.literal("Набор: " + cls.kitText), 146)) {
            g.drawString(font, line, x, y, DIM);
            y += 10;
        }
        int bal = balance();
        g.drawString(font, "Черты (как в Project Zomboid)", left + 282, top + 26, GOLD);
        String b = "Очки: " + (bal >= 0 ? "+" : "") + bal + (cls.traitPoints > 0 ? " (класс +" + cls.traitPoints + ")" : "")
                + "  ·  черт " + traits.size() + "/" + MAX_TRAITS;
        g.drawString(font, b, left + 282, top + panelH - 20, bal >= 0 ? GREEN : RED);
        if (bal < 0) g.drawString(font, "Возьмите недостатки или откажитесь от достоинств", left + 10, top + panelH - 20, RED);
    }

    // ================================================================== шаг 2: очки

    private void initStep1() {
        int y = top + 44;
        for (Attr a : Attr.values()) {
            int v = attrValue(a);
            Button minus = Button.builder(Component.literal("−"), b -> {
                extraAttr.merge(a, -1, Integer::sum);
                init();
            }).bounds(left + 120, y - 2, 12, 11).build();
            minus.active = extraAttr.getOrDefault(a, 0) > 0;
            Button plus = Button.builder(Component.literal("+"), b -> {
                extraAttr.merge(a, 1, Integer::sum);
                init();
            }).bounds(left + 134, y - 2, 12, 11).build();
            plus.active = spentAttr() < cls.freeAttr && v < 7;
            addRenderableWidget(minus);
            addRenderableWidget(plus);
            y += 14;
        }
        int col = 0, row = 0;
        for (Skill s : Skill.values()) {
            int x = left + 180 + col * 170, yy = top + 44 + row * 13;
            Button minus = Button.builder(Component.literal("−"), b -> {
                extraFocus.merge(s, -1, Integer::sum);
                init();
            }).bounds(x + 122, yy - 2, 12, 11).build();
            minus.active = extraFocus.getOrDefault(s, 0) > 0;
            Button plus = Button.builder(Component.literal("+"), b -> {
                extraFocus.merge(s, 1, Integer::sum);
                init();
            }).bounds(x + 136, yy - 2, 12, 11).build();
            plus.active = spentFocus() < cls.freeFocus && focusValue(s) < 3;
            addRenderableWidget(minus);
            addRenderableWidget(plus);
            row++;
            if (row == 9) {
                row = 0;
                col++;
            }
        }
        addRenderableWidget(Button.builder(Component.literal("← Назад"), b -> {
            step = 0;
            init();
        }).bounds(left + 10, top + panelH - 24, 80, 18).build());
        Button create = Button.builder(Component.literal("Создать героя"), b -> confirm())
                .bounds(left + panelW - 130, top + panelH - 24, 120, 18).build();
        create.active = spentAttr() == cls.freeAttr && spentFocus() == cls.freeFocus;
        addRenderableWidget(create);
    }

    private void drawStep1(GuiGraphics g) {
        g.drawString(font, "Шаг 2 из 2 — " + cls.title + ": распределите очки", left + 10, top + 26, GOLD);
        int y = top + 44;
        for (Attr a : Attr.values()) {
            g.drawString(font, a.title, left + 10, y, JournalScreen.colorOf(a));
            g.drawString(font, String.valueOf(attrValue(a)), left + 100, y, GOLD);
            y += 14;
        }
        y += 4;
        g.drawString(font, "Атрибуты: " + spentAttr() + "/" + cls.freeAttr, left + 10, y, spentAttr() == cls.freeAttr ? GREEN : TEXT);
        y += 11;
        g.drawString(font, "Фокус: " + spentFocus() + "/" + cls.freeFocus, left + 10, y, spentFocus() == cls.freeFocus ? GREEN : TEXT);
        y += 16;
        for (var line : font.split(Component.literal("Атрибут ускоряет все свои навыки. Фокус — один навык: "
                + "без него навык упрётся в предел обучения. Дальше очки даются за уровни героя."), 140)) {
            g.drawString(font, line, left + 10, y, DIM);
            y += 10;
        }
        int col = 0, row = 0;
        for (Skill s : Skill.values()) {
            int x = left + 180 + col * 170, yy = top + 44 + row * 13;
            int f = focusValue(s);
            g.drawString(font, s.title, x, yy, f > 0 ? TEXT : DIM);
            for (int i = 0; i < 3; i++) g.fill(x + 96 + i * 7, yy + 1, x + 101 + i * 7, yy + 7, i < f ? 0xFF7FB8E8 : 0xFF3A3328);
            row++;
            if (row == 9) {
                row = 0;
                col++;
            }
        }
    }

    private void confirm() {
        StringBuilder a = new StringBuilder(), f = new StringBuilder(), t = new StringBuilder();
        for (Attr x : Attr.values()) a.append(a.isEmpty() ? "" : ",").append(x.key).append("=").append(attrValue(x));
        for (Skill s : Skill.values()) {
            int v = focusValue(s);
            if (v > 0) f.append(f.isEmpty() ? "" : ",").append(s.key).append("=").append(v);
        }
        for (Trait x : traits) t.append(t.isEmpty() ? "" : ",").append(x.name());
        PacketDistributor.sendToServer(new HeroActionPayload(HeroActionPayload.CREATE, 0, cls.name() + "|" + a + "|" + f + "|" + t));
        onClose();
    }

    @Override
    protected void drawContents(GuiGraphics g, int mouseX, int mouseY) {
        if (step == 0) drawStep0(g);
        else drawStep1(g);
    }
}
