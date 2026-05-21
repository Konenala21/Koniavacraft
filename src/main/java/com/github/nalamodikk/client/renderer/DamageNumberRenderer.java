package com.github.nalamodikk.client.renderer;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class DamageNumberRenderer {

    private static final float MAX_AGE    = 30f;
    private static final float FADE_START = 15f;
    private static final long  MERGE_WINDOW = 5L; // ticks — same entity hits within this window merge

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final Random RNG = new Random();

    private static final Matrix4f storedProjection = new Matrix4f();
    private static final Matrix4f storedModelView  = new Matrix4f();
    private static boolean hasMatrices = false;

    private static final class Entry {
        final int entityId;
        final double spawnX, spawnY, spawnZ;
        float damage;
        int colorRGB;
        final float driftX, driftZ;
        long spawnTick;

        Entry(int entityId, double x, double y, double z,
              float damage, int colorRGB, float driftX, float driftZ, long spawnTick) {
            this.entityId = entityId;
            this.spawnX = x; this.spawnY = y; this.spawnZ = z;
            this.damage = damage;
            this.colorRGB = colorRGB;
            this.driftX = driftX; this.driftZ = driftZ;
            this.spawnTick = spawnTick;
        }
    }

    // Color priority: MAGIC > CRIT > NORMAL
    private static int mergePriority(int colorRGB) {
        if (colorRGB == 0xCC55FF) return 2;
        if (colorRGB == 0xFFD700) return 1;
        return 0;
    }

    public static void add(double x, double y, double z, float damage, byte dmgType, int entityId) {
        int colorRGB = switch (dmgType) {
            case 1  -> 0xFFD700;
            case 2  -> 0xCC55FF;
            default -> 0xFFFFFF;
        };

        Minecraft mc = Minecraft.getInstance();
        long currentTick = mc.level != null ? mc.level.getGameTime() : 0L;

        // Try to merge into a recent entry for the same entity
        for (Entry e : ENTRIES) {
            if (e.entityId == entityId && currentTick - e.spawnTick <= MERGE_WINDOW) {
                e.damage += damage;
                e.spawnTick = currentTick; // reset lifetime so merged number stays visible
                // Upgrade color to highest priority type
                if (mergePriority(colorRGB) > mergePriority(e.colorRGB)) {
                    e.colorRGB = colorRGB;
                }
                return;
            }
        }

        // No recent entry — create a new one
        float angle = RNG.nextFloat() * (float)(Math.PI * 2);
        float speed = 0.006f + RNG.nextFloat() * 0.006f;
        ENTRIES.add(new Entry(entityId, x, y, z, damage, colorRGB,
                (float)Math.cos(angle) * speed, (float)Math.sin(angle) * speed, currentTick));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        storedProjection.set(event.getProjectionMatrix());
        storedModelView.set(event.getModelViewMatrix());
        hasMatrices = true;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!hasMatrices || ENTRIES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.hideGui) return;

        long currentTick = mc.level.getGameTime();
        ENTRIES.removeIf(e -> currentTick - e.spawnTick >= (long)MAX_AGE);
        if (ENTRIES.isEmpty()) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;

        for (Entry e : ENTRIES) {
            float age = currentTick - e.spawnTick;
            if (age < 0 || age >= MAX_AGE) continue;

            double wx = e.spawnX + e.driftX * age;
            double wy = e.spawnY + 0.04 * age;
            double wz = e.spawnZ + e.driftZ * age;

            Vector4f pos = new Vector4f(
                    (float)(wx - camPos.x),
                    (float)(wy - camPos.y),
                    (float)(wz - camPos.z),
                    1.0f);
            storedModelView.transform(pos);
            storedProjection.transform(pos);

            if (pos.w <= 0) continue;
            float ndcX = pos.x / pos.w;
            float ndcY = pos.y / pos.w;
            if (ndcX < -1.1f || ndcX > 1.1f || ndcY < -1.1f || ndcY > 1.1f) continue;

            int sx = (int)((ndcX + 1.0f) * 0.5f * screenW);
            int sy = (int)((1.0f - ndcY) * 0.5f * screenH);

            float alphaF = age < FADE_START
                    ? 1.0f
                    : 1.0f - (age - FADE_START) / (MAX_AGE - FADE_START);
            int alpha = (int)(alphaF * 255);
            if (alpha <= 0) continue;

            int argb = (alpha << 24) | e.colorRGB;
            String text = String.format("%.0f", e.damage);
            int tw = font.width(text);

            font.drawInBatch(text, sx - tw / 2f, sy, argb, true,
                    graphics.pose().last().pose(),
                    graphics.bufferSource(),
                    Font.DisplayMode.NORMAL,
                    0, LightTexture.FULL_BRIGHT);
        }

        graphics.flush();
    }
}
