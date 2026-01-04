package com.github.nalamodikk.common.block.blockentity.conduit;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * 導管等級系統
 * 定義不同等級導管的傳輸速率和容量
 */
public enum ConduitTier implements StringRepresentable {
    BASIC("basic", 100, 100, 0x4A90E2),           // 基礎：100/tick，100容量，藍色
    ADVANCED("advanced", 500, 500, 0x9B59B6),     // 進階：500/tick，500容量，紫色
    ELITE("elite", 2000, 2000, 0xF39C12);         // 精英：2000/tick，2000容量，金色

    private final String name;
    private final int transferRate;      // 每tick最大傳輸量
    private final int bufferCapacity;    // 緩衝區容量
    private final int particleColor;     // 粒子顏色（RGB hex）

    ConduitTier(String name, int transferRate, int bufferCapacity, int particleColor) {
        this.name = name;
        this.transferRate = transferRate;
        this.bufferCapacity = bufferCapacity;
        this.particleColor = particleColor;
    }

    /**
     * 獲取傳輸速率
     */
    public int getTransferRate() {
        return transferRate;
    }

    /**
     * 獲取緩衝區容量
     */
    public int getBufferCapacity() {
        return bufferCapacity;
    }

    /**
     * 獲取粒子顏色
     */
    public int getParticleColor() {
        return particleColor;
    }

    /**
     * 獲取顯示名稱（用於翻譯鍵）
     */
    public String getDisplayName() {
        return "tier.koniava.conduit." + name;
    }

    /**
     * 從索引獲取等級（用於升級系統）
     */
    public static ConduitTier fromIndex(int index) {
        ConduitTier[] values = values();
        if (index < 0 || index >= values.length) {
            return BASIC;
        }
        return values[index];
    }

    /**
     * 獲取下一等級（用於升級）
     */
    public ConduitTier getNext() {
        int nextOrdinal = this.ordinal() + 1;
        if (nextOrdinal >= values().length) {
            return this; // 已經是最高等級
        }
        return values()[nextOrdinal];
    }

    /**
     * 檢查是否有下一等級
     */
    public boolean hasNext() {
        return this.ordinal() < values().length - 1;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    /**
     * 從字串解析等級
     */
    public static ConduitTier fromString(String name) {
        for (ConduitTier tier : values()) {
            if (tier.name.equals(name)) {
                return tier;
            }
        }
        return BASIC; // 預設為基礎等級
    }
}
