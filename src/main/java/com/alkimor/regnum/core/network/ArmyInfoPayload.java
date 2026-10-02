package com.alkimor.regnum.core.network;

import com.alkimor.regnum.Regnum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Сервер → клиент: сводка по армии рядом с королём, открывает командный экран.
 * squadCounts[0] — всего, [1..4] — по отрядам. squadOrders[1..4] — текущий приказ отряда (ordinal), -1 если пусто.
 */
public record ArmyInfoPayload(int[] squadCounts, int[] squadOrders, int selectedSquad, int formation) implements CustomPacketPayload {
    public static final Type<ArmyInfoPayload> TYPE = new Type<>(Regnum.id("army_info"));
    public static final StreamCodec<FriendlyByteBuf, ArmyInfoPayload> CODEC = StreamCodec.ofMember(ArmyInfoPayload::write, ArmyInfoPayload::read);

    private void write(FriendlyByteBuf b) {
        b.writeVarIntArray(squadCounts);
        b.writeVarIntArray(squadOrders);
        b.writeVarInt(selectedSquad);
        b.writeVarInt(formation);
    }

    private static ArmyInfoPayload read(FriendlyByteBuf b) {
        return new ArmyInfoPayload(b.readVarIntArray(), b.readVarIntArray(), b.readVarInt(), b.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
