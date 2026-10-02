package com.alkimor.regnum.core.network;

import com.alkimor.regnum.client.ClientAccess;
import com.alkimor.regnum.kingdom.ArmyCommands;
import com.alkimor.regnum.kingdom.CityActions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Регистрация всех пакетов. Клиентские обработчики вызывают {@link ClientAccess}
 * только внутри лямбды — на выделенном сервере этот класс не загружается.
 */
public final class RegnumNetwork {
    private RegnumNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("2");

        r.playToClient(SkillSyncPayload.TYPE, SkillSyncPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> ClientAccess.onSkillSync(p)));
        r.playToClient(CityInfoPayload.TYPE, CityInfoPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> ClientAccess.onCityInfo(p)));
        r.playToClient(ArmyInfoPayload.TYPE, ArmyInfoPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> ClientAccess.onArmyInfo(p)));
        r.playToClient(TradeInfoPayload.TYPE, TradeInfoPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> ClientAccess.onTradeInfo(p)));
        r.playToServer(TradeActionPayload.TYPE, TradeActionPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> {
                    if (asServer(ctx) instanceof ServerPlayer sp) com.alkimor.regnum.trade.Trading.handle(sp, p);
                }));

        r.playToClient(QuestSnapshotPayload.TYPE, QuestSnapshotPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> com.alkimor.regnum.client.StoryClient.onSnapshot(p)));
        r.playToClient(CutscenePayload.TYPE, CutscenePayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> com.alkimor.regnum.client.StoryClient.onCutscene(p)));
        r.playToClient(ScienceInfoPayload.TYPE, ScienceInfoPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> ClientAccess.onScienceInfo(p)));
        r.playToClient(CommandersPayload.TYPE, CommandersPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> ClientAccess.onCommanders(p)));
        r.playToServer(ScienceActionPayload.TYPE, ScienceActionPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> {
                    if (asServer(ctx) instanceof ServerPlayer sp) com.alkimor.regnum.kingdom.Science.handleAction(sp, p);
                }));

        r.playToServer(CityActionPayload.TYPE, CityActionPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> {
                    if (asServer(ctx) instanceof ServerPlayer sp) CityActions.handle(sp, p);
                }));
        r.playToServer(HeroActionPayload.TYPE, HeroActionPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> {
                    if (asServer(ctx) instanceof ServerPlayer sp) com.alkimor.regnum.survival.HeroActions.handle(sp, p);
                }));
        r.playToServer(ArmyOrderPayload.TYPE, ArmyOrderPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> {
                    if (asServer(ctx) instanceof ServerPlayer sp) ArmyCommands.handle(sp, p);
                }));
    }

    private static Player asServer(IPayloadContext ctx) {
        return ctx.player();
    }
}
