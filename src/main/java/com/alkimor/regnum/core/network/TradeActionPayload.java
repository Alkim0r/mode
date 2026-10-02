package com.alkimor.regnum.core.network;

import com.alkimor.regnum.Regnum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Клиент → сервер: купить (amount > 0) или продать (amount < 0) товар. */
public record TradeActionPayload(boolean entity, long key, int good, int amount) implements CustomPacketPayload {
    public static final Type<TradeActionPayload> TYPE = new Type<>(Regnum.id("trade_action"));
    public static final StreamCodec<FriendlyByteBuf, TradeActionPayload> CODEC = StreamCodec.ofMember(TradeActionPayload::write, TradeActionPayload::read);

    private void write(FriendlyByteBuf b) {
        b.writeBoolean(entity);
        b.writeLong(key);
        b.writeVarInt(good);
        b.writeInt(amount);
    }

    private static TradeActionPayload read(FriendlyByteBuf b) {
        return new TradeActionPayload(b.readBoolean(), b.readLong(), b.readVarInt(), b.readInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
