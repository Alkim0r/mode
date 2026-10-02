package com.alkimor.regnum.client.render;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.client.StoryClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Cinematic framing for the server-authoritative boss introduction. */
@EventBusSubscriber(modid = Regnum.MODID, value = Dist.CLIENT)
public final class CutsceneLetterboxOverlay {
    private static final int GOLD = 0xFFE8C872;
    private static final long INTRO_MS = 280L;

    private CutsceneLetterboxOverlay() {}

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        if (!StoryClient.cutsceneActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphics gui = event.getGuiGraphics();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        if (width <= 0 || height <= 0) return;

        float t = Math.min(1.0f, Math.max(0.0f, StoryClient.cutsceneAgeMs() / (float) INTRO_MS));
        // Ease the bars in so the transition reads as a deliberate cinematic cue.
        float eased = t * t * (3.0f - 2.0f * t);
        int barHeight = Math.round(Math.min(28, height / 9.0f) * eased);
        if (barHeight <= 0) return;

        gui.fill(0, 0, width, barHeight, 0xFF050505);
        gui.fill(0, height - barHeight, width, height, 0xFF050505);

        String title = StoryClient.cutsceneTitle();
        if (title == null || title.isBlank() || barHeight < 18) return;
        int maxWidth = Math.max(1, width - 32);
        String fitted = mc.font.plainSubstrByWidth(title, maxWidth);
        int textWidth = mc.font.width(fitted);
        gui.drawString(mc.font, Component.literal(fitted), (width - textWidth) / 2,
                Math.max(2, (barHeight - mc.font.lineHeight) / 2), GOLD, true);
    }
}
