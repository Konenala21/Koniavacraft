package com.github.nalamodikk.client.renderer.turret;

import com.github.nalamodikk.KoniavacraftMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class TurretHitEffectRenderer {

    private static final int   DURATION_TICKS = TurretHitEffectManager.DURATION_TICKS;
    private static final int   RING_SEGMENTS  = 32;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long gameTime = mc.level.getGameTime();
        float pt      = event.getPartialTick().getGameTimeDeltaTicks();

        TurretHitEffectManager.prune(gameTime);
        var effects = TurretHitEffectManager.getActive();
        if (effects.isEmpty()) return;

        Vec3 camPos = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        for (var effect : effects) {
            float age      = (gameTime - effect.spawnTick()) + pt;
            float progress = age / DURATION_TICKS;
            if (progress < 0 || progress >= 1.0f) continue;

            ps.pushPose();
            ps.translate(
                    effect.pos().x - camPos.x,
                    effect.pos().y - camPos.y,
                    effect.pos().z - camPos.z);

            renderRing(ps, buffers, progress, effect.chargeRatio());

            ps.popPose();
        }

        buffers.endBatch(RenderType.lightning());
    }

    private static void renderRing(PoseStack ps, MultiBufferSource buffers,
                                    float progress, float chargeRatio) {
        VertexConsumer vc   = buffers.getBuffer(RenderType.lightning());
        Matrix4f       pose = ps.last().pose();

        float maxRadius  = 1.8f + chargeRatio * 1.2f;
        float expandedR  = maxRadius * progress;
        float ringWidth  = maxRadius * 0.18f * (1.0f - progress * 0.55f);
        float outerR     = expandedR;
        float innerR     = Math.max(0.02f, expandedR - ringWidth);

        // Alpha: eases out, charged stays brighter longer
        float fadeAlpha  = (float) Math.pow(1.0 - progress, 1.3 - chargeRatio * 0.4);

        int r  = 50  + (int)(chargeRatio * 160);
        int g  = 140 + (int)(chargeRatio * 90);
        int b  = 255;
        int ai = (int)(fadeAlpha * 230); // inner edge — bright
        int ao = (int)(fadeAlpha * 80);  // outer edge — dim (glow falloff)

        for (int i = 0; i < RING_SEGMENTS; i++) {
            float a1   = (float)(i       * 2 * Math.PI / RING_SEGMENTS);
            float a2   = (float)((i + 1) * 2 * Math.PI / RING_SEGMENTS);
            float cos1 = (float) Math.cos(a1), sin1 = (float) Math.sin(a1);
            float cos2 = (float) Math.cos(a2), sin2 = (float) Math.sin(a2);

            // Quad: inner1 → outer1 → outer2 → inner2
            vc.addVertex(pose, innerR * cos1, 0, innerR * sin1).setColor(r, g, b, ai);
            vc.addVertex(pose, outerR * cos1, 0, outerR * sin1).setColor(r, g, b, ao);
            vc.addVertex(pose, outerR * cos2, 0, outerR * sin2).setColor(r, g, b, ao);
            vc.addVertex(pose, innerR * cos2, 0, innerR * sin2).setColor(r, g, b, ai);
        }

        // Brief centre flash in the first 15% of the effect
        if (progress < 0.15f) {
            float flashT  = 1.0f - progress / 0.15f;
            int   fa      = (int)(flashT * flashT * 180);
            float flashR  = maxRadius * 0.35f * (1.0f - flashT * 0.5f);
            vc.addVertex(pose, -flashR, 0, -flashR).setColor(210, 235, 255, fa);
            vc.addVertex(pose,  flashR, 0, -flashR).setColor(210, 235, 255, fa);
            vc.addVertex(pose,  flashR, 0,  flashR).setColor(210, 235, 255, fa);
            vc.addVertex(pose, -flashR, 0,  flashR).setColor(210, 235, 255, fa);
        }

        // Second thin ring: slightly behind the main ring, creates "wave trailing" look
        if (progress > 0.08f) {
            float innerR2 = Math.max(0.01f, innerR * 0.85f);
            float outerR2 = innerR * 0.98f;
            int   ai2     = (int)(fadeAlpha * 100);
            for (int i = 0; i < RING_SEGMENTS; i++) {
                float a1 = (float)(i       * 2 * Math.PI / RING_SEGMENTS);
                float a2 = (float)((i + 1) * 2 * Math.PI / RING_SEGMENTS);
                float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                float c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);
                vc.addVertex(pose, innerR2 * c1, 0, innerR2 * s1).setColor(r, g, b, ai2);
                vc.addVertex(pose, outerR2 * c1, 0, outerR2 * s1).setColor(r, g, b, 10);
                vc.addVertex(pose, outerR2 * c2, 0, outerR2 * s2).setColor(r, g, b, 10);
                vc.addVertex(pose, innerR2 * c2, 0, innerR2 * s2).setColor(r, g, b, ai2);
            }
        }
    }
}
