package com.alkimor.regnum.core.network;

import com.alkimor.regnum.Regnum;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Клиент → сервер: действие на экране ратуши. */
public record CityActionPayload(BlockPos hall, int action, int arg, String text) implements CustomPacketPayload {
    public static final int DEPOSIT = 0;
    public static final int WITHDRAW = 1;
    public static final int RECRUIT = 2;      // arg = SoldierType.ordinal()
    public static final int UPGRADE = 3;
    public static final int RENAME = 4;       // text = новое имя
    public static final int SET_SQUAD = 5;    // arg = номер отряда 1..4
    public static final int INVEST = 6;       // вложить 16 изумрудов казны в облигации
    public static final int DIVEST = 7;       // снять все облигации в казну
    public static final int STRATEGY = 8;     // сменить стратегию
    public static final int CULTURE = 9;      // сменить культуру

    public static final Type<CityActionPayload> TYPE = new Type<>(Regnum.id("city_action"));
    public static final StreamCodec<FriendlyByteBuf, CityActionPayload> CODEC = StreamCodec.ofMember(CityActionPayload::write, CityActionPayload::read);

    private void write(FriendlyByteBuf b) {
        b.writeBlockPos(hall);
        b.writeVarInt(action);
        b.writeVarInt(arg);
        b.writeUtf(text, 64);
    }

    private static CityActionPayload read(FriendlyByteBuf b) {
        return new CityActionPayload(b.readBlockPos(), b.readVarInt(), b.readVarInt(), b.readUtf(64));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
