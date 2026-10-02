package com.alkimor.regnum.core.network;

import com.alkimor.regnum.Regnum;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Сервер → клиент: состояние города для экрана ратуши.
 * extra (EXTRA_LEN чисел): [0..5] запасы по Resource.ordinal(), 6 вместимость склада, 7 дней эпидемии, 8 сила эпидемии,
 * 9 карантин (0/1), 10 дней голода, 11 бонус брони кузницы, 12 нужно дерева на апгрейд, 13 нужно камня на апгрейд.
 */
public record CityInfoPayload(BlockPos hall, String name, String ruler, int level, int radius, int population,
                              int treasury, int glory, int army, int armyCap,
                              int barracks, int markets, int towers,
                              int daysToRaid, boolean raidActive, int recruitSquad,
                              int upgradeCost, int upgradeGlory, int dailyIncome, int dailyUpkeep,
                              int swordsmen, int archers, int knights,
                              int bonds, int bondStrategy, int daysToBond, int culture, int[] byType, int[] extra) implements CustomPacketPayload {
    public static final int EXTRA_LEN = 14;
    public static final Type<CityInfoPayload> TYPE = new Type<>(Regnum.id("city_info"));
    public static final StreamCodec<FriendlyByteBuf, CityInfoPayload> CODEC = StreamCodec.ofMember(CityInfoPayload::write, CityInfoPayload::read);

    private void write(FriendlyByteBuf b) {
        b.writeBlockPos(hall);
        b.writeUtf(name, 64);
        b.writeUtf(ruler, 64);
        int[] head = {level, radius, population, treasury, glory, army, armyCap, barracks, markets, towers,
                daysToRaid, raidActive ? 1 : 0, recruitSquad, upgradeCost, upgradeGlory, dailyIncome, dailyUpkeep,
                swordsmen, archers, knights, bonds, bondStrategy, daysToBond, culture};
        int[] all = java.util.Arrays.copyOf(head, head.length + byType.length);
        System.arraycopy(byType, 0, all, head.length, byType.length);
        b.writeVarIntArray(all);
        b.writeVarIntArray(extra);
    }

    private static CityInfoPayload read(FriendlyByteBuf b) {
        BlockPos hall = b.readBlockPos();
        String name = b.readUtf(64);
        String ruler = b.readUtf(64);
        int[] v = b.readVarIntArray();
        int[] extra = java.util.Arrays.copyOf(b.readVarIntArray(), EXTRA_LEN);
        return new CityInfoPayload(hall, name, ruler, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9],
                v[10], v[11] == 1, v[12], v[13], v[14], v[15], v[16], v[17], v[18], v[19], v[20], v[21], v[22], v[23], java.util.Arrays.copyOfRange(v, 24, v.length), extra);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
