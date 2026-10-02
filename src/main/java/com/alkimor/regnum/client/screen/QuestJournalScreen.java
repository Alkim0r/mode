package com.alkimor.regnum.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import com.alkimor.regnum.client.render.QuestTrackerOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Журнал сюжетных заданий. Серверный снимок передаётся через Snapshot. */
public final class QuestJournalScreen extends RegnumScreen {
    public enum Line { MAIN, SIDE, FACTION }
    public enum State { ACTIVE, AVAILABLE, LOCKED, COMPLETED }
    public record Objective(String text, int progress, int required, boolean done) {
        public Objective {
            text = text == null ? "" : text;
            progress = Math.max(0, progress);
            required = Math.max(0, required);
        }
    }
    public record Quest(String id, Line line, State state, String title, String giver, String faction,
                        String description, List<Objective> objectives, List<String> rewards, String lockReason) {
        public Quest {
            id = id == null ? "" : id;
            line = line == null ? Line.SIDE : line;
            state = state == null ? State.LOCKED : state;
            title = safe(title, "Без названия");
            giver = safe(giver, "Неизвестно");
            faction = safe(faction, "");
            description = safe(description, "");
            objectives = objectives == null ? List.of() : List.copyOf(objectives);
            rewards = rewards == null ? List.of() : List.copyOf(rewards);
            lockReason = safe(lockReason, "");
        }
    }
    public record Snapshot(List<Quest> quests, String trackedId, Map<String, Integer> reputation) {
        public Snapshot {
            quests = quests == null ? List.of() : List.copyOf(quests);
            trackedId = trackedId == null ? "" : trackedId;
            reputation = reputation == null ? Map.of() : Map.copyOf(reputation);
        }
    }

    private static final String[] TABS = {"Главная", "Побочные", "Фракции", "Завершено"};
    private static final int GOLD = 0xFFE8C872, TEXT = 0xFFDDD5C0, DIM = 0xFF9A9080;
    private static final int ROW_H = 38;
    private Snapshot snapshot;
    private final List<Quest> visible = new ArrayList<>();
    private record TextLine(FormattedCharSequence text, int color) {}
    private final List<TextLine> details = new ArrayList<>();
    private String reputationText = "";
    private List<Component> reputationTooltip = List.of();
    private int tab, scroll;
    private int detailScroll;
    private int actionCooldown;
    private String selectedId = "";

    public QuestJournalScreen(Snapshot snapshot) {
        super(Component.literal("Летопись похода"), 760, 450);
        this.snapshot = snapshot == null ? new Snapshot(List.of(), "", Map.of()) : snapshot;
        Quest first = this.snapshot.quests().stream().filter(q -> q.id().equals(this.snapshot.trackedId())).findFirst()
                .orElseGet(() -> this.snapshot.quests().stream().filter(q -> q.state() == State.ACTIVE).findFirst()
                        .orElse(this.snapshot.quests().stream().findFirst().orElse(null)));
        if (first != null) {
            selectedId = first.id();
            tab = first.state() == State.COMPLETED ? 3 : first.line().ordinal();
        }
    }

    public static void open(Snapshot snapshot) {
        QuestTrackerOverlay.update(snapshot);
        if (!(Minecraft.getInstance().screen instanceof QuestJournalScreen))
            Minecraft.getInstance().setScreen(new QuestJournalScreen(snapshot));
    }

    /** Called on the client thread by the existing story snapshot receiver. */
    public static void refreshIfOpen(Snapshot snapshot) {
        if (snapshot != null && Minecraft.getInstance().screen instanceof QuestJournalScreen journal)
            journal.refresh(snapshot);
    }

