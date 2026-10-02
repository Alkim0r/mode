package com.alkimor.regnum.client.screen;

import com.alkimor.regnum.core.network.HeroActionPayload;
import com.alkimor.regnum.survival.Attr;
import com.alkimor.regnum.survival.CharClass;
import com.alkimor.regnum.survival.Perk;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import com.alkimor.regnum.survival.SurvivalModule;
import com.alkimor.regnum.survival.SurvivorData;
import com.alkimor.regnum.survival.Trait;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Дневник героя: атрибуты и навыки (Bannerlord), перки, черты (Zomboid), летопись, род. */
public class JournalScreen extends RegnumScreen {
    private int page = 0;
    private int perkScroll = 0;
    private static final String[] PAGES = {"Герой", "Перки", "Черты", "Летопись", "Род"};
    static final int GOLD = 0xFFE8C872, TEXT = 0xFFDDD5C0, DIM = 0xFF9A9080, RED = 0xFFE05A4A, GREEN = 0xFF8FD16A, BLUE = 0xFF7FB8E8;

    public JournalScreen() {
        super(Component.literal("Дневник героя"), 440, 276);
    }

    public void refresh() {
        init();
    }

    public void openPage(int p) {
        page = p;
        init();
    }

    private static void send(int action, int arg, String text) {
        PacketDistributor.sendToServer(new HeroActionPayload(action, arg, text));
    }

    static int xp(Skill s) {
        return Skills.clientXp.length > s.ordinal() ? Skills.clientXp[s.ordinal()] : 0;
    }

    static int focus(Skill s) {
        return Skills.clientFocus.length > s.ordinal() ? Skills.clientFocus[s.ordinal()] : 0;
    }

    static int attr(Attr a) {
        return Skills.clientAttrs.length > a.ordinal() ? Skills.clientAttrs[a.ordinal()] : Attr.BASE;
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        int tabW = 64;
        for (int i = 0; i < PAGES.length; i++) {
            final int idx = i;
            String label = PAGES[i];
            if (i == 1 && !pending().isEmpty()) label = "Перки (!)";
            addRenderableWidget(Button.builder(Component.literal((page == i ? "▶ " : "") + label), b -> {
                page = idx;
                init();
            }).bounds(left + 8 + i * (tabW + 2), top + panelH - 24, tabW, 18).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> onClose())
                .bounds(left + panelW - 68, top + panelH - 24, 60, 18).build());
        if (page == 0) initHero();
        if (page == 1) initPerks();
    }

    // ================================================================== Герой

    private int blockX(int i) {
        return left + 10 + (i % 2) * 214;
    }

    private int blockY(int i) {
        return top + 42 + (i / 2) * 64;
    }

    private void initHero() {
        for (Attr a : Attr.values()) {
            int bx = blockX(a.ordinal()), by = blockY(a.ordinal());
            if (Skills.clientFreeAttr > 0 && attr(a) < Attr.MAX) {
                Button b = Button.builder(Component.literal("+"), x -> send(HeroActionPayload.SPEND_ATTR, a.ordinal(), ""))
                        .bounds(bx + 192, by - 1, 11, 11).build();
                b.setTooltip(Tooltip.create(Component.literal("Повысить «" + a.title + "»: выше скорость обучения и предел его навыков")));
                addRenderableWidget(b);
            }
            Skill[] ss = a.skills();
            for (int k = 0; k < ss.length; k++) {
                Skill s = ss[k];
                if (Skills.clientFreeFocus > 0 && focus(s) < Skill.MAX_FOCUS) {
                    Button b = Button.builder(Component.literal("+"), x -> send(HeroActionPayload.SPEND_FOCUS, s.ordinal(), ""))
                            .bounds(bx + 192, by + 13 + k * 15, 11, 11).build();
                    b.setTooltip(Tooltip.create(Component.literal("Вложить фокус в «" + s.title + "»: обучение быстрее, предел выше")));
                    addRenderableWidget(b);
                }
            }
        }
    }

