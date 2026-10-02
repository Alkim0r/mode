package com.alkimor.regnum.client.render;

import com.alkimor.regnum.kingdom.SiegeTowerEntity;
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

/** Осадная башня из блоков: четыре столба, три яруса, обшивка спереди и откидной мост. */
public class SiegeTowerRenderer extends EntityRenderer<SiegeTowerEntity> {
    private static final BlockState LOG = Blocks.DARK_OAK_LOG.defaultBlockState();
    private static final BlockState PLANKS = Blocks.SPRUCE_PLANKS.defaultBlockState();
    private static final BlockState DARK = Blocks.DARK_OAK_PLANKS.defaultBlockState();
    private static final BlockState WHEEL = Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
    private static final BlockState HIDE = Blocks.BROWN_WOOL.defaultBlockState();

    public SiegeTowerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 2.0f;
    }

    private static void box(PoseStack ps, MultiBufferSource buf, BlockState st, int light,
                            double x, double y, double z, double sx, double sy, double sz) {
        ps.pushPose();
        ps.translate(x, y, z);
        ps.scale((float) sx, (float) sy, (float) sz);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(st, ps, buf, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    @Override
    public void render(SiegeTowerEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        float rot = e.yBodyRotO + (e.yBodyRot - e.yBodyRotO) * partial;
        ps.mulPose(Axis.YP.rotationDegrees(-rot));
        // колёса и основание
        for (int sx : new int[]{-1, 1})
            for (int sz : new int[]{-1, 1})
                box(ps, buf, WHEEL, light, sx > 0 ? 1.0 : -1.5, 0.0, sz * 0.9 - 0.4, 0.5, 1.0, 1.0);
        box(ps, buf, DARK, light, -1.4, 0.8, -1.4, 2.8, 0.3, 2.8);
        // четыре столба
        for (int sx : new int[]{-1, 1})
            for (int sz : new int[]{-1, 1})
                box(ps, buf, LOG, light, sx > 0 ? 1.0 : -1.4, 1.1, sz > 0 ? 1.0 : -1.4, 0.4, 7.4, 0.4);
        // ярусные настилы
        for (double y : new double[]{3.6, 6.2, 8.1}) box(ps, buf, PLANKS, light, -1.4, y, -1.4, 2.8, 0.2, 2.8);
        // обшивка: перед (+Z) и бока, на нижних двух ярусах
        box(ps, buf, HIDE, light, -1.4, 1.1, 1.0, 2.8, 5.1, 0.12);
        box(ps, buf, DARK, light, -1.4, 1.1, -1.4, 0.12, 5.1, 2.8);
        box(ps, buf, DARK, light, 1.28, 1.1, -1.4, 0.12, 5.1, 2.8);
        box(ps, buf, DARK, light, -1.4, 1.1, -1.4, 2.8, 5.1, 0.12);
        // бойницы-зубцы по верху
        for (int i = 0; i < 4; i++) box(ps, buf, LOG, light, -1.4 + i * 0.8, 8.3, 1.2, 0.4, 0.5, 0.2);
        // мост
        if (e.bridgeDown) box(ps, buf, PLANKS, light, -0.9, 8.1, 1.4, 1.8, 0.15, 3.6);
        else box(ps, buf, PLANKS, light, -0.9, 8.3, 1.2, 1.8, 2.4, 0.15);
        ps.popPose();
        super.render(e, yaw, partial, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SiegeTowerEntity e) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
