package com.github.nalamodikk.common.utils.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * 通用 GUI 繪圖工具：支援貼圖 + 顏色 tint
 */
public class GuiRenderUtils {
    private final PoseStack pose;

    public GuiRenderUtils(PoseStack pose) {
        this.pose = pose;
    }

    /**
     * 在 GUI 上畫一張圖片的部分，並染上顏色。
     *
     * @param texture 貼圖資源
     * @param x 左上角 X
     * @param y 左上角 Y
     * @param width 寬度
     * @param height 高度
     * @param u1 貼圖起點 U (0.0 ~ 1.0)
     * @param v1 貼圖起點 V (0.0 ~ 1.0)
     * @param u2 貼圖終點 U
     * @param v2 貼圖終點 V
     * @param r 顏色 R (0~1)
     * @param g 顏色 G
     * @param b 顏色 B
     * @param a 顏色 A
     */
    public void blitWithColor(ResourceLocation texture,
                              int x, int y,
                              int width, int height,
                              float u1, float v1, float u2, float v2,
                              float r, float g, float b, float a) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();

        Matrix4f matrix = this.pose.last().pose();
        int x2 = x + width;
        int y2 = y + height;

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(matrix, x, y, 0).setUv(u1, v1).setColor(r, g, b, a);
        buffer.addVertex(matrix, x, y2, 0).setUv(u1, v2).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y2, 0).setUv(u2, v2).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y, 0).setUv(u2, v1).setColor(r, g, b, a);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.disableBlend();
    }

    public void blitRawTexture(
            ResourceLocation texture,
            int x, int y,             // 畫面左上角位置
            int u, int v,             // 貼圖起點像素位置
            int width, int height,    // 要繪製的寬高
            int texWidth, int texHeight // 貼圖實際尺寸
    ) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexShader); // ⚠️ 注意這是原版 GUI 常用 shader
        RenderSystem.enableBlend();

        Matrix4f matrix = this.pose.last().pose();
        int x2 = x + width;
        int y2 = y + height;

        float u1 = u / (float) texWidth;
        float v1 = v / (float) texHeight;
        float u2 = (u + width) / (float) texWidth;
        float v2 = (v + height) / (float) texHeight;

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        buffer.addVertex(matrix, x,  y, 0).setUv(u1, v1).setColor(1f, 1f, 1f, 1f);
        buffer.addVertex(matrix, x,  y2, 0).setUv(u1, v2).setColor(1f, 1f, 1f, 1f);
        buffer.addVertex(matrix, x2, y2, 0).setUv(u2, v2).setColor(1f, 1f, 1f, 1f);
        buffer.addVertex(matrix, x2, y, 0).setUv(u2, v1).setColor(1f, 1f, 1f, 1f);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }


}
