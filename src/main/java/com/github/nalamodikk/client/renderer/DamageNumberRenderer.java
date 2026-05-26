package com.github.nalamodikk.client.renderer;

import com.github.nalamodikk.KoniavacraftMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class DamageNumberRenderer {

    private static final float MAX_AGE    = 30f;
    private static final float FADE_START = 15f;
    private static final long  MERGE_WINDOW = 5L;

    private static final List<Entry> ENTRIES = new CopyOnWriteArrayList<>();
    private static final Random RNG = new Random();

    private static final class Entry {
        final int entityId;
        final double spawnX, spawnY, spawnZ;
        volatile float damage;
        volatile int colorRGB;
        final float driftX, driftZ;
        volatile long spawnTick;
        volatile String text;

        Entry(int entityId, double x, double y, double z,
              float damage, int colorRGB, float driftX, float driftZ, long spawnTick) {
            this.entityId = entityId;
            this.spawnX = x; this.spawnY = y; this.spawnZ = z;
            this.damage = damage;
            this.colorRGB = colorRGB;
            this.driftX = driftX; this.driftZ = driftZ;
            this.spawnTick = spawnTick;
            this.text = formatDamage(damage);
        }
    }

    private static String formatDamage(float damage) {
        return String.format("%.0f", damage);
    }

    private static int mergePriority(int colorRGB) {
        if (colorRGB == 0xCC55FF) return 2;
        if (colorRGB == 0xFFD700) return 1;
        return 0;
    }

    public static void clear() { ENTRIES.clear(); }

    public static void add(double x, double y, double z, float damage, byte dmgType, int entityId) {
        int colorRGB = switch (dmgType) {
            case 1  -> 0xFFD700;
            case 2  -> 0xCC55FF;
            default -> 0xFFFFFF;
        };

        Minecraft mc = Minecraft.getInstance();
        long currentTick = mc.level != null ? mc.level.getGameTime() : 0L;

        for (Entry e : ENTRIES) {
            if (e.entityId == entityId && currentTick - e.spawnTick <= MERGE_WINDOW) {
                e.damage += damage;
                e.spawnTick = currentTick;
                if (mergePriority(colorRGB) > mergePriority(e.colorRGB)) {
                    e.colorRGB = colorRGB;
                }
                e.text = formatDamage(e.damage);
                return;
            }
        }

        float angle = RNG.nextFloat() * (float)(Math.PI * 2);
        float speed = 0.006f + RNG.nextFloat() * 0.006f;
        ENTRIES.add(new Entry(entityId, x, y, z, damage, colorRGB,
                (float)Math.cos(angle) * speed, (float)Math.sin(angle) * speed, currentTick));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.hideGui) return;

        long currentTick = mc.level.getGameTime();
        ENTRIES.removeIf(e -> currentTick - e.spawnTick >= (long) MAX_AGE);
        if (ENTRIES.isEmpty()) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        RenderSystem.disableCull();

        for (Entry e : ENTRIES) {
            float age = currentTick - e.spawnTick;
            if (age < 0 || age >= MAX_AGE) continue;

            double wx = e.spawnX + e.driftX * age;
            double wy = e.spawnY + 0.04 * age;
            double wz = e.spawnZ + e.driftZ * age;

            float alphaF = age < FADE_START
                    ? 1.0f
                    : 1.0f - (age - FADE_START) / (MAX_AGE - FADE_START);
            int alpha = (int)(alphaF * 255);
            if (alpha <= 0) continue;

            double dx = wx - camPos.x, dy = wy - camPos.y, dz = wz - camPos.z;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            float scale = Math.max(0.045f, 0.012f * dist);

            int argb = (alpha << 24) | e.colorRGB;
            String text = e.text;
            int tw = font.width(text);

            pose.pushPose();
            pose.translate(dx, dy, dz);
            pose.mulPose(camera.rotation());
            pose.scale(scale, -scale, scale);

            font.drawInBatch(text, -tw / 2f, 0, argb, true,
                    pose.last().pose(),
                    bufferSource,
                    Font.DisplayMode.SEE_THROUGH,
                    0, LightTexture.FULL_BRIGHT);

            pose.popPose();
        }

        bufferSource.endLastBatch();
        RenderSystem.enableCull();
    }
}
