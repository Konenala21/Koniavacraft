package com.github.nalamodikk.common.block.blockentity.mana_generator.logic;

import com.github.nalamodikk.common.utils.upgrade.UpgradeInventory;
import com.github.nalamodikk.common.utils.upgrade.UpgradeType;
import net.minecraft.util.Mth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 🔧 魔力發電機升級效果處理器
 * 
 * 負責計算和應用升級模組的效果：
 * - ACCELERATED_PROCESSING: 加速處理 - 減少燃燒時間，但增加產出速度
 * - EXPANDED_FUEL_CHAMBER: 擴展燃料室 - 增加燃燒時間
 * - CATALYTIC_CONVERTER: 催化轉換器 - 提升燃料效率
 * - DIAGNOSTIC_DISPLAY: 診斷顯示 - 提供詳細統計（UI功能）
 */
public class ManaGeneratorUpgradeHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManaGeneratorUpgradeHandler.class);
    
    private final UpgradeInventory upgradeInventory;
    
    public ManaGeneratorUpgradeHandler(UpgradeInventory upgradeInventory) {
        this.upgradeInventory = upgradeInventory;
    }
    
    /**
     * 🚀 計算加速處理效果 - 影響燃燒速度
     * 每個加速處理升級增加25%的燃燒速度（減少燃燒時間）
     * 
     * @param baseBurnTime 基礎燃燒時間
     * @return 修改後的燃燒時間
     */
    public int getModifiedBurnTime(int baseBurnTime) {
        int acceleratedCount = upgradeInventory.getUpgradeCount(UpgradeType.ACCELERATED_PROCESSING);
        int expandedCount = upgradeInventory.getUpgradeCount(UpgradeType.EXPANDED_FUEL_CHAMBER);
        
        float burnTimeMultiplier = 1.0f;
        
        // 加速處理：每個升級減少20%燃燒時間（增加速度）
        if (acceleratedCount > 0) {
            burnTimeMultiplier *= Math.pow(0.8f, acceleratedCount); // 0.8^n
        }
        
        // 擴展燃料室：每個升級增加50%燃燒時間
        if (expandedCount > 0) {
            burnTimeMultiplier *= Math.pow(1.5f, expandedCount); // 1.5^n
        }
        
        int modifiedTime = (int) (baseBurnTime * burnTimeMultiplier);
        
        // 確保不會太快或太慢
        modifiedTime = Math.max(5, modifiedTime); // 最少5 tick
        modifiedTime = Math.min(12000, modifiedTime); // 最多10分鐘
        
        return modifiedTime;
    }
    
    /**
     * ⚡ 計算催化轉換器效果 - 影響產出效率
     * 每個催化轉換器升級增加30%的產出
     * 
     * @param baseOutput 基礎產出量
     * @return 修改後的產出量
     */
    public int getModifiedOutput(int baseOutput) {
        int catalyticCount = upgradeInventory.getUpgradeCount(UpgradeType.CATALYTIC_CONVERTER);
        
        if (catalyticCount == 0) {
            return baseOutput;
        }
        
        // 每個催化轉換器增加30%產出，但有遞減效果
        float outputMultiplier = 1.0f;
        for (int i = 0; i < catalyticCount; i++) {
            // 第1個30%，第2個25%，第3個20%...最低10%
            float bonus = Math.max(0.1f, 0.35f - i * 0.05f);
            outputMultiplier += bonus;
        }
        
        return (int) (baseOutput * outputMultiplier);
    }
    
    private static final int[] MANA_OUTPUT_BONUS_PER_MK = {25, 50, 80, 120};

    /**
     * Returns the generation multiplier from MANA_OUTPUT Mk upgrades.
     * Stacks additively: each installed slot adds its Mk bonus (%).
     * Capped at 5x total.
     */
    public double getManaOutputMultiplier() {
        int totalBonus = upgradeInventory.getTotalMkBonus(UpgradeType.MANA_OUTPUT, MANA_OUTPUT_BONUS_PER_MK);
        double multiplier = 1.0 + totalBonus / 100.0;
        return Mth.clamp(multiplier, 1.0, 5.0);
    }

    /**
     * 📊 獲取升級統計信息（用於診斷顯示升級）
     */
    public UpgradeStats getUpgradeStats() {
        return new UpgradeStats(
            upgradeInventory.getUpgradeCount(UpgradeType.ACCELERATED_PROCESSING),
            upgradeInventory.getUpgradeCount(UpgradeType.EXPANDED_FUEL_CHAMBER),
            upgradeInventory.getUpgradeCount(UpgradeType.CATALYTIC_CONVERTER),
            upgradeInventory.getUpgradeCount(UpgradeType.DIAGNOSTIC_DISPLAY)
        );
    }
    
    /**
     * 🔍 檢查是否有診斷顯示升級（用於GUI顯示額外信息）
     */
    public boolean hasDiagnosticDisplay() {
        return upgradeInventory.getUpgradeCount(UpgradeType.DIAGNOSTIC_DISPLAY) > 0;
    }
    
    /**
     * 📈 計算總體效率倍數（用於顯示）
     */
    public float getTotalEfficiencyMultiplier() {
        float speedMultiplier = getSpeedMultiplier();
        float outputMultiplier = getOutputMultiplier();
        return speedMultiplier * outputMultiplier;
    }
    
    /**
     * ⚡ 獲取速度倍數
     */
    public float getSpeedMultiplier() {
        int acceleratedCount = upgradeInventory.getUpgradeCount(UpgradeType.ACCELERATED_PROCESSING);
        int expandedCount = upgradeInventory.getUpgradeCount(UpgradeType.EXPANDED_FUEL_CHAMBER);
        
        float speedMultiplier = 1.0f;
        
        if (acceleratedCount > 0) {
            speedMultiplier /= Math.pow(0.8f, acceleratedCount); // 速度增加
        }
        
        if (expandedCount > 0) {
            speedMultiplier /= Math.pow(1.5f, expandedCount); // 速度降低
        }
        
        return speedMultiplier;
    }
    
    /**
     * 🔥 獲取產出倍數
     */
    public float getOutputMultiplier() {
        int catalyticCount = upgradeInventory.getUpgradeCount(UpgradeType.CATALYTIC_CONVERTER);
        
        if (catalyticCount == 0) {
            return 1.0f;
        }
        
        float multiplier = 1.0f;
        for (int i = 0; i < catalyticCount; i++) {
            float bonus = Math.max(0.1f, 0.35f - i * 0.05f);
            multiplier += bonus;
        }
        
        return multiplier;
    }
    
    /**
     * 升級統計數據類
     */
    public static class UpgradeStats {
        public final int acceleratedProcessing;
        public final int expandedFuelChamber;
        public final int catalyticConverter;
        public final int diagnosticDisplay;
        
        public UpgradeStats(int acceleratedProcessing, int expandedFuelChamber, 
                           int catalyticConverter, int diagnosticDisplay) {
            this.acceleratedProcessing = acceleratedProcessing;
            this.expandedFuelChamber = expandedFuelChamber;
            this.catalyticConverter = catalyticConverter;
            this.diagnosticDisplay = diagnosticDisplay;
        }
        
        public boolean hasAnyUpgrades() {
            return acceleratedProcessing > 0 || expandedFuelChamber > 0 
                || catalyticConverter > 0 || diagnosticDisplay > 0;
        }
    }
}