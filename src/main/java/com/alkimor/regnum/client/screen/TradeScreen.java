package com.alkimor.regnum.client.screen;

import com.alkimor.regnum.core.network.TradeActionPayload;
import com.alkimor.regnum.core.network.TradeInfoPayload;
import com.alkimor.regnum.trade.TradeGood;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Прилавок: покупка и продажа торговых грузов. Shift — по 5 штук. */
public class TradeScreen extends RegnumScreen {
    private final TradeInfoPayload info;

    public TradeScreen(TradeInfoPayload info) {
        super(Component.literal(info.title() + " — " + info.region()), 360, 40 + TradeGood.values().length * 22 + 40);
        this.info = info;
    }

    private void act(TradeGood g, int sign) {
        int n = Screen.hasShiftDown() ? 5 : 1;
        PacketDistributor.sendToServer(new TradeActionPayload(info.entity(), info.key(), g.ordinal(), sign * n));
    }

    @Override
    protected void init() {
        super.init();
        int y = top + 40;
        for (TradeGood g : TradeGood.values()) {
            int i = g.ordinal();
            addRenderableWidget(Button.builder(Component.literal("Купить"), b -> act(g, 1)).bounds(left + 236, y - 4, 54, 16).build());
            Button sell = Button.builder(Component.literal("Продать"), b -> act(g, -1)).bounds(left + 294, y - 4, 56, 16).build();
            sell.active = info.have()[i] > 0;
            addRenderableWidget(sell);
            y += 22;
        }
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> onClose())
                .bounds(left + panelW / 2 - 40, top + panelH - 24, 80, 18).build());
    }

    @Override
    protected void drawContents(GuiGraphics g, int mouseX, int mouseY) {
        int gold = 0xFFE8C872, text = 0xFFDDD5C0, dim = 0xFF9A9080;
        g.drawString(font, "Товар", left + 10, top + 26, gold);
        g.drawString(font, "Купить", left + 132, top + 26, gold);
        g.drawString(font, "Продать", left + 172, top + 26, gold);
        g.drawString(font, "У вас", left + 210, top + 26, gold);
        int y = top + 40;
        for (TradeGood good : TradeGood.values()) {
            int i = good.ordinal();
            int t = info.trend()[i];
            String arrow = t == -1 ? " ▼" : t == 1 ? " ▲" : "";
            int color = t == -1 ? 0xFF8FD16A : t == 1 ? 0xFFE0805A : text;
            g.drawString(font, good.title + arrow, left + 10, y, color);
            g.drawString(font, String.valueOf(info.buy()[i]), left + 138, y, text);
            g.drawString(font, String.valueOf(info.sell()[i]), left + 180, y, text);
            g.drawString(font, String.valueOf(info.have()[i]), left + 214, y, info.have()[i] > 0 ? gold : dim);
            y += 22;
        }
        String hint = info.tradeLevel() >= 5 ? "▼ дёшево здесь (выгодно купить), ▲ дорого (выгодно продать)"
                : "Shift — по 5 шт. Цены в изумрудах. Торговля 5 ур. — видно тренды.";
        g.drawString(font, hint, left + 10, top + panelH - 38, dim);
    }
}
