package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.client.screenAPI.MIRenderTypes;
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
    // 仿 vanilla LightningBoltRenderer：8 段、每段隨機 X 跳動、白色加色、無平滑
    private static final int LIGHTNING_BOLTS = 8;
    private static final int LIGHTNING_SEGMENTS = 8;

    // 黑洞核心 — 用 GLSL shader 處理徑向漸層 + dithering，純單一 quad（4 頂點）

    public SpaceCrackRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(SpaceCrackEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 實際渲染由 SpaceCrackStageRenderer 在 AFTER_PARTICLES 階段做（穿透所有 3D 物件包含 BE）
        // 這裡只負責 super.render（hitbox 等 vanilla 行為）
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /** 給 stage renderer 呼叫的真正渲染邏輯（脫離 entity render dispatcher）。 */
    public static void renderCrack(SpaceCrackEntity entity, PoseStack poseStack,
                                   MultiBufferSource buffer, float partialTick) {
        poseStack.pushPose();
        poseStack.translate(0.0, 1.1, 0.0);
        // 只繞 Y 軸正對相機（直立的撕裂，不再貼著視角俯仰/打滾）
        // Snap 到整數度避免 sub-pixel 抖動造成 strip 邊緣 aliasing 閃爍
        var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        double dxCam = cam.getPosition().x - entity.getX();
        double dzCam = cam.getPosition().z - entity.getZ();
        float yawToCam = (float) (Math.atan2(dxCam, dzCam) * (180.0 / Math.PI));
        yawToCam = Math.round(yawToCam); // 1° snap 防 sub-pixel 抖動
        poseStack.mulPose(Axis.YP.rotationDegrees(yawToCam));

        float t = entity.tickCount + partialTick;
        Matrix4f pose = poseStack.last().pose();

        // 1. 黑洞核心（shader 平滑徑向漸層）
        VertexConsumer dark = buffer.getBuffer(MIRenderTypes.voidCoreShader());
        drawVoidShaderQuad(dark, pose, 0.55f, 1.20f, 0, 0, 8, 230);

        // 2. 紫色撕裂縫 — alpha 降低避免 3 層 additive 疊起來像閃光彈
        VertexConsumer rift = buffer.getBuffer(MIRenderTypes.voidRiftShader());
        drawVoidShaderQuad(rift, pose, 1.20f, 2.50f, 90, 50, 160, 50);    // 外暈：80→50
        drawVoidShaderQuad(rift, pose, 0.95f, 2.30f, 130, 90, 200, 70);   // 中層：140→70
        drawVoidShaderQuad(rift, pose, 0.70f, 2.10f, 180, 130, 230, 90);  // 內亮：220→90，色彩也降一點

        // 3. 邊緣放射閃電弧 — 用穿透方塊版本
        VertexConsumer bolts = buffer.getBuffer(MIRenderTypes.lightningNoDepth());
        for (int i = 0; i < LIGHTNING_BOLTS; i++) {
            drawLightningBolt(bolts, pose, t, i);
        }

        poseStack.popPose();
    }

    // 透鏡狀（中間寬、上下尖）+ 中央鋸齒位移 = 紫色撕裂縫的視覺識別
    // additive lightning RT，多層疊出輝光
    // 時間動畫慢到幾乎察覺不到（避免每幀邊緣移動造成「閃動黑點」）
    private static void drawRift(VertexConsumer vc, Matrix4f pose, float maxHalfWidth, float zigAmp,
                                  float t, int r, int g, int b, int a) {
        float slowT = t * 0.02f;
        // 微小垂直重疊填補 strip seam（GL rasterization rule 會讓共用邊像素被漏渲染 → 黑點）
        float overlap = 0.004f;
        for (int i = 0; i < STRIPS; i++) {
            float v0 = -HALF_HEIGHT + 2f * HALF_HEIGHT * i / STRIPS;
            float v1 = -HALF_HEIGHT + 2f * HALF_HEIGHT * (i + 1) / STRIPS;
            // 寬度仍用原 v 算（保持 lens 形狀），但 render 時垂直略擴一點
            float hw0 = halfWidth(v0, maxHalfWidth, slowT);
            float hw1 = halfWidth(v1, maxHalfWidth, slowT);
            float c0 = zigAmp * (float) Math.sin(v0 * 7.0 + slowT);
            float c1 = zigAmp * (float) Math.sin(v1 * 7.0 + slowT);
            float v0r = v0 - overlap; // 上邊往上擴
            float v1r = v1 + overlap; // 下邊往下擴
            vc.addVertex(pose, c0 - hw0, v0r, 0).setColor(r, g, b, a);
            vc.addVertex(pose, c0 + hw0, v0r, 0).setColor(r, g, b, a);
            vc.addVertex(pose, c1 + hw1, v1r, 0).setColor(r, g, b, a);
            vc.addVertex(pose, c1 - hw1, v1r, 0).setColor(r, g, b, a);
        }
    }

    // 黑洞核心 quad：傳 (0,0)~(1,1) UV 給 shader，fragment 自己算徑向 alpha + dithering
    // POSITION_TEX_COLOR 格式：頂點要有 UV，setColor 的 alpha 是 shader 的 vertexColor.a 倍數
    private static void drawVoidShaderQuad(VertexConsumer vc, Matrix4f pose, float halfW, float halfH,
                                           int r, int g, int b, int a) {
        vc.addVertex(pose, -halfW, -halfH, 0).setUv(0, 0).setColor(r, g, b, a);
        vc.addVertex(pose,  halfW, -halfH, 0).setUv(1, 0).setColor(r, g, b, a);
        vc.addVertex(pose,  halfW,  halfH, 0).setUv(1, 1).setColor(r, g, b, a);
        vc.addVertex(pose, -halfW,  halfH, 0).setUv(0, 1).setColor(r, g, b, a);
    }

    // 閃電：採用「太陽輝光」的多層 additive 加色技巧
    // 每支閃電畫 3 道相同路徑：外暈（寬+暗紫）+ 中層（中+亮紫）+ 內核（細+純白）
    // additive 疊起來邊緣會自然發光暈，邊界不再硬切 → 無顆粒感
    private static void drawLightningBolt(VertexConsumer vc, Matrix4f pose, float t, int boltIdx) {
        int phaseTick = (int) (t / 10); // 10 tick 換一輪（之前 4 太快會閃爍）
        int seed = boltIdx * 73 + phaseTick * 191;
        float yOnEdge = (pseudoRand(seed + 1) - 0.5f) * 1.8f;
        float edgeHW = halfWidth(yOnEdge, 0.32f, t);
        float xStart = (pseudoRand(seed + 2) > 0.5f ? 1f : -1f) * edgeHW;
        float dirX = (xStart > 0 ? 1f : -1f) * (0.5f + pseudoRand(seed + 4) * 0.5f);
        float dirY = (pseudoRand(seed + 5) - 0.5f) * 1.5f;
        float dn = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        dirX /= dn; dirY /= dn;
        float boltLen = 0.5f + pseudoRand(seed + 3) * 0.7f;
        // 路徑：減少抖動使弧線更滑順，避免雜訊感（從 0.18 降到 0.08）
        float[] xs = new float[LIGHTNING_SEGMENTS + 1];
        float[] ys = new float[LIGHTNING_SEGMENTS + 1];
        float runningJX = 0f, runningJY = 0f;
        for (int s = 0; s <= LIGHTNING_SEGMENTS; s++) {
            float u = s / (float) LIGHTNING_SEGMENTS;
            xs[s] = xStart + dirX * boltLen * u + runningJX;
            ys[s] = yOnEdge + dirY * boltLen * u + runningJY;
            runningJX += (pseudoRand(seed + 10 + s) - 0.5f) * 0.08f;
            runningJY += (pseudoRand(seed + 30 + s) - 0.5f) * 0.08f;
        }
        float nx = -dirY, ny = dirX;
        // 3 層 additive：溫和 alpha 避免在黑色背景上「閃爍黑點」對比過強
        drawBoltStroke(vc, pose, xs, ys, nx, ny, 0.12f,  80,  40, 160,  60);  // 外暈：寬+暗紫
        drawBoltStroke(vc, pose, xs, ys, nx, ny, 0.06f, 180, 130, 255, 120);  // 中層：紫
        drawBoltStroke(vc, pose, xs, ys, nx, ny, 0.025f, 230, 220, 255, 200); // 內核：白紫
    }

    // 沿預算好的路徑畫一道閃電筆觸（寬度與顏色由參數決定）
    private static void drawBoltStroke(VertexConsumer vc, Matrix4f pose, float[] xs, float[] ys,
                                       float nx, float ny, float thickness, int r, int g, int b, int peakAlpha) {
        float zFront = 0.05f; // 推到 dark + glow 前方
        for (int s = 0; s < LIGHTNING_SEGMENTS; s++) {
            float u0 = s / (float) LIGHTNING_SEGMENTS;
            float u1 = (s + 1) / (float) LIGHTNING_SEGMENTS;
            // 起點較粗、末端尖（同時 alpha 也末端淡）
            float thick0 = thickness * (1 - u0);
            float thick1 = thickness * (1 - u1);
            int alpha0 = (int) (peakAlpha * (1 - u0 * u0));
            int alpha1 = (int) (peakAlpha * (1 - u1 * u1));
            float x0 = xs[s],     y0 = ys[s];
            float x1 = xs[s + 1], y1 = ys[s + 1];
            vc.addVertex(pose, x0 - nx * thick0, y0 - ny * thick0, zFront).setColor(r, g, b, alpha0);
            vc.addVertex(pose, x0 + nx * thick0, y0 + ny * thick0, zFront).setColor(r, g, b, alpha0);
            vc.addVertex(pose, x1 + nx * thick1, y1 + ny * thick1, zFront).setColor(r, g, b, alpha1);
            vc.addVertex(pose, x1 - nx * thick1, y1 - ny * thick1, zFront).setColor(r, g, b, alpha1);
        }
    }

    // 偽隨機：根據種子回 0..1 之間穩定的數
    private static float pseudoRand(int seed) {
        int x = (seed * 0x9E3779B1) ^ (seed >> 13);
        x = (x ^ (x >> 17)) * 0x9E3779B1;
        return ((x >>> 8) & 0xFFFFFF) / (float) 0x1000000;
    }

    private static float halfWidth(float v, float maxHalfWidth, float t) {
        float n = v / HALF_HEIGHT;                       // -1..1
        float lens = (float) Math.pow(Math.max(0.0, 1.0 - n * n), 0.7); // 上下尖、中間寬
        // 鋸齒幅度減小（0.3→0.15）+ 時間動畫由 caller 控制慢速 → 避免邊緣每幀跳動產生閃動
        float jag = 0.85f + 0.15f * (float) Math.sin(v * 15.0 + t);
        return maxHalfWidth * lens * jag;
    }

    @Override
    public ResourceLocation getTextureLocation(SpaceCrackEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}
