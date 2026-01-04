package com.github.nalamodikk.common.block.blockentity.conduit.manager.core;

import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 導管緩存管理器
 * 負責管理網路拓撲緩存、目標緩存和性能優化
 */
public class CacheManager {

    // === 常量 ===
    private static final long CACHE_VERSION_CHECK_INTERVAL = 30000; // 30秒驗證一次
    private static final int TARGET_CACHE_DURATION = 2000; // 2秒緩存持續時間

    // === 實例相關緩存 ===
    private final BlockPos position;
    private final Map<Direction, TargetInfo> cachedTargets = new HashMap<>();
    private final Map<Direction, CacheableEndpoint> localCache = new HashMap<>();
    private long lastTargetScan = 0;

    // === 靜態緩存（所有導管共享） ===
    private static final Map<BlockPos, Long> lastScanTime = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Map<Direction, ManaEndpoint>> sharedCache = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Set<BlockPos>> sharedNetworkNodes = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Map<Direction, CacheableEndpoint>> persistentCache = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> cacheVersions = new ConcurrentHashMap<>();

    // === 建構子 ===
    public CacheManager(BlockPos position) {
        this.position = position;
    }

    // === 目標緩存管理 ===

    /**
     * 獲取緩存的目標信息
     */
    public TargetInfo getCachedTarget(Direction direction) {
        TargetInfo target = cachedTargets.get(direction);
        return (target != null && target.isValid()) ? target : null;
    }

    /**
     * 設定目標緩存
     */
    public void setCachedTarget(Direction direction, IUnifiedManaHandler handler, boolean isConduit) {
        TargetInfo targetInfo = new TargetInfo(handler, isConduit);
        cachedTargets.put(direction, targetInfo);
    }

    /**
     * 清除指定方向的目標緩存
     */
    public void clearTargetCache(Direction direction) {
        cachedTargets.remove(direction);
    }

    /**
     * 清除所有目標緩存
     */
    public void clearAllTargetCache() {
        cachedTargets.clear();
        lastTargetScan = 0;
    }

    /**
     * 檢查是否需要重新掃描目標
     */
    public boolean needsTargetRescan() {
        return System.currentTimeMillis() - lastTargetScan > 5000 || cachedTargets.isEmpty();
    }

    /**
     * 更新最後掃描時間
     */
    public void updateLastScanTime() {
        lastTargetScan = System.currentTimeMillis();
    }

    // === 網路拓撲緩存 ===

    /**
     * 獲取共享緩存
     */
    public Map<Direction, ManaEndpoint> getSharedCache() {
        return sharedCache.get(position);
    }

    /**
     * 設定共享緩存
     */
    public void setSharedCache(Map<Direction, ManaEndpoint> cache) {
        sharedCache.put(position, new HashMap<>(cache));
        lastScanTime.put(position, System.currentTimeMillis());
    }

    /**
     * 獲取網路節點緩存
     */
    public Set<BlockPos> getSharedNetworkNodes() {
        return sharedNetworkNodes.get(position);
    }

    /**
     * 設定網路節點緩存
     */
    public void setSharedNetworkNodes(Set<BlockPos> nodes) {
        sharedNetworkNodes.put(position, Set.copyOf(nodes));
    }

    /**
     * 檢查緩存是否有效
     */
    public boolean isCacheValid(long maxAge) {
        Long lastScan = lastScanTime.get(position);
        if (lastScan == null) return false;
        return System.currentTimeMillis() - lastScan < maxAge;
    }

    // === 緩存失效管理 ===

    /**
     * 使所有緩存失效
     */
    public void invalidateAll() {
        // 清除實例緩存
        cachedTargets.clear();
        localCache.clear();

        // 清除靜態緩存
        sharedCache.remove(position);
        sharedNetworkNodes.remove(position);
        lastScanTime.remove(position);
        persistentCache.remove(position);
        cacheVersions.remove(position);
    }

