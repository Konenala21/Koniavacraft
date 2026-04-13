package com.github.nalamodikk.common.block.blockentity.conduit.manager.core;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;

/**
 * 導管統計管理器
 * 負責追蹤傳輸統計、活動狀態和性能監控
 */
public class StatsManager {

    // === 常量 ===
    private static final int IDLE_THRESHOLD = 600; // 30秒無活動視為閒置

    // === 統計數據 ===
    private final EnumMap<Direction, TransferStats> transferStats = new EnumMap<>(Direction.class);
    private long lastActivity;
    private boolean isIdle = false;
    private long tickCounter = 0;

    // === 建構子 ===
    public StatsManager() {
        for (Direction dir : Direction.values()) {
            transferStats.put(dir, new TransferStats());
        }
        this.lastActivity = 0;
    }

    // === 傳輸統計 ===

    /**
     * 記錄傳輸事件
     */
    public void recordTransfer(Direction direction, int amount, boolean success, long gameTick) {
        TransferStats stats = transferStats.get(direction);
        if (stats != null) {
            stats.recordTransfer(amount, success, gameTick);
            lastActivity = tickCounter;
            isIdle = false;
        }
    }

    /**
     * 獲取指定方向的傳輸統計
     */
    public TransferStats getTransferStats(Direction direction) {
        return transferStats.get(direction);
    }

    /**
     * 獲取所有傳輸統計的副本
     */
    public EnumMap<Direction, TransferStats> getAllTransferStats() {
        return new EnumMap<>(transferStats);
    }

    /**
     * 獲取傳輸歷史（用於渲染）
     */
    public int getTransferHistory(Direction direction) {
        TransferStats stats = transferStats.get(direction);
        return stats != null ? stats.totalTransferred : 0;
    }

    // === 活動狀態管理 ===

    /**
     * 更新tick計數器
     */
    public void tick() {
        tickCounter++;
        updateIdleStatus();
    }

    /**
     * 記錄活動
     */
    public void recordActivity() {
        lastActivity = tickCounter;
        isIdle = false;
    }

    /**
     * 檢查是否閒置
     */
    public boolean isIdle() {
        return isIdle;
    }

    /**
     * 獲取最後活動時間
     */
    public long getLastActivity() {
        return lastActivity;
    }

    /**
     * 獲取tick計數器
     */
    public long getTickCounter() {
        return tickCounter;
    }

    /**
     * 更新閒置狀態
     */
    private void updateIdleStatus() {
        isIdle = (tickCounter - lastActivity) > IDLE_THRESHOLD;
    }

    // === 維護和清理 ===

    /**
     * 執行定期維護
     */
    public void performMaintenance() {
        // 衰減長時間無傳輸的統計 (6000 ticks = 5 分鐘)
        transferStats.values().forEach(stats -> {
            if (tickCounter - stats.lastTransfer > 6000) {
                stats.averageRate *= 0.8;
            }
        });
    }

    /**
     * 重置所有統計
     */
    public void resetAllStats() {
        for (TransferStats stats : transferStats.values()) {
            stats.reset();
        }
        lastActivity = tickCounter;
        isIdle = false;
    }

    // === NBT 序列化 ===

    /**
     * 保存到NBT
     */
    public void saveToNBT(CompoundTag tag) {
        // 保存統計數據
        CompoundTag statsTag = new CompoundTag();
        for (var entry : transferStats.entrySet()) {
            CompoundTag dirStats = new CompoundTag();
            TransferStats stats = entry.getValue();
            dirStats.putInt("Total", stats.totalTransferred);
            dirStats.putInt("Success", stats.successfulTransfers);
            dirStats.putInt("Failed", stats.failedTransfers);
            dirStats.putDouble("Rate", stats.averageRate);
            dirStats.putLong("LastTransfer", stats.lastTransfer);
            statsTag.put(entry.getKey().name(), dirStats);
        }
        tag.put("Stats", statsTag);

        tag.putLong("TickCounter", tickCounter);
        tag.putLong("LastActivity", lastActivity);
    }

    /**
     * 從NBT載入
     */
    public void loadFromNBT(CompoundTag tag) {
        // 載入統計數據
        if (tag.contains("Stats")) {
            CompoundTag statsTag = tag.getCompound("Stats");
            for (Direction dir : Direction.values()) {
                if (statsTag.contains(dir.name())) {
                    CompoundTag dirStats = statsTag.getCompound(dir.name());
                    TransferStats stats = transferStats.get(dir);
                    stats.totalTransferred = dirStats.getInt("Total");
                    stats.successfulTransfers = dirStats.getInt("Success");
                    stats.failedTransfers = dirStats.getInt("Failed");
                    stats.averageRate = dirStats.getDouble("Rate");
                    stats.lastTransfer = dirStats.getLong("LastTransfer");
                }
            }
        }

        tickCounter = tag.getLong("TickCounter");
        lastActivity = tag.getLong("LastActivity");
        updateIdleStatus();
    }

    // === 內部類：傳輸統計 ===

    public static class TransferStats {
        public int totalTransferred = 0;
        public int successfulTransfers = 0;
        public int failedTransfers = 0;
        public long lastTransfer = 0;
        public double averageRate = 0.0;

        public void recordTransfer(int amount, boolean success, long gameTick) {
            if (success) {
                totalTransferred += amount;
                successfulTransfers++;
                averageRate = averageRate * 0.9 + amount * 0.1;
            } else {
                failedTransfers++;
            }
            lastTransfer = gameTick;
        }

        public double getReliability() {
            int total = successfulTransfers + failedTransfers;
            return total > 0 ? (double) successfulTransfers / total : 1.0;
        }

        public void reset() {
            totalTransferred = 0;
            successfulTransfers = 0;
            failedTransfers = 0;
            lastTransfer = 0;
            averageRate = 0.0;
        }
    }
}