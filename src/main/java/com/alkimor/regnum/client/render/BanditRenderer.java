package com.alkimor.regnum.client.render;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.client.model.ModelIndex;
import com.alkimor.regnum.kingdom.BanditEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Разбойники: головорез, лучник и атаман — свои модели. */
public class BanditRenderer extends MobRenderer<BanditEntity, HumanoidModel<BanditEntity>> {
    private final RegnumHumanoidModel<?>[] models;
    private final ResourceLocation[][] textures;

    public BanditRenderer(EntityRendererProvider.Context ctx) {
        this(ctx, bake(ctx));
    }

    @SuppressWarnings("unchecked")
    private BanditRenderer(EntityRendererProvider.Context ctx, RegnumHumanoidModel<?>[] models) {
        super(ctx, (HumanoidModel<BanditEntity>) models[0], 0.5f);
        this.models = models;
        textures = new ResourceLocation[models.length][3];
        for (int k = 0; k < models.length; k++)
            for (int v = 0; v < 3; v++)
                textures[k][v] = Regnum.id("textures/entity/bandit_" + ModelIndex.BANDIT_IDS[k] + "_" + v + ".png");
        addLayer(new UnitItemLayer<>(this, ctx.getItemInHandRenderer()));
    }

    private static RegnumHumanoidModel<?>[] bake(EntityRendererProvider.Context ctx) {
        RegnumHumanoidModel<?>[] m = new RegnumHumanoidModel<?>[ModelIndex.BANDITS.length];
        for (int k = 0; k < m.length; k++) m[k] = new RegnumHumanoidModel<BanditEntity>(ctx.bakeLayer(ModelIndex.BANDITS[k]), ModelIndex.BANDITS[k]);
        return m;
    }

    private int kind(BanditEntity e) {
        return Math.floorMod(e.getVariant(), models.length);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void render(BanditEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        RegnumHumanoidModel<BanditEntity> m = (RegnumHumanoidModel<BanditEntity>) models[kind(e)];
        this.model = m;
        m.showDecor(UnitRenderer.showDecor(e));
        super.render(e, yaw, partialTick, pose, buffer, light);
    }

    @Override
    protected void scale(BanditEntity e, PoseStack pose, float partialTick) {
        if (kind(e) == BanditEntity.CAPTAIN) pose.scale(1.08f, 1.08f, 1.08f);
    }

    @Override
    public ResourceLocation getTextureLocation(BanditEntity e) {
        return textures[kind(e)][Math.floorMod(e.getSkinIndex(), 3)];
    }
}