    private void drawHero(GuiGraphics g, int mx, int my) {
        int lvl = Skill.charLevelFor(Skills.clientCharXp);
        int from = Skill.charXpFor(lvl), to = Skill.charXpFor(lvl + 1);
        CharClass c = CharClass.byName(Skills.clientClass);
        String head = "Уровень героя " + lvl + (c != null ? " · " + c.title : " · класс не выбран");
        g.drawString(font, head, left + 10, top + 26, GOLD);
        int barX = left + 10 + font.width(head) + 8;
        bar(g, barX, top + 28, 70, 5, (Skills.clientCharXp - from) / (float) Math.max(1, to - from), 0xFFC9A227);
        String free = "Свободно: атрибуты " + Skills.clientFreeAttr + ", фокус " + Skills.clientFreeFocus;
        g.drawString(font, free, left + panelW - 10 - font.width(free), top + 26,
                Skills.clientFreeAttr + Skills.clientFreeFocus > 0 ? GREEN : DIM);

        List<Component> tip = null;
        for (Attr a : Attr.values()) {
            int bx = blockX(a.ordinal()), by = blockY(a.ordinal());
            int av = attr(a);
            g.fill(bx - 2, by - 3, bx + 204, by + 57, 0x40000000);
            g.drawString(font, a.title + " " + av, bx, by, colorOf(a));
            if (mx >= bx && mx <= bx + 150 && my >= by - 2 && my <= by + 9) {
                tip = List.of(Component.literal(a.title + ": " + av), Component.literal(a.desc),
                        Component.literal("Каждая единица ускоряет обучение и поднимает предел навыков на 8"));
            }
            Skill[] ss = a.skills();
            for (int k = 0; k < ss.length; k++) {
                Skill s = ss[k];
                int y = by + 14 + k * 15;
                int x = xp(s), sl = Skill.levelFor(x);
                int f = focus(s);
                int limit = Skill.learningLimit(av, f);
                g.drawString(font, s.title, bx + 4, y, TEXT);
                // шкала 0..100 + предел обучения
                int barL = bx + 84, barW = 58;
                g.fill(barL, y + 1, barL + barW, y + 7, 0xFF2A2218);
                g.fill(barL, y + 1, barL + Math.round(barW * sl / 100f), y + 7, 0xFFB08D3A);
                int nextFrom = Skill.xpFor(sl), nextTo = Skill.xpFor(Math.min(100, sl + 1));
                if (sl < 100) g.fill(barL, y + 7, barL + Math.round(barW * (x - nextFrom) / (float) Math.max(1, nextTo - nextFrom)), y + 8, GREEN);
                int lim = Math.min(100, limit);
                g.fill(barL + Math.round(barW * lim / 100f) - 1, y, barL + Math.round(barW * lim / 100f), y + 8, RED);
                g.drawString(font, String.valueOf(sl), barL + barW + 4, y, GOLD);  // уровень — до bx+164
                // фокус
                for (int i = 0; i < Skill.MAX_FOCUS; i++) {
                    int fx = bx + 166 + i * 5;
                    g.fill(fx, y + 1, fx + 3, y + 7, i < f ? 0xFF7FB8E8 : 0xFF3A3328);
                }
                if (mx >= bx && mx <= bx + 188 && my >= y - 1 && my <= y + 9) {
                    float rate = Skill.learningRate(av, f, sl);
                    tip = new ArrayList<>();
                    tip.add(Component.literal(s.title + " — " + sl + " / 100"));
                    tip.add(Component.literal("Растёт от: " + s.grows));
                    tip.add(Component.literal("Бонус: " + s.bonus));
                    tip.add(Component.literal("Скорость обучения: ×" + String.format("%.2f", rate) + ", предел: " + limit
                            + (sl >= limit - 10 ? " — вложите фокус!" : "")));
                    for (int t = 0; t < 4; t++) {
                        Perk p0 = Perk.get(s, t, 0), p1 = Perk.get(s, t, 1);
                        String mark = Skills.clientPerks.contains(p0) ? "✔ " + p0.title : Skills.clientPerks.contains(p1) ? "✔ " + p1.title
                                : sl >= p0.level() ? "★ выбор: " + p0.title + " / " + p1.title : "✖ " + p0.title + " / " + p1.title;
                        tip.add(Component.literal(p0.level() + ": " + mark));
                    }
                }
            }
        }
        // состояние
        int sy = top + panelH - 40;
        Player p = minecraft != null ? minecraft.player : null;
        StringBuilder st = new StringBuilder("Честь " + Skills.clientHonor + " «" + SurvivorData.honorTitle(Skills.clientHonor) + "»   ");
        if (p != null) {
            if (p.hasEffect(SurvivalModule.BLEEDING)) st.append("• кровотечение ");
            if (p.hasEffect(SurvivalModule.FRACTURE)) st.append("• перелом ");
            if (p.hasEffect(SurvivalModule.INFECTION)) st.append("• инфекция ");
        }
        g.drawString(font, st.toString(), left + 10, sy, DIM);
        if (tip != null) deferTooltip(tip);
    }

    static int colorOf(Attr a) {
        Integer c = a.color.getColor();
        return c == null ? GOLD : 0xFF000000 | c;
    }

    static void bar(GuiGraphics g, int x, int y, int w, int h, float part, int color) {
        g.fill(x, y, x + w, y + h, 0xFF2A2218);
        g.fill(x, y, x + Math.round(w * Math.max(0, Math.min(1, part))), y + h, color);
    }

    // ================================================================== Перки

    private static List<Perk> pending() {
        List<Perk> out = new ArrayList<>();
        for (Perk p : Perk.values()) {
            if (p.side != 0) continue;
            int lvl = Skill.levelFor(xp(p.skill));
            if (lvl >= p.level() && !Skills.clientPerks.contains(p) && !Skills.clientPerks.contains(p.other())) out.add(p);
        }
        return out;
    }

    private void initPerks() {
        List<Perk> pend = pending();
        int y = top + 40;
        for (int i = 0; i < Math.min(5, pend.size()); i++) {
            Perk a = pend.get(i), b = a.other();
            Button ba = Button.builder(Component.literal(a.title), x -> send(HeroActionPayload.CHOOSE_PERK, 0, a.name()))
                    .bounds(left + 150, y, 136, 18).build();
            ba.setTooltip(Tooltip.create(Component.literal(a.title + "\n" + a.desc)));
            Button bb = Button.builder(Component.literal(b.title), x -> send(HeroActionPayload.CHOOSE_PERK, 0, b.name()))
                    .bounds(left + 292, y, 136, 18).build();
            bb.setTooltip(Tooltip.create(Component.literal(b.title + "\n" + b.desc)));
            addRenderableWidget(ba);
            addRenderableWidget(bb);
            y += 22;
        }
    }

    private void drawPerks(GuiGraphics g, int mx, int my) {
        List<Perk> pend = pending();
        int y = top + 26;
        g.drawString(font, pend.isEmpty() ? "Нет перков для выбора. Они открываются на 25, 50, 75 и 100 уровне навыка."
                : "Выберите один перк из пары (навсегда):", left + 10, y, pend.isEmpty() ? DIM : GREEN);
        y = top + 45;
        for (int i = 0; i < Math.min(5, pend.size()); i++) {
            Perk a = pend.get(i);
            g.drawString(font, a.skill.title + " " + a.level(), left + 10, y, colorOf(a.skill.attr));
            y += 22;
        }
        if (pend.size() > 5) g.drawString(font, "…и ещё " + (pend.size() - 5), left + 10, y, DIM);
        int oy = top + 42 + Math.min(5, pend.size()) * 22 + 10;
        g.drawString(font, "Освоенные перки:", left + 10, oy, GOLD);
        oy += 12;
        List<Perk> owned = new ArrayList<>(Skills.clientPerks);
        int perCol = Math.max(1, (top + panelH - 34 - oy) / 11);
        Component tip = null;
        for (int i = 0; i < owned.size() && i < perCol * 2; i++) {
            Perk p = owned.get(i);
            int x = left + 10 + (i / perCol) * 214, yy = oy + (i % perCol) * 11;
            g.drawString(font, "• " + p.title + " (" + p.skill.title + ")", x, yy, TEXT);
            if (mx >= x && mx <= x + 200 && my >= yy && my <= yy + 10) tip = Component.literal(p.desc);
        }
        if (owned.isEmpty()) g.drawString(font, "пока нет", left + 10, oy, DIM);
        if (tip != null) deferTooltip(List.of(tip));
    }

    // ================================================================== Черты и класс

    private void drawTraits(GuiGraphics g) {
        int y = top + 26;
        CharClass c = CharClass.byName(Skills.clientClass);
        if (c != null) {
            g.drawString(font, "Класс: " + c.title, left + 10, y, GOLD);
            y += 11;
            for (var line : font.split(Component.literal(c.desc), panelW - 20)) {
                g.drawString(font, line, left + 10, y, DIM);
                y += 10;
            }
            y += 6;
        }
        g.drawString(font, "Черты:", left + 10, y, GOLD);
        y += 12;
        if (Skills.clientTraits.isEmpty()) g.drawString(font, "нет особых черт", left + 10, y, DIM);
        for (Trait t : Skills.clientTraits) {
            g.drawString(font, (t.positive() ? "+ " : "− ") + t.title, left + 10, y, t.positive() ? GREEN : RED);
            g.drawString(font, t.desc, left + 150, y, DIM);
            y += 11;
        }
    }

    // ================================================================== Летопись и род

    private void drawFamily(GuiGraphics g) {
        int x = left + 10, y = top + 28;
        for (String line : Skills.clientFamily) {
            for (var seq : font.split(Component.literal(line), panelW - 20)) {
                g.drawString(font, seq, x, y, TEXT);
                y += 11;
            }
        }
        g.drawString(font, "Сватайтесь к знатным особам среди странников: подарки, беседы и добрая слава.", x, top + panelH - 44, DIM);
    }

    private void drawChronicle(GuiGraphics g) {
        int x = left + 10, y = top + 28;
        var list = Skills.clientChronicle;
        if (list.isEmpty()) {
            g.drawString(font, "Ваша история ещё не написана.", x, y, DIM);
            return;
        }
        int maxLines = (panelH - 60) / 10;
        List<net.minecraft.util.FormattedCharSequence> lines = new ArrayList<>();
        for (int i = list.size() - 1; i >= 0 && lines.size() < maxLines; i--) {
            lines.addAll(font.split(Component.literal("• " + list.get(i)), panelW - 20));
        }
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) g.drawString(font, lines.get(i), x, y + i * 10, TEXT);
    }

    @Override
    protected void drawContents(GuiGraphics g, int mouseX, int mouseY) {
        switch (page) {
            case 1 -> drawPerks(g, mouseX, mouseY);
            case 2 -> drawTraits(g);
            case 3 -> drawChronicle(g);
            case 4 -> drawFamily(g);
            default -> drawHero(g, mouseX, mouseY);
        }
    }
}
