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
 */
@OnlyIn(Dist.CLIENT)
public final class GhostProjectionState {

    private static boolean active = false;
    private static Map<BlockPos, BlockState> blocks = Collections.emptyMap();
    /** World-space origin; all block offsets in {@code blocks} are relative to this. */
    private static BlockPos origin = BlockPos.ZERO;

    private GhostProjectionState() {}

    public static void activate(Map<BlockPos, BlockState> structureBlocks) {
        blocks = new HashMap<>(structureBlocks);
        active = true;
    }

    public static void deactivate() {
        active = false;
        blocks = Collections.emptyMap();
    }

    public static boolean isActive()                       { return active; }
    public static Map<BlockPos, BlockState> getBlocks()    { return active ? blocks : Collections.emptyMap(); }
    public static BlockPos getOrigin()                     { return origin; }
    public static void setOrigin(BlockPos pos)             { origin = pos; }
}
