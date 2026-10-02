package com.alkimor.regnum.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

import java.util.function.ToIntFunction;

/** Человекоподобный NPC на модели игрока со стандартными скинами и бронёй. */
public class NpcRenderer<T extends Mob> extends HumanoidMobRenderer<T, PlayerModel<T>> {
    private final ToIntFunction<T> skin;
    private final ResourceLocation[] textures;

    public NpcRenderer(EntityRendererProvider.Context ctx, ToIntFunction<T> skin, String[] skins) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.skin = skin;
        this.textures = new ResourceLocation[skins.length];
        for (int i = 0; i < skins.length; i++) {
            textures[i] = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/" + skins[i] + ".png");
        }
        addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
    }

    @Override
    protected void scale(T entity, com.mojang.blaze3d.vertex.PoseStack pose, float partialTick) {
        if (entity instanceof com.alkimor.regnum.wanderers.WandererEntity w && w.isChild()) pose.scale(0.55f, 0.55f, 0.55f);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return textures[Math.floorMod(skin.applyAsInt(entity), textures.length)];
    }
}
