package com.alkimor.regnum.core.network;

import com.alkimor.regnum.Regnum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Клиент → сервер: действия с героем (вложить очко, выбрать перк, создать персонажа). */
public record HeroActionPayload(int action, int arg, String text) implements CustomPacketPayload {
    public static final int SPEND_ATTR = 0, SPEND_FOCUS = 1, CHOOSE_PERK = 2, CREATE = 3, OPEN = 4;
    public static final Type<HeroActionPayload> TYPE = new Type<>(Regnum.id("hero_action"));
    public static final StreamCodec<FriendlyByteBuf, HeroActionPayload> CODEC = StreamCodec.ofMember(HeroActionPayload::write, HeroActionPayload::read);

    private void write(FriendlyByteBuf b) {
        b.writeVarInt(action);
        b.writeVarInt(arg);
        b.writeUtf(text, 1024);
    }

    private static HeroActionPayload read(FriendlyByteBuf b) {
        return new HeroActionPayload(b.readVarInt(), b.readVarInt(), b.readUtf(1024));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
