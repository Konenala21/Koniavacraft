package com.github.nalamodikk.space.ship;

import net.minecraft.core.BlockPos;

/**
 * ShipEntity(common)拿不到 client 的 ShipMeshCache 型別(存成 Object)，用這個介面在編輯時通知
 * 「這格變了，之後重烤它所在的 section VBO」而不必整艘重建。實作在 client 的 ShipMeshCache(分塊版)。
 */
public interface ShipMeshHandle {
    /** local = 變動方塊在 contraption 的座標。只重烤該 section + 邊界鄰居 section。 */
    void markDirty(BlockPos local);
}
