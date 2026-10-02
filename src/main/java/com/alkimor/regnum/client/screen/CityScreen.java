package com.alkimor.regnum.client.screen;

import com.alkimor.regnum.core.network.CityActionPayload;
import com.alkimor.regnum.core.network.CityInfoPayload;
import com.alkimor.regnum.kingdom.Culture;
import com.alkimor.regnum.kingdom.SoldierType;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Экран ратуши: казна, армия, постройки, найм и развитие. */
public class CityScreen extends RegnumScreen {
    private final CityInfoPayload info;
    private EditBox nameBox;

    public CityScreen(CityInfoPayload info) {
        super(Component.literal("«" + info.name() + "» — город " + info.level() + " уровня"), 400, 336);
        this.info = info;
    }

    private static int selType = 0;

    private static SoldierType[] hireList() {
        java.util.List<SoldierType> l = new java.util.ArrayList<>();
        for (SoldierType t : SoldierType.values()) if (t != SoldierType.MILITIA) l.add(t);
        return l.toArray(new SoldierType[0]);
    }

    private void send(int action, int arg, String text) {
        PacketDistributor.sendToServer(new CityActionPayload(info.hall(), action, arg, text));
    }

    @Override
    protected void init() {
        super.init();
        int bx = left + 204, bw = 186, half = 91, y = top + 28;
        addRenderableWidget(Button.builder(Component.literal("Внести изумруды"), b -> send(CityActionPayload.DEPOSIT, 0, ""))
                .bounds(bx, y, half + 8, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Взять 16"), b -> send(CityActionPayload.WITHDRAW, 0, ""))
                .bounds(bx + half + 12, y, bw - half - 12, 18).build());
        y += 24;
        SoldierType[] hire = hireList();
        if (selType >= hire.length) selType = 0;
        SoldierType t = hire[selType];
        Button sel = Button.builder(Component.literal("◀ " + t.title + " ▶  (" + info.byType()[t.ordinal()] + " в строю)"),
                b -> { selType = (selType + 1) % hire.length; rebuildWidgets(); }).bounds(bx, y, bw, 18).build();
        sel.setTooltip(Tooltip.create(Component.literal(t.title + ": " + t.blurb + "\nЖалованье " + t.upkeep + "/день, от " + t.minCityLevel
                + " ур. города" + (t.mounted() ? "\nЗанимает 2 места в армии (конь)" : ""))));
        addRenderableWidget(sel);
        y += 20;
        Button hireBtn = Button.builder(Component.literal("Нанять: " + t.title + " (" + t.cost + " изумр.)"),
                b -> send(CityActionPayload.RECRUIT, t.ordinal(), "")).bounds(bx, y, bw, 18).build();
        hireBtn.active = info.barracks() > 0 && info.level() >= t.minCityLevel && info.treasury() >= t.cost && info.army() < info.armyCap();
        addRenderableWidget(hireBtn);
        y += 20;
        addRenderableWidget(Button.builder(Component.literal("Отряд для найма: " + info.recruitSquad()),
                b -> send(CityActionPayload.SET_SQUAD, info.recruitSquad() % 4 + 1, "")).bounds(bx, y, bw, 18).build());
        y += 20;
        Culture cul = Culture.byId(info.culture());
        Button cb = Button.builder(Component.literal("Культура: " + cul.title), b -> send(CityActionPayload.CULTURE, 0, ""))
                .bounds(bx, y, bw, 18).build();
        cb.setTooltip(Tooltip.create(Component.literal(cul.title + ": " + cul.look + ".\nОпределяет облик и снаряжение войск. Нажмите, чтобы сменить.")));
        addRenderableWidget(cb);
        y += 24;
        Button up = Button.builder(Component.literal(info.level() >= 5 ? "Город развит полностью"
                        : "Развить: " + info.upgradeCost() + " изумр., " + info.upgradeGlory() + " славы"),
                b -> send(CityActionPayload.UPGRADE, 0, "")).bounds(bx, y, bw, 18).build();
        up.active = info.level() < 5 && info.treasury() >= info.upgradeCost() && info.glory() >= info.upgradeGlory()
                && info.extra()[1] >= info.extra()[12] && info.extra()[2] >= info.extra()[13];
        addRenderableWidget(up);
        y += 24;
        nameBox = new EditBox(font, bx, y, bw - 84, 18, Component.literal("Имя города"));
        nameBox.setMaxLength(32);
        nameBox.setValue(info.name());
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.literal("Переименовать"), b -> send(CityActionPayload.RENAME, 0, nameBox.getValue()))
                .bounds(bx + bw - 80, y, 80, 18).build());
        // казначейство
        int ty = top + 296;
        addRenderableWidget(Button.builder(Component.literal("Вложить 16"), b -> send(CityActionPayload.INVEST, 0, ""))
                .bounds(left + 10, ty, 84, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Продать всё"), b -> send(CityActionPayload.DIVEST, 0, ""))
                .bounds(left + 98, ty, 90, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Стратегия: " + com.alkimor.regnum.kingdom.City.STRATEGIES[info.bondStrategy()]),
                        b -> send(CityActionPayload.STRATEGY, 0, ""))
                .bounds(left + 192, ty, 198, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> onClose())
                .bounds(left + panelW / 2 - 40, top + panelH - 24, 80, 18).build());
    }

