package com.alkimor.regnum.client.screen;

import com.alkimor.regnum.core.network.CommandersPayload;
import com.alkimor.regnum.kingdom.Commissions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Военачальники: фильтр по роду войск, страницы и репутация до следующего звания. */
public final class CommandersScreen extends RegnumScreen {
    private final CommandersPayload info;
    private final List<Integer> visible = new ArrayList<>();
    private int branch = -1, page, rowsPerPage = 1;
    private static final int ROW_HEIGHT = 49;

    public static void open(CommandersPayload p) {
        Minecraft mc = Minecraft.getInstance();
        CommandersScreen next = new CommandersScreen(p);
        if (mc.screen instanceof CommandersScreen old) {
            next.branch = old.branch;
            next.page = old.page;
        }
        mc.setScreen(next);
    }

    public CommandersScreen(CommandersPayload info) {
        super(Component.literal("Военачальники королевства"), 530, 380);
        this.info = new CommandersPayload(info.names().clone(), info.data().clone(), info.isKing());
    }

    private int count() { return Math.min(info.names().length, info.data().length / CommandersPayload.STRIDE); }
    private int value(int row, int column) { return info.data()[row * CommandersPayload.STRIDE + column]; }
    private String branchName(int id) {
        return id >= 0 && id < Commissions.Branch.values().length ? Commissions.Branch.values()[id].title : "Неизвестный род войск";
    }
    private int tier(int row) { return Math.max(0, Math.min(Commissions.RANKS.length - 1, value(row, 1))); }
    private void prepareCommand(String command) { minecraft.setScreen(new ChatScreen("/regnum commander " + command)); }

    @Override
    protected void init() {
        panelW = Math.min(530, width - 12);
        panelH = Math.min(380, height - 12);
        super.init();
        clearWidgets();
        visible.clear();
        for (int i = 0; i < count(); i++) if (branch == -1 || value(i, 0) == branch) visible.add(i);
        rowsPerPage = Math.max(1, (panelH - 113) / ROW_HEIGHT);
        page = Math.max(0, Math.min(page, Math.max(0, (visible.size() - 1) / rowsPerPage)));
        addRenderableWidget(Button.builder(Component.literal(branch == -1 ? "Все рода войск" : branchName(branch)), b -> {
            branch++;
            if (branch >= Commissions.Branch.values().length) branch = -1;
            page = 0;
            rebuildWidgets();
        }).bounds(left + 10, top + 26, panelW - 114, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Обновить"), b -> {
            if (minecraft.getConnection() != null) minecraft.getConnection().sendCommand("regnum commander gui");
        }).bounds(left + panelW - 100, top + 26, 90, 18).build());
        int start = page * rowsPerPage;
        for (int pos = start; pos < Math.min(visible.size(), start + rowsPerPage); pos++) {
            int row = visible.get(pos), y = top + 55 + (pos - start) * ROW_HEIGHT;
            String name = info.names()[row];
            if (info.isKing() && name.matches("[A-Za-z0-9_]{1,16}")) {
                Button assign = addRenderableWidget(Button.builder(Component.literal("+"), b -> prepareCommand("assign " + name))
                        .bounds(left + panelW - 52, y, 19, 18).build());
                assign.setTooltip(Tooltip.create(Component.literal("Передать бойцов: откроется команда в чате")));
                Button dismiss = addRenderableWidget(Button.builder(Component.literal("−"), b -> prepareCommand("dismiss " + name))
                        .bounds(left + panelW - 31, y, 19, 18).build());
                dismiss.setTooltip(Tooltip.create(Component.literal("Снять с должности: подтвердите команду в чате")));
            }
        }
        Button previous = addRenderableWidget(Button.builder(Component.literal("◀"), b -> { page--; rebuildWidgets(); })
                .bounds(left + 10, top + panelH - 54, 25, 18).build());
        previous.active = page > 0;
        Button next = addRenderableWidget(Button.builder(Component.literal("▶"), b -> { page++; rebuildWidgets(); })
                .bounds(left + panelW - 35, top + panelH - 54, 25, 18).build());
        next.active = (page + 1) * rowsPerPage < visible.size();
        if (info.isKing()) addRenderableWidget(Button.builder(Component.literal("Назначить…"), b -> prepareCommand("appoint "))
                .bounds(left + 10, top + panelH - 26, Math.min(120, panelW - 100), 18).build());
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> onClose())
                .bounds(left + panelW - 80, top + panelH - 26, 70, 18).build());
    }

    @Override
    protected void drawContents(GuiGraphics g, int mouseX, int mouseY) {
        int gold = 0xFFE8C872, text = 0xFFDDD5C0, dim = 0xFF9A9080;
        if (visible.isEmpty()) {
            int y = top + 60;
            String message = count() == 0 ? "Военачальники ещё не назначены." : "В этом роде войск нет командиров.";
            for (var line : font.split(Component.literal(message), panelW - 24)) {
                g.drawString(font, line, left + 12, y, text); y += 12;
            }
            if (info.isKing()) for (var line : font.split(Component.literal("Назначьте друга на род войск кнопкой внизу. Игрок должен быть в сети."), panelW - 24)) {
                g.drawString(font, line, left + 12, y + 8, dim); y += 12;
            }
        }
        int start = page * rowsPerPage;
        for (int pos = start; pos < Math.min(visible.size(), start + rowsPerPage); pos++) {
            int row = visible.get(pos), y = top + 53 + (pos - start) * ROW_HEIGHT;
            int tier = tier(row), rep = Math.max(0, value(row, 2));
            g.fill(left + 8, y, left + panelW - 8, y + ROW_HEIGHT - 3, pos % 2 == 0 ? 0xFF30281E : 0xFF261F18);
            String title = info.names()[row] + " · " + branchName(value(row, 0));
            g.drawString(font, font.plainSubstrByWidth(title, panelW - 76), left + 13, y + 4, gold);
            String stats = Commissions.RANKS[tier] + " · бойцов " + Math.max(0, value(row, 5)) + "/" + Math.max(0, value(row, 4)) + " · побед " + Math.max(0, value(row, 3));
            g.drawString(font, font.plainSubstrByWidth(stats, panelW - 26), left + 13, y + 16, text);
            int nextRep = tier + 1 < Commissions.REP_TIERS.length ? Commissions.REP_TIERS[tier + 1] : rep;
            String progress = "Авторитет: " + rep + (tier + 1 < Commissions.REP_TIERS.length ? " / " + nextRep : " · высшее звание");
            g.drawString(font, progress, left + 13, y + 28, dim);
            int barW = panelW - 26, previousRep = Commissions.REP_TIERS[tier];
            double fraction = nextRep > previousRep ? (rep - previousRep) / (double) (nextRep - previousRep) : 1;
            g.fill(left + 13, y + 40, left + 13 + barW, y + 42, 0xFF514332);
            g.fill(left + 13, y + 40, left + 13 + (int) (barW * Math.max(0, Math.min(1, fraction))), y + 42, gold);
            if (mouseX >= left + 8 && mouseX < left + panelW - 58 && mouseY >= y && mouseY < y + ROW_HEIGHT - 3) {
                deferTooltip(List.of(Component.literal(title), Component.literal(stats), Component.literal(progress)));
            }
        }
        g.drawCenteredString(font, "Страница " + (page + 1) + "/" + Math.max(1, (visible.size() + rowsPerPage - 1) / rowsPerPage)
                + " · всего " + visible.size(), left + panelW / 2, top + panelH - 49, dim);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x >= left && x < left + panelW && y >= top + 48 && y < top + panelH - 56 && vertical != 0) {
            page += vertical > 0 ? -1 : 1;
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
}
