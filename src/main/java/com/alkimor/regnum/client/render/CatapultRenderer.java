package com.alkimor.regnum.client.render;

import com.alkimor.regnum.kingdom.CatapultEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Катапульта, собранная из блоков: рама, колёса, стойки и качающееся плечо с ковшом. */
public class CatapultRenderer extends EntityRenderer<CatapultEntity> {
    private static final BlockState PLANKS = Blocks.SPRUCE_PLANKS.defaultBlockState();
    private static final BlockState DARK = Blocks.DARK_OAK_PLANKS.defaultBlockState();
    private static final BlockState WHEEL = Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
    private static final BlockState ARM = Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
    private static final BlockState AXLE = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
    private static final BlockState BUCKET = Blocks.BARREL.defaultBlockState();
    private static final BlockState WEIGHT = Blocks.COBBLESTONE.defaultBlockState();
    private static final BlockState ROCK = Blocks.COBBLESTONE.defaultBlockState();

    public CatapultRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 1.4f;
    }

    private static void box(PoseStack ps, MultiBufferSource buf, BlockState st, int light,
                            double x, double y, double z, double sx, double sy, double sz) {
        ps.pushPose();
        ps.translate(x, y, z);
        ps.scale((float) sx, (float) sy, (float) sz);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(st, ps, buf, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    /** Угол плеча над горизонталью: взведено вниз, быстрый бросок вверх, медленный возврат. */
    private static float lift(float t) {
        float cocked = -42f, fired = 78f;
        if (t >= 40f) return cocked;
        if (t < 8f) {
            float k = t / 8f;
            return cocked + (fired - cocked) * (1f - (1f - k) * (1f - k));
        }
        if (t < 14f) return fired;
        float k = (t - 14f) / 26f;
        return fired + (cocked - fired) * k * k * (3 - 2 * k);
    }

    @Override
    public void render(CatapultEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        float rot = e.yBodyRotO + (e.yBodyRot - e.yBodyRotO) * partial;
        ps.mulPose(Axis.YP.rotationDegrees(-rot));
        // рама и колёса (локально +Z — вперёд, к цели)
        box(ps, buf, PLANKS, light, -0.8, 0.55, -1.4, 1.6, 0.25, 2.8);
        box(ps, buf, DARK, light, -0.8, 0.8, -1.4, 0.2, 0.2, 2.8);
        box(ps, buf, DARK, light, 0.6, 0.8, -1.4, 0.2, 0.2, 2.8);
        for (int sx : new int[]{-1, 1})
            for (int sz : new int[]{-1, 1})
                box(ps, buf, WHEEL, light, sx > 0 ? 0.8 : -1.1, 0.0, sz * 0.9 - 0.4, 0.3, 0.8, 0.8);
        // стойки и ось плеча
        box(ps, buf, PLANKS, light, -0.65, 0.8, -0.15, 0.2, 1.35, 0.3);
        box(ps, buf, PLANKS, light, 0.45, 0.8, -0.15, 0.2, 1.35, 0.3);
        box(ps, buf, AXLE, light, -0.7, 2.05, -0.1, 1.4, 0.2, 0.2);
        // плечо вращается вокруг оси
        float t = e.fireAnim >= 99 ? 99 : e.fireAnim + partial;
        ps.pushPose();
        ps.translate(0, 2.15, 0);
        ps.mulPose(Axis.XP.rotationDegrees(-lift(t)));
        box(ps, buf, ARM, light, -0.1, -0.1, -0.7, 0.2, 0.2, 2.6);
        box(ps, buf, WEIGHT, light, -0.35, -0.7, -0.95, 0.7, 0.7, 0.7);
        box(ps, buf, BUCKET, light, -0.3, -0.25, 1.65, 0.6, 0.45, 0.6);
        if (t >= 8f && t < 99f || t >= 99f) {
            // заряженное ядро в ковше (после броска плечо пустое первые тики)
            if (t >= 99f || t > 20f) box(ps, buf, ROCK, light, -0.2, 0.15, 1.75, 0.4, 0.4, 0.4);
        }
        ps.popPose();
        ps.popPose();
        super.render(e, yaw, partial, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(CatapultEntity e) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
