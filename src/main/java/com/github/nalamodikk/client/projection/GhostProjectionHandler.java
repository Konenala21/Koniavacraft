package com.github.nalamodikk.client.projection;

import com.github.nalamodikk.KoniavacraftMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

/**
 * Ghost projection overlay — input & render handler.
 *
 * ─── Modes ───────────────────────────────────────────────────────────────────
 * PLACING  Ghost follows player look every tick (blue).
 * PLACED   Ghost frozen at locked position (green); player can freely interact.
 *
 * ─── Controls ────────────────────────────────────────────────────────────────
 *   Left-click  or Right-click (no sneak)  → PLACING→PLACED (click consumed)
 *   Sneak + Right-click                    → deactivate (click consumed)
 *   E  or  Escape                          → deactivate
 *
 * ─── Render ──────────────────────────────────────────────────────────────────
 * Fill uses a private Tesselator (own ByteBufferBuilder) + POSITION_COLOR to
 * avoid the "Not building!" crash that occurs when re-acquiring
 * RenderType.translucent() from the shared entity bufferSource at
 * AFTER_TRANSLUCENT_BLOCKS.
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class GhostProjectionHandler {

    // PLACING colours (blue)
    private static final float P_R = 0.5f, P_G = 0.7f, P_B = 1.0f;
    // PLACED colours (green)
    private static final float D_R = 0.3f, D_G = 1.0f, D_B = 0.4f;

    private static final float FILL_A = 0.30f;
    private static final float LINE_A = 0.85f;
    private static final double REACH = 32.0;

    /** Private tesselator — never shared with game systems. */
    private static final Tesselator GHOST_TESS = new Tesselator(65536);

    private GhostProjectionHandler() {}

    // ── Origin tracking (PLACING mode only) ───────────────────────────────────

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Pre event) {
        if (!GhostProjectionState.isActive()) return;
        if (GhostProjectionState.getMode() != GhostProjectionState.Mode.PLACING) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        HitResult hit = mc.player.pick(REACH, 1.0f, false);
        BlockPos newOrigin;
        if (hit instanceof BlockHitResult blockHit) {
            newOrigin = blockHit.getBlockPos().relative(blockHit.getDirection());
        } else {
            Vec3 eye  = mc.player.getEyePosition();
            Vec3 look = mc.player.getLookAngle().scale(10.0);
            newOrigin = BlockPos.containing(eye.add(look));
        }
        GhostProjectionState.setOrigin(newOrigin);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (!GhostProjectionState.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        // Only Escape closes the projection; E (inventory) is intentionally NOT intercepted
        // so the player can open JEI/inventory while the ghost is visible and re-click PRJ to dismiss.
        if (event.getKey() == GLFW.GLFW_KEY_ESCAPE) {
            GhostProjectionState.deactivate();
        }
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (!GhostProjectionState.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        if (mc.player == null) return;

        boolean sneaking = mc.player.isShiftKeyDown();
        int btn = event.getButton();
        boolean isRight = btn == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        boolean isLeft  = btn == GLFW.GLFW_MOUSE_BUTTON_LEFT;

        if (isRight && sneaking) {
            // ── 蹲下 + 右鍵 → 關閉 ──────────────────────────────────────────
            GhostProjectionState.deactivate();
            event.setCanceled(true);
            return;
        }

        if ((isLeft || isRight) && GhostProjectionState.getMode() == GhostProjectionState.Mode.PLACING) {
            // ── 左鍵 / 右鍵 → 固定位置 (PLACING → PLACED) ─────────────────
            GhostProjectionState.lockPosition();
            event.setCanceled(true); // prevent block break / place during planning
        }
        // PLACED mode: all non-sneak clicks pass through normally.
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!GhostProjectionState.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        Map<BlockPos, net.minecraft.world.level.block.state.BlockState> blocks =
                GhostProjectionState.getBlocks();
        if (blocks.isEmpty()) return;

        boolean placed = GhostProjectionState.getMode() == GhostProjectionState.Mode.PLACED;
        float fr = placed ? D_R : P_R;
        float fg = placed ? D_G : P_G;
        float fb = placed ? D_B : P_B;

        BlockPos origin = GhostProjectionState.getOrigin();
        Vec3      cam   = event.getCamera().getPosition();
        PoseStack ps    = event.getPoseStack();

        // ── Translucent fill ──────────────────────────────────────────────────
        BufferBuilder fill = GHOST_TESS.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (BlockPos local : blocks.keySet()) {
            BlockPos world = origin.offset(local);
            float tx = (float)(world.getX() - cam.x);
            float ty = (float)(world.getY() - cam.y);
            float tz = (float)(world.getZ() - cam.z);

            ps.pushPose();
            ps.translate(tx, ty, tz);
            addBox(fill, ps.last().pose(), 0, 0, 0, 1, 1, 1, fr, fg, fb, FILL_A);
            ps.popPose();
        }

        MeshData mesh = fill.build();
        if (mesh != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferUploader.drawWithShader(mesh);
            RenderSystem.disableBlend();
        }
        GHOST_TESS.clear();

        // ── Outlines ──────────────────────────────────────────────────────────
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buf.getBuffer(RenderType.lines());

        for (BlockPos local : blocks.keySet()) {
            BlockPos world = origin.offset(local);
            double dx = world.getX() - cam.x;
            double dy = world.getY() - cam.y;
            double dz = world.getZ() - cam.z;

            ps.pushPose();
            ps.translate(dx, dy, dz);
            LevelRenderer.renderLineBox(ps, lines, new AABB(0, 0, 0, 1, 1, 1),
                    fr, fg, fb, LINE_A);
            ps.popPose();
        }

        buf.endBatch(RenderType.lines());
    }

    /** 24 vertices (6 faces × 4) for an axis-aligned box, POSITION_COLOR format. */
    private static void addBox(BufferBuilder b, Matrix4f m,
                               float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               float r, float g, float bl, float a) {
        // Bottom
        b.addVertex(m,x1,y1,z1).setColor(r,g,bl,a); b.addVertex(m,x2,y1,z1).setColor(r,g,bl,a);
        b.addVertex(m,x2,y1,z2).setColor(r,g,bl,a); b.addVertex(m,x1,y1,z2).setColor(r,g,bl,a);
        // Top
        b.addVertex(m,x1,y2,z1).setColor(r,g,bl,a); b.addVertex(m,x1,y2,z2).setColor(r,g,bl,a);
        b.addVertex(m,x2,y2,z2).setColor(r,g,bl,a); b.addVertex(m,x2,y2,z1).setColor(r,g,bl,a);
        // North
        b.addVertex(m,x1,y1,z1).setColor(r,g,bl,a); b.addVertex(m,x1,y2,z1).setColor(r,g,bl,a);
        b.addVertex(m,x2,y2,z1).setColor(r,g,bl,a); b.addVertex(m,x2,y1,z1).setColor(r,g,bl,a);
        // South
        b.addVertex(m,x1,y1,z2).setColor(r,g,bl,a); b.addVertex(m,x2,y1,z2).setColor(r,g,bl,a);
        b.addVertex(m,x2,y2,z2).setColor(r,g,bl,a); b.addVertex(m,x1,y2,z2).setColor(r,g,bl,a);
        // West
        b.addVertex(m,x1,y1,z1).setColor(r,g,bl,a); b.addVertex(m,x1,y1,z2).setColor(r,g,bl,a);
        b.addVertex(m,x1,y2,z2).setColor(r,g,bl,a); b.addVertex(m,x1,y2,z1).setColor(r,g,bl,a);
        // East
        b.addVertex(m,x2,y1,z1).setColor(r,g,bl,a); b.addVertex(m,x2,y2,z1).setColor(r,g,bl,a);
        b.addVertex(m,x2,y2,z2).setColor(r,g,bl,a); b.addVertex(m,x2,y1,z2).setColor(r,g,bl,a);
    }
}
