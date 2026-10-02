package com.alkimor.regnum.client;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.client.screen.CityScreen;
import com.alkimor.regnum.client.screen.CommandScreen;
import com.alkimor.regnum.client.screen.JournalScreen;
import com.alkimor.regnum.client.screen.TradeScreen;
import com.alkimor.regnum.client.screen.ScienceScreen;
import com.alkimor.regnum.client.screen.CommandersScreen;
import com.alkimor.regnum.client.screen.QuestJournalScreen;
import com.alkimor.regnum.core.network.ArmyInfoPayload;
import com.alkimor.regnum.core.network.CityInfoPayload;
import com.alkimor.regnum.core.network.TradeInfoPayload;
import com.alkimor.regnum.core.network.ScienceInfoPayload;
import com.alkimor.regnum.core.network.CommandersPayload;
import com.alkimor.regnum.kingdom.Science;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Клиентский автотест (-Dregnum.clienttest=true): открывает все экраны мода и закрывает игру. */
@EventBusSubscriber(modid = Regnum.MODID, value = Dist.CLIENT)
public final class ClientSelfTest {
    private ClientSelfTest() {}

    private static int ticks = -1;
    private static int originalGuiScale;
    private static long waitingForTitleSinceMs;

    private static void capture(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> {});
        Regnum.LOGGER.info("[REGNUM-CLIENTTEST] screenshot {}", name);
    }

    private static ScienceInfoPayload science() {
        return new ScienceInfoPayload(new int[]{Science.Tech.WRITING.ordinal(), Science.Tech.AGRICULTURE.ordinal(), -1, 999},
                Science.Tech.MEDICINE.ordinal(), 19, 7);
    }

    private static CommandersPayload commanders() {
        String[] names = new String[17];
        int[] data = new int[names.length * CommandersPayload.STRIDE];
        for (int i = 0; i < names.length; i++) {
            names[i] = i == 0 ? "(не в сети)" : "Commander_" + i;
            int at = i * CommandersPayload.STRIDE;
            data[at] = i % 5;
            data[at + 1] = i % 7;
            data[at + 2] = i * 60;
            data[at + 3] = i * 3;
            data[at + 4] = 12 + i % 7 * 4;
            data[at + 5] = i + 4;
        }
        return new CommandersPayload(names, data, true);
    }

    private static QuestJournalScreen.Snapshot quests() {
        var entries = new java.util.ArrayList<QuestJournalScreen.Quest>();
        QuestJournalScreen.Line[] lines = QuestJournalScreen.Line.values();
        for (int i = 0; i < 42; i++) {
            QuestJournalScreen.State state = switch (i % 6) {
                case 0, 5 -> QuestJournalScreen.State.ACTIVE;
                case 1, 4 -> QuestJournalScreen.State.AVAILABLE;
                case 3 -> QuestJournalScreen.State.COMPLETED;
                default -> QuestJournalScreen.State.LOCKED;
            };
            entries.add(new QuestJournalScreen.Quest("quest_" + i, lines[i % lines.length], state,
                    "Испытание " + (i + 1), "Хранитель Кассиан", i % 2 == 0 ? "Хранители печати" : "Стража границ",
                    "Проследуйте по старому тракту, поговорите с союзниками и подготовьте отряд к новой угрозе. "
                            + "В заброшенной шахте погасли огни; разведчики услышали скрежет за запечатанным проходом. "
                            + "Исследуйте нижние галереи, соберите припасы для похода и найдите безопасную дорогу обратно. ".repeat(3),
                    java.util.List.of(new QuestJournalScreen.Objective("Победить разбойников на дороге", i % 5, 5, false),
                            new QuestJournalScreen.Objective("Вернуться к хранителю", 0, 1, false)),
                    java.util.List.of("Опыт героя", "Репутация фракции"), state == QuestJournalScreen.State.LOCKED ? "Сначала завершите предыдущее задание" : ""));
        }
        return new QuestJournalScreen.Snapshot(entries, "quest_0",
                java.util.Map.of("Хранители печати", 45, "Стража границ", 20, "Вольные торговцы", 10, "Охотники склепов", 5));
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("regnum.clienttest")) return;
        Minecraft mc = Minecraft.getInstance();
        if (ticks < 0) {
            if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen onboarding) {
                Regnum.LOGGER.info("[REGNUM-CLIENTTEST] completing first-launch onboarding in test profile");
                onboarding.onClose();
                return;
            }
            if (mc.screen instanceof TitleScreen) {
                ticks = 0;
                originalGuiScale = mc.options.guiScale().get();
                Regnum.LOGGER.info("[REGNUM-CLIENTTEST] title screen reached; starting UI checks");
            } else {
                long now = System.currentTimeMillis();
                if (waitingForTitleSinceMs == 0) waitingForTitleSinceMs = now;
                else if (now - waitingForTitleSinceMs > 60_000L) {
                    Regnum.LOGGER.error("[REGNUM-CLIENTTEST] RESULT: FAIL — title screen did not load within 60 seconds");
                    mc.stop();
                }
            }
            return;
        }
        ticks++;
        try {
            switch (ticks) {
                case 20 -> mc.setScreen(new JournalScreen());
                case 60 -> mc.setScreen(new CityScreen(new CityInfoPayload(BlockPos.ZERO, "Тестоград", "Вождь", 2, 40, 7, 55, 30, 6, 13,
                        1, 1, 1, 2, false, 1, 60, 20, 12, 6, 3, 2, 1, 32, 1, 5, 0, new int[com.alkimor.regnum.kingdom.SoldierType.values().length], new int[com.alkimor.regnum.core.network.CityInfoPayload.EXTRA_LEN])));
                case 100 -> mc.setScreen(new CommandScreen(new ArmyInfoPayload(new int[]{6, 3, 2, 1, 0}, new int[]{-1, 0, 2, 3, -1}, 1, 0)));
                case 140 -> mc.setScreen(new TradeScreen(new TradeInfoPayload(false, 0L, "Рынок", "равнины",
                        new int[]{7, 18, 22, 14, 8, 11, 9, 27}, new int[]{5, 12, 15, 10, 6, 8, 7, 19},
                        new int[]{0, 2, 0, 0, 1, 0, 0, 0}, new int[]{-1, 1, 0, 9, 0, 0, -1, 1}, 5)));
                case 180 -> {
                    mc.options.guiScale().set(2);
                    mc.resizeDisplay();
                    ScienceScreen.open(science());
                }
                case 210 -> capture(mc, "x1_science");
                case 230 -> {
                    mc.screen.keyPressed(262, 0, 0);
                    mc.screen.keyPressed(263, 0, 0);
                    mc.screen.mouseScrolled(100, 100, 0, -3);
                }
                case 250 -> capture(mc, "x1_science_scrolled");
                case 270 -> CommandersScreen.open(commanders());
                case 300 -> capture(mc, "x1_commanders");
                case 320 -> mc.screen.mouseScrolled(mc.screen.width / 2.0, 90, 0, -1);
                case 340 -> capture(mc, "x1_commanders_page2");
                case 360 -> CommandersScreen.open(new CommandersPayload(new String[0], new int[0], true));
                case 390 -> capture(mc, "x1_commanders_empty");
                case 410 -> {
                    mc.options.guiScale().set(4);
                    mc.resizeDisplay();
                    ScienceScreen.open(science());
                }
                case 440 -> capture(mc, "x1_science_small");
                case 460 -> CommandersScreen.open(commanders());
                case 490 -> capture(mc, "x1_commanders_small");
                case 510 -> {
                    mc.options.guiScale().set(originalGuiScale);
                    mc.resizeDisplay();
                    QuestJournalScreen.open(quests());
                }
                case 540 -> capture(mc, "p3_quest_journal");
                case 550 -> mc.screen.keyPressed(264, 0, 0);
                case 560 -> capture(mc, "p3_quest_journal_selected");
                case 570 -> mc.screen.mouseScrolled(mc.screen.width * 0.15, mc.screen.height * 0.5, 0, -3);
                case 580 -> capture(mc, "p3_quest_journal_scrolled");
                case 590 -> {
                    var previous = mc.screen;
                    var original = quests();
                    var updated = new java.util.ArrayList<>(original.quests());
                    var q = updated.get(6);
                    updated.set(6, new QuestJournalScreen.Quest(q.id(), q.line(), QuestJournalScreen.State.COMPLETED,
                            q.title(), q.giver(), q.faction(), q.description(), q.objectives(), q.rewards(), q.lockReason()));
                    com.alkimor.regnum.client.render.QuestTrackerOverlay.update(new QuestJournalScreen.Snapshot(updated, "quest_0", original.reputation()));
                    if (mc.screen != previous) throw new IllegalStateException("Snapshot replaced the open journal");
                }
                case 610 -> capture(mc, "p5_quest_journal_live_update");
                case 630 -> {
                    mc.options.guiScale().set(4);
                    mc.resizeDisplay();
                    QuestJournalScreen.open(quests());
                }
                case 650 -> capture(mc, "p5_quest_journal_small");
                case 670 -> mc.screen.keyPressed(267, 0, 0);
                case 690 -> capture(mc, "p5_quest_journal_detail_scrolled");
                case 710 -> {
                    mc.options.guiScale().set(originalGuiScale);
                    mc.resizeDisplay();
                    mc.setScreen(new TitleScreen());
                    Regnum.LOGGER.info("[REGNUM-CLIENTTEST] RESULT: OK — все экраны открылись и отрисовались");
                }
                case 730 -> mc.stop();
                default -> {}
            }
        } catch (Throwable t) {
            Regnum.LOGGER.error("[REGNUM-CLIENTTEST] RESULT: FAIL", t);
            mc.options.guiScale().set(originalGuiScale);
            mc.stop();
        }
    }
}