    @Override
    protected void drawContents(GuiGraphics g, int mouseX, int mouseY) {
        int x = left + 10, y = top + 28;
        int gold = 0xFFE8C872, text = 0xFFDDD5C0, dim = 0xFF9A9080, red = 0xFFE05A4A;
        g.drawString(font, "Правитель: " + info.ruler(), x, y, gold); y += 14;
        g.drawString(font, "Казна: " + info.treasury() + " изумр.", x, y, text); y += 11;
        g.drawString(font, "  в день: +" + info.dailyIncome() + " / −" + info.dailyUpkeep(), x, y, dim); y += 13;
        g.drawString(font, "Слава: " + info.glory(), x, y, text); y += 11;
        g.drawString(font, "Жители: " + info.population() + "   Территория: " + info.radius(), x, y, text); y += 15;
        g.drawString(font, "Армия: " + info.army() + " / " + info.armyCap(), x, y, gold); y += 11;
        StringBuilder comp = new StringBuilder("  ");
        int[] bt = info.byType();
        for (SoldierType st : SoldierType.values()) if (st.ordinal() < bt.length && bt[st.ordinal()] > 0) comp.append(st.title.toLowerCase()).append(' ').append(bt[st.ordinal()]).append(", ");
        g.drawString(font, comp.length() > 2 ? comp.substring(0, comp.length() - 2) : "  отряды пусты", x, y, dim); y += 15;
        g.drawString(font, "Постройки:", x, y, gold); y += 11;
        g.drawString(font, "  казармы " + info.barracks() + ", рынки " + info.markets() + ", башни " + info.towers(), x, y, dim); y += 15;
        if (info.raidActive()) {
            g.drawString(font, "⚔ ИДЁТ НАБЕГ!", x, y, red);
        } else {
            g.drawString(font, "Набег разбойников: через " + info.daysToRaid() + " дн.", x, y, info.daysToRaid() <= 1 ? red : text);
        }
        y += 15;
        if (info.barracks() == 0) g.drawString(font, "Постройте казарму для найма войск", x, y, dim);
        int[] ex = info.extra();
        g.fill(left + 8, top + 190, left + panelW - 8, top + 191, 0xFF7A5C30);
        g.drawString(font, "Склад (до " + ex[6] + " каждого): провиант " + ex[0] + ", дерево " + ex[1] + ", камень " + ex[2], left + 10, top + 196, gold);
        g.drawString(font, "железо " + ex[3] + ", травы " + ex[4] + ", порох " + ex[5] + "   (ПКМ предметом по блоку Склада)", left + 10, top + 207, text);
        if (info.level() < 5) {
            boolean ok = ex[1] >= ex[12] && ex[2] >= ex[13];
            g.drawString(font, "Для развития нужно: дерево " + ex[12] + ", камень " + ex[13], left + 10, top + 218, ok ? text : red);
        }
        StringBuilder st = new StringBuilder();
        if (ex[10] > 0) st.append("ГОЛОД ").append(ex[10]).append(" дн.  ");
        if (ex[7] > 0) st.append("ЭПИДЕМИЯ (сила ").append(ex[8]).append(", ещё ").append(ex[7]).append(" дн.").append(ex[9] == 1 ? ", карантин" : "").append(")  ");
        if (ex[11] > 0) st.append("кузница: броня +").append(ex[11]);
        if (st.length() == 0) st.append("Город благополучен: нет голода и болезней");
        g.drawString(font, st.toString(), left + 10, top + 232, ex[10] > 0 || ex[7] > 0 ? red : dim);
        g.fill(left + 8, top + 262, left + panelW - 8, top + 263, 0xFF7A5C30);
        g.drawString(font, "Казначей: облигации " + info.bonds() + " изумр., пересчёт через " + info.daysToBond() + " дн.", left + 10, top + 274, gold);
    }
}
