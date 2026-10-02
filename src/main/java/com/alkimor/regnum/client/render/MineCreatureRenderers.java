package com.alkimor.regnum.client.render;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.client.model.CrawlerModel;
import com.alkimor.regnum.client.model.CrawlerQueenModel;
import com.alkimor.regnum.mine.CrawlerEntity;
import com.alkimor.regnum.mine.CrawlerQueenEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Отдельные читаемые силуэты шахтных существ вместо унаследованной модели ванильного паука. */
public final class MineCreatureRenderers {
    private MineCreatureRenderers() {}

    public static final class Crawler extends MobRenderer<CrawlerEntity, CrawlerModel> {
        private static final ResourceLocation TEXTURE = Regnum.id("textures/entity/crawler.png");

        public Crawler(EntityRendererProvider.Context context) {
            super(context, new CrawlerModel(context.bakeLayer(CrawlerModel.LAYER)), 0.38F);
            addLayer(new GlowLayer<>(this, Regnum.id("textures/entity/crawler_glow.png")));
        }

        @Override
        public ResourceLocation getTextureLocation(CrawlerEntity entity) {
            return TEXTURE;
        }
    }

    public static final class Queen extends MobRenderer<CrawlerQueenEntity, CrawlerQueenModel> {
        private static final ResourceLocation TEXTURE = Regnum.id("textures/entity/crawler_queen.png");

        public Queen(EntityRendererProvider.Context context) {
            super(context, new CrawlerQueenModel(context.bakeLayer(CrawlerQueenModel.LAYER)), BossVisualScale.enlarged(0.72F));
            addLayer(new GlowLayer<>(this, Regnum.id("textures/entity/crawler_queen_glow.png")));
        }

        @Override
        protected void scale(CrawlerQueenEntity entity, PoseStack pose, float partialTick) {
            float size = BossVisualScale.enlarged(1.5F);
            pose.scale(size, size, size);
        }

        @Override
        public ResourceLocation getTextureLocation(CrawlerQueenEntity entity) {
            return TEXTURE;
        }
    }
}
