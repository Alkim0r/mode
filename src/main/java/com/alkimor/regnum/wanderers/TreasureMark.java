package com.alkimor.regnum.wanderers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Метка на карте сокровищ: куда она ведёт и не засада ли это. */
public record TreasureMark(BlockPos pos, boolean trap) {
    public static final Codec<TreasureMark> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(TreasureMark::pos),
            Codec.BOOL.fieldOf("trap").forGetter(TreasureMark::trap)
    ).apply(i, TreasureMark::new));

    public static final StreamCodec<ByteBuf, TreasureMark> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TreasureMark::pos,
            ByteBufCodecs.BOOL, TreasureMark::trap,
            TreasureMark::new);
}
