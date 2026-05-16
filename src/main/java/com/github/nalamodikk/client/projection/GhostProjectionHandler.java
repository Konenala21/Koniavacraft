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
 * Renders the ghost projection overlay in the world.
 *
 * Render approach (why NOT using bufferSource.getBuffer(RenderType.translucent)):
 *   At AFTER_TRANSLUCENT_BLOCKS the shared entity BufferSource has already had its
 *   translucent batch built and consumed (building=false). Re-acquiring the same buffer
 *   can throw "Not building!" inside BufferBuilder.ensureBuilding().
 *
 *   Fix: use a private Tesselator (own ByteBufferBuilder) with POSITION_COLOR format
 *   for the fill quads.  Lines are safe at this stage because the selection highlight
 *   is drawn later in the frame.
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class GhostProjectionHandler {

    private static final float FILL_R = 0.5f, FILL_G = 0.7f, FILL_B = 1.0f, FILL_A = 0.30f;
    private static final float LINE_R = 0.3f, LINE_G = 0.6f, LINE_B = 1.0f, LINE_A = 0.85f;
    private static final double REACH = 32.0;

    /** Private tesselator — avoids any conflict with the game's shared singleton. */
    private static final Tesselator GHOST_TESS = new Tesselator(65536);

    private GhostProjectionHandler() {}

    // ── Origin tracking (one raytrace per tick) ────────────────────────────────

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Pre event) {
        if (!GhostProjectionState.isActive()) return;
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

    // ── Dismiss inputs ─────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (!GhostProjectionState.isActive()) return;
        if (Minecraft.getInstance().screen != null) return;
        if (event.getKey() == GLFW.GLFW_KEY_ESCAPE || event.getKey() == GLFW.GLFW_KEY_E) {
            GhostProjectionState.deactivate();
        }
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (!GhostProjectionState.isActive()) return;
        if (Minecraft.getInstance().screen != null) return;
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            GhostProjectionState.deactivate();
        }
    }

    // ── World rendering ────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // AFTER_TRANSLUCENT_BLOCKS: entity bufferSource is idle, lines batch not yet started.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!GhostProjectionState.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        Map<BlockPos, net.minecraft.world.level.block.state.BlockState> blocks =
                GhostProjectionState.getBlocks();
        if (blocks.isEmpty()) return;

        BlockPos origin = GhostProjectionState.getOrigin();
        Vec3      cam   = event.getCamera().getPosition();
        PoseStack ps    = event.getPoseStack();

        // ── Translucent fill (POSITION_COLOR, private Tesselator) ─────────────
        BufferBuilder fill = GHOST_TESS.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (BlockPos local : blocks.keySet()) {
            BlockPos world = origin.offset(local);
            float tx = (float)(world.getX() - cam.x);
            float ty = (float)(world.getY() - cam.y);
            float tz = (float)(world.getZ() - cam.z);

            ps.pushPose();
            ps.translate(tx, ty, tz);
            addBox(fill, ps.last().pose(), 0, 0, 0, 1, 1, 1, FILL_R, FILL_G, FILL_B, FILL_A);
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

        // ── Outline boxes (RenderType.lines, safe at this stage) ─────────────
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
                    LINE_R, LINE_G, LINE_B, LINE_A);
            ps.popPose();
        }

        buf.endBatch(RenderType.lines());
    }

    /** Adds 6 faces (24 vertices) of an axis-aligned box to a POSITION_COLOR buffer. */
    private static void addBox(BufferBuilder b, Matrix4f m,
                               float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               float r, float g, float bl, float a) {
        // Bottom (Y=y1)
        b.addVertex(m, x1, y1, z1).setColor(r, g, bl, a);
        b.addVertex(m, x2, y1, z1).setColor(r, g, bl, a);
        b.addVertex(m, x2, y1, z2).setColor(r, g, bl, a);
        b.addVertex(m, x1, y1, z2).setColor(r, g, bl, a);
        // Top (Y=y2)
        b.addVertex(m, x1, y2, z1).setColor(r, g, bl, a);
        b.addVertex(m, x1, y2, z2).setColor(r, g, bl, a);
        b.addVertex(m, x2, y2, z2).setColor(r, g, bl, a);
        b.addVertex(m, x2, y2, z1).setColor(r, g, bl, a);
        // North (Z=z1)
        b.addVertex(m, x1, y1, z1).setColor(r, g, bl, a);
        b.addVertex(m, x1, y2, z1).setColor(r, g, bl, a);
        b.addVertex(m, x2, y2, z1).setColor(r, g, bl, a);
        b.addVertex(m, x2, y1, z1).setColor(r, g, bl, a);
        // South (Z=z2)
        b.addVertex(m, x1, y1, z2).setColor(r, g, bl, a);
        b.addVertex(m, x2, y1, z2).setColor(r, g, bl, a);
        b.addVertex(m, x2, y2, z2).setColor(r, g, bl, a);
        b.addVertex(m, x1, y2, z2).setColor(r, g, bl, a);
        // West (X=x1)
        b.addVertex(m, x1, y1, z1).setColor(r, g, bl, a);
        b.addVertex(m, x1, y1, z2).setColor(r, g, bl, a);
        b.addVertex(m, x1, y2, z2).setColor(r, g, bl, a);
        b.addVertex(m, x1, y2, z1).setColor(r, g, bl, a);
        // East (X=x2)
        b.addVertex(m, x2, y1, z1).setColor(r, g, bl, a);
        b.addVertex(m, x2, y2, z1).setColor(r, g, bl, a);
        b.addVertex(m, x2, y2, z2).setColor(r, g, bl, a);
        b.addVertex(m, x2, y1, z2).setColor(r, g, bl, a);
    }
}
