package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.entity.NaraPhantomEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class NaraPhantomRenderer extends HumanoidMobRenderer<NaraPhantomEntity, PlayerModel<NaraPhantomEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/entity/nara/konenala21.png");
    // 幻影：25% 透明（75% 可見），讓玩家一看就知道不是本體
    private static final float PHANTOM_ALPHA = 0.75F;

    public NaraPhantomRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(NaraPhantomEntity entity) {
        return TEXTURE;
    }

    @Override
    protected RenderType getRenderType(NaraPhantomEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        return RenderType.entityTranslucent(getTextureLocation(entity)); // 支援 alpha 混合
    }

    @Override
    public void render(NaraPhantomEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 把所有頂點的 alpha 乘上 PHANTOM_ALPHA，使整個幻影半透明
        MultiBufferSource faded = type -> new AlphaConsumer(buffer.getBuffer(type), PHANTOM_ALPHA);
        super.render(entity, entityYaw, partialTick, poseStack, faded, packedLight);
    }

    private record AlphaConsumer(VertexConsumer delegate, float alpha) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int a) {
            delegate.setColor(red, green, blue, (int) (a * alpha));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            delegate.setNormal(normalX, normalY, normalZ);
            return this;
        }
    }
}
