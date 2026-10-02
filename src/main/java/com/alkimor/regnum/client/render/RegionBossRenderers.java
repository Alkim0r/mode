package com.alkimor.regnum.client.render;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.client.model.ForgemasterModel;
import com.alkimor.regnum.client.model.MireMotherModel;
import com.alkimor.regnum.client.model.ModelIndex;
import com.alkimor.regnum.client.model.ScarabQueenModel;
import com.alkimor.regnum.dungeon.boss.ForgemasterEntity;
import com.alkimor.regnum.dungeon.boss.MireMotherEntity;
import com.alkimor.regnum.dungeon.boss.ScarabQueenEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Рендеры региональных боссов на собственных моделях из tools/modelgen. */
public final class RegionBossRenderers {
    private RegionBossRenderers() {}

    /** Матушка Топь — сгорбленная болотная ведьма с ветвями-рогами и фонарём. */
    public static class MireMother extends BossRenderer<MireMotherEntity> {
        public MireMother(EntityRendererProvider.Context ctx) {
            super(ctx, MireMotherModel.LAYER, "mire_mother", 1.6f, 0.9f,
                    new RegionalHumanoidAnimatedModel<>(ctx.bakeLayer(MireMotherModel.LAYER), MireMotherModel.LAYER, false));
        }
    }

    /** Сехмет-ра — мумия-царица с крыльями скарабея. */
    public static class ScarabQueen extends BossRenderer<ScarabQueenEntity> {
        public ScarabQueen(EntityRendererProvider.Context ctx) {
            super(ctx, ScarabQueenModel.LAYER, "scarab_queen", 1.45f, 0.8f,
                    new RegionalHumanoidAnimatedModel<>(ctx.bakeLayer(ScarabQueenModel.LAYER), ScarabQueenModel.LAYER, true));
        }
    }

    /** Горновой — кузнечный голем с горном в груди, трубами и молотом. */
    public static class Forgemaster extends MobRenderer<ForgemasterEntity, IronGolemModel<ForgemasterEntity>> {
        private static final ResourceLocation TEX = Regnum.id("textures/entity/forgemaster.png");
        private final List<ModelPart> decor;

        public Forgemaster(EntityRendererProvider.Context ctx) {
            super(ctx, new ForgemasterAnimatedModel(ctx.bakeLayer(ForgemasterModel.LAYER)), BossVisualScale.enlarged(1.2f));
            this.decor = ModelIndex.decor(ForgemasterModel.LAYER, model.root());
            addLayer(new GlowLayer<>(this, Regnum.id("textures/entity/forgemaster_glow.png")));
        }

        @Override
        public void render(ForgemasterEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
            boolean show = UnitRenderer.showDecor(e);
            for (ModelPart p : decor) p.visible = show;
            super.render(e, yaw, partialTick, pose, buffer, light);
        }

        @Override
        protected void scale(ForgemasterEntity e, PoseStack pose, float pt) {
            float size = BossVisualScale.enlarged(1.35F);
            pose.scale(size, size, size);
        }

        @Override
        public ResourceLocation getTextureLocation(ForgemasterEntity e) {
            return TEX;
        }
    }
}
