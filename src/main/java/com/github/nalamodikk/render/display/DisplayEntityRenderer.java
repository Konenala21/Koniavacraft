package com.github.nalamodikk.render.display;

import com.github.nalamodikk.display.DisplayEntity;
import com.github.nalamodikk.display.DisplayEntityManager;
import com.github.nalamodikk.display.impl.MagicCircleDisplayEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * DisplayEntity 渲染器
 * 負責實際繪製虛擬實體
 */
public class DisplayEntityRenderer {

    private static final ResourceLocation MAGIC_CIRCLE_TEXTURE = ResourceLocation.fromNamespaceAndPath("koniavacraft",
            "textures/entity/magic_circle.png");

    public static void renderAll(PoseStack poseStack, MultiBufferSource bufferSource, float partialTick) {
        for (DisplayEntity entity : DisplayEntityManager.getAllEntities()) {
            if (!entity.isRemoved()) {
                if (entity instanceof MagicCircleDisplayEntity circle) {
                    renderMagicCircle(circle, poseStack, bufferSource, partialTick);
                }
            }
        }
    }

    private static void renderMagicCircle(MagicCircleDisplayEntity circle, PoseStack poseStack,
            MultiBufferSource bufferSource, float partialTick) {
        poseStack.pushPose();

        // 獲取插值後的位置
        Vec3 pos = circle.getLerpPos(partialTick);
        poseStack.translate(pos.x, pos.y, pos.z);

        // 旋轉
        poseStack.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(circle.getYaw())));

        // 繪製魔法陣（簡化版：使用四邊形）
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(MAGIC_CIRCLE_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();

        float radius = circle.getRadius();
        int color = circle.getColor();
        float alpha = circle.getAlpha();

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        // 繪製四個頂點
        consumer.addVertex(matrix, -radius, 0, -radius).setColor(r, g, b, alpha).setUv(0, 0).setLight(240).setNormal(0,
                1, 0);
        consumer.addVertex(matrix, -radius, 0, radius).setColor(r, g, b, alpha).setUv(0, 1).setLight(240).setNormal(0,
                1, 0);
        consumer.addVertex(matrix, radius, 0, radius).setColor(r, g, b, alpha).setUv(1, 1).setLight(240).setNormal(0, 1,
                0);
        consumer.addVertex(matrix, radius, 0, -radius).setColor(r, g, b, alpha).setUv(1, 0).setLight(240).setNormal(0,
                1, 0);

        poseStack.popPose();
    }
}
