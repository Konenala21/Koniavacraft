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

    // 黑洞核心預算 cos/sin（pie-slice 用，N 楔形片從中心向外，alpha 線性插值，零銜接縫）
    private static final int VOID_SEGS = 36;
    private static final float[] VOID_COS = new float[VOID_SEGS];
    private static final float[] VOID_SIN = new float[VOID_SEGS];
    static {
        for (int i = 0; i < VOID_SEGS; i++) {
            double a = i * 2 * Math.PI / VOID_SEGS;
            VOID_COS[i] = (float) Math.cos(a);
            VOID_SIN[i] = (float) Math.sin(a);
        }
    }

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

        // 1. 黑洞核心先畫（alpha blend，蓋住背景）
        // 順序要早於 glow，否則 alpha blend 會把後畫的 additive 光暈中央吃掉
        VertexConsumer dark = buffer.getBuffer(MIRenderTypes.voidCore());
        float pulse = 1.0f + (float) Math.sin(t * 0.08) * 0.05f;
        drawVoidPie(dark, pose, 0.85f * pulse, 1.40f * pulse, 60, 30, 90, 130);   // 最外暈：暗紫
        drawVoidPie(dark, pose, 0.65f * pulse, 1.20f * pulse, 30, 10, 50, 180);   // 中外：深紫
        drawVoidPie(dark, pose, 0.45f * pulse, 1.05f * pulse,  8,  0, 20, 220);   // 中內：近黑帶冷紫
        drawVoidPie(dark, pose, 0.25f * pulse, 0.85f * pulse,  0,  0,  6, 250);   // 核心：純黑

        // 2. 紫色光暈外圈（additive 加色在黑色上 → 變光暈色，在天空上 → 也是光暈色，邊緣兩側都亮）
        VertexConsumer glow = buffer.getBuffer(RenderType.lightning());
        drawRift(glow, pose, 0.55f, 0.0f, t, 90, 50, 160, 50);
        drawRift(glow, pose, 0.32f, 0.04f, t, 150, 100, 230, 110);
        drawRift(glow, pose, 0.20f, 0.06f, t, 200, 150, 255, 180);

        // 3. 邊緣放射閃電弧（additive 三層輝光）
        for (int i = 0; i < LIGHTNING_BOLTS; i++) {
            drawLightningBolt(glow, pose, t, i);
        }

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

    // Pie-slice 黑洞片：N 楔形從中心輻射，每片用一個 quad（中心點重複當第 4 頂點）
    // 中心 alpha=a，外緣 alpha=0 → GPU 線性插值整片 = 完全平滑徑向漸層
    private static void drawVoidPie(VertexConsumer vc, Matrix4f pose, float halfW, float halfH,
                                    int r, int g, int b, int a) {
        for (int i = 0; i < VOID_SEGS; i++) {
            int j = (i + 1) % VOID_SEGS;
            float x1 = halfW * VOID_COS[i], y1 = halfH * VOID_SIN[i];
            float x2 = halfW * VOID_COS[j], y2 = halfH * VOID_SIN[j];
            vc.addVertex(pose, 0, 0, 0).setColor(r, g, b, a);   // 中心（滿 alpha）
            vc.addVertex(pose, x1, y1, 0).setColor(r, g, b, 0); // 外緣 i（透明）
            vc.addVertex(pose, x2, y2, 0).setColor(r, g, b, 0); // 外緣 j（透明）
            vc.addVertex(pose, 0, 0, 0).setColor(r, g, b, a);   // 中心（QUADS 第 4 頂點）
        }
    }

    // 閃電：採用「太陽輝光」的多層 additive 加色技巧
    // 每支閃電畫 3 道相同路徑：外暈（寬+暗紫）+ 中層（中+亮紫）+ 內核（細+純白）
    // additive 疊起來邊緣會自然發光暈，邊界不再硬切 → 無顆粒感
    private static void drawLightningBolt(VertexConsumer vc, Matrix4f pose, float t, int boltIdx) {
        int phaseTick = (int) (t / 4);
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
        // 3 層 additive 疊起來：仿 renderSolarCore 的多層輝光手法
        // 外暈：寬 0.12、暗紫、低 alpha → 模糊邊緣
        drawBoltStroke(vc, pose, xs, ys, nx, ny, 0.12f,  80,  40, 160, 70);
        // 中層：中等寬、亮紫
        drawBoltStroke(vc, pose, xs, ys, nx, ny, 0.06f, 180, 130, 255, 140);
        // 內核：細白色高亮
        drawBoltStroke(vc, pose, xs, ys, nx, ny, 0.025f, 245, 240, 255, 230);
    }

    // 沿預算好的路徑畫一道閃電筆觸（寬度與顏色由參數決定）
    private static void drawBoltStroke(VertexConsumer vc, Matrix4f pose, float[] xs, float[] ys,
                                       float nx, float ny, float thickness, int r, int g, int b, int peakAlpha) {
        float zFront = 0.02f;
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
        float jag = 0.7f + 0.3f * (float) Math.sin(v * 15.0 + t * 0.18); // 邊緣鋸齒抖動
        return maxHalfWidth * lens * jag;
    }

    @Override
    public ResourceLocation getTextureLocation(SpaceCrackEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}
