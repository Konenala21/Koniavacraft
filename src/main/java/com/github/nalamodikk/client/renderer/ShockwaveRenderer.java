package com.github.nalamodikk.client.renderer;

import com.github.nalamodikk.client.screenAPI.MIRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 衝擊波(坤載體)的專屬視覺:一圈圈沿地面擴散的環,純頂點色(走 vanilla position_color
 * shader,不依賴自訂 shader 程式)。獨立於 {@link ManaStrikeShaderRenderer}:衝擊波是地面
 * 擴散環,不該帶魔力打擊那道垂直光柱,所以自己一份,不共用。
 *
 * server 端 {@code CarrierFx.shockwave} 送 CarrierFxPacket → {@link CarrierFxClient} 呼叫
 * {@link #spawn};{@link #onRenderLevel} 每幀畫(由 ClientEffectEvents 轉發)。
 */
public final class ShockwaveRenderer {

    private ShockwaveRenderer() {}

    private record Wave(Vec3 pos, long startTick) {}

    private static final List<Wave> WAVES = new ArrayList<>();

    private static final int   RING_COUNT = 7;
    private static final float RING_DELAY = 5f;
    private static final float RING_SPEED = 0.32f;
    private static final float RING_MAX_R = 15f;
    private static final float TOTAL_LIFE = RING_DELAY * (RING_COUNT - 1) + RING_MAX_R / RING_SPEED;
    private static final int   SEGS = 48;
    private static final float GROUND_OFFSET = 0.05f; // 離地一點避免 z-fighting

    private static final float[] COS = new float[SEGS];
    private static final float[] SIN = new float[SEGS];
    static {
        for (int i = 0; i < SEGS; i++) {
            double a = i * 2 * Math.PI / SEGS;
            COS[i] = (float) Math.cos(a);
            SIN[i] = (float) Math.sin(a);
        }
    }

    /** 在世界座標生成一道衝擊波(client 端)。 */
    public static void spawn(Vec3 worldPos, long gameTick) {
        WAVES.add(new Wave(worldPos, gameTick));
    }

    /** 登出時清掉殘留的環,避免跨存檔殘影(對齊其他 renderer 的清理慣例)。 */
    public static void release() {
        WAVES.clear();
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (WAVES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float gameTick = mc.level.getGameTime() + partial;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        var buf = mc.renderBuffers().bufferSource();
        // no-cull:環是水平鋪地的,預設 backface culling 會讓正上方視角看不到(剔掉朝上那面)。
        VertexConsumer vc = buf.getBuffer(MIRenderTypes.solarGlowNoCull());

        Iterator<Wave> it = WAVES.iterator();
        while (it.hasNext()) {
            Wave w = it.next();
            float elapsed = gameTick - w.startTick();
            if (elapsed > TOTAL_LIFE) { it.remove(); continue; }

            ps.pushPose();
            // 抬高一點點離開地表,避免跟地面方塊頂面 z-fighting。
            ps.translate(w.pos().x - cam.x, w.pos().y - cam.y + GROUND_OFFSET, w.pos().z - cam.z);
            Matrix4f mat = ps.last().pose();

            for (int i = 0; i < RING_COUNT; i++) {
                float ringElapsed = elapsed - i * RING_DELAY;
                if (ringElapsed <= 0) continue;
                float radius = ringElapsed * RING_SPEED;
                if (radius > RING_MAX_R) continue;
                float fadeOut = 1f - radius / RING_MAX_R;
                float thickness = 0.12f + fadeOut * 0.55f;
                drawRing(mat, vc, radius + thickness * 1.6f, thickness * 1.6f, 60, 140, (int) (30 * fadeOut));
                drawRing(mat, vc, radius, thickness, 100, 190, (int) (140 * fadeOut));
                drawRing(mat, vc, radius - thickness * 0.15f, thickness * 0.25f, 210, 235, (int) (200 * fadeOut));
            }
            ps.popPose();
        }
        buf.endBatch(MIRenderTypes.solarGlowNoCull());
    }

    private static void drawRing(Matrix4f mat, VertexConsumer vc, float radius, float halfWidth, int r, int g, int a) {
        if (a <= 0 || radius <= 0) return;
        float inner = Math.max(0, radius - halfWidth);
        float outer = radius + halfWidth;
        for (int i = 0; i < SEGS; i++) {
            int j = (i + 1 == SEGS) ? 0 : i + 1;
            vc.addVertex(mat, inner * COS[i], 0f, inner * SIN[i]).setColor(r, g, 255, 0);
            vc.addVertex(mat, outer * COS[i], 0f, outer * SIN[i]).setColor(r, g, 255, a);
            vc.addVertex(mat, outer * COS[j], 0f, outer * SIN[j]).setColor(r, g, 255, a);
            vc.addVertex(mat, inner * COS[j], 0f, inner * SIN[j]).setColor(r, g, 255, 0);
        }
    }
}
