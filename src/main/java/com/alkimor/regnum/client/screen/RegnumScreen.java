package com.alkimor.regnum.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Общая «пергаментно-тёмная» панель для экранов мода. */
public abstract class RegnumScreen extends Screen {
    protected int panelW, panelH, left, top;

    protected RegnumScreen(Component title, int w, int h) {
        super(title);
        this.panelW = w;
        this.panelH = h;
    }

    @Override
    protected void init() {
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
    }

    protected void drawPanel(GuiGraphics g) {
        g.fill(left - 2, top - 2, left + panelW + 2, top + panelH + 2, 0xFFB08D57);
        g.fill(left, top, left + panelW, top + panelH, 0xF0201810);
        g.fill(left, top + 20, left + panelW, top + 21, 0xFF7A5C30);
        g.drawCenteredString(font, title, left + panelW / 2, top + 6, 0xFFE8C872);
    }

    private java.util.List<Component> deferredTip;

    /** Подсказка, которая рисуется поверх кнопок (после всего экрана). */
    protected void deferTooltip(java.util.List<Component> tip) {
        deferredTip = tip;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        deferredTip = null;
        super.render(g, mouseX, mouseY, partialTick);
        if (deferredTip != null && !deferredTip.isEmpty()) g.renderComponentTooltip(font, deferredTip, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        drawPanel(g);
        drawContents(g, mouseX, mouseY);
    }

    /** Текст и графика панели (рисуются под кнопками). */
    protected abstract void drawContents(GuiGraphics g, int mouseX, int mouseY);

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
