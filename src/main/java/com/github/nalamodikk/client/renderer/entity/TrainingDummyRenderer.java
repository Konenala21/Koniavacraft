package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.entity.TrainingDummyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 訓練假人渲染器。貼圖是程式生成的仙人掌綠 placeholder（均勻圖案，容忍目前重疊的 UV），之後可換正式美術。
 */
public class TrainingDummyRenderer extends MobRenderer<TrainingDummyEntity, TrainingDummyModel<TrainingDummyEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/entity/training_dummy.png");

    public TrainingDummyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new TrainingDummyModel<>(ctx.bakeLayer(TrainingDummyModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(TrainingDummyEntity entity) {
        return TEXTURE;
    }
}
