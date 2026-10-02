package com.alkimor.regnum.core.network;

import com.alkimor.regnum.Regnum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Клиент → сервер: начать исследование технологии (ordinal Science.Tech). */
public record ScienceActionPayload(int tech) implements CustomPacketPayload {
    public static final Type<ScienceActionPayload> TYPE = new Type<>(Regnum.id("science_action"));
    public static final StreamCodec<FriendlyByteBuf, ScienceActionPayload> CODEC = StreamCodec.ofMember(ScienceActionPayload::write, ScienceActionPayload::read);

    private void write(FriendlyByteBuf b) {
        b.writeVarInt(tech);
    }

    private static ScienceActionPayload read(FriendlyByteBuf b) {
        return new ScienceActionPayload(b.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
