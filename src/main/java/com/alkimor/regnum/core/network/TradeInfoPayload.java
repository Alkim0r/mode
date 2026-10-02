package com.alkimor.regnum.core.network;

import com.alkimor.regnum.Regnum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Сервер → клиент: витрина рынка или купца. trend: -1 дешевле обычного, 0 норма, 1 дороже, 9 — скрыто (нужен перк). */
public record TradeInfoPayload(boolean entity, long key, String title, String region,
                               int[] buy, int[] sell, int[] have, int[] trend, int tradeLevel) implements CustomPacketPayload {
    public static final Type<TradeInfoPayload> TYPE = new Type<>(Regnum.id("trade_info"));
    public static final StreamCodec<FriendlyByteBuf, TradeInfoPayload> CODEC = StreamCodec.ofMember(TradeInfoPayload::write, TradeInfoPayload::read);

    private void write(FriendlyByteBuf b) {
        b.writeBoolean(entity);
        b.writeLong(key);
        b.writeUtf(title, 128);
        b.writeUtf(region, 64);
        b.writeVarIntArray(buy);
        b.writeVarIntArray(sell);
        b.writeVarIntArray(have);
        b.writeVarIntArray(trend);
        b.writeVarInt(tradeLevel);
    }

    private static TradeInfoPayload read(FriendlyByteBuf b) {
        return new TradeInfoPayload(b.readBoolean(), b.readLong(), b.readUtf(128), b.readUtf(64),
                b.readVarIntArray(), b.readVarIntArray(), b.readVarIntArray(), b.readVarIntArray(), b.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
