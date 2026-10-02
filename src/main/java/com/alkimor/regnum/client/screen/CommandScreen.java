package com.alkimor.regnum.client.screen;

import com.alkimor.regnum.core.network.ArmyInfoPayload;
import com.alkimor.regnum.core.network.ArmyOrderPayload;
import com.alkimor.regnum.kingdom.Formation;
import com.alkimor.regnum.kingdom.Order;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Командный шатёр: выбор отряда, строя и приказа. */
public class CommandScreen extends RegnumScreen {
    private final ArmyInfoPayload info;
    private int squad;
    private int formation;

    public CommandScreen(ArmyInfoPayload info) {
        super(Component.literal("Командный шатёр"), 330, 246);
        this.info = info;
        this.squad = info.selectedSquad();
        this.formation = info.formation();
    }

    private void sendSelection() {
        PacketDistributor.sendToServer(new ArmyOrderPayload(squad, -1, formation));
    }

    private void order(Order o) {
        PacketDistributor.sendToServer(new ArmyOrderPayload(squad, o.ordinal(), formation));
        onClose();
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        int y = top + 40;
        int x = left + 10;
        for (int s = 0; s <= 4; s++) {
            final int sq = s;
            String label = (squad == s ? "▶ " : "") + (s == 0 ? "Все" : "Отряд " + s) + " (" + info.squadCounts()[s] + ")";
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                squad = sq;
                sendSelection();
                init();
            }).bounds(x + s * 62, y, 60, 18).build());
        }
        y += 36;
        for (Formation f : Formation.values()) {
            String label = (formation == f.ordinal() ? "▶ " : "") + f.title;
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                formation = f.ordinal();
                sendSelection();
                init();
            }).bounds(x + (f.ordinal() % 3) * 104, y + (f.ordinal() / 3) * 20, 102, 18).build());
        }
        y += 56;
        addRenderableWidget(Button.builder(Component.literal("Следовать"), b -> order(Order.FOLLOW)).bounds(x, y, 100, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Держать позицию"), b -> order(Order.HOLD)).bounds(x + 105, y, 100, 18).build());
        addRenderableWidget(Button.builder(Component.literal("В атаку!"), b -> order(Order.CHARGE)).bounds(x + 210, y, 100, 18).build());
        y += 22;
        addRenderableWidget(Button.builder(Component.literal("Выдвинуться к точке взгляда"), b -> order(Order.MOVE)).bounds(x, y, 152, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Отступить к ратуше"), b -> order(Order.RETREAT)).bounds(x + 158, y, 152, 18).build());
        y += 22;
        addRenderableWidget(Button.builder(Component.literal("Патрулировать город"), b -> order(Order.PATROL)).bounds(x, y, 152, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Охранять город"), b -> order(Order.GUARD)).bounds(x + 158, y, 152, 18).build());
    }

    @Override
    protected void drawContents(GuiGraphics g, int mouseX, int mouseY) {
        int gold = 0xFFE8C872, dim = 0xFF9A9080;
        g.drawString(font, "Отряд:", left + 10, top + 28, gold);
        g.drawString(font, "Строй:", left + 10, top + 64, gold);
        g.drawString(font, "Приказ:", left + 10, top + 120, gold);
        if (squad > 0 && info.squadOrders()[squad] >= 0) {
            g.drawString(font, "Текущий приказ: " + Order.byId(info.squadOrders()[squad]).title, left + 120, top + 28, dim);
        }
        g.drawString(font, "Жезл: ПКМ по врагу — атака, по земле — выдвинуться,", left + 10, top + panelH - 24, dim);
        g.drawString(font, "в небо — следовать. ПКМ по солдату — сменить его отряд.", left + 10, top + panelH - 13, dim);
    }
}
