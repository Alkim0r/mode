package com.alkimor.regnum.dynasty;

import com.alkimor.regnum.core.RegnumModule;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.function.Supplier;

import static com.alkimor.regnum.core.ModRegistries.ATTACHMENTS;

/** Модуль «Династия»: брак, дети, наследники, продолжение рода. */
public class DynastyModule implements RegnumModule {

    public static final Supplier<AttachmentType<DynastyData>> DYNASTY = ATTACHMENTS.register("dynasty",
            () -> AttachmentType.builder(() -> new DynastyData()).serialize(DynastyData.CODEC).copyOnDeath().build());

    @Override
    public String id() {
        return "dynasty";
    }

    @Override
    public String title() {
        return "Династия";
    }

    @Override
    public void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(DynastyModule::onClone);
        NeoForge.EVENT_BUS.addListener(DynastyModule::onServerTick);
    }

    private static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer p && event.getOriginal() instanceof ServerPlayer orig) {
            Dynasty.onRespawn(p, orig);
        }
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 1200 != 0) return;
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) Dynasty.tickBirths(p);
    }
}
