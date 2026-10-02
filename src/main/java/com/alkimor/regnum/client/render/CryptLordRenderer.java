package com.alkimor.regnum.client.render;

import com.alkimor.regnum.client.model.CryptLordModel;
import com.alkimor.regnum.dungeon.CryptLordEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Морграт, Костяной Владыка: своя модель (рога, корона, рёбра с кристаллом, рваная мантия). */
public class CryptLordRenderer extends BossRenderer<CryptLordEntity> {
    public CryptLordRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, CryptLordModel.LAYER, "crypt_lord", 1.45f, 0.9f,
                new CryptLordAnimatedModel(ctx.bakeLayer(CryptLordModel.LAYER)));
    }
}
