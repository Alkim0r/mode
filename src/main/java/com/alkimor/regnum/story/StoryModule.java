package com.alkimor.regnum.story;

import com.alkimor.regnum.core.RegnumModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/** Модуль «Сюжет»: катсцены боссов, квесты, репутация фракций. */
public class StoryModule implements RegnumModule {
    @Override
    public String id() {
        return "story";
    }

    @Override
    public String title() {
        return "Сюжет";
    }

    @Override
    public void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(Cutscene.class);
        NeoForge.EVENT_BUS.register(Quests.class);
        NeoForge.EVENT_BUS.register(Recovery.class);
        NeoForge.EVENT_BUS.register(NightArmy.class);
        NeoForge.EVENT_BUS.register(ExternalBosses.class);
    }
}
