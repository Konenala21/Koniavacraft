package com.github.nalamodikk.client.projection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Client-side state for the ghost structure projection overlay.
 * All fields are accessed exclusively on the render/game thread.
 *
 * Mode lifecycle:
 *   activate()     → PLACING (follows player look each tick)
 *   lockPosition() → PLACED  (frozen at current origin; player can interact freely)
 *   deactivate()   → inactive, mode reset to PLACING
 */
@OnlyIn(Dist.CLIENT)
public final class GhostProjectionState {

    public enum Mode { PLACING, PLACED }

    private static boolean active = false;
    private static Mode    mode   = Mode.PLACING;
    private static Map<BlockPos, BlockState> blocks = Collections.emptyMap();
    /** World-space origin; all block offsets in {@code blocks} are relative to this. */
    private static BlockPos origin = BlockPos.ZERO;

    private GhostProjectionState() {}

    public static void activate(Map<BlockPos, BlockState> structureBlocks) {
        blocks = new HashMap<>(structureBlocks);
        mode   = Mode.PLACING;
        active = true;
    }

    /** Freeze the ghost at the current origin. Visual changes to green. */
    public static void lockPosition() {
        if (active) mode = Mode.PLACED;
    }

    public static void deactivate() {
        active = false;
        mode   = Mode.PLACING;
        blocks = Collections.emptyMap();
    }

    public static boolean isActive()                       { return active; }
    public static Mode    getMode()                        { return mode; }
    public static Map<BlockPos, BlockState> getBlocks()    { return active ? blocks : Collections.emptyMap(); }
    public static BlockPos getOrigin()                     { return origin; }
    public static void setOrigin(BlockPos pos)             { origin = pos; }
}
