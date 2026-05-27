package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.common.entity.SpaceCrackEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class SpaceCrackRenderer extends EntityRenderer<SpaceCrackEntity> {

    private static final float HALF_HEIGHT = 1.25f;
    private static final int STRIPS = 18;

    public SpaceCrackRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(SpaceCrackEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0, 1.1, 0.0);
        // 只繞 Y 軸正對相機（直立的撕裂，不再貼著視角俯仰/打滾）
        var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        double dxCam = cam.getPosition().x - entity.getX();
        double dzCam = cam.getPosition().z - entity.getZ();
        float yawToCam = (float) (Math.atan2(dxCam, dzCam) * (180.0 / Math.PI));
        poseStack.mulPose(Axis.YP.rotationDegrees(yawToCam));

        float t = entity.tickCount + partialTick;
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());

        // 外層幽光（寬透鏡，淡紫）
        drawRift(vc, pose, 0.45f, 0.0f, t, 100, 70, 150, 45);
        // 中層
        drawRift(vc, pose, 0.22f, 0.04f, t, 160, 120, 215, 110);
        // 中央細鋸齒亮線（這條才是「裂縫」感）
        drawRift(vc, pose, 0.05f, 0.09f, t, 235, 225, 255, 240);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    // 透鏡狀（中間寬、上下尖）+ 中央鋸齒位移，畫成一道撕裂縫
    private static void drawRift(VertexConsumer vc, Matrix4f pose, float maxHalfWidth, float zigAmp,
                                  float t, int r, int g, int b, int a) {
        for (int i = 0; i < STRIPS; i++) {
            float v0 = -HALF_HEIGHT + 2f * HALF_HEIGHT * i / STRIPS;
            float v1 = -HALF_HEIGHT + 2f * HALF_HEIGHT * (i + 1) / STRIPS;
            float hw0 = halfWidth(v0, maxHalfWidth, t);
            float hw1 = halfWidth(v1, maxHalfWidth, t);
            float c0 = zigAmp * (float) Math.sin(v0 * 7.0 + t * 0.12);
            float c1 = zigAmp * (float) Math.sin(v1 * 7.0 + t * 0.12);
            vc.addVertex(pose, c0 - hw0, v0, 0).setColor(r, g, b, a);
            vc.addVertex(pose, c0 + hw0, v0, 0).setColor(r, g, b, a);
            vc.addVertex(pose, c1 + hw1, v1, 0).setColor(r, g, b, a);
            vc.addVertex(pose, c1 - hw1, v1, 0).setColor(r, g, b, a);
        }
    }

    private static float halfWidth(float v, float maxHalfWidth, float t) {
        float n = v / HALF_HEIGHT;                       // -1..1
        float lens = (float) Math.pow(Math.max(0.0, 1.0 - n * n), 0.7); // 上下尖、中間寬
        float jag = 0.7f + 0.3f * (float) Math.sin(v * 15.0 + t * 0.18); // 邊緣鋸齒抖動
        return maxHalfWidth * lens * jag;
    }

    @Override
    public ResourceLocation getTextureLocation(SpaceCrackEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}
