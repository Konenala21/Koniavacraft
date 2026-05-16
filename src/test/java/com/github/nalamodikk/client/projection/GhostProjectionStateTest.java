package com.github.nalamodikk.client.projection;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GhostProjectionState 純 JUnit 單元測試。
 * 不依賴 BlockState / Blocks — 用 Object 佔位符 + raw Map 繞過 MC registry 初始化。
 * Java 泛型在 runtime 型別擦除，GhostProjectionState 只存 Map 引用，不呼叫 BlockState 方法，
 * 因此 raw Map 完全合法且行為與型別安全版本相同。
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class GhostProjectionStateTest {

    @BeforeEach
    void resetState() {
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
        Object fakeState = new Object();
        Map input = new HashMap();
        input.put(BlockPos.ZERO, fakeState);

        GhostProjectionState.activate(input);

        Map stored = GhostProjectionState.getBlocks();
        assertEquals(1, stored.size(), "應存儲 1 個 block");
        assertSame(fakeState, stored.get(BlockPos.ZERO), "BlockPos.ZERO 應對應傳入的 state 物件");
    }

    @Test
    void activateDefensiveCopiesInput() {
        Map mutable = new HashMap();
        Object stateA = new Object();
        mutable.put(BlockPos.ZERO, stateA);
        GhostProjectionState.activate(mutable);

        mutable.put(new BlockPos(1, 0, 0), new Object());

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
        Map blocks = new HashMap();
        blocks.put(BlockPos.ZERO, new Object());
        GhostProjectionState.activate(blocks);
        GhostProjectionState.deactivate();
        assertTrue(GhostProjectionState.getBlocks().isEmpty(), "deactivate() 後 blocks 應清空");
    }

    @Test
    void deactivateResetsOrigin() {
        GhostProjectionState.setOrigin(new BlockPos(5, 10, 15));
        GhostProjectionState.deactivate();
        assertEquals(BlockPos.ZERO, GhostProjectionState.getOrigin(), "deactivate() 後 origin 應重置為 ZERO");
    }

    @Test
    void deactivateWhenAlreadyInactiveIsSafe() {
        assertDoesNotThrow(() -> {
            GhostProjectionState.deactivate();
            GhostProjectionState.deactivate();
        });
    }

    // ── getBlocks() 在未激活時 ────────────────────────────────────────────────

    @Test
    void getBlocksReturnsEmptyWhenInactive() {
        Map blocks = new HashMap();
        blocks.put(BlockPos.ZERO, new Object());
        GhostProjectionState.activate(blocks);
        GhostProjectionState.deactivate();
        assertTrue(GhostProjectionState.getBlocks().isEmpty(), "未激活時 getBlocks() 應回傳空 Map");
    }

    // ── setOrigin / getOrigin ─────────────────────────────────────────────────

    @Test
    void originDefaultsToZero() {
        assertEquals(BlockPos.ZERO, GhostProjectionState.getOrigin(), "初始 origin 應為 BlockPos.ZERO");
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
        Map first = new HashMap();
        first.put(BlockPos.ZERO, new Object());
        GhostProjectionState.activate(first);

        Map second = new HashMap();
        second.put(new BlockPos(5, 0, 5), new Object());
        GhostProjectionState.activate(second);

        Map stored = GhostProjectionState.getBlocks();
        assertEquals(1, stored.size(), "重新 activate 應替換 blocks（不累加）");
        assertNull(stored.get(BlockPos.ZERO), "舊的 BlockPos.ZERO 不應存在");
        assertNotNull(stored.get(new BlockPos(5, 0, 5)), "新的 (5,0,5) 應存在");
    }
}
