package com.alkimor.regnum.survival;

import com.alkimor.regnum.core.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Времена года (идея «Serene Seasons», переписано под Regnum): по 8 игровых суток на сезон.
 * Зимой посевы растут втрое реже и холоднее, летом жарче, весной и осенью — мягко; осенью урожай богаче.
 */
public final class Seasons {
    private Seasons() {}

    public static final int DAYS = 8;
    public static final String[] NAMES = {"весна", "лето", "осень", "зима"};
    private static int lastAnnounced = -1;

    public static long day(Level l) { return l.getDayTime() / 24000L; }

    public static int index(Level l) { return (int) ((day(l) / DAYS) % 4); }

    public static String name(Level l) { return NAMES[index(l)]; }

    public static int dayInSeason(Level l) { return (int) (day(l) % DAYS) + 1; }

    /** Поправка к температуре окружения для {@link Needs}. */
    public static int warmthOffset(Level l) {
        return switch (index(l)) { case 1 -> 14; case 3 -> -26; case 2 -> -6; default -> 0; };
    }

    @SubscribeEvent
    public static void onCrop(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel l)) return;
        int s = index(l);
        if (s == 3 && l.random.nextInt(3) != 0) event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
        else if (s == 2 && l.random.nextInt(8) == 0) event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
        else if (s == 0 && l.random.nextInt(5) == 0) event.setResult(CropGrowEvent.Pre.Result.GROW);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 200 != 0) return;
        ServerLevel ow = event.getServer().overworld();
        long d = day(ow);
        if (d % DAYS != 0 || ow.getDayTime() % 24000L > 2000L) return;
        int s = index(ow);
        if (lastAnnounced == d) return;
        lastAnnounced = (int) d;
        String tip = switch (s) {
            case 0 -> "Тает снег: посевы растут чуть бодрее.";
            case 1 -> "Жаркое время: больше жажды, в пустынях пекло.";
            case 2 -> "Время урожая, но уже холодает.";
            default -> "Зима: холодно, посевы почти не растут. Запасайтесь едой и дровами.";
        };
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers())
            p.sendSystemMessage(Text.of("Наступила " + NAMES[s] + ". " + tip, ChatFormatting.GOLD));
    }
}
