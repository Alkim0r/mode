package com.alkimor.regnum.client.render;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.client.model.ModelIndex;
import com.alkimor.regnum.core.RegnumClientConfig;
import com.alkimor.regnum.kingdom.SoldierEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Войска королевства: своя модель для каждой пары «культура × род войск», 3 варианта лица. */
public class UnitRenderer extends MobRenderer<SoldierEntity, HumanoidModel<SoldierEntity>> {
    private final RegnumHumanoidModel<?>[][] models;
    private final ResourceLocation[][][] textures;

    public UnitRenderer(EntityRendererProvider.Context ctx) {
        this(ctx, bake(ctx));
    }

    @SuppressWarnings("unchecked")
    private UnitRenderer(EntityRendererProvider.Context ctx, RegnumHumanoidModel<?>[][] models) {
        super(ctx, (HumanoidModel<SoldierEntity>) models[0][1], 0.5f);
        this.models = models;
        int nc = ModelIndex.CULTURE_IDS.length, nt = ModelIndex.TYPE_IDS.length;
        textures = new ResourceLocation[nc][nt][3];
        for (int c = 0; c < nc; c++)
            for (int t = 0; t < nt; t++)
                for (int v = 0; v < 3; v++)
                    textures[c][t][v] = Regnum.id("textures/entity/unit_" + ModelIndex.CULTURE_IDS[c] + "_" + ModelIndex.TYPE_IDS[t] + "_" + v + ".png");
        addLayer(new UnitItemLayer<>(this, ctx.getItemInHandRenderer()));
    }

    private static RegnumHumanoidModel<?>[][] bake(EntityRendererProvider.Context ctx) {
        int nc = ModelIndex.UNITS.length, nt = ModelIndex.UNITS[0].length;
        RegnumHumanoidModel<?>[][] m = new RegnumHumanoidModel<?>[nc][nt];
        for (int c = 0; c < nc; c++)
            for (int t = 0; t < nt; t++)
                m[c][t] = new RegnumHumanoidModel<SoldierEntity>(ctx.bakeLayer(ModelIndex.UNITS[c][t]), ModelIndex.UNITS[c][t]);
        return m;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void render(SoldierEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        int c = e.getCulture().ordinal(), t = ModelIndex.typeIndex(e.getSoldierType().name());
        RegnumHumanoidModel<SoldierEntity> m = (RegnumHumanoidModel<SoldierEntity>) models[c][t];
        this.model = m;
        m.showDecor(showDecor(e));
        super.render(e, yaw, partialTick, pose, buffer, light);
    }

    static boolean showDecor(net.minecraft.world.entity.Entity e) {
        if (RegnumClientConfig.decorHidden()) return false;
        var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        int d = RegnumClientConfig.NPC_DETAIL_DISTANCE.get();
        return e.distanceToSqr(cam) <= (double) d * d;
    }

    @Override
    public ResourceLocation getTextureLocation(SoldierEntity e) {
        return textures[e.getCulture().ordinal()][ModelIndex.typeIndex(e.getSoldierType().name())][Math.floorMod(e.getSkinIndex(), 3)];
    }
}
