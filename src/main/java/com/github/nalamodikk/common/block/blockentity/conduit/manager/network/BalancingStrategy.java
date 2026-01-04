package com.github.nalamodikk.common.block.blockentity.conduit.manager.network;

import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlockEntity;
import com.github.nalamodikk.common.block.blockentity.conduit.manager.core.CacheManager;
import com.github.nalamodikk.common.block.blockentity.conduit.manager.core.IOManager;
import net.minecraft.core.Direction;

import java.util.Comparator;
import java.util.List;
// === 負載平衡策略類 ===

public class BalancingStrategy {

    // 🆕 優先級權重因子
    private static final int PRIORITY_WEIGHT = 1000; // 優先級每增加1，權重增加1000
    private static final int SPACE_WEIGHT = 1;       // 可用空間每增加1，權重增加1
    private static final int LOAD_PENALTY = 2;       // 當前負載每增加1，權重減少2

    /**
     * 🆕 選擇最佳目標方向（改進版）
     * 優先級制度：
     * 1. 高優先級絕對優先（除非完全無法傳輸）
     * 2. 相同優先級時，選擇可用空間最大的
     * 3. 如果都滿了，輪詢等待
     */
    public static Direction selectBestTarget(List<Direction> validTargets,
                                             ArcaneConduitBlockEntity conduit,
                                             IOManager ioManager,
                                             NetworkManager networkManager,
                                             long tickCounter) {
        if (validTargets.isEmpty()) return null;

        // 🆕 使用權重系統選擇最佳目標
        return validTargets.stream()
                .max(Comparator.comparingInt(dir ->
                    calculateTargetScore(dir, ioManager, networkManager)))
                .orElse(null);
    }

    /**
     * 🆕 計算目標方向的得分
     * 得分越高，優先級越高
     */
    private static int calculateTargetScore(Direction dir,
                                           IOManager ioManager,
                                           NetworkManager networkManager) {
        CacheManager.TargetInfo target = networkManager.getTargetInfo(dir);
        if (target == null || !target.canReceive) {
            return Integer.MIN_VALUE; // 無法接收的目標直接排除
        }

        int score = 0;

        // 1. 優先級權重（最重要）
        int priority = ioManager.getPriority(dir);
        score += priority * PRIORITY_WEIGHT;

        // 2. 可用空間權重（優先送到空間大的地方）
        score += target.availableSpace * SPACE_WEIGHT;

        // 3. 當前負載懲罰（避免送到已經很滿的地方）
        if (target.isConduit) {
            score -= target.storedMana * LOAD_PENALTY;
        }

        // 4. 如果目標完全沒有空間，大幅降低分數
        if (target.availableSpace <= 0) {
            score -= 1000000; // 嚴重懲罰
        }

        return score;
    }

    // === 🆕 輔助方法：供外部調試和監控使用 ===

    /**
     * 🆕 獲取目標的詳細評分信息（用於調試）
     */
    public static String getTargetScoreDebugInfo(Direction dir,
                                                 IOManager ioManager,
                                                 NetworkManager networkManager) {
        CacheManager.TargetInfo target = networkManager.getTargetInfo(dir);
        if (target == null) {
            return dir.name() + ": No target info";
        }

        int priority = ioManager.getPriority(dir);
        int priorityScore = priority * PRIORITY_WEIGHT;
        int spaceScore = target.availableSpace * SPACE_WEIGHT;
        int loadPenalty = target.isConduit ? -(target.storedMana * LOAD_PENALTY) : 0;
        int totalScore = calculateTargetScore(dir, ioManager, networkManager);

        return String.format("%s: Total=%d (Priority=%d*%d=%d, Space=%d*%d=%d, Load Penalty=%d)",
                dir.name(), totalScore,
                priority, PRIORITY_WEIGHT, priorityScore,
                target.availableSpace, SPACE_WEIGHT, spaceScore,
                loadPenalty);
    }

    /**
     * 🆕 檢查是否所有目標都已滿
     */
    public static boolean areAllTargetsFull(List<Direction> validTargets,
                                           NetworkManager networkManager) {
        for (Direction dir : validTargets) {
            CacheManager.TargetInfo target = networkManager.getTargetInfo(dir);
            if (target != null && target.availableSpace > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 🆕 獲取最高優先級值（用於UI顯示）
     */
    public static int getHighestPriority(List<Direction> validTargets,
                                        IOManager ioManager) {
        return validTargets.stream()
                .mapToInt(ioManager::getPriority)
                .max()
                .orElse(0);
    }
}
