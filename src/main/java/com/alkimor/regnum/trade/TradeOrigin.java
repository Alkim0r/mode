package com.alkimor.regnum.trade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Откуда везут груз: точка закупки и её регион. Чем дальше доставка — тем выше цена продажи. */
public record TradeOrigin(BlockPos pos, int region) {
    public static final Codec<TradeOrigin> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(TradeOrigin::pos),
            Codec.INT.fieldOf("region").forGetter(TradeOrigin::region)
    ).apply(i, TradeOrigin::new));

    public static final StreamCodec<ByteBuf, TradeOrigin> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TradeOrigin::pos,
            ByteBufCodecs.VAR_INT, TradeOrigin::region,
            TradeOrigin::new);
}
