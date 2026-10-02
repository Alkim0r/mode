package com.alkimor.regnum.client.screen;

import com.alkimor.regnum.core.network.ScienceActionPayload;
import com.alkimor.regnum.core.network.ScienceInfoPayload;
import com.alkimor.regnum.kingdom.Science;
import com.alkimor.regnum.kingdom.Science.Tech;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** Древо знаний: зависимости, состояние исследований и выбор следующей технологии. */
public final class ScienceScreen extends RegnumScreen {
    private static final int COLUMN = 158, ROW = 38, NODE_W = 140, NODE_H = 30;
    private static final int GOLD = 0xFFE8C872, TEXT = 0xFFDDD5C0, DIM = 0xFF9A9080;
    private final ScienceInfoPayload info;
    private final EnumSet<Tech> done = EnumSet.noneOf(Tech.class);
    private Tech selected;
    private int scrollX, scrollY;
    private Button research;
    private boolean sent;
    private boolean locateSelection = true;

    public ScienceScreen(ScienceInfoPayload info) {
        super(Component.literal("Наука королевства"), 660, 450);
        this.info = info;
        for (int id : info.done()) if (id >= 0 && id < Tech.values().length) done.add(Tech.values()[id]);
        selected = current() != null ? current() : Tech.values()[0];
    }

    public static void open(ScienceInfoPayload info) {
        Minecraft mc = Minecraft.getInstance();
        ScienceScreen screen = new ScienceScreen(info);
        if (mc.screen instanceof ScienceScreen previous) {
            screen.selected = previous.selected;
            screen.scrollX = previous.scrollX;
            screen.scrollY = previous.scrollY;
            screen.locateSelection = false;
        }
        mc.setScreen(screen);
    }

    private Tech current() {
        return info.current() >= 0 && info.current() < Tech.values().length ? Tech.values()[info.current()] : null;
    }

    private boolean available(Tech tech) {
        if (done.contains(tech)) return false;
        for (Tech prerequisite : tech.prereq) if (!done.contains(prerequisite)) return false;
        return true;
    }

    private int treeTop() { return top + 62; }
    private boolean detailed() { return panelH >= 300; }
    private int treeBottom() { return top + panelH - (detailed() ? 109 : 75); }
    private int treeWidth() { return panelW - 16; }
    private int row(Tech tech) {
        int row = 0;
        for (Tech other : Tech.values()) {
            if (other == tech) break;
            if (other.era == tech.era) row++;
        }
        return row;
    }
    private int nodeX(Tech tech) { return left + 14 + (tech.era - 1) * COLUMN - scrollX; }
    private int nodeY(Tech tech) { return treeTop() + 6 + row(tech) * ROW - scrollY; }
    private void clampScroll() {
        int rows = 1;
        for (Tech tech : Tech.values()) rows = Math.max(rows, row(tech) + 1);
        scrollX = Math.max(0, Math.min(scrollX, Math.max(0, 4 * COLUMN - treeWidth())));
        scrollY = Math.max(0, Math.min(scrollY, Math.max(0, rows * ROW + 12 - (treeBottom() - treeTop()))));
    }

