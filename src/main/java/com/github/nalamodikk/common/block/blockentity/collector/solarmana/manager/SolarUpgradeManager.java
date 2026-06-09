package com.github.nalamodikk.common.block.blockentity.collector.solarmana.manager;

import com.github.nalamodikk.common.block.blockentity.collector.solarmana.SolarManaCollectorBlockEntity;
import com.github.nalamodikk.common.item.UpgradeItem;
import com.github.nalamodikk.common.utils.upgrade.UpgradeInventory;
import com.github.nalamodikk.common.utils.upgrade.UpgradeType;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 🌞 太陽能升級管理器
 *
 * 🤔 概念解釋：為什麼需要專門的升級管理器？
 * - 遵循單一職責原則：只負責升級效果計算
 * - 保持主類簡潔：SolarManaCollectorBlockEntity 只負責協調
 * - 易於測試和維護：升級邏輯獨立，可以單獨測試
 * - 未來擴展友善：新增升級類型只需修改此管理器
 *
 * 💡 設計理念：MEK風格的自由配置 + 遞減效應平衡
 * - 🆓 任意槽位可放任意升級（符合你的靈活性偏好）
 * - 📉 相同升級類型遞減效應（避免無腦堆疊）
 * - 🎯 智能建議系統（引導玩家找到最優配置）
 * - 🔧 簡潔接口設計（主類只需調用簡單方法）
 */
