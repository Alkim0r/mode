package com.alkimor.regnum.client.render;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.client.screen.QuestJournalScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.List;

/** Компактная цель отслеживаемого задания; обновляется из серверного снимка журнала. */
@EventBusSubscriber(modid = Regnum.MODID, value = Dist.CLIENT)
public final class QuestTrackerOverlay {
    private static final int GOLD = 0xFFE8C872, TEXT = 0xFFE4DDCF, DIM = 0xFFB2A895;
    private static volatile QuestJournalScreen.Quest tracked;
    private static QuestJournalScreen.Quest layoutQuest;
    private static int layoutWidth = -1;
    private static String layoutTitle = "";
    private static List<net.minecraft.util.FormattedCharSequence> layoutLines = List.of();

    private QuestTrackerOverlay() {}

    public static void update(QuestJournalScreen.Snapshot snapshot) {
        QuestJournalScreen.refreshIfOpen(snapshot);
        if (snapshot == null || snapshot.trackedId().isBlank()) {
            tracked = null;
            return;
        }
        tracked = snapshot.quests().stream().filter(q -> q.id().equals(snapshot.trackedId())
                && q.state() == QuestJournalScreen.State.ACTIVE).findFirst().orElse(null);
    }

    public static void clear() { tracked = null; }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        QuestJournalScreen.Quest quest = tracked;
        Minecraft mc = Minecraft.getInstance();
        if (quest == null || mc.player == null || mc.screen != null || mc.options.hideGui) return;
        GuiGraphics g = event.getGuiGraphics();
        int maxWidth = Math.min(290, mc.getWindow().getGuiScaledWidth() - 16);
        if (maxWidth < 120) return;
        int x = 8, y = 8, textX = x + 8, textWidth = maxWidth - 16;
        if (quest != layoutQuest || textWidth != layoutWidth) {
            QuestJournalScreen.Objective next = quest.objectives().stream().filter(o -> !o.done()).findFirst().orElse(null);
            String objective = next == null ? "Все цели выполнены" : next.text()
                    + (next.required() > 1 ? "  " + next.progress() + "/" + next.required() : "");
            layoutLines = mc.font.split(Component.literal(objective), textWidth);
            layoutTitle = mc.font.plainSubstrByWidth(quest.title(), textWidth);
            layoutQuest = quest;
            layoutWidth = textWidth;
        }
        int shownLines = Math.min(2, layoutLines.size());
        int height = 12 + shownLines * 10;
        g.fill(x, y, x + maxWidth, y + height, 0xB0100E0A);
        g.fill(x, y, x + 2, y + height, 0xFFE8C872);
        g.drawString(mc.font, layoutTitle, textX, y + 3, GOLD);
        for (int i = 0; i < shownLines; i++) g.drawString(mc.font, layoutLines.get(i), textX, y + 14 + i * 10, i == 0 ? TEXT : DIM);
    }
}
