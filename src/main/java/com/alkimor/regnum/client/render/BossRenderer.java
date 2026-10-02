package com.alkimor.regnum.client.render;

import com.alkimor.regnum.Regnum;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Человекоподобный босс со своей моделью, светящимся слоем и масштабом. */
public class BossRenderer<T extends Mob> extends MobRenderer<T, RegnumHumanoidModel<T>> {
    private final ResourceLocation texture;
    private final float size;

    public BossRenderer(EntityRendererProvider.Context ctx, ModelLayerLocation layer, String name, float size, float shadow) {
        this(ctx, layer, name, size, shadow, new RegnumHumanoidModel<>(ctx.bakeLayer(layer), layer));
    }

    protected BossRenderer(EntityRendererProvider.Context ctx, ModelLayerLocation layer, String name, float size,
                           float shadow, RegnumHumanoidModel<T> animatedModel) {
        super(ctx, animatedModel, BossVisualScale.enlarged(shadow));
        this.texture = Regnum.id("textures/entity/" + name + ".png");
        this.size = BossVisualScale.enlarged(size);
        addLayer(new GlowLayer<>(this, Regnum.id("textures/entity/" + name + "_glow.png")));
        addLayer(new UnitItemLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public void render(T e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        model.showDecor(UnitRenderer.showDecor(e));
        super.render(e, yaw, partialTick, pose, buffer, light);
    }

    @Override
    protected void scale(T e, PoseStack pose, float partialTick) {
        pose.scale(size, size, size);
    }

    @Override
    public ResourceLocation getTextureLocation(T e) {
        return texture;
    }
}
