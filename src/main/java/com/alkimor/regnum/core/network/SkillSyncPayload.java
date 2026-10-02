package com.alkimor.regnum.core.network;

import com.alkimor.regnum.Regnum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Сервер → клиент: всё о герое — навыки, атрибуты, фокус, перки, черты, класс, честь, летопись, род. */
public record SkillSyncPayload(int[] xp, int[] attrs, int[] focus, int freeAttr, int freeFocus, int charXp,
                               List<String> perks, List<String> traits, String cls, int honor,
                               List<String> chronicle, List<String> family, int open) implements CustomPacketPayload {
    public static final int OPEN_JOURNAL = 1, OPEN_CREATION = 2;
    public static final Type<SkillSyncPayload> TYPE = new Type<>(Regnum.id("skill_sync"));
    public static final StreamCodec<FriendlyByteBuf, SkillSyncPayload> CODEC = StreamCodec.ofMember(SkillSyncPayload::write, SkillSyncPayload::read);

    private void write(FriendlyByteBuf buf) {
        buf.writeVarIntArray(xp);
        buf.writeVarIntArray(attrs);
        buf.writeVarIntArray(focus);
        buf.writeVarInt(freeAttr);
        buf.writeVarInt(freeFocus);
        buf.writeVarInt(charXp);
        buf.writeCollection(perks, (b, v) -> b.writeUtf(v, 64));
        buf.writeCollection(traits, (b, v) -> b.writeUtf(v, 64));
        buf.writeUtf(cls, 64);
        buf.writeVarInt(honor);
        buf.writeCollection(chronicle, (b, v) -> b.writeUtf(v, 256));
        buf.writeCollection(family, (b, v) -> b.writeUtf(v, 256));
        buf.writeVarInt(open);
    }

    private static SkillSyncPayload read(FriendlyByteBuf buf) {
        return new SkillSyncPayload(buf.readVarIntArray(), buf.readVarIntArray(), buf.readVarIntArray(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readList(b -> b.readUtf(64)), buf.readList(b -> b.readUtf(64)), buf.readUtf(64), buf.readVarInt(),
                buf.readList(b -> b.readUtf(256)), buf.readList(b -> b.readUtf(256)), buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
