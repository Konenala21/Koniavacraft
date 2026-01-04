package com.github.nalamodikk.common.utils.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
/**
 * A batch renderer for GUI textures in Minecraft using NeoForge's rendering pipeline.
 * <p>
 * This class allows multiple textured quads to be drawn with the same texture and shader
 * in a single draw call, significantly reducing rendering overhead compared to individual blit calls.
 * <p>
 * Typical use:
 * <pre>{@code
 * GuiRenderBatcher batcher = new GuiRenderBatcher(pose, TEXTURE);
 * batcher.blit(x, y, u, v, width, height, texW, texH, 1f, 1f, 1f, 1f);
 * batcher.blit(...); // multiple draw commands
 * batcher.draw();
 * }</pre>
 */
public class GuiQuadBatcher {
    private final PoseStack pose;
    private final BufferBuilder builder;
    private final ResourceLocation texture;
    private final Matrix4f matrix;

    /**
     * Creates a new GUI render batcher for the given texture.
     *
     * @param pose    The current {@link PoseStack} used for GUI rendering.
     * @param texture The texture to be bound and used throughout this batch.
     */
    public GuiQuadBatcher(PoseStack pose, ResourceLocation texture) {
        this.pose = pose;
        this.texture = texture;
        this.matrix = pose.last().pose();
        this.builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
    }

    /**
     * Adds a textured quad to the batch.
     *
     * @param x     The screen-space X coordinate (top-left corner).
     * @param y     The screen-space Y coordinate (top-left corner).
     * @param u     The U coordinate (texture-space in pixels).
     * @param v     The V coordinate (texture-space in pixels).
     * @param width The width of the quad to draw.
     * @param height The height of the quad to draw.
     * @param texW  The full texture width (usually 256 for GUI).
     * @param texH  The full texture height (usually 256 for GUI).
     * @param r     Red color component (0.0 – 1.0).
     * @param g     Green color component (0.0 – 1.0).
     * @param b     Blue color component (0.0 – 1.0).
     * @param a     Alpha (opacity) component (0.0 – 1.0).
     */
    public void blit(
            int x, int y, int u, int v, int width, int height,
            int texW, int texH,
            float r, float g, float b, float a
    ) {
        int x2 = x + width;
        int y2 = y + height;

        float u1 = u / (float) texW;
        float v1 = v / (float) texH;
        float u2 = (u + width) / (float) texW;
        float v2 = (v + height) / (float) texH;

        builder.addVertex(matrix, x,  y, 0).setUv(u1, v1).setColor(r, g, b, a);
        builder.addVertex(matrix, x,  y2, 0).setUv(u1, v2).setColor(r, g, b, a);
        builder.addVertex(matrix, x2, y2, 0).setUv(u2, v2).setColor(r, g, b, a);
        builder.addVertex(matrix, x2, y, 0).setUv(u2, v1).setColor(r, g, b, a);
    }

    /**
     * Submits all queued quads to the GPU in a single draw call.
     * <p>
     * This must be called once you're done adding quads via {@link #blit}.
     */
    public void draw() {
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.disableBlend();
    }
}
