package com.alkimor.regnum.client.render;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.core.RegnumClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.Arrays;
import java.util.Locale;

/** Opt-in client-side frame-time probe for comparing normal and Lite rendering. */
@EventBusSubscriber(modid = Regnum.MODID, value = Dist.CLIENT)
public final class ClientPerformanceOverlay {
    private static final int WINDOW = 120;
    private static final double[] FRAME_MS = new double[WINDOW];
    private static long previousFrameNs;
    private static int cursor;
    private static int samples;
    private static int framesSinceRefresh;
    private static double p95Ms;
    private static boolean previousLite;
    private static boolean hasLiteState;
    private static String displayText = "";

    private ClientPerformanceOverlay() {}

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!RegnumClientConfig.PERFORMANCE_OVERLAY.get() || mc.level == null || mc.player == null) {
            if (previousFrameNs != 0) resetWindow();
            return;
        }

        boolean lite = RegnumClientConfig.lite();
        if (hasLiteState && previousLite != lite) resetWindow();
        previousLite = lite;
        hasLiteState = true;

        long now = System.nanoTime();
        if (previousFrameNs != 0) {
            FRAME_MS[cursor] = Math.max(0.0, (now - previousFrameNs) / 1_000_000.0);
            cursor = (cursor + 1) % WINDOW;
            samples = Math.min(WINDOW, samples + 1);
            if (++framesSinceRefresh >= 15) {
                updatePercentile();
                displayText = String.format(Locale.ROOT, "FPS %d  |  кадр p95 %.1f мс  |  Lite %s",
                        mc.getFps(), p95Ms, lite ? "вкл" : "выкл");
                framesSinceRefresh = 0;
            }
        }
        previousFrameNs = now;

        GuiGraphics gui = event.getGuiGraphics();
        int right = mc.getWindow().getGuiScaledWidth() - 8;
        int top = 8;
        int textWidth = mc.font.width(displayText);
        gui.fill(right - textWidth - 10, top - 3, right + 2, top + mc.font.lineHeight + 3, 0xB0100E0A);
        gui.drawString(mc.font, displayText, right - textWidth - 4, top, 0xFFE8C872, true);
    }

    private static void resetWindow() {
        Arrays.fill(FRAME_MS, 0.0);
        previousFrameNs = 0;
        cursor = 0;
        samples = 0;
        framesSinceRefresh = 0;
        p95Ms = 0;
        displayText = "";
    }

    private static void updatePercentile() {
        if (samples == 0) {
            p95Ms = 0;
            return;
        }
        double[] sorted = Arrays.copyOf(FRAME_MS, samples);
        Arrays.sort(sorted);
        int index = Math.min(samples - 1, (int) Math.ceil(samples * 0.95) - 1);
        p95Ms = sorted[index];
    }
}