    private void refresh(Snapshot next) {
        if (next.equals(snapshot)) return;
        Quest old = selected();
        snapshot = next;
        actionCooldown = 0;
        // Keep the same quest visible when accepting or completing moves it to another tab.
        Quest updated = next.quests().stream().filter(q -> q.id().equals(selectedId)).findFirst().orElse(null);
        if (old != null && updated != null && old.state() != updated.state())
            tab = updated.state() == State.COMPLETED ? 3 : updated.line().ordinal();
        rebuildWidgets();
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean inTab(Quest quest) {
        if (tab == 3) return quest.state() == State.COMPLETED;
        if (quest.state() == State.COMPLETED) return false;
        return quest.line().ordinal() == tab;
    }

    private Quest selected() {
        return visible.stream().filter(q -> q.id().equals(selectedId)).findFirst()
                .orElse(visible.isEmpty() ? null : visible.get(Math.min(scroll, visible.size() - 1)));
    }

    private int listX() { return left + 9; }
    private int listW() { return Math.max(92, (panelW - 24) * 2 / 5); }
    private int contentTop() { return top + 64; }
    private int contentBottom() { return top + panelH - 53; }
    private int detailX() { return listX() + listW() + 8; }
    private int detailW() { return left + panelW - 9 - detailX(); }
    private int visibleRows() { return Math.max(1, (contentBottom() - contentTop()) / ROW_H); }
    private int detailRows() { return Math.max(1, (contentBottom() - contentTop() - 12) / 12); }

    private void textLines(String text, int color) {
        for (var line : font.split(Component.literal(text), Math.max(20, detailW() - 22)))
            details.add(new TextLine(line, color));
    }

    private void layoutDetails(Quest quest) {
        details.clear();
        if (quest == null) return;
        textLines(quest.title(), GOLD);
        textLines("Выдаёт: " + quest.giver(), DIM);
        if (!quest.faction().isEmpty()) textLines(quest.faction(), DIM);
        textLines("", DIM);
        textLines(quest.description(), TEXT);
        textLines("", DIM);
        textLines("Цели", GOLD);
        for (Objective objective : quest.objectives()) {
            String progress = objective.required() > 1 ? " " + objective.progress() + "/" + objective.required() : "";
            textLines((objective.done() ? "✓ " : "• ") + objective.text() + progress, objective.done() ? 0xFF8FD16A : TEXT);
        }
        if (!quest.rewards().isEmpty()) {
            textLines("", DIM);
            textLines("Награды", GOLD);
            for (String reward : quest.rewards()) textLines("• " + reward, TEXT);
        }
        if (quest.state() == State.LOCKED && !quest.lockReason().isEmpty()) textLines("Закрыто: " + quest.lockReason(), 0xFFE07A62);
        else if (quest.state() == State.COMPLETED) textLines("Задание завершено", 0xFF8FD16A);
        detailScroll = Math.max(0, Math.min(detailScroll, Math.max(0, details.size() - detailRows())));
    }

    @Override
    protected void init() {
        panelW = Math.min(760, width - 12);
        panelH = Math.min(450, height - 12);
        super.init();
        clearWidgets();
        reputationTooltip = snapshot.reputation().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .<Component>map(e -> Component.literal(e.getKey() + ": " + e.getValue())).toList();
        reputationText = snapshot.reputation().isEmpty() ? "" : "Репутация: " + snapshot.reputation().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + " " + e.getValue()).collect(java.util.stream.Collectors.joining(" · "));
        reputationText = font.plainSubstrByWidth(reputationText, panelW - 20);
        visible.clear();
        for (Quest quest : snapshot.quests()) if (inTab(quest)) visible.add(quest);
        if (visible.stream().noneMatch(q -> q.id().equals(selectedId))) {
            scroll = Math.max(0, Math.min(scroll, Math.max(0, visible.size() - 1)));
            selectedId = visible.isEmpty() ? "" : visible.get(scroll).id();
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, visible.size() - visibleRows())));
        int selectedIndex = -1;
        for (int i = 0; i < visible.size(); i++) if (visible.get(i).id().equals(selectedId)) selectedIndex = i;
        if (selectedIndex < scroll) scroll = Math.max(0, selectedIndex);
        else if (selectedIndex >= scroll + visibleRows()) scroll = selectedIndex - visibleRows() + 1;
        int tabW = (panelW - 18) / 4;
        for (int i = 0; i < TABS.length; i++) {
            final int target = i;
            addRenderableWidget(Button.builder(Component.literal((tab == i ? "▶ " : "") + TABS[i]), b -> {
                tab = target;
                scroll = 0;
                selectedId = "";
                detailScroll = 0;
                rebuildWidgets();
            }).bounds(left + 9 + i * tabW, top + 25, tabW - 2, 18).build());
        }
        Quest quest = selected();
        layoutDetails(quest);
        if (quest != null && quest.state() == State.AVAILABLE && validId(quest.id())) {
            addRenderableWidget(Button.builder(Component.literal("Взять задание"), b -> command("accept", quest.id()))
                    .bounds(left + 10, top + panelH - 25, Math.min(120, panelW - 100), 18).build());
        } else if (quest != null && quest.state() == State.ACTIVE && validId(quest.id())) {
            int actionW = Math.min(102, Math.max(78, (panelW - 110) / 2));
            String trackLabel = snapshot.trackedId().equals(quest.id()) ? "Отслеживается" : "Отслеживать";
            Button track = addRenderableWidget(Button.builder(Component.literal(trackLabel), b -> {
                if (!snapshot.trackedId().equals(quest.id())) command("track", quest.id());
            }).bounds(left + 10, top + panelH - 25, actionW, 18).build());
            track.active = !snapshot.trackedId().equals(quest.id());
            if (quest.line() != Line.MAIN)
                addRenderableWidget(Button.builder(Component.literal("Отказаться"), b -> command("abandon", quest.id()))
                        .bounds(left + 16 + actionW, top + panelH - 25, actionW, 18).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> onClose())
                .bounds(left + panelW - 80, top + panelH - 25, 70, 18).build());
    }

    private static boolean validId(String id) { return id.matches("[a-z0-9_]{1,32}"); }

    private void command(String action, String id) {
        if (actionCooldown == 0 && validId(id) && minecraft.getConnection() != null) {
            minecraft.getConnection().sendCommand("regnum story " + action + " " + id);
            actionCooldown = 40;
        }
    }

    @Override
    public void tick() {
        if (actionCooldown > 0) actionCooldown--;
    }

    @Override
    protected void drawContents(GuiGraphics g, int mouseX, int mouseY) {
        if (!reputationText.isEmpty()) g.drawString(font, reputationText, left + 10, top + 48, DIM);
        if (!reputationText.isEmpty() && mouseY >= top + 46 && mouseY < contentTop() && mouseX >= left && mouseX < left + panelW)
            deferTooltip(reputationTooltip);
        int bottom = Math.max(contentTop() + 20, contentBottom());
        int right = listX() + listW();
        g.fill(listX(), contentTop(), right, bottom, 0xFF17130F);
        g.fill(detailX(), contentTop(), left + panelW - 9, bottom, 0xFF17130F);
        g.enableScissor(listX(), contentTop(), right, bottom);
        int rows = visibleRows();
        if (visible.isEmpty()) {
            String message = tab == 3 ? "В этой летописи пока нет завершённых заданий." : "В этой линии пока нет открытых заданий.";
            g.drawString(font, font.plainSubstrByWidth(message, listW() - 10), listX() + 5, contentTop() + 7, DIM);
        }
        for (int i = scroll; i < Math.min(visible.size(), scroll + rows); i++) {
            Quest quest = visible.get(i);
            int y = contentTop() + (i - scroll) * ROW_H;
            boolean chosen = quest.id().equals(selectedId);
            g.fill(listX() + 2, y + 1, right - 2, y + ROW_H - 1, chosen ? 0xFF49391F : (i % 2 == 0 ? 0xFF30281E : 0xFF261F18));
            int color = quest.state() == State.ACTIVE ? 0xFF8FD16A : quest.state() == State.LOCKED ? DIM : TEXT;
            String icon = quest.state() == State.ACTIVE ? "▶ " : quest.state() == State.LOCKED ? "× " : "◇ ";
            g.drawString(font, font.plainSubstrByWidth(icon + quest.title(), listW() - 14), listX() + 6, y + 5, chosen ? GOLD : color);
            String sub = quest.giver() + (quest.state() == State.ACTIVE ? " · в пути" : quest.state() == State.LOCKED ? " · закрыто" : quest.state() == State.COMPLETED ? " · завершено" : " · доступно");
            g.drawString(font, font.plainSubstrByWidth(sub, listW() - 14), listX() + 6, y + 19, DIM);
        }
        g.disableScissor();
        if (visible.size() > rows) {
            String page = (scroll + 1) + "–" + Math.min(visible.size(), scroll + rows) + " / " + visible.size();
            g.drawCenteredString(font, page, listX() + listW() / 2, bottom + 3, DIM);
        }

        Quest selected = selected();
        if (selected == null) {
            int y = contentTop() + 8;
            for (var line : font.split(Component.literal("Выберите задание, чтобы увидеть подробности."), Math.max(20, detailW() - 18))) {
                if (y > bottom - 12) break;
                g.drawString(font, line, detailX() + 9, y, DIM);
                y += 12;
            }
            return;
        }
        g.enableScissor(detailX(), contentTop(), left + panelW - 9, bottom);
        for (int i = detailScroll; i < Math.min(details.size(), detailScroll + detailRows()); i++) {
            TextLine line = details.get(i);
            g.drawString(font, line.text(), detailX() + 9, contentTop() + 7 + (i - detailScroll) * 12, line.color());
        }
        g.disableScissor();
        if (details.size() > detailRows()) {
            int railX = left + panelW - 13;
            int railH = bottom - contentTop() - 6;
            int thumbH = Math.max(8, railH * detailRows() / details.size());
            int thumbY = contentTop() + 3 + (railH - thumbH) * detailScroll / (details.size() - detailRows());
            g.fill(railX, contentTop() + 3, railX + 2, bottom - 3, 0xFF49391F);
            g.fill(railX, thumbY, railX + 2, thumbY + thumbH, GOLD);
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && x >= listX() && x < listX() + listW() && y >= contentTop() && y < contentBottom()) {
            int index = scroll + (int) ((y - contentTop()) / ROW_H);
            if (index >= 0 && index < visible.size()) {
                selectedId = visible.get(index).id();
                detailScroll = 0;
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x >= detailX() && x < left + panelW - 9 && y >= contentTop() && y < contentBottom() && vertical != 0) {
            detailScroll = Math.max(0, Math.min(Math.max(0, details.size() - detailRows()), detailScroll + (vertical < 0 ? 3 : -3)));
            return true;
        }
        if (x >= listX() && x < listX() + listW() && y >= contentTop() && y < contentBottom() && vertical != 0) {
            scroll = Math.max(0, Math.min(Math.max(0, visible.size() - visibleRows()), scroll + (vertical < 0 ? 1 : -1)));
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 264 || key == 265) {
            int index = visible.indexOf(selected());
            index = Math.max(0, Math.min(visible.size() - 1, index + (key == 264 ? 1 : -1)));
            if (!visible.isEmpty()) {
                selectedId = visible.get(index).id();
                detailScroll = 0;
                if (index < scroll) scroll = index;
                else if (index >= scroll + visibleRows()) scroll = index - visibleRows() + 1;
                rebuildWidgets();
            }
            return true;
        }
        if (key == 266 || key == 267) {
            detailScroll = Math.max(0, Math.min(Math.max(0, details.size() - detailRows()), detailScroll + (key == 266 ? -detailRows() : detailRows())));
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
}
