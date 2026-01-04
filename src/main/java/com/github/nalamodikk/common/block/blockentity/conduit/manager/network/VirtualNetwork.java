// 🎯 第一步：創建這個新文件
// 文件位置：src/main/java/com/github/nalamodikk/common/block/conduit/SimpleVirtualNetwork.java

package com.github.nalamodikk.common.block.blockentity.conduit.manager.network;

import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlockEntity;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VirtualNetwork {
    private static final Logger LOGGER = LogUtils.getLogger();

    // 共享的魔力池
    private final ManaStorage sharedManaPool = new ManaStorage(10000);

    // 記錄哪些導管在這個網路中
    private final Set<BlockPos> connectedConduits = new HashSet<>();
    private final Map<BlockPos, ArcaneConduitBlockEntity> conduitMap = new HashMap<>();

    public VirtualNetwork() {
        LOGGER.info("Created simple virtual network");
    }

    public void setTotalManaStored(int amount) {
        sharedManaPool.setMana(Math.max(0, Math.min(amount, sharedManaPool.getMaxManaStored())));
        LOGGER.info("Virtual network mana set to: {}", sharedManaPool.getManaStored());
    }

    // 🆕 添加 getter 方法
    public int getMaxManaStored() {
        return sharedManaPool.getMaxManaStored();
    }

    // 🆕 添加獲取連接導管的方法
    public Set<BlockPos> getConnectedConduits() {
        return new HashSet<>(connectedConduits); // 返回副本，避免外部修改
    }

    // 🆕 添加網路信息日誌
    public void logNetworkInfo() {
        LOGGER.info("Virtual Network - Mana: {}/{}, Conduits: {}",
                sharedManaPool.getManaStored(),
                sharedManaPool.getMaxManaStored(),
                connectedConduits.size());
    }

    /**
     * 導管加入網路
     */
    public void addConduit(ArcaneConduitBlockEntity conduit) {
        BlockPos pos = conduit.getBlockPos();
        connectedConduits.add(pos);
        conduitMap.put(pos, conduit);

        // 把導管的魔力合併到共享池
        int conduitMana = conduit.getBufferManaStored(); // 我們需要添加這個方法
        if (conduitMana > 0) {
            sharedManaPool.receiveMana(conduitMana, ManaAction.EXECUTE);
            conduit.setBufferMana(0); // 清空導管自己的魔力
        }

        LOGGER.debug("Added conduit {} to network. Total conduits: {}", pos, connectedConduits.size());
    }

    /**
     * 導管離開網路
     */
    public void removeConduit(BlockPos pos) {
        if (connectedConduits.remove(pos)) {
            conduitMap.remove(pos);
            LOGGER.debug("Removed conduit {} from network. Remaining: {}", pos, connectedConduits.size());
        }
    }

    /**
     * 從網路提取魔力
     */
    public int extractManaFromNetwork(int maxExtract, ManaAction action) { // ✅ 添加 action 參數
        return sharedManaPool.extractMana(maxExtract, action); // ✅ 使用正確的 action
    }

    /**
     * 向網路存入魔力
     */
    public int receiveManaToNetwork(int maxReceive, ManaAction action) { // ✅ 添加 action 參數
        return sharedManaPool.receiveMana(maxReceive, action); // ✅ 使用正確的 action
    }

    /**
     * 獲取網路總魔力
     */
    public int getTotalManaStored() {
        return sharedManaPool.getManaStored();
    }

    /**
     * 獲取網路總容量
     */
    public int getTotalManaCapacity() {
        return sharedManaPool.getMaxManaStored();
    }

    /**
     * 檢查導管是否在網路中
     */
    public boolean contains(BlockPos pos) {
        return connectedConduits.contains(pos);
    }

    /**
     * 獲取網路大小
     */
    public int getNetworkSize() {
        return connectedConduits.size();
    }
}