package com.alkimor.regnum.core.network;

import com.alkimor.regnum.Regnum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Сервер → клиент: древо знаний игрока. done — ordinal'ы изученных Science.Tech, current — ordinal или -1. */
public record ScienceInfoPayload(int[] done, int current, int progress, int perDay) implements CustomPacketPayload {
    public static final Type<ScienceInfoPayload> TYPE = new Type<>(Regnum.id("science_info"));
    public static final StreamCodec<FriendlyByteBuf, ScienceInfoPayload> CODEC = StreamCodec.ofMember(ScienceInfoPayload::write, ScienceInfoPayload::read);

    private void write(FriendlyByteBuf b) {
        b.writeVarIntArray(done);
        b.writeVarInt(current + 1);
        b.writeVarInt(progress);
        b.writeVarInt(perDay);
    }

    private static ScienceInfoPayload read(FriendlyByteBuf b) {
        return new ScienceInfoPayload(b.readVarIntArray(), b.readVarInt() - 1, b.readVarInt(), b.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
