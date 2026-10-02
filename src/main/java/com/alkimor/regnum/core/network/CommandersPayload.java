package com.alkimor.regnum.core.network;

import com.alkimor.regnum.Regnum;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Сервер → клиент: военачальники королевства. names[i] — ник; data — по 6 чисел на каждого:
 * ветка (ordinal Commissions.Branch), звание-тир (0..6), репутация, победы, лимит бойцов, бойцов сейчас.
 */
public record CommandersPayload(String[] names, int[] data, boolean isKing) implements CustomPacketPayload {
    public static final int STRIDE = 6;
    public static final Type<CommandersPayload> TYPE = new Type<>(Regnum.id("commanders"));
    public static final StreamCodec<FriendlyByteBuf, CommandersPayload> CODEC = StreamCodec.ofMember(CommandersPayload::write, CommandersPayload::read);

    private void write(FriendlyByteBuf b) {
        b.writeVarInt(names.length);
        for (String n : names) b.writeUtf(n, 64);
        b.writeVarIntArray(data);
        b.writeBoolean(isKing);
    }

    private static CommandersPayload read(FriendlyByteBuf b) {
        int n = b.readVarInt();
        if (n < 0 || n > 64) throw new io.netty.handler.codec.DecoderException("commanders: bad count " + n);
        String[] names = new String[n];
        for (int i = 0; i < n; i++) names[i] = b.readUtf(64);
        int[] data = b.readVarIntArray();
        if (data.length != n * STRIDE) throw new io.netty.handler.codec.DecoderException("commanders: bad data length");
        return new CommandersPayload(names, data, b.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
