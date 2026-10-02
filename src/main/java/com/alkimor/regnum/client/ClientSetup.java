package com.alkimor.regnum.client;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.client.render.CryptLordRenderer;
import com.alkimor.regnum.client.render.NpcRenderer;
import com.alkimor.regnum.dungeon.DungeonModule;
import com.alkimor.regnum.kingdom.KingdomModule;
import com.alkimor.regnum.wanderers.WandererEntity;
import com.alkimor.regnum.wanderers.WanderersModule;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Регистрация клиентских рендеров. */
@EventBusSubscriber(modid = Regnum.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    private static final String[] WANDERER_SKINS = {"ari", "sunny", "noor", "makena", "alex"};

    /** Встроенный ресурспак реалистичной природы: включён всегда, кроме облегчённого режима (тогда ваниль). */
    @SubscribeEvent
    public static void addPacks(net.neoforged.neoforge.event.AddPackFindersEvent event) {
        if (event.getPackType() != net.minecraft.server.packs.PackType.CLIENT_RESOURCES) return;
        if (com.alkimor.regnum.core.RegnumClientConfig.LITE_MODE.get()) return;
        var file = net.neoforged.fml.ModList.get().getModFileById(Regnum.MODID).getFile();
        java.nio.file.Path root = file.findResource("resourcepacks", "natural");
        var info = new net.minecraft.server.packs.PackLocationInfo("regnum_natural",
                net.minecraft.network.chat.Component.literal("Regnum: реалистичная природа"),
                net.minecraft.server.packs.repository.PackSource.BUILT_IN, java.util.Optional.empty());
        var pack = net.minecraft.server.packs.repository.Pack.readMetaAndCreate(info,
                new net.minecraft.server.packs.PathPackResources.PathResourcesSupplier(root), net.minecraft.server.packs.PackType.CLIENT_RESOURCES,
                new net.minecraft.server.packs.PackSelectionConfig(true, net.minecraft.server.packs.repository.Pack.Position.TOP, false));
        if (pack != null) event.addRepositorySource(consumer -> consumer.accept(pack));
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        com.alkimor.regnum.client.model.ModelIndex.register(event);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(KingdomModule.SOLDIER.get(), com.alkimor.regnum.client.render.UnitRenderer::new);
        event.registerEntityRenderer(KingdomModule.BANDIT.get(), com.alkimor.regnum.client.render.BanditRenderer::new);
        event.registerEntityRenderer(KingdomModule.CARAVAN.get(), ctx -> new NpcRenderer<com.alkimor.regnum.kingdom.CaravanEntity>(ctx, e -> 3, WANDERER_SKINS));
        event.registerEntityRenderer(KingdomModule.CATAPULT.get(), com.alkimor.regnum.client.render.CatapultRenderer::new);
        event.registerEntityRenderer(KingdomModule.SIEGE_TOWER.get(), com.alkimor.regnum.client.render.SiegeTowerRenderer::new);
        event.registerEntityRenderer(KingdomModule.SIEGE_BOULDER.get(), ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 2.2f, true));
        event.registerEntityRenderer(WanderersModule.WANDERER.get(),
                ctx -> new NpcRenderer<WandererEntity>(ctx, WandererEntity::getSkinIndex, WANDERER_SKINS));
        event.registerEntityRenderer(DungeonModule.CRYPT_LORD.get(), CryptLordRenderer::new);
        event.registerEntityRenderer(com.alkimor.regnum.survival.kit.KitModule.THROWN_WEAPON.get(),
                ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 1.25f, false));
        event.registerEntityRenderer(com.alkimor.regnum.survival.kit.KitModule.SMOKE_BOMB_ENTITY.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(com.alkimor.regnum.survival.kit.KitModule.WILDFIRE_FLASK_ENTITY.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(com.alkimor.regnum.dungeon.RegionsModule.MIRE_MOTHER.get(),
                com.alkimor.regnum.client.render.RegionBossRenderers.MireMother::new);
        event.registerEntityRenderer(com.alkimor.regnum.dungeon.RegionsModule.FORGEMASTER.get(),
                com.alkimor.regnum.client.render.RegionBossRenderers.Forgemaster::new);
        event.registerEntityRenderer(com.alkimor.regnum.dungeon.RegionsModule.SCARAB_QUEEN.get(),
                com.alkimor.regnum.client.render.RegionBossRenderers.ScarabQueen::new);
        event.registerEntityRenderer(com.alkimor.regnum.mine.MineModule.CRAWLER.get(),
                com.alkimor.regnum.client.render.MineCreatureRenderers.Crawler::new);
        event.registerEntityRenderer(com.alkimor.regnum.mine.MineModule.CRAWLER_QUEEN.get(),
                com.alkimor.regnum.client.render.MineCreatureRenderers.Queen::new);
    }
}
