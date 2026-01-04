package com.github.nalamodikk.experimental.render.effects;

import com.github.nalamodikk.KoniavacraftMod;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 魔法陣渲染器
 * 
 * 負責渲染旋轉的符文魔法陣，支援：
 * - 基礎圓形渲染
 * - 符文紋理渲染
 * - 發光效果
 * - 旋轉動畫
 * - 淡入淡出效果
 */
@OnlyIn(Dist.CLIENT)
public class MagicCircleRenderer {
    
    // 預設紋理
    private static final ResourceLocation DEFAULT_CIRCLE_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/effects/magic_circle_default.png");
    private static final ResourceLocation RUNE_CIRCLE_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/effects/magic_circle_runes.png");
    
    // 渲染參數
    private static final int CIRCLE_SEGMENTS = 32; // 圓形分段數
    private static final float MAX_RENDER_DISTANCE = 64.0f; // 最大渲染距離
    
    /**
     * 渲染魔法陣
     * 
     * @param poseStack 姿態矩陣堆疊
     * @param bufferSource 緩衝區來源
     * @param position 世界位置
     * @param rotation 旋轉角度（弧度）
     * @param size 大小倍數
     * @param color ARGB 顏色
     * @param alpha 透明度 (0.0-1.0)
     * @param glowEffect 是否發光
     * @param runeTexture 符文紋理（null 則使用預設）
     * @param partialTicks 部分 tick 時間
     */
    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 position, 
                             float rotation, float size, int color, float alpha, boolean glowEffect,
                             ResourceLocation runeTexture, float partialTicks) {
        
        if (alpha <= 0.0f || size <= 0.0f) {
            return; // 不渲染透明或零大小的魔法陣
        }
        
        // 檢查渲染距離
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        double distance = cameraPos.distanceTo(position);
        
        if (distance > MAX_RENDER_DISTANCE) {
            return; // 超出渲染距離
        }
        
        // 根據距離調整 LOD
        float lodFactor = Math.max(0.1f, 1.0f - (float)(distance / MAX_RENDER_DISTANCE));
        int segments = Math.max(8, (int)(CIRCLE_SEGMENTS * lodFactor));
        
        poseStack.pushPose();
        
        try {
            // 移動到目標位置
            poseStack.translate(position.x - cameraPos.x, position.y - cameraPos.y, position.z - cameraPos.z);
            
            // 讓魔法陣面向攝影機 (Billboard效果)
            Vector3f cameraDirection = new Vector3f(
                (float)(cameraPos.x - position.x),
                0, // 保持水平
                (float)(cameraPos.z - position.z)
            ).normalize();
            
            float yaw = (float) Math.atan2(cameraDirection.x, cameraDirection.z);
            poseStack.mulPose(new org.joml.Quaternionf().rotationY(-yaw));
            
            // 應用旋轉
            poseStack.mulPose(new org.joml.Quaternionf().rotationY(rotation));
            
            // 應用縮放
            poseStack.scale(size, size, size);
            
            // 選擇渲染類型
            RenderType renderType = getRenderType(runeTexture, glowEffect);
            VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
            
            // 渲染魔法陣
            if (runeTexture != null) {
                renderTexturedCircle(poseStack, vertexConsumer, color, alpha, segments);
            } else {
                renderBasicCircle(poseStack, vertexConsumer, color, alpha, segments);
            }
            
            // 如果啟用發光效果，渲染額外的發光層
            if (glowEffect && alpha > 0.1f) {
                renderGlowEffect(poseStack, bufferSource, color, alpha * 0.3f, size * 1.2f, segments);
            }
            
        } finally {
            poseStack.popPose();
        }
    }
    
    /**
     * 渲染基礎圓形魔法陣（無紋理）
     */
    private static void renderBasicCircle(PoseStack poseStack, VertexConsumer vertexConsumer, 
                                         int color, float alpha, int segments) {
        Matrix4f matrix = poseStack.last().pose();
        
        // 提取顏色分量
        float red = FastColor.ARGB32.red(color) / 255.0f;
        float green = FastColor.ARGB32.green(color) / 255.0f;
        float blue = FastColor.ARGB32.blue(color) / 255.0f;
        
        // 渲染外環
        renderCircleRing(matrix, vertexConsumer, 0.8f, 1.0f, red, green, blue, alpha, segments);
        
        // 渲染中環
        renderCircleRing(matrix, vertexConsumer, 0.5f, 0.6f, red, green, blue, alpha * 0.7f, segments);
        
        // 渲染內環
        renderCircleRing(matrix, vertexConsumer, 0.2f, 0.3f, red, green, blue, alpha * 0.5f, segments);
        
        // 渲染中心點
        renderCircleFilled(matrix, vertexConsumer, 0.0f, 0.1f, red, green, blue, alpha * 0.8f, segments / 2);
    }
    
    /**
     * 渲染帶紋理的魔法陣
     */
    private static void renderTexturedCircle(PoseStack poseStack, VertexConsumer vertexConsumer,
                                           int color, float alpha, int segments) {
        Matrix4f matrix = poseStack.last().pose();
        
        // 提取顏色分量
        float red = FastColor.ARGB32.red(color) / 255.0f;
        float green = FastColor.ARGB32.green(color) / 255.0f;
        float blue = FastColor.ARGB32.blue(color) / 255.0f;
        
        // 渲染完整的紋理圓形
        renderTexturedQuad(matrix, vertexConsumer, red, green, blue, alpha);
    }
    
    /**
     * 渲染圓環
     */
    private static void renderCircleRing(Matrix4f matrix, VertexConsumer vertexConsumer,
                                        float innerRadius, float outerRadius, 
                                        float red, float green, float blue, float alpha, int segments) {
        
        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (2 * Math.PI * i / segments);
            float angle2 = (float) (2 * Math.PI * (i + 1) / segments);
            
            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2);
            float sin2 = (float) Math.sin(angle2);
            
            // 外圈頂點
            float x1_outer = cos1 * outerRadius;
            float z1_outer = sin1 * outerRadius;
            float x2_outer = cos2 * outerRadius;
            float z2_outer = sin2 * outerRadius;
            
            // 內圈頂點
            float x1_inner = cos1 * innerRadius;
            float z1_inner = sin1 * innerRadius;
            float x2_inner = cos2 * innerRadius;
            float z2_inner = sin2 * innerRadius;
            
            // 渲染四邊形 (兩個三角形)
            // 三角形 1
            vertexConsumer.addVertex(matrix, x1_outer, 0, z1_outer)
                .setColor(red, green, blue, alpha)
                .setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0, 1, 0);
                
            vertexConsumer.addVertex(matrix, x1_inner, 0, z1_inner)
                .setColor(red, green, blue, alpha)
                .setUv(0, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0, 1, 0);
                
            vertexConsumer.addVertex(matrix, x2_inner, 0, z2_inner)
                .setColor(red, green, blue, alpha)
                .setUv(1, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0, 1, 0);
            
            // 三角形 2
            vertexConsumer.addVertex(matrix, x1_outer, 0, z1_outer)
                .setColor(red, green, blue, alpha)
                .setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0, 1, 0);
                
            vertexConsumer.addVertex(matrix, x2_inner, 0, z2_inner)
                .setColor(red, green, blue, alpha)
                .setUv(1, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0, 1, 0);
                
            vertexConsumer.addVertex(matrix, x2_outer, 0, z2_outer)
                .setColor(red, green, blue, alpha)
                .setUv(1, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0, 1, 0);
        }
    }
    
    /**
     * 渲染實心圓形
     */
    private static void renderCircleFilled(Matrix4f matrix, VertexConsumer vertexConsumer,
                                          float innerRadius, float outerRadius,
                                          float red, float green, float blue, float alpha, int segments) {
        
        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (2 * Math.PI * i / segments);
            float angle2 = (float) (2 * Math.PI * (i + 1) / segments);
            
            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2);
            float sin2 = (float) Math.sin(angle2);
            
            // 三角形扇形
            vertexConsumer.addVertex(matrix, 0, 0, 0)
                .setColor(red, green, blue, alpha)
                .setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0, 1, 0);
                
            vertexConsumer.addVertex(matrix, cos1 * outerRadius, 0, sin1 * outerRadius)
                .setColor(red, green, blue, alpha)
                .setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0, 1, 0);
                
            vertexConsumer.addVertex(matrix, cos2 * outerRadius, 0, sin2 * outerRadius)
                .setColor(red, green, blue, alpha)
                .setUv(1, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0, 1, 0);
        }
    }
    
    /**
     * 渲染帶紋理的四邊形
     */
    private static void renderTexturedQuad(Matrix4f matrix, VertexConsumer vertexConsumer,
                                          float red, float green, float blue, float alpha) {
        
        // 四個頂點構成一個正方形
        vertexConsumer.addVertex(matrix, -1, 0, -1)
            .setColor(red, green, blue, alpha)
            .setUv(0, 0)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(0, 1, 0);
            
        vertexConsumer.addVertex(matrix, -1, 0, 1)
            .setColor(red, green, blue, alpha)
            .setUv(0, 1)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(0, 1, 0);
            
        vertexConsumer.addVertex(matrix, 1, 0, 1)
            .setColor(red, green, blue, alpha)
            .setUv(1, 1)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(0, 1, 0);
        
        vertexConsumer.addVertex(matrix, -1, 0, -1)
            .setColor(red, green, blue, alpha)
            .setUv(0, 0)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(0, 1, 0);
            
        vertexConsumer.addVertex(matrix, 1, 0, 1)
            .setColor(red, green, blue, alpha)
            .setUv(1, 1)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(0, 1, 0);
            
        vertexConsumer.addVertex(matrix, 1, 0, -1)
            .setColor(red, green, blue, alpha)
            .setUv(1, 0)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(0, 1, 0);
    }
    
    /**
     * 渲染發光效果
     */
    private static void renderGlowEffect(PoseStack poseStack, MultiBufferSource bufferSource,
                                        int color, float alpha, float size, int segments) {
        
        // 創建額外的發光層
        RenderType glowRenderType = RenderType.entityTranslucentCull(DEFAULT_CIRCLE_TEXTURE);
        VertexConsumer glowConsumer = bufferSource.getBuffer(glowRenderType);
        
        poseStack.pushPose();
        poseStack.scale(size, size, size);
        
        Matrix4f matrix = poseStack.last().pose();
        
        float red = FastColor.ARGB32.red(color) / 255.0f;
        float green = FastColor.ARGB32.green(color) / 255.0f;
        float blue = FastColor.ARGB32.blue(color) / 255.0f;
        
        // 渲染發光的外環
        renderCircleRing(matrix, glowConsumer, 0.9f, 1.3f, red, green, blue, alpha, segments);
        
        poseStack.popPose();
    }
    
    /**
     * 獲取適當的渲染類型
     */
    private static RenderType getRenderType(ResourceLocation texture, boolean glow) {
        if (texture != null) {
            return glow ? 
                RenderType.entityTranslucentCull(texture) : 
                RenderType.entityCutout(texture);
        } else {
            return RenderType.debugLineStrip(1.0);
        }
    }
    
    /**
     * 檢查是否應該渲染（距離檢查等）
     */
    public static boolean shouldRender(Vec3 position, float size) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        double distance = cameraPos.distanceTo(position);
        
        // 根據大小調整渲染距離
        float adjustedMaxDistance = MAX_RENDER_DISTANCE * Math.max(0.5f, size);
        
        return distance <= adjustedMaxDistance;
    }
}