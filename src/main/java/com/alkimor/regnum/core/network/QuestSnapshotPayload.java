package com.alkimor.regnum.core.network;

import com.alkimor.regnum.Regnum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/** Сервер → клиент: снимок журнала заданий. state: 0 активно, 1 доступно, 2 закрыто, 3 завершено; line: 0 главная, 1 побочные, 2 фракции. */
public record QuestSnapshotPayload(List<Q> quests, String trackedId, List<String> repFactions, List<Integer> repValues, boolean open)
        implements CustomPacketPayload {
    public record Q(String id, int line, int state, String title, String giver, String faction, String description,
                    List<String> objText, List<Integer> objProgress, List<Integer> objRequired, List<Boolean> objDone,
                    List<String> rewards, String lockReason) {}

    public static final Type<QuestSnapshotPayload> TYPE = new Type<>(Regnum.id("quest_snapshot"));
    public static final StreamCodec<FriendlyByteBuf, QuestSnapshotPayload> CODEC = StreamCodec.ofMember(QuestSnapshotPayload::write, QuestSnapshotPayload::read);

    private void write(FriendlyByteBuf b) {
        b.writeBoolean(open);
        b.writeUtf(trackedId);
        b.writeVarInt(repFactions.size());
        for (int i = 0; i < repFactions.size(); i++) { b.writeUtf(repFactions.get(i)); b.writeVarInt(repValues.get(i)); }
        b.writeVarInt(quests.size());
        for (Q q : quests) {
            b.writeUtf(q.id); b.writeVarInt(q.line); b.writeVarInt(q.state);
            b.writeUtf(q.title); b.writeUtf(q.giver); b.writeUtf(q.faction); b.writeUtf(q.description, 1024); b.writeUtf(q.lockReason, 512);
            b.writeVarInt(q.objText.size());
            for (int i = 0; i < q.objText.size(); i++) {
                b.writeUtf(q.objText.get(i)); b.writeVarInt(q.objProgress.get(i)); b.writeVarInt(q.objRequired.get(i)); b.writeBoolean(q.objDone.get(i));
            }
            b.writeVarInt(q.rewards.size());
            for (String s : q.rewards) b.writeUtf(s);
        }
    }

    private static int cap(int v, int max) {
        if (v < 0 || v > max) throw new io.netty.handler.codec.DecoderException("quest snapshot size out of range: " + v);
        return v;
    }

    private static QuestSnapshotPayload read(FriendlyByteBuf b) {
        boolean open = b.readBoolean();
        String tr = b.readUtf();
        List<String> rf = new ArrayList<>();
        List<Integer> rv = new ArrayList<>();
        int n = cap(b.readVarInt(), 16);
        for (int i = 0; i < n; i++) { rf.add(b.readUtf()); rv.add(b.readVarInt()); }
        List<Q> qs = new ArrayList<>();
        int m = cap(b.readVarInt(), 64);
        for (int k = 0; k < m; k++) {
            String id = b.readUtf();
            int line = Math.max(0, Math.min(2, b.readVarInt())), state = Math.max(0, Math.min(3, b.readVarInt()));
            String title = b.readUtf(), giver = b.readUtf(), fac = b.readUtf(), desc = b.readUtf(1024), lock = b.readUtf(512);
            List<String> ot = new ArrayList<>();
            List<Integer> op = new ArrayList<>(), orq = new ArrayList<>();
            List<Boolean> od = new ArrayList<>();
            int c = cap(b.readVarInt(), 12);
            for (int i = 0; i < c; i++) { ot.add(b.readUtf()); op.add(b.readVarInt()); orq.add(b.readVarInt()); od.add(b.readBoolean()); }
            List<String> rw = new ArrayList<>();
            int rc = cap(b.readVarInt(), 12);
            for (int i = 0; i < rc; i++) rw.add(b.readUtf());
            qs.add(new Q(id, line, state, title, giver, fac, desc, ot, op, orq, od, rw, lock));
        }
        return new QuestSnapshotPayload(qs, tr, rf, rv, open);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
