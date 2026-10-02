package com.alkimor.regnum.client;

import com.alkimor.regnum.client.render.QuestTrackerOverlay;
import com.alkimor.regnum.client.screen.QuestJournalScreen;
import com.alkimor.regnum.core.network.CutscenePayload;
import com.alkimor.regnum.core.network.QuestSnapshotPayload;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Приёмник сюжетных пакетов. Катсцена: читайте cutsceneActive()/cutsceneTitle()/cutsceneAge() для полос и титров. */
public final class StoryClient {
    private StoryClient() {}

    private static volatile boolean cutscene;
    private static volatile String cutsceneTitle = "";
    private static volatile long cutsceneStart;

    public static boolean cutsceneActive() { return cutscene; }
    public static String cutsceneTitle() { return cutsceneTitle; }
    /** Миллисекунды с начала сцены (для плавного выезда полос). */
    public static long cutsceneAgeMs() { return cutscene ? System.currentTimeMillis() - cutsceneStart : 0; }

    public static void onCutscene(CutscenePayload p) {
        cutscene = p.start();
        cutsceneTitle = p.title();
        if (p.start()) cutsceneStart = System.currentTimeMillis();
    }

    public static void onSnapshot(QuestSnapshotPayload p) {
        List<QuestJournalScreen.Quest> qs = new ArrayList<>();
        QuestJournalScreen.Line[] lines = QuestJournalScreen.Line.values();
        QuestJournalScreen.State[] states = QuestJournalScreen.State.values();
        for (QuestSnapshotPayload.Q q : p.quests()) {
            List<QuestJournalScreen.Objective> obj = new ArrayList<>();
            for (int i = 0; i < q.objText().size(); i++)
                obj.add(new QuestJournalScreen.Objective(q.objText().get(i), q.objProgress().get(i), q.objRequired().get(i), q.objDone().get(i)));
            qs.add(new QuestJournalScreen.Quest(q.id(), lines[Math.max(0, Math.min(q.line(), lines.length - 1))], states[Math.max(0, Math.min(q.state(), states.length - 1))],
                    q.title(), q.giver(), q.faction(), q.description(), obj, q.rewards(), q.lockReason()));
        }
        Map<String, Integer> rep = new LinkedHashMap<>();
        for (int i = 0; i < p.repFactions().size(); i++) rep.put(p.repFactions().get(i), p.repValues().get(i));
        QuestJournalScreen.Snapshot s = new QuestJournalScreen.Snapshot(qs, p.trackedId(), rep);
        QuestTrackerOverlay.update(s);
        if (p.open()) QuestJournalScreen.open(s);
    }
}
