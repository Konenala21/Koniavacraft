package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.common.entity.FloatingTurretProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class FloatingTurretProjectileRenderer extends EntityRenderer<FloatingTurretProjectile> {

    public FloatingTurretProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(FloatingTurretProjectile entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Billboard：永遠正對鏡頭
        poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());

        float ratio  = entity.getChargeRatio();
        float scale  = 1.0F + ratio * 1.5F; // 普通：1x，蓄力滿：2.5x

        // 顏色：普通藍 → 蓄力藍白（加入暖色偏移）
        int r = (int) Math.min(255, 30  + ratio * 200);
        int g = (int) Math.min(255, 140 + ratio * 100);
        int b = 255;

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());

        // 外層光暈
        addQuad(vc, pose, 0.40F * scale, r, g, b, 55);
        // 中層
        addQuad(vc, pose, 0.24F * scale, Math.min(255, r + 50), Math.min(255, g + 45), b, 130);
        // 亮核心
        addQuad(vc, pose, 0.11F * scale, 210, 240, 255, 235);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void addQuad(VertexConsumer vc, Matrix4f pose,
                                 float size, int r, int g, int b, int a) {
        vc.addVertex(pose, -size, -size, 0).setColor(r, g, b, a);
        vc.addVertex(pose,  size, -size, 0).setColor(r, g, b, a);
        vc.addVertex(pose,  size,  size, 0).setColor(r, g, b, a);
        vc.addVertex(pose, -size,  size, 0).setColor(r, g, b, a);
    }

    @Override
    public ResourceLocation getTextureLocation(FloatingTurretProjectile entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}