    /**
     * 清除指定方向的所有緩存
     */
    public void invalidateDirection(Direction direction) {
        cachedTargets.remove(direction);
        localCache.remove(direction);

        // 如果有共享緩存，也要清除對應方向
        Map<Direction, ManaEndpoint> shared = sharedCache.get(position);
        if (shared != null) {
            shared.remove(direction);
        }
    }

    // === 維護和清理 ===

    /**
     * 執行定期清理
     */
    public void cleanup() {
        long now = System.currentTimeMillis();

        // 清理過期的目標緩存
        cachedTargets.entrySet().removeIf(entry -> !entry.getValue().isValid());

        // 清理過期的本地緩存
        localCache.entrySet().removeIf(entry -> entry.getValue().isExpired(300000)); // 5分鐘
    }

    /**
     * 全域緩存維護（靜態方法）
     */
    public static void performGlobalMaintenance() {
        long now = System.currentTimeMillis();

        // 清理超過5分鐘沒更新的緩存
        lastScanTime.entrySet().removeIf(entry -> now - entry.getValue() > 300000);

        // 清理對應的緩存數據
        sharedCache.entrySet().removeIf(entry -> !lastScanTime.containsKey(entry.getKey()));
        sharedNetworkNodes.entrySet().removeIf(entry -> !lastScanTime.containsKey(entry.getKey()));

        // 清理過期的持久化緩存
        persistentCache.entrySet().removeIf(entry -> {
            Map<Direction, CacheableEndpoint> endpoints = entry.getValue();
            return endpoints.values().stream().allMatch(endpoint -> endpoint.isExpired(600000)); // 10分鐘
        });

        // 清理對應的版本信息
        cacheVersions.entrySet().removeIf(entry -> now - entry.getValue() > 600000);
    }

    /**
     * 清理所有靜態緩存（世界卸載時使用）
     */
    public static void clearAllStaticCaches() {
        sharedCache.clear();
        sharedNetworkNodes.clear();
        lastScanTime.clear();
        persistentCache.clear();
        cacheVersions.clear();
    }

    // === 內部類 ===

    public static class TargetInfo {
        public final int availableSpace;
        public final int storedMana;
        public final boolean canReceive;
        public final long scanTime;
        public final boolean isConduit;

        public TargetInfo(IUnifiedManaHandler handler, boolean isConduit) {
            this.availableSpace = handler.getMaxManaStored() - handler.getManaStored();
            this.storedMana = handler.getManaStored();
            this.canReceive = handler.canReceive() && availableSpace > 0;
            this.scanTime = System.currentTimeMillis();
            this.isConduit = isConduit;
        }

        public boolean isValid() {
            return System.currentTimeMillis() - scanTime < TARGET_CACHE_DURATION;
        }

        public int getPriority() {
            return availableSpace; // 空間越大優先級越高
        }
    }

    public static class ManaEndpoint {
        public final IUnifiedManaHandler handler;
        public final boolean isConduit;
        public final int priority;
        public long lastAccess;

        public ManaEndpoint(IUnifiedManaHandler handler, boolean isConduit, int priority) {
            this.handler = handler;
            this.isConduit = isConduit;
            this.priority = priority;
            this.lastAccess = System.currentTimeMillis();
        }
    }

    public static class CacheableEndpoint {
        public final BlockPos position;
        public final boolean isConduit;
        public final int priority;
        public final IOHandlerUtils.IOType expectedIOType;
        public final long cacheTime;

        // 運行時引用（不保存）
        public transient IUnifiedManaHandler handler;
        public transient boolean validated;

        public CacheableEndpoint(BlockPos pos, IUnifiedManaHandler handler, boolean isConduit, int priority) {
            this.position = pos;
            this.handler = handler;
            this.isConduit = isConduit;
            this.priority = priority;
            this.cacheTime = System.currentTimeMillis();
            this.expectedIOType = null; // 可以擴展保存IO類型
            this.validated = true;
        }

        public boolean isExpired(long maxAge) {
            return System.currentTimeMillis() - cacheTime > maxAge;
        }
    }
}