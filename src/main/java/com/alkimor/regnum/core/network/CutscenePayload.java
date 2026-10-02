package com.alkimor.regnum.core.network;

import com.alkimor.regnum.Regnum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Сервер → клиент: начало/конец катсцены (для «киношных» полос). title — имя босса/сцены, пусто если нет. */
public record CutscenePayload(boolean start, String title, int ticks) implements CustomPacketPayload {
    public static final Type<CutscenePayload> TYPE = new Type<>(Regnum.id("cutscene"));
    public static final StreamCodec<FriendlyByteBuf, CutscenePayload> CODEC = StreamCodec.ofMember(CutscenePayload::write, CutscenePayload::read);

    private void write(FriendlyByteBuf b) { b.writeBoolean(start); b.writeUtf(title); b.writeVarInt(ticks); }
    private static CutscenePayload read(FriendlyByteBuf b) { return new CutscenePayload(b.readBoolean(), b.readUtf(), b.readVarInt()); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
