package com.alkimor.regnum.combat;

import com.alkimor.regnum.core.RegnumModule;
import net.neoforged.bus.api.IEventBus;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Модуль «Бой»: умные враги (тактика, ранги, парирование) и правила дефицита. */
public class CombatModule implements RegnumModule {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "regnum");

    static {
        LOOT_MODIFIERS.register("scarcity", () -> ScarcityModifier.CODEC);
    }

    @Override
    public String id() {
        return "combat";
    }

    @Override
    public String title() {
        return "Бой и сложность";
    }

    @Override
    public void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(Tactics.class);
        NeoForge.EVENT_BUS.register(PlayerParry.class);
        NeoForge.EVENT_BUS.register(Posture.class);
        NeoForge.EVENT_BUS.register(Combo.class);
        LOOT_MODIFIERS.register(modBus);
    }
}
