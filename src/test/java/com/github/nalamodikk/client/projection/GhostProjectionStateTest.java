package com.github.nalamodikk.client.projection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GhostProjectionState 純 JUnit 單元測試。
 * 不需要遊戲世界或 GPU，只測狀態邏輯。
 */
class GhostProjectionStateTest {

    @BeforeEach
    void resetState() {
        // 每個測試前確保狀態乾淨
        GhostProjectionState.deactivate();
    }

    @AfterEach
    void cleanUp() {
        GhostProjectionState.deactivate();
    }

    // ── 初始狀態 ──────────────────────────────────────────────────────────────

    @Test
    void initiallyInactive() {
        assertFalse(GhostProjectionState.isActive(), "初始狀態應為未激活");
    }

    @Test
    void initialBlocksAreEmpty() {
        assertTrue(GhostProjectionState.getBlocks().isEmpty(), "初始 blocks 應為空");
    }

    // ── activate() ────────────────────────────────────────────────────────────

    @Test
    void activateSetsActiveTrue() {
        GhostProjectionState.activate(Map.of());
        assertTrue(GhostProjectionState.isActive(), "activate() 後應為激活狀態");
    }

    @Test
    void activateStoresBlocks() {
        BlockState stone = Blocks.STONE.defaultBlockState();
        Map<BlockPos, BlockState> input = Map.of(BlockPos.ZERO, stone);

        GhostProjectionState.activate(input);

        Map<BlockPos, BlockState> stored = GhostProjectionState.getBlocks();
        assertEquals(1, stored.size(), "應存儲 1 個 block");
        assertEquals(stone, stored.get(BlockPos.ZERO), "BlockPos.ZERO 應對應石頭方塊");
    }

    @Test
    void activateDefensiveCopiesInput() {
        // 確保外部修改不影響 State 內部
        java.util.Map<BlockPos, BlockState> mutable = new java.util.HashMap<>();
        mutable.put(BlockPos.ZERO, Blocks.DIRT.defaultBlockState());
        GhostProjectionState.activate(mutable);

        // 外部修改
        mutable.put(new BlockPos(1, 0, 0), Blocks.GRASS_BLOCK.defaultBlockState());

        assertEquals(1, GhostProjectionState.getBlocks().size(),
            "外部修改 input Map 不應影響 State 內部的 blocks");
    }

    // ── deactivate() ──────────────────────────────────────────────────────────

    @Test
    void deactivateSetsActiveFalse() {
        GhostProjectionState.activate(Map.of());
        GhostProjectionState.deactivate();
        assertFalse(GhostProjectionState.isActive(), "deactivate() 後應為未激活狀態");
    }

    @Test
    void deactivateClearsBlocks() {
        GhostProjectionState.activate(Map.of(BlockPos.ZERO, Blocks.STONE.defaultBlockState()));
        GhostProjectionState.deactivate();
        assertTrue(GhostProjectionState.getBlocks().isEmpty(),
            "deactivate() 後 blocks 應清空");
    }

    @Test
    void deactivateWhenAlreadyInactiveIsSafe() {
        // 雙重 deactivate 不應拋出例外
        assertDoesNotThrow(() -> {
            GhostProjectionState.deactivate();
            GhostProjectionState.deactivate();
        });
    }

    // ── getBlocks() 在未激活時 ────────────────────────────────────────────────

    @Test
    void getBlocksReturnsEmptyWhenInactive() {
        GhostProjectionState.activate(Map.of(BlockPos.ZERO, Blocks.STONE.defaultBlockState()));
        GhostProjectionState.deactivate();
        assertTrue(GhostProjectionState.getBlocks().isEmpty(),
            "未激活時 getBlocks() 應回傳空 Map");
    }

    // ── setOrigin / getOrigin ─────────────────────────────────────────────────

    @Test
    void originDefaultsToZero() {
        assertEquals(BlockPos.ZERO, GhostProjectionState.getOrigin(),
            "初始 origin 應為 BlockPos.ZERO");
    }

    @Test
    void setOriginUpdatesOrigin() {
        BlockPos pos = new BlockPos(10, 64, -5);
        GhostProjectionState.setOrigin(pos);
        assertEquals(pos, GhostProjectionState.getOrigin(), "getOrigin() 應回傳設置的 pos");
    }

    @Test
    void setOriginCanBeCalledWhileInactive() {
        assertDoesNotThrow(() -> GhostProjectionState.setOrigin(new BlockPos(1, 2, 3)));
    }

    // ── 重複 activate ────────────────────────────────────────────────────────

    @Test
    void reactivateReplacesBlocks() {
        GhostProjectionState.activate(Map.of(BlockPos.ZERO, Blocks.STONE.defaultBlockState()));
        GhostProjectionState.activate(Map.of(new BlockPos(5, 0, 5), Blocks.DIRT.defaultBlockState()));

        Map<BlockPos, BlockState> blocks = GhostProjectionState.getBlocks();
        assertEquals(1, blocks.size(), "重新 activate 應替換 blocks（不累加）");
        assertNull(blocks.get(BlockPos.ZERO), "舊的 BlockPos.ZERO 不應存在");
        assertNotNull(blocks.get(new BlockPos(5, 0, 5)), "新的 (5,0,5) 應存在");
    }
}
