package com.alkimor.regnum.core.network;

import com.alkimor.regnum.Regnum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Клиент → сервер: команда с командного экрана.
 * order = -1 означает «только сменить выбранный отряд/строй на жезле».
 */
public record ArmyOrderPayload(int squad, int order, int formation) implements CustomPacketPayload {
    public static final Type<ArmyOrderPayload> TYPE = new Type<>(Regnum.id("army_order"));
    public static final StreamCodec<FriendlyByteBuf, ArmyOrderPayload> CODEC = StreamCodec.ofMember(ArmyOrderPayload::write, ArmyOrderPayload::read);

    private void write(FriendlyByteBuf b) {
        b.writeVarInt(squad);
        b.writeVarInt(order);
        b.writeVarInt(formation);
    }

    private static ArmyOrderPayload read(FriendlyByteBuf b) {
        return new ArmyOrderPayload(b.readVarInt(), b.readVarInt(), b.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
