package com.github.nalamodikk.common.block.blockentity.conduit.manager.transfer;

import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlockEntity;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 🔄 奧術導管主動拉取管理器
 *
 * 職責：
 * - 主動從鄰居機器拉取魔力
 * - 尊重機器的 IO 配置
 * - 處理不同的拉取策略
 */
public class PullManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PullManager.class);

    private final Level level;
    private final BlockPos conduitPos;
    private final ArcaneConduitBlockEntity conduit;

    // === 日誌頻率控制 ===
    private long lastPullLogTime = 0;
    private static final long PULL_LOG_INTERVAL = 3000; // 3秒內最多輸出一次拉取日誌
    private int totalPullsSinceLastLog = 0;
    private int totalManaSinceLastLog = 0;

    public PullManager(Level level, BlockPos conduitPos, ArcaneConduitBlockEntity conduit) {
        this.level = level;
        this.conduitPos = conduitPos;
        this.conduit = conduit;
    }

    /**
     * 🔄 執行主動拉取邏輯
     * @param maxTransferPerTick 每tick最大傳輸量
     * @return 實際拉取的魔力量
     */
    /**
     * 🔄 執行主動拉取邏輯
     * @param maxTransferPerTick 每tick最大傳輸量
     * @return 實際拉取的魔力量
     */
    public int performActivePull(int maxTransferPerTick) {
        if (level == null || level.isClientSide) return 0;

        int totalPulled = 0;

        // 🔍 檢查所有六個方向的鄰居
        for (Direction direction : Direction.values()) {
            if (totalPulled >= maxTransferPerTick) break;

            // 🎯 關鍵修復：只從配置為 INPUT 或 BOTH 的方向拉取！
            IOHandlerUtils.IOType conduitConfig = conduit.getIOConfig(direction);
            if (conduitConfig != IOHandlerUtils.IOType.INPUT &&
                    conduitConfig != IOHandlerUtils.IOType.BOTH) {
                continue; // 跳過非輸入方向
            }

            BlockPos neighborPos = conduitPos.relative(direction);
            BlockEntity neighborBE = level.getBlockEntity(neighborPos);

            if (neighborBE == null) continue;

            // 🚨 防循環：不要從其他導管拉取！
            if (neighborBE instanceof ArcaneConduitBlockEntity) {
                LOGGER.trace("Skipping pull from conduit at {} to avoid loops", neighborPos);
                continue; // 跳過其他導管
            }

            // 🎯 嘗試從這個方向拉取魔力
            int pulledFromThisDirection = tryPullFromNeighbor(
                    neighborBE,
                    direction,
                    maxTransferPerTick - totalPulled
            );

            totalPulled += pulledFromThisDirection;
        }

        return totalPulled;
    }

    /**
     * 🔍 嘗試從特定鄰居拉取魔力
     */
    private int tryPullFromNeighbor(BlockEntity neighborBE, Direction directionFromConduit, int maxAmount) {
        Direction directionTowardsNeighbor = directionFromConduit.getOpposite();

        // 🔗 獲取鄰居的魔力能力
        var neighborManaHandler = neighborBE.getLevel()
                .getCapability(ModCapabilities.MANA, neighborBE.getBlockPos(), directionTowardsNeighbor);

        if (neighborManaHandler == null) return 0;

        // 🎯 檢查鄰居是否允許從這個面輸出
        if (!canPullFromThisSide(neighborBE, directionTowardsNeighbor)) {
            return 0;
        }

        // 📊 計算實際可以拉取的量
        int availableInNeighbor = neighborManaHandler.getManaStored();
        if (availableInNeighbor <= 0) return 0;

        // 🔄 獲取導管當前的容量狀態（適配虛擬網路）
        int currentMana = conduit.isInVirtualNetwork() ?
                conduit.getVirtualNetwork().getTotalManaStored() :
                conduit.getBufferManaStoredDirect();
        int maxMana = conduit.isInVirtualNetwork() ?
                conduit.getVirtualNetwork().getTotalManaCapacity() :
                conduit.getMaxManaStored();

        int spaceInConduit = maxMana - currentMana;
        if (spaceInConduit <= 0) return 0;

        int actualAmount = Math.min(Math.min(maxAmount, availableInNeighbor), spaceInConduit);

        if (actualAmount <= 0) return 0;

        // 🔄 執行拉取操作
        int extracted = neighborManaHandler.extractMana(actualAmount, ManaAction.EXECUTE);
        if (extracted > 0) {
            // 🆕 使用導管的接收方法（會自動處理虛擬網路）
            int received = conduit.receiveMana(extracted, ManaAction.EXECUTE);

            if (received > 0) {
                // 🔊 拉取成功的效果
                onSuccessfulPull(neighborBE.getBlockPos(), directionFromConduit, received);
                logSuccessfulPull(extracted, neighborBE.getBlockPos(), conduit.getBlockPos());
            }

            return received;
        }

        return 0;
    }

    /**
     * 🔧 頻率控制的拉取成功日誌
     */
    private void logSuccessfulPull(int amount, BlockPos fromPos, BlockPos toPos) {
        long currentTime = System.currentTimeMillis();

        // 累積統計數據
        totalPullsSinceLastLog++;
        totalManaSinceLastLog += amount;

        if (currentTime - lastPullLogTime > PULL_LOG_INTERVAL) {
            // 輸出累積的統計信息
            if (totalPullsSinceLastLog == 1) {
                // 單次拉取，使用原來的格式
                LOGGER.debug("Successfully pulled {} mana from {} to conduit at {}",
                        amount, fromPos, toPos);
            }
//            else {
//                // 多次拉取，使用統計格式
//                LOGGER.debug("Successfully pulled {} mana in {} operations from {} to conduit at {} (last {}ms)",
//                        totalManaSinceLastLog, totalPullsSinceLastLog, fromPos, toPos, PULL_LOG_INTERVAL);
//            }

            // 重置統計
            lastPullLogTime = currentTime;
            totalPullsSinceLastLog = 0;
            totalManaSinceLastLog = 0;
        }
    }

    /**
     * 🔍 檢查是否可以從鄊居的特定面拉取
     *
     * 邏輯：
     * - 如果鄰居該面設為 OUTPUT → 可以拉取
     * - 如果鄰居該面設為 BOTH → 可以拉取
     * - 如果鄰居該面設為 INPUT 或 DISABLED → 不能拉取
     */
    private boolean canPullFromThisSide(BlockEntity neighborBE, Direction neighborSide) {
        // 🎯 使用你現有的 IConfigurableBlock 系統
        if (neighborBE instanceof IConfigurableBlock configurable) {
            IOHandlerUtils.IOType ioType = configurable.getIOConfig(neighborSide);

            return switch (ioType) {
                case OUTPUT, BOTH -> true;  // 可以從輸出面和雙向面拉取
                case INPUT, DISABLED -> false; // 不能從輸入面和禁用面拉取
            };
        }

        // 🔄 如果鄰居沒有 IO 配置，預設允許拉取
        // 這對於原版方塊或其他模組的方塊很重要
        return true;
    }

    /**
     * 🎉 拉取成功時的效果回調
     */
    private void onSuccessfulPull(BlockPos fromPos, Direction direction, int amount) {
        // 🔊 這裡可以添加：
        // - 粒子效果
        // - 音效
        // - 日誌記錄
        // - 統計數據更新

        // 暫時用簡單日誌記錄（你可以稍後添加粒子效果）
        if (amount > 0) {
            LOGGER.trace("Pulled {} mana from {} in direction {}", amount, fromPos, direction);
        }
    }

    /**
     * 🎯 獲取所有可拉取的鄰居資訊（用於調試或GUI顯示）
     */
    public List<PullableNeighborInfo> getPullableNeighbors() {
        List<PullableNeighborInfo> result = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = conduitPos.relative(direction);
            BlockEntity neighborBE = level.getBlockEntity(neighborPos);

            if (neighborBE == null) continue;

            Direction neighborSide = direction.getOpposite();
            var manaHandler = neighborBE.getLevel()
                    .getCapability(ModCapabilities.MANA, neighborBE.getBlockPos(), neighborSide);

            if (manaHandler != null && canPullFromThisSide(neighborBE, neighborSide)) {
                result.add(new PullableNeighborInfo(
                        neighborPos,
                        direction,
                        manaHandler.getManaStored(),
                        manaHandler.getMaxManaStored(),
                        neighborBE.getBlockState().getBlock().getName().getString()
                ));
            }
        }

        return result;
    }

    /**
     * 📊 可拉取鄰居的資訊記錄
     */
    public record PullableNeighborInfo(
            BlockPos pos,
            Direction direction,
            int currentMana,
            int maxMana,
            String blockName
    ) {}
}