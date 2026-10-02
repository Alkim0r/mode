package com.alkimor.regnum;

import com.alkimor.regnum.core.ModRegistries;
import com.alkimor.regnum.core.RegnumConfig;
import com.alkimor.regnum.core.RegnumModule;
import com.alkimor.regnum.core.command.RegnumCommands;
import com.alkimor.regnum.core.network.RegnumNetwork;
import com.alkimor.regnum.crafting.CraftingModule;
import com.alkimor.regnum.dungeon.DungeonModule;
import com.alkimor.regnum.kingdom.KingdomModule;
import com.alkimor.regnum.survival.SurvivalModule;
import com.alkimor.regnum.wanderers.WanderersModule;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.util.List;

/**
 * Regnum: Королевства и Легенды.
 * <p>
 * Мод собран из независимых подмодулей ({@link RegnumModule}). Каждый модуль сам регистрирует
 * свой контент и обработчики событий, а в конфиге его геймплей можно выключить.
 * Чтобы добавить новый подмод — создайте класс-модуль и допишите его в {@link #MODULES}.
 */
@Mod(Regnum.MODID)
public class Regnum {
    public static final String MODID = "regnum";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Порядок важен: выживание и ремёсла раньше остальных, на них опираются другие модули. */
    public static final List<RegnumModule> MODULES = List.of(
            new SurvivalModule(),
            new CraftingModule(),
            new KingdomModule(),
            new DungeonModule(),
            new com.alkimor.regnum.dungeon.RegionsModule(),
            new WanderersModule(),
            new com.alkimor.regnum.trade.TradeModule(),
            new com.alkimor.regnum.dynasty.DynastyModule(),
            new com.alkimor.regnum.combat.CombatModule(),
            new com.alkimor.regnum.armor.ArmorModule(),
            new com.alkimor.regnum.story.StoryModule(),
            new com.alkimor.regnum.mine.MineModule()
    );

    public Regnum(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, RegnumConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, com.alkimor.regnum.core.RegnumClientConfig.SPEC);

        for (RegnumModule module : MODULES) {
            LOGGER.info("[Regnum] Подключаю модуль: {}", module.title());
            module.init(modBus);
        }

        ModRegistries.register(modBus);
        modBus.addListener(RegnumNetwork::register);
        NeoForge.EVENT_BUS.addListener(RegnumCommands::register);
        NeoForge.EVENT_BUS.register(com.alkimor.regnum.core.SaveGuard.class);
        NeoForge.EVENT_BUS.addListener(com.alkimor.regnum.core.SelfTest::onServerStarted);
        if (com.alkimor.regnum.core.SelfTest.enabled()) NeoForge.EVENT_BUS.addListener(com.alkimor.regnum.core.SelfTest::onServerTick);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
