package com.alkimor.regnum.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Клиентские настройки (у каждого игрока свои): облегчённый режим для слабых ПК. */
public final class RegnumClientConfig {
    private RegnumClientConfig() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue LITE_MODE;
    public static final ModConfigSpec.BooleanValue HIDE_MODEL_DECOR;
    public static final ModConfigSpec.BooleanValue GLOW_LAYERS;
    public static final ModConfigSpec.BooleanValue PERFORMANCE_OVERLAY;
    public static final ModConfigSpec.IntValue NPC_DETAIL_DISTANCE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Облегчённый режим: для слабых ПК. Не влияет на мир и на других игроков — можно играть вместе.").push("lite");
        LITE_MODE = b.comment("Включить облегчённый режим (скрывает декор моделей, отключает свечение и природный ресурспак)")
                .define("liteMode", false);
        HIDE_MODEL_DECOR = b.comment("Скрывать декоративные детали моделей (плащи, плюмажи, колчаны) даже без облегчённого режима")
                .define("hideModelDecor", false);
        GLOW_LAYERS = b.comment("Светящиеся глаза/руны у боссов")
                .define("glowLayers", true);
        PERFORMANCE_OVERLAY = b.comment("Показывать клиентские FPS и 95-й процентиль времени кадра для диагностики")
                .define("performanceOverlay", false);
        NPC_DETAIL_DISTANCE = b.comment("Дальше этого расстояния (блоков) декор моделей не рисуется")
                .defineInRange("npcDetailDistance", 32, 8, 128);
        b.pop();
        SPEC = b.build();
    }

    public static boolean lite() {
        return LITE_MODE.get();
    }

    public static boolean decorHidden() {
        return LITE_MODE.get() || HIDE_MODEL_DECOR.get();
    }

}
