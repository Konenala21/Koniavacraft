package com.github.nalamodikk.space.ship;

/**
 * ShipEntity(common)拿不到 client 的 ShipMeshCache 型別(存成 Object)，用這個介面在編輯時通知
 * 「方塊變了，之後重烤 VBO」而不必每次砍掉重建。實作在 client 的 ShipMeshCache。
 */
public interface ShipMeshHandle {
    void markDirty();
}
