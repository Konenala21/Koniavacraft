package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.entity.NaraPhantomEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class NaraPhantomRenderer extends HumanoidMobRenderer<NaraPhantomEntity, PlayerModel<NaraPhantomEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/entity/nara/konenala21.png");

    public NaraPhantomRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(NaraPhantomEntity entity) {
        return TEXTURE;
    }
}
