package com.github.nalamodikk.client.projection;

import com.github.nalamodikk.KoniavacraftMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

/**
 * Handles the ghost-block projection overlay.
 *
 * Lifecycle:
 *   JEI "Project" button → GhostProjectionState.activate(blocks)
 *   Each client tick       → update origin from player look raytrace
 *   Each frame             → render translucent ghost blocks + blue outlines
 *   Right-click / Escape   → GhostProjectionState.deactivate()
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class GhostProjectionHandler {

    private static final float FILL_R = 0.5f, FILL_G = 0.7f, FILL_B = 1.0f, FILL_A = 0.35f;
    private static final float LINE_R = 0.3f, LINE_G = 0.6f, LINE_B = 1.0f, LINE_A = 0.8f;
    private static final double REACH = 32.0;

    private GhostProjectionHandler() {}

    // ── Origin tracking ────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Pre event) {
        if (!GhostProjectionState.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        HitResult hit = mc.player.pick(REACH, 1.0f, false);
        BlockPos newOrigin;
        if (hit instanceof BlockHitResult blockHit) {
            // Place structure origin on the surface the player is looking at
            newOrigin = blockHit.getBlockPos().relative(blockHit.getDirection());
        } else {
            // No block in range — float 10 blocks in front of player
            Vec3 eye = mc.player.getEyePosition();
            Vec3 look = mc.player.getLookAngle().scale(10.0);
            newOrigin = BlockPos.containing(eye.add(look));
        }
        GhostProjectionState.setOrigin(newOrigin);
    }

    // ── Dismiss input ──────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (!GhostProjectionState.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return; // let GUIs handle their own Escape
        if (event.getKey() == GLFW.GLFW_KEY_ESCAPE || event.getKey() == GLFW.GLFW_KEY_E) {
            GhostProjectionState.deactivate();
        }
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (!GhostProjectionState.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            GhostProjectionState.deactivate();
            // Right-click is NOT cancelled — block interaction still proceeds normally.
        }
    }

    // ── World rendering ────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        if (!GhostProjectionState.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        Map<BlockPos, BlockState> blocks = GhostProjectionState.getBlocks();
        if (blocks.isEmpty()) return;

        BlockPos origin       = GhostProjectionState.getOrigin();
        Vec3 cam              = event.getCamera().getPosition();
        PoseStack ps          = event.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        RandomSource rng      = RandomSource.create(42L);

        VertexConsumer fillConsumer    = buf.getBuffer(RenderType.translucent());
        VertexConsumer outlineConsumer = buf.getBuffer(RenderType.lines());

        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos localPos = entry.getKey();
            BlockState state  = entry.getValue();
            BlockPos world    = origin.offset(localPos);

            double tx = world.getX() - cam.x;
            double ty = world.getY() - cam.y;
            double tz = world.getZ() - cam.z;

            ps.pushPose();
            ps.translate(tx, ty, tz);

            // ── Translucent fill ────────────────────────────────────────────
            var model = mc.getBlockRenderer().getBlockModel(state);
            for (Direction dir : Direction.values()) {
                for (BakedQuad q : model.getQuads(state, dir, rng, ModelData.EMPTY, null))
                    fillConsumer.putBulkData(ps.last(), q, FILL_R, FILL_G, FILL_B, FILL_A,
                            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            for (BakedQuad q : model.getQuads(state, null, rng, ModelData.EMPTY, null))
                fillConsumer.putBulkData(ps.last(), q, FILL_R, FILL_G, FILL_B, FILL_A,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

            // ── Outline box ─────────────────────────────────────────────────
            net.minecraft.client.renderer.LevelRenderer.renderLineBox(
                    ps, outlineConsumer, new AABB(0, 0, 0, 1, 1, 1),
                    LINE_R, LINE_G, LINE_B, LINE_A);

            ps.popPose();
        }

        buf.endBatch(RenderType.translucent());
        buf.endBatch(RenderType.lines());
    }
}