public class SolarUpgradeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SolarUpgradeManager.class);

    // === 🎯 配置常數 ===
    public static final int UPGRADE_SLOT_COUNT = 8;  // MEK風格：更多槽位
    public static final int BASE_OUTPUT = 20;
    public static final int BASE_INTERVAL = 60;
    public static final int MIN_INTERVAL = 20;
    private static final double SPEED_INTERVAL_MULTIPLIER = 0.75; // 每個速度升級降低 25%
    private static final double EFFICIENCY_OUTPUT_MULTIPLIER = 1.25; // 每個效率升級增加 25%

    // === 🔧 組件 ===
    private final SolarManaCollectorBlockEntity collector;
    private final UpgradeInventory upgrades;

    // === 📊 緩存效果（效能優化）===
    private int cachedOutput = BASE_OUTPUT;
    private int cachedInterval = BASE_INTERVAL;
    private boolean effectsDirty = true;

    public SolarUpgradeManager(SolarManaCollectorBlockEntity collector) {
        this.collector = collector;
        this.upgrades = new UpgradeInventory(UPGRADE_SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                markEffectsDirty();
                collector.setChanged();

                // 🆕 升級變化時立即同步
                if (collector.getLevel() != null && !collector.getLevel().isClientSide()) {
                    collector.getSyncHelper().syncFrom(collector);
                    collector.getLevel().sendBlockUpdated(
                            collector.getBlockPos(),
                            collector.getBlockState(),
                            collector.getBlockState(),
                            3
                    );
                }
            }
        };

        LOGGER.debug("🌞 太陽能升級管理器已初始化，槽位數: {}", UPGRADE_SLOT_COUNT);
    }

    // === ⚡ 公開接口（主類調用）===

    /**
     * 🔥 獲取升級後的產量
     * 主類調用：int output = upgradeManager.getUpgradedOutput();
     */
    public int getUpgradedOutput() {
        if (effectsDirty) recalculateEffects();
        return cachedOutput;
    }

    /**
     * ⚡ 獲取升級後的間隔
     * 主類調用：int interval = upgradeManager.getUpgradedInterval();
     */
    public int getUpgradedInterval() {
        if (effectsDirty) recalculateEffects();
        return cachedInterval;
    }

    /**
     * 📊 獲取升級統計文字（用於GUI顯示）
     */
    public Component getUpgradeStatsText() {
        int effCount = countUpgradeType(UpgradeType.EFFICIENCY);
        int speedCount = countUpgradeType(UpgradeType.SPEED);

        if (effCount == 0 && speedCount == 0) {
            return Component.translatable("screen.koniava.solar.no_upgrades");
        }

        return Component.translatable("screen.koniava.solar.upgrade_stats",
                effCount, speedCount, String.format("%.1f", getTotalEfficiencyMultiplier()));
    }

    /**
     * 💡 獲取智能建議
     */
    public String getUpgradeSuggestion() {
        int effCount = countUpgradeType(UpgradeType.EFFICIENCY);
        int speedCount = countUpgradeType(UpgradeType.SPEED);

        if (effCount == 0 && speedCount == 0) {
            return "§e建議：先放入效率升級，立即提升產量！";
        }

        // 計算最優下一步
        double currentEff = getTotalEfficiencyMultiplier();
        double withEff = simulateWithExtraUpgrade(UpgradeType.EFFICIENCY);
        double withSpeed = simulateWithExtraUpgrade(UpgradeType.SPEED);

        if (withEff > withSpeed) {
            return "§a建議：添加效率升級獲得更好效果";
        } else if (withSpeed > withEff) {
            return "§b建議：添加速度升級獲得更好效果";
        } else {
            return "§6當前配置已達到良好平衡！";
        }
    }

    /**
     * 🔧 升級清單接口（用於GUI）
     */
    public UpgradeInventory getUpgradeInventory() {
        return upgrades;
    }

    /**
     * 🎯 檢查是否可以放置升級（MEK風格：完全自由）
     */
    public boolean canPlaceUpgrade(int slot, UpgradeType upgradeType) {
        if (slot < 0 || slot >= UPGRADE_SLOT_COUNT) return false;

        // MEK風格：任意槽位可放任意升級
        return upgradeType == UpgradeType.SPEED || upgradeType == UpgradeType.EFFICIENCY;
    }

    /**
     * 🔍 獲取指定槽位的升級類型（輔助方法）
     */
    private UpgradeType getUpgradeTypeAt(int slot) {
        if (slot < 0 || slot >= UPGRADE_SLOT_COUNT) return null;

        ItemStack stack = upgrades.getItem(slot);
        if (stack.getItem() instanceof UpgradeItem upgradeItem) {
            return upgradeItem.getUpgradeType();
        }
        return null;
    }

    // === 🔧 內部計算邏輯 ===

    /**
     * 🔄 重新計算所有升級效果（私有方法，主類不直接調用）
     */
    private void recalculateEffects() {
        int efficiencyCount = countUpgradeType(UpgradeType.EFFICIENCY);
        int speedCount = countUpgradeType(UpgradeType.SPEED);

        // 🔥 計算效率升級效果（遞減增長）
        cachedOutput = calculateEfficiencyOutput(efficiencyCount);

        // ⚡ 計算速度升級效果（遞減減少）
        cachedInterval = calculateSpeedInterval(speedCount);

        effectsDirty = false;

//        LOGGER.debug("🌞 升級效果重新計算：{}效率+{}速度 → 產量={}, 間隔={}tick",
//                efficiencyCount, speedCount, cachedOutput, cachedInterval);
    }

    /**
     * 📈 計算效率升級的產量（遞減效應）
     */
    private int calculateEfficiencyOutput(int upgradeCount) {
        if (upgradeCount <= 0) return BASE_OUTPUT;

        double output = BASE_OUTPUT;

        for (int i = 0; i < upgradeCount; i++) {
            output *= EFFICIENCY_OUTPUT_MULTIPLIER;
        }

        return (int) output;
    }

    /**
     * ⚡ 計算速度升級的間隔（遞減減少）
     */
    private int calculateSpeedInterval(int upgradeCount) {
        if (upgradeCount <= 0) return BASE_INTERVAL;

        double interval = BASE_INTERVAL;
        for (int i = 0; i < upgradeCount; i++) {
            interval *= SPEED_INTERVAL_MULTIPLIER;
        }

        return Math.max(MIN_INTERVAL, (int) Math.round(interval));
    }

    /**
     * 📋 統計指定類型的升級數量（使用現有的 UpgradeInventory 方法）
     */
    private int countUpgradeType(UpgradeType upgradeType) {
        return upgrades.getUpgradeCount(upgradeType);
    }

    /**
     * 📊 計算總體效率倍數
     */
    private double getTotalEfficiencyMultiplier() {
        double outputMultiplier = (double) getUpgradedOutput() / BASE_OUTPUT;
        double speedMultiplier = (double) BASE_INTERVAL / getUpgradedInterval();
        return outputMultiplier * speedMultiplier;
    }

    /**
     * 🧮 模擬添加指定升級的效果（用於智能建議）
     */
    private double simulateWithExtraUpgrade(UpgradeType upgradeType) {
        int currentEff = countUpgradeType(UpgradeType.EFFICIENCY);
        int currentSpeed = countUpgradeType(UpgradeType.SPEED);

        if (upgradeType == UpgradeType.EFFICIENCY) {
            currentEff++;
        } else {
            currentSpeed++;
        }

        int simOutput = calculateEfficiencyOutput(currentEff);
        int simInterval = calculateSpeedInterval(currentSpeed);

        double outputMult = (double) simOutput / BASE_OUTPUT;
        double speedMult = (double) BASE_INTERVAL / simInterval;

        return outputMult * speedMult;
    }

    /**
     * 🔄 標記效果需要重新計算（當升級變更時調用）
     */
    public void markEffectsDirty() {
        effectsDirty = true;
    }

    // === 💾 數據持久化（使用正確的方法名）===
    /**
     * 💾 修復版 NBT 保存 - 傳入正確的 HolderLookup
     */
    public void saveToNBT(CompoundTag tag, HolderLookup.Provider registries) {
        try {
            CompoundTag upgradeTag = upgrades.serializeNBT(registries);
            tag.put("SolarUpgradeManager", upgradeTag);

//            // 🔍 調試日誌
//            LOGGER.debug("🌞 保存升級管理器: 槽位數 {}, 升級物品數 {}",
//                    UPGRADE_SLOT_COUNT,
//                    upgrades.getAll().stream().mapToInt(stack -> stack.isEmpty() ? 0 : 1).sum());
        } catch (Exception e) {
            LOGGER.error("Failed to save solar upgrade manager.", e);
        }
    }


    /**
     * 🔄 修復版 NBT 載入 - 傳入正確的 HolderLookup
     */
    public void loadFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        try {
            if (tag.contains("SolarUpgradeManager")) {
                CompoundTag upgradeTag = tag.getCompound("SolarUpgradeManager");
                upgrades.deserializeNBT(registries, upgradeTag);
                markEffectsDirty(); // 重新計算效果

                // 🔍 調試日誌
                LOGGER.debug("🌞 載入升級管理器: 槽位數 {}, 升級物品數 {}",
                        UPGRADE_SLOT_COUNT,
                        upgrades.getAll().stream().mapToInt(stack -> stack.isEmpty() ? 0 : 1).sum());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load solar upgrade manager.", e);
        }
    }


}