    @Override
    protected void init() {
        panelW = Math.min(660, width - 12);
        panelH = Math.min(450, height - 12);
        super.init();
        clearWidgets();
        if (locateSelection) {
            scrollX = (selected.era - 1) * COLUMN;
            scrollY = Math.max(0, row(selected) * ROW - (treeBottom() - treeTop()) / 2);
            locateSelection = false;
        }
        clampScroll();
        int tabWidth = (panelW - 20) / 4;
        for (int era = 1; era <= 4; era++) {
            final int target = era;
            addRenderableWidget(Button.builder(Component.literal(era + ". " + Science.ERAS[era]), b -> {
                scrollX = (target - 1) * COLUMN;
                scrollY = 0;
                clampScroll();
            }).bounds(left + 10 + (era - 1) * tabWidth, top + 40, tabWidth - 2, 18).build());
        }
        research = addRenderableWidget(Button.builder(Component.literal("Исследовать"), b -> {
            if (!sent && available(selected) && selected != current()) {
                sent = true;
                PacketDistributor.sendToServer(new ScienceActionPayload(selected.ordinal()));
                updateAction();
            }
        }).bounds(left + 10, top + panelH - 25, Math.min(220, panelW - 100), 18).build());
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> onClose())
                .bounds(left + panelW - 80, top + panelH - 25, 70, 18).build());
        updateAction();
    }

    private void updateAction() {
        if (research == null) return;
        research.active = !sent && available(selected) && selected != current();
        String label = sent ? "Ожидание сервера…" : done.contains(selected) ? "Уже изучено"
                : selected == current() ? "Исследуется" : !available(selected) ? "Нужны предпосылки"
                : current() != null ? "Сменить: потеря ½ прогресса" : "Исследовать";
        research.setMessage(Component.literal(label));
    }

    private String status(Tech tech) {
        if (done.contains(tech)) return "Изучено";
        if (tech == current()) return Math.max(0, info.progress()) + " / " + tech.cost + " знаний";
        return available(tech) ? "Доступно · " + tech.cost + " знаний" : "Закрыто · " + tech.cost + " знаний";
    }

    private List<Component> details(Tech tech) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(tech.title + " — " + status(tech)));
        StringBuilder line = new StringBuilder();
        for (String word : tech.desc.split(" ")) {
            if (font.width(line + word) > Math.max(120, Math.min(300, width - 30)) && !line.isEmpty()) {
                lines.add(Component.literal(line.toString()));
                line.setLength(0);
            }
            line.append(word).append(' ');
        }
        if (!line.isEmpty()) lines.add(Component.literal(line.toString()));
        for (Tech prerequisite : tech.prereq) lines.add(Component.literal((done.contains(prerequisite) ? "✓ " : "• ") + prerequisite.title));
        return lines;
    }

    @Override
    protected void drawContents(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, "Изучено: " + done.size() + "/" + Tech.values().length + "   Знания: +" + Math.max(0, info.perDay()) + "/день",
                left + 10, top + 27, GOLD);
        g.fill(left + 8, treeTop(), left + panelW - 8, treeBottom(), 0xFF16130F);
        g.enableScissor(left + 8, treeTop(), left + panelW - 8, treeBottom());
        // All edges remain visible; the selected technology's prerequisites are highlighted.
        for (Tech tech : Tech.values()) for (Tech prerequisite : tech.prereq) {
            int x1 = nodeX(prerequisite) + NODE_W, y1 = nodeY(prerequisite) + NODE_H / 2;
            int x2 = nodeX(tech), y2 = nodeY(tech) + NODE_H / 2;
            int color = tech == selected ? GOLD : 0xFF554733;
            if (prerequisite.era == tech.era) {
                int route = x1 + 5;
                g.hLine(x1, route, y1, color);
                g.vLine(route, Math.min(y1, y2), Math.max(y1, y2), color);
                g.hLine(nodeX(tech) + NODE_W, route, y2, color);
            } else {
                int route = x2 - 8;
                g.hLine(Math.min(x1, route), Math.max(x1, route), y1, color);
                g.vLine(route, Math.min(y1, y2), Math.max(y1, y2), color);
                g.hLine(route, x2, y2, color);
            }
        }
        for (Tech tech : Tech.values()) {
            int x = nodeX(tech), y = nodeY(tech);
            int color = done.contains(tech) ? 0xFF8DBC83 : tech == current() ? GOLD : available(tech) ? TEXT : DIM;
            g.fill(x, y, x + NODE_W, y + NODE_H, tech == selected ? 0xFF59452B : 0xFF30281E);
            g.renderOutline(x, y, NODE_W, NODE_H, tech == selected ? GOLD : 0xFF756044);
            g.drawString(font, font.plainSubstrByWidth(tech.title, NODE_W - 8), x + 4, y + 4, color);
            g.drawString(font, status(tech), x + 4, y + 17, color);
        }
        g.disableScissor();
        int maxRows = 1;
        for (Tech tech : Tech.values()) maxRows = Math.max(maxRows, row(tech) + 1);
        int viewHeight = treeBottom() - treeTop(), contentHeight = maxRows * ROW + 12;
        if (contentHeight > viewHeight) {
            int thumb = Math.max(8, viewHeight * viewHeight / contentHeight);
            int offset = scrollY * (viewHeight - thumb) / (contentHeight - viewHeight);
            g.fill(left + panelW - 10, treeTop() + offset, left + panelW - 8, treeTop() + offset + thumb, GOLD);
        }
        Tech hovered = hit(mouseX, mouseY);
        if (hovered != null) deferTooltip(details(hovered));
        int y = treeBottom() + 5;
        g.drawString(font, font.plainSubstrByWidth(selected.title + " — " + status(selected), panelW - 20), left + 10, y, GOLD);
        int lineCount = 0;
        for (var line : font.split(Component.literal(selected.desc), panelW - 20)) {
            if (lineCount == (detailed() ? 2 : 1)) break;
            g.drawString(font, line, left + 10, y + 13 + lineCount++ * 10, TEXT);
        }
        String requires = selected.prereq.length == 0 ? "Предпосылок нет" : "Нужно: " + java.util.Arrays.stream(selected.prereq)
                .map(t -> (done.contains(t) ? "✓ " : "") + t.title).collect(java.util.stream.Collectors.joining(", "));
        if (detailed()) g.drawString(font, font.plainSubstrByWidth(requires, panelW - 20), left + 10, y + 36, DIM);
        String hint = "Колесо: вверх/вниз · Shift: влево/вправо";
        if (selected == current() && info.perDay() > 0) hint = "Осталось: ~" + (int) Math.ceil(Math.max(0, selected.cost - info.progress()) / (double) info.perDay()) + " игровых дней";
        g.drawString(font, font.plainSubstrByWidth(hint, panelW - 20), left + 10, y + (detailed() ? 49 : 27), DIM);
        if (detailed()) g.drawString(font, font.plainSubstrByWidth("Стрелки: выбор · Enter: исследовать", panelW - 20), left + 10, y + 61, DIM);
    }

    private Tech hit(double x, double y) {
        if (x < left + 8 || x >= left + panelW - 8 || y < treeTop() || y >= treeBottom()) return null;
        for (Tech tech : Tech.values()) if (x >= nodeX(tech) && x < nodeX(tech) + NODE_W && y >= nodeY(tech) && y < nodeY(tech) + NODE_H) return tech;
        return null;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        Tech tech = hit(x, y);
        if (button == 0 && tech != null) {
            selected = tech;
            updateAction();
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x >= left && x < left + panelW && y >= treeTop() && y < treeBottom()) {
            if (hasShiftDown()) scrollX -= (int) (vertical * 30);
            else scrollY -= (int) (vertical * 30);
            scrollX -= (int) (horizontal * 30);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key >= 262 && key <= 265) {
            int direction = key == 262 || key == 264 ? 1 : -1;
            selected = Tech.values()[Math.floorMod(selected.ordinal() + direction, Tech.values().length)];
            scrollX = (selected.era - 1) * COLUMN;
            scrollY = Math.max(0, row(selected) * ROW - (treeBottom() - treeTop()) / 2);
            clampScroll();
            updateAction();
            return true;
        }
        if (key == 257 && getFocused() == null && research.active) {
            research.onPress();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
}
