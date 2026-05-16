package com.github.nalamodikk.client.projection;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarBlockEntity;
import com.github.nalamodikk.register.client.ModKeyMappings;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
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
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Map;

/**
 * Ghost projection overlay — input & render handler.
 *
 * ─── Modes ───────────────────────────────────────────────────────────────────
 * PLACING  Ghost follows player look every tick (blue tint).
 * PLACED   Ghost frozen at locked position (no tint = natural); player free.
 *
 * ─── Controls ────────────────────────────────────────────────────────────────
 *   Shift held (PLACING)    → short reach (REACH_CLOSE) — ghost snaps to 6 blocks
 *   PRJ button (JEI)        → activate / deactivate (only way to close)
 *   All mouse / keyboard    → NOT intercepted; player interacts normally
 *
 * ─── Render ──────────────────────────────────────────────────────────────────
 * Uses a dedicated ByteBufferBuilder (GHOST_BB) + MultiBufferSource.immediate()
 * to render real block textures semi-transparently via RenderType.translucent().
 *
 * Why not mc.renderBuffers().bufferSource().getBuffer(translucent):
 *   At AFTER_TRANSLUCENT_BLOCKS the entity BufferSource has already flushed;
 *   the translucent BufferBuilder is building=false → "Not building!" crash.
 *   Creating our own BufferSource from a fresh ByteBufferBuilder avoids this.
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class GhostProjectionHandler {

    // PLACING outline: blue; PLACED outline: green; conflict outline: red
    private static final float PL_R = 0.3f, PL_G = 0.6f, PL_B = 1.0f;
    private static final float FX_R = 0.2f, FX_G = 0.9f, FX_B = 0.3f;
    private static final float CF_R = 1.0f, CF_G = 0.2f, CF_B = 0.2f;
    private static final float LINE_A = 0.9f;

    // Ghost alpha and PLACING colour tint (PLACED = no tint)
    private static final float GHOST_ALPHA   = 0.45f;
    private static final float PLACING_TINT  = 0.75f; // RGB multiplier for PLACING mode

    private static final double REACH       = 32.0;
    private static final double REACH_CLOSE =  6.0; // Shift held → pull ghost closer

    /**
     * Dedicated byte buffer for ghost block rendering.
     * 2 MB is enough for ~2600 blocks (24 verts × 32 bytes × 2600 ≈ 2 MB).
     * Reused each frame — cleared after endBatch().
     */
    private static final ByteBufferBuilder GHOST_BB = new ByteBufferBuilder(2 * 1024 * 1024);

    private GhostProjectionHandler() {}

    // ── Origin tracking (PLACING mode only) ───────────────────────────────────

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Pre event) {
        if (!GhostProjectionState.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;

        // G 鍵切換 PLACING ↔ PLACED
        if (ModKeyMappings.GHOST_LOCK.consumeClick()) {
            if (GhostProjectionState.getMode() == GhostProjectionState.Mode.PLACING) {
                boolean anyBlocked = false;
                BlockPos org = GhostProjectionState.getOrigin();
                for (Map.Entry<BlockPos, BlockState> e : GhostProjectionState.getBlocks().entrySet()) {
                    BlockState existing = mc.level.getBlockState(org.offset(e.getKey()));
                    if (!existing.canBeReplaced() && !existing.is(e.getValue().getBlock())) {
                        anyBlocked = true;
                        break;
                    }
                }
                if (!anyBlocked) GhostProjectionState.lockPosition();
            } else {
                GhostProjectionState.unlock();
            }
        }

        // PLACED 模式：altar 已成形 → 自動關閉投影
        if (GhostProjectionState.getMode() == GhostProjectionState.Mode.PLACED) {
            if (mc.level.getBlockEntity(GhostProjectionState.getOrigin())
                    instanceof AspectAltarBlockEntity altar && altar.isFormed()) {
                GhostProjectionState.deactivate();
                return;
            }
            return;
        }

        double reach = mc.player.isShiftKeyDown() ? REACH_CLOSE : REACH;
        HitResult hit = mc.player.pick(reach, 1.0f, false);
        BlockPos newOrigin;
        if (hit instanceof BlockHitResult blockHit) {
            newOrigin = blockHit.getBlockPos().relative(blockHit.getDirection());
        } else {
            Vec3 eye  = mc.player.getEyePosition();
            Vec3 look = mc.player.getLookAngle().scale(reach);
            newOrigin = BlockPos.containing(eye.add(look));
        }
        GhostProjectionState.setOrigin(newOrigin);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    // ── World rendering ────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!GhostProjectionState.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        Map<BlockPos, BlockState> blocks = GhostProjectionState.getBlocks();
        if (blocks.isEmpty()) return;

        boolean placing = GhostProjectionState.getMode() == GhostProjectionState.Mode.PLACING;

        // Colour tint: slight blue in PLACING, neutral (1,1,1) when PLACED
        float tr = placing ? PLACING_TINT : 1.0f;
        float tg = placing ? PLACING_TINT : 1.0f;
        float tb = placing ? 1.0f : 1.0f;   // blue channel always full for PLACING tint

        BlockPos origin  = GhostProjectionState.getOrigin();
        Vec3 cam         = event.getCamera().getPosition();
        PoseStack ps     = event.getPoseStack();
        var dispatcher   = mc.getBlockRenderer();
        RandomSource rng = RandomSource.create(42L);

        // Fresh BufferSource — no "Not building!" crash (separate from entity bufferSource)
        MultiBufferSource.BufferSource ghostSrc = MultiBufferSource.immediate(GHOST_BB);

        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos local   = entry.getKey();
            BlockState state = entry.getValue();
            BlockPos world   = origin.offset(local);

            // PLACED 模式才判斷「已完成」；PLACING 模式位置未鎖定，不跳過
            if (!placing && mc.level != null && mc.level.getBlockState(world).is(state.getBlock())) continue;

            double dx = world.getX() - cam.x;
            double dy = world.getY() - cam.y;
            double dz = world.getZ() - cam.z;

            ps.pushPose();
            ps.translate(dx, dy, dz);

            try {
                VertexConsumer consumer = ghostSrc.getBuffer(RenderType.translucent());
                var model = dispatcher.getBlockModel(state);
                var pose  = ps.last();

                // Render all quads with semi-transparency and optional colour tint
                for (BakedQuad q : model.getQuads(state, null, rng, ModelData.EMPTY, null))
                    consumer.putBulkData(pose, q, tr, tg, tb, GHOST_ALPHA,
                            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

                for (Direction dir : Direction.values())
                    for (BakedQuad q : model.getQuads(state, dir, rng, ModelData.EMPTY, null))
                        consumer.putBulkData(pose, q, tr, tg, tb, GHOST_ALPHA,
                                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

            } catch (Exception e) {
                KoniavacraftMod.LOGGER.warn("[GhostProjection] block render failed for {}: {}",
                        state, e.getMessage());
            }

            ps.popPose();
        }

        // Draw all ghost blocks, then reset the buffer for next frame
        try {
            ghostSrc.endBatch(RenderType.translucent());
        } finally {
            GHOST_BB.clear();
        }

        // ── Outline boxes — per-block colour: conflict=red, placing=blue, placed=green ───────────────────────────────
        MultiBufferSource.BufferSource mainBuf = mc.renderBuffers().bufferSource();
        VertexConsumer lines = mainBuf.getBuffer(RenderType.lines());

        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos local    = entry.getKey();
            BlockState needed = entry.getValue();
            BlockPos world    = origin.offset(local);

            // PLACED 模式才消除已完成格的輪廓；PLACING 模式全部顯示（位置未鎖定）
            if (!placing && mc.level != null && mc.level.getBlockState(world).is(needed.getBlock())) continue;

            boolean blocked = mc.level != null && !mc.level.getBlockState(world).canBeReplaced();
            float lr = blocked ? CF_R : placing ? PL_R : FX_R;
            float lg = blocked ? CF_G : placing ? PL_G : FX_G;
            float lb = blocked ? CF_B : placing ? PL_B : FX_B;

            double dx = world.getX() - cam.x;
            double dy = world.getY() - cam.y;
            double dz = world.getZ() - cam.z;

            ps.pushPose();
            ps.translate(dx, dy, dz);
            LevelRenderer.renderLineBox(ps, lines, new AABB(0, 0, 0, 1, 1, 1), lr, lg, lb, LINE_A);
            ps.popPose();
        }

        mainBuf.endBatch(RenderType.lines());
    }
}
