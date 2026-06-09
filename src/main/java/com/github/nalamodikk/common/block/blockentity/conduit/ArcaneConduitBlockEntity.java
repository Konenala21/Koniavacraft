package com.github.nalamodikk.common.block.blockentity.conduit;// 🏗️ 簡化後的 ArcaneConduitBlockEntity.java 主要修改

// === 1. 在頂部添加所有 Manager imports ===

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.conduit.ConduitTier;
import com.github.nalamodikk.common.block.blockentity.conduit.manager.core.CacheManager;
import com.github.nalamodikk.common.block.blockentity.conduit.manager.core.IOManager;
import com.github.nalamodikk.common.block.blockentity.conduit.manager.core.StatsManager;
import com.github.nalamodikk.common.block.blockentity.conduit.manager.network.NetworkManager;
import com.github.nalamodikk.common.block.blockentity.conduit.manager.network.VirtualNetwork;
import com.github.nalamodikk.common.block.blockentity.conduit.manager.transfer.PullManager;
import com.github.nalamodikk.common.block.blockentity.conduit.manager.transfer.TransferManager;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.research.ResearchGate;
import com.github.nalamodikk.register.ModBlockEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ArcaneConduitBlockEntity extends BlockEntity implements IUnifiedManaHandler, IConfigurableBlock {
    // === 保留的常量和靜態字段 ===
    public static final Logger LOGGER = LogUtils.getLogger();

    // ⚠️ 已棄用：使用 ConduitTier 系統取代固定容量
    @Deprecated
    private static final int LEGACY_BUFFER_SIZE = 100;

    private static final int NETWORK_SCAN_INTERVAL = 600;
    private static final int PULL_INTERVAL_TICKS = 10; // 每10tick拉取一次
    private static final int MAX_PULL_PER_TICK = 100;  // 每次最多拉取100魔力
    private int pullTickCounter = 0;
    // === 日誌控制 ===
    private int lastLoggedMana = -1;
    private int lastLoggedConduitCount = -1;

    // 保留全域緩存管理的靜態字段（由CacheManager管理）
    private static int globalTickOffset = 0;
    private static final Map<BlockPos, Integer> conduitTickOffsets = new ConcurrentHashMap<>();

    // === 🆕 組件化核心 ===
    // 🆕 導管等級系統
    private ConduitTier tier = ConduitTier.BASIC; // 預設為基礎等級

    // 緩衝區根據等級動態調整
    private final ManaStorage buffer;
    private final IOManager ioManager;
    private final StatsManager statsManager;
    private final CacheManager cacheManager;
    private final NetworkManager networkManager;
    private final TransferManager transferManager;
    // package-private：由 ConduitNetworkMembership 管理加入/離開/還原；魔力 facade 與 NBT 直接讀此欄位
    VirtualNetwork virtualNetwork;
    private PullManager activePullManager;
    private UUID ownerId;

    // === 簡化的狀態 ===
    private int tickOffset;

    private final ConduitNetworkMembership networkMembership = new ConduitNetworkMembership(this);
    private final ConduitInteractionHandler interactionHandler = new ConduitInteractionHandler(this);


    /**
     * 🔧 獲取緩衝區魔力（不觸發虛擬網路邏輯）
     * 用於網路掃描時避免遞迴
     */
    public int getBufferManaStoredDirect() {
        return buffer.getManaStored();
    }

    /**
     * 🔧 獲取緩衝區最大容量（不觸發虛擬網路邏輯）
     * 用於網路掃描時避免遞迴
     */
    public int getBufferMaxManaStoredDirect() {
        return buffer.getMaxManaStored();
    }

    // === 🆕 簡化的建構子 ===
    public ArcaneConduitBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ARCANE_CONDUIT_BE.get(), pos, blockState);

        // 🆕 根據等級初始化緩衝區
        this.buffer = new ManaStorage(tier.getBufferCapacity());

        // 初始化所有管理器
        this.ioManager = new IOManager();
        this.statsManager = new StatsManager();
        this.cacheManager = new CacheManager(pos);

        // 設定tick偏移
        this.tickOffset = conduitTickOffsets.computeIfAbsent(pos,
                k -> (globalTickOffset++) % NETWORK_SCAN_INTERVAL);

        // 初始化需要相互引用的管理器
        this.networkManager = new NetworkManager(this, cacheManager, ioManager, tickOffset);
        this.transferManager = new TransferManager(this, networkManager, statsManager, ioManager);
        this.activePullManager = null; // 暫時為null，在onLoad時初始化

        // 設定回調監聽器
        setupEventListeners();
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
        setChanged();
    }

    // === 🆕 設定事件監聽器 ===
    private void setupEventListeners() {
        ioManager.setChangeListener(new IOManager.IOConfigChangeListener() {
            @Override
            public void onIOConfigChanged(Direction direction, IOHandlerUtils.IOType newType) {
                handleIOConfigChange(direction, newType);
            }

            @Override
            public void onPriorityChanged(Direction direction, int newPriority) {
                handlePriorityChange(direction, newPriority);
            }
        });
    }

    // === 🆕 事件處理回調 ===
    private void handleIOConfigChange(Direction direction, IOHandlerUtils.IOType newType) {
        // 通知網路管理器
        networkManager.onDirectionConfigChanged(direction);
        setChanged();

        // 通知相鄰導管
        if (level != null && !level.isClientSide) {
            BlockPos neighborPos = worldPosition.relative(direction);
            BlockEntity neighborBE = level.getBlockEntity(neighborPos);
            if (neighborBE instanceof ArcaneConduitBlockEntity neighborConduit) {
                neighborConduit.markNetworkDirty();
            }
        }

        // 更新方塊狀態連接
        updateBlockStateConnections();
    }

    private void handlePriorityChange(Direction direction, int newPriority) {
        networkManager.markDirty();
        setChanged();
    }

    // === 🆕 超級簡化的 tick 方法 ===
    public void tick() {
        if (level == null || level.isClientSide) return;
        if (!ResearchGate.canOperate(tier.getSerializedName() + "_arcane_conduit", level, ownerId)) {
            return;
        }

        // 更新統計管理器
        statsManager.tick();
        int availableMana = getManaStored();

        // ✅ 性能優化：智能休眠 - 閒置且無魔力時大幅降低更新頻率
        // 從每 200 tick (10秒) 改為每 400 tick (20秒) 檢查一次
        // 注意：這裡必須用總魔力（含虛擬網路），不能只看本地 buffer
        if (statsManager.isIdle() && availableMana <= 0) {
            // ✅ 深度休眠：完全閒置時每 20 秒才檢查一次
            if (statsManager.getTickCounter() % 400 != tickOffset % 400) {
                return;
            }
        }

        // 各管理器協調工作
        networkManager.updateIfNeeded(statsManager.getTickCounter());
        transferManager.processManaFlow();
        performActivePull();

        // 定期維護
        if (statsManager.getTickCounter() % 72000 == tickOffset) { // 1小時
            performMaintenance();
        }

        if (statsManager.getTickCounter() % 12000 == tickOffset) { // 10分鐘
            networkManager.performPassiveCleanup();
        }
    }


    /**
     * 🔄 執行主動拉取邏輯
     */
    private void performActivePull() {
        if (activePullManager == null) return;

        pullTickCounter++;

        // 🕒 每隔一定 tick 執行一次拉取
        if (pullTickCounter >= PULL_INTERVAL_TICKS) {
            pullTickCounter = 0;

            // 🎯 只有在導管有空間時才拉取
            // 注意：這裡使用你的虛擬網路系統
            int currentMana = virtualNetwork != null ?
                    virtualNetwork.getTotalManaStored() : buffer.getManaStored();
            int maxMana = virtualNetwork != null ?
                    virtualNetwork.getTotalManaCapacity() : buffer.getMaxManaStored();

            if (currentMana < maxMana) {
                int pulledAmount = activePullManager.performActivePull(MAX_PULL_PER_TICK);

                if (pulledAmount > 0) {
                    // 🔄 拉取成功，標記為需要保存和同步
                    setChanged();

                    // 🌐 通知客戶端更新
                    if (level != null) {
                        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    }

                    // 📊 更新統計（使用你現有的統計系統）
                    statsManager.recordActivity();
                }
            }
        }
    }


    // === 🆕 簡化的維護方法 ===
    private void performMaintenance() {
        statsManager.performMaintenance();
        cacheManager.cleanup();
    }

    // === 🆕 委派給管理器的方法 ===

    // IO 配置委派
    @Override
    public IOHandlerUtils.IOType getIOConfig(Direction direction) {
        return ioManager.getIOConfig(direction);
    }

    @Override
    public void setIOConfig(Direction direction, IOHandlerUtils.IOType type) {
        ioManager.setIOConfig(direction, type);
    }

    @Override
    public EnumMap<Direction, IOHandlerUtils.IOType> getIOMap() {
        return ioManager.getIOMap();
    }

    @Override
    public void setIOMap(EnumMap<Direction, IOHandlerUtils.IOType> newIOMap) {
        ioManager.setIOMap(newIOMap);
    }

    // 優先級委派
    public void setPriority(Direction direction, int priority) {
        ioManager.setPriority(direction, priority);
    }

    public int getPriority(Direction direction) {
        return ioManager.getPriority(direction);
    }

    public void resetAllPriorities() {
        ioManager.resetAllPriorities();
    }

    // 統計委派
    public int getActiveConnectionCount() {
        return networkManager.getActiveConnectionCount();
    }

    public Map<Direction, StatsManager.TransferStats> getTransferStats() {
        return statsManager.getAllTransferStats();
    }

    public int getTransferHistory(Direction direction) {
        return statsManager.getTransferHistory(direction);
    }

    public boolean isTransferringMana(Direction direction) {
        StatsManager.TransferStats stats = statsManager.getTransferStats(direction);
        if (stats == null || level == null) return false;
        return (level.getGameTime() - stats.lastTransfer) < 20; // 20 ticks = 1 second
    }

    // 連接查詢委派
    public boolean hasConnectionInDirection(Direction direction) {
        return networkManager.hasConnection(direction);
    }

    public boolean isConnectedToConduit(Direction direction) {
        return networkManager.isConnectedToConduit(direction);
    }

    // === 🆕 簡化的接收魔力方法 ===
    public int receiveManaFromDirection(int maxReceive, ManaAction action, Direction fromDirection) {
        return transferManager.receiveManaFromDirection(maxReceive, action, fromDirection);
    }

    // === 🆕 簡化的鄰居變化處理 ===
    public void onNeighborChanged() {
        LOGGER.debug("Neighbor changed for conduit at {}", worldPosition);

        // 委派給網路管理器
        networkManager.onNeighborChanged();

        // 更新方塊狀態
        if (level != null && !level.isClientSide) {
            updateBlockStateConnections();
            if (virtualNetwork == null) {
                networkMembership.tryJoin();
            }
            // 通知所有相鄰的導管也重新掃描
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = worldPosition.relative(dir);
                BlockEntity neighborBE = level.getBlockEntity(neighborPos);

                if (neighborBE instanceof ArcaneConduitBlockEntity neighborConduit) {
                    neighborConduit.markNetworkDirty();
                }
            }
        }

        LOGGER.debug("Network state reset for conduit at {}", worldPosition);
    }

    // === 🆕 簡化的移除處理 ===
    @Override
    public void setRemoved() {
        LOGGER.debug("Removing conduit at {}", worldPosition);

        try {
            networkMembership.leave();

            // 委派給緩存管理器清理
            cacheManager.invalidateAll();

            // ✅ 性能優化：清理靜態 map 中的條目，防止內存洩漏
            conduitTickOffsets.remove(worldPosition);

            LOGGER.debug("Conduit removed successfully: {}", worldPosition);
        } catch (Exception e) {
            LOGGER.error("Error during cleanup: {}", e.getMessage());
        }

        super.setRemoved();
    }

    // === 🆕 簡化的網路標記 ===
    public void markNetworkDirty() {
        networkManager.markDirty();
        setChanged();
    }

    // === 🆕 超級簡化的 NBT 序列化 ===
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // 🆕 保存等級
        tag.putString("ConduitTier", tier.getSerializedName());
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }

        // 保存緩衝區
        tag.put("Buffer", buffer.serializeNBT(registries));
        if (virtualNetwork != null) {
            tag.putInt("VirtualNetworkMana", virtualNetwork.getTotalManaStored());
            tag.putInt("VirtualNetworkMaxMana", virtualNetwork.getMaxManaStored());
            int currentMana = virtualNetwork.getTotalManaStored();
            int conduitCount = virtualNetwork.getConnectedConduits().size();

            // 🔧 保存網路中的所有導管位置
            ListTag conduitList = new ListTag();
            for (BlockPos pos : virtualNetwork.getConnectedConduits()) {
                CompoundTag posTag = new CompoundTag();
                posTag.putInt("x", pos.getX());
                posTag.putInt("y", pos.getY());
                posTag.putInt("z", pos.getZ());
                conduitList.add(posTag);
            }
            tag.put("VirtualNetworkConduits", conduitList);

            // 🔧 使用頻率控制的日誌
            logVirtualNetworkSave(currentMana, conduitCount);
        }
        tag.putInt("pullTickCounter", pullTickCounter);

        // 委派給各管理器
        ioManager.saveToNBT(tag);
        statsManager.saveToNBT(tag);
        transferManager.saveToNBT(tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // 🆕 載入等級
        if (tag.contains("ConduitTier")) {
            tier = ConduitTier.fromString(tag.getString("ConduitTier"));
            // 更新緩衝區容量
            buffer.setCapacity(tier.getBufferCapacity());
        }
        if (tag.hasUUID("Owner")) {
            ownerId = tag.getUUID("Owner");
        }

        // 載入緩衝區
        if (tag.contains("Buffer")) {
            buffer.deserializeNBT(registries, tag.getCompound("Buffer"));
        }

        // 🔧 關鍵修復：載入虛擬網路數據（暫存待 onLoad 網路建立後還原）
        networkMembership.queueRestoreFromNBT(tag);
        // 🆕 載入拉取計數器
        pullTickCounter = tag.getInt("pullTickCounter");

        // 委派給各管理器
        ioManager.loadFromNBT(tag);
        statsManager.loadFromNBT(tag);
        transferManager.loadFromNBT(tag);

        // 標記網路需要重新掃描
        networkManager.markDirty();
    }

    /**
     * 🔧 頻率控制的虛擬網路保存日誌
     */
    private void logVirtualNetworkSave(int currentMana, int conduitCount) {
        // 1. 網路剛建立（第一次保存）
        if (lastLoggedMana == -1) {
            if (KoniavacraftMod.IS_DEV) {
                LOGGER.debug("💾 虛擬網路已建立，魔力: {}, 連接數: {}", currentMana, conduitCount);
            }

            lastLoggedMana = currentMana;
            lastLoggedConduitCount = conduitCount;
            return;
        }

        // 2. 連接數變化（網路拓撲改變）
        if (conduitCount != lastLoggedConduitCount) {
            if (KoniavacraftMod.IS_DEV) {
                LOGGER.debug("Virtual network conduit count changed: {} -> {}, mana={}", lastLoggedConduitCount, conduitCount, currentMana);
            }
            lastLoggedConduitCount = conduitCount;
            lastLoggedMana = currentMana;
            return;
        }

        // 3. 魔力值有重大變化（變化超過2000）
        if (Math.abs(currentMana - lastLoggedMana) > 2000) {
            if (KoniavacraftMod.IS_DEV) {

                LOGGER.debug("Virtual network mana changed significantly: {} -> {}, conduits={}",
                    lastLoggedMana, currentMana, conduitCount);
        }
            lastLoggedMana = currentMana;
            return;
        }

        // 其他情況：靜默保存（不輸出任何日誌）
    }


    // === 🆕 簡化的載入處理 ===
    @Override
    public void onLoad() {
        super.onLoad();

        if (level != null) {
            // 標記需要驗證網路狀態
            networkManager.markDirty();
            statsManager.recordActivity();
            this.activePullManager = new PullManager(
                    this.level,
                    this.worldPosition,
                    this);  // ← 改成傳入 this

            if (!level.isClientSide) {
                networkMembership.tryJoin();
                // 🔧 關鍵修復：恢復虛擬網路數據（若 loadAdditional 有暫存）
                networkMembership.restoreIfNeeded();
            }
        }

    }

    // === 🆕 等級系統相關方法 ===

    /**
     * 獲取導管等級
     */
    public ConduitTier getTier() {
        return tier;
    }

    /**
     * 設定導管等級（用於升級）
     */
    public void setTier(ConduitTier newTier) {
        if (newTier != this.tier) {
            int oldCapacity = this.tier.getBufferCapacity();
            this.tier = newTier;
            // 更新緩衝區容量
            buffer.setCapacity(newTier.getBufferCapacity());
            // 通知虛擬網路更新共享池容量
            if (virtualNetwork != null) {
                virtualNetwork.updateConduitCapacity(worldPosition, oldCapacity, newTier.getBufferCapacity());
            }
            setChanged();

            // 通知客戶端
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }

            LOGGER.debug("Conduit at {} upgraded to tier: {}", worldPosition, newTier.getSerializedName());
        }
    }

    /**
     * 升級到下一等級
     * @return 是否成功升級
     */
    public boolean upgradeToNextTier() {
        if (tier.hasNext()) {
            setTier(tier.getNext());
            return true;
        }
        return false;
    }

    // === 保留的 IUnifiedManaHandler 實現 ===
    @Override
    public int receiveMana(int maxReceive, ManaAction action) {
        // 🔄 如果在虛擬網路中，使用網路的魔力池
        if (virtualNetwork != null) {
            int received = virtualNetwork.receiveManaToNetwork(maxReceive,action);
            if (received > 0) {
                setChanged();
            }
            return received;
        }

        // 否則使用原來的邏輯
        return buffer.receiveMana(maxReceive, action);
    }


    @Override
    public int extractMana(int maxExtract, ManaAction action) {
        // 🔄 如果在虛擬網路中，從網路提取魔力
        if (virtualNetwork != null) {
            int extracted = virtualNetwork.extractManaFromNetwork(maxExtract, action); // ✅ 傳遞 action
            if (extracted > 0 && action.execute()) { // ✅ 只有 EXECUTE 時才 setChanged
                setChanged();
            }
            return extracted;
        }

        // 否則使用原來的邏輯
        return buffer.extractMana(maxExtract, action);
    }

    @Override
    public int getManaStored() {
        // 🔄 如果在虛擬網路中，顯示網路總魔力
        if (virtualNetwork != null) {
            return virtualNetwork.getTotalManaStored();
        }

        // 否則使用原來的邏輯
        return buffer.getManaStored();
    }

    @Override
    public int getMaxManaStored() {
        // 🔄 如果在虛擬網路中，顯示網路總容量
        if (virtualNetwork != null) {
            return virtualNetwork.getTotalManaCapacity();
        }

        // 否則使用原來的邏輯
        return buffer.getMaxManaStored();
    }

    @Override
    public void addMana(int amount) {
        buffer.receiveMana(amount, ManaAction.EXECUTE);
        setChanged();
    }

    @Override
    public void consumeMana(int amount) {
        buffer.extractMana(amount, ManaAction.EXECUTE);
        setChanged();
    }

    @Override
    public void setMana(int amount) {
        buffer.setMana(amount);
        setChanged();
    }

    @Override
    public void onChanged() {
        setChanged();
    }

    @Override
    public boolean canExtract() {
        // 🔧 修復：使用一致的邏輯，支援虛擬網路
        return getManaStored() > 0;
    }

    @Override
    public boolean canReceive() {
        // 🔧 修復：使用一致的邏輯，支援虛擬網路
        return getManaStored() < getMaxManaStored();
    }


    // === 多容器支援（簡化實現）===
    @Override
    public int getManaContainerCount() {
        return 1;
    }

    @Override
    public int getManaStored(int container) {
        return container == 0 ? buffer.getManaStored() : 0;
    }

    @Override
    public void setMana(int container, int mana) {
        if (container == 0) {
            buffer.setMana(mana);
            setChanged();
        }
    }

    @Override
    public int getMaxManaStored(int container) {
        return container == 0 ? buffer.getMaxManaStored() : 0;
    }

    @Override
    public int getNeededMana(int container) {
        return container == 0 ? buffer.getMaxManaStored() - buffer.getManaStored() : 0;
    }

    @Override
    public int insertMana(int container, int amount, ManaAction action) {
        return container == 0 ? buffer.receiveMana(amount, action) : 0;
    }

    @Override
    public int extractMana(int container, int amount, ManaAction action) {
        return container == 0 ? buffer.extractMana(amount, action) : 0;
    }

    // === 保留的用戶交互邏輯（委派給 ConduitInteractionHandler）===
    public InteractionResult onUse(BlockState state, Level level, BlockPos pos,
                                   Player player, BlockHitResult hit) {
        return interactionHandler.onUse(state, level, pos, player, hit);
    }

    // === 保留的輔助方法 ===
    private void updateBlockStateConnections() {
        BlockState currentState = level.getBlockState(worldPosition);
        if (currentState.getBlock() instanceof ArcaneConduitBlock conduitBlock) {
            BlockState newState = conduitBlock.updateConnections(level, worldPosition, currentState);
            if (newState != currentState) {
                level.setBlock(worldPosition, newState, 3);
            }
        }
    }

    // === 🆕 靜態清理方法（保留但簡化） ===
    public static void clearAllStaticCachesGracefully() {
        try {
            LOGGER.info("Starting graceful static cache cleanup");

            // 委派給緩存管理器
            CacheManager.clearAllStaticCaches();

            // 清理其他靜態數據
            conduitTickOffsets.clear();
            globalTickOffset = 0;

            LOGGER.info("Graceful cleanup completed");

        } catch (Exception e) {
            LOGGER.error("Error during graceful cleanup: {}", e.getMessage());
        }
    }

    public static void performMaintenanceCleanup() {
        CacheManager.performGlobalMaintenance();
    }


    /**
     * 🆕 獲取緩衝區的魔力（給SimpleVirtualNetwork使用）
     */
    public int getBufferManaStored() {
        return buffer.getManaStored();
    }

    /**
     * 🆕 設置緩衝區的魔力（給SimpleVirtualNetwork使用）
     */
    public void setBufferMana(int amount) {
        buffer.setMana(amount);
        setChanged();
    }

    /**
     * 🆕 獲取虛擬網路
     */
    public VirtualNetwork getVirtualNetwork() {
        return virtualNetwork;
    }

    /**
     * 🆕 檢查是否在虛擬網路中
     */
    public boolean isInVirtualNetwork() {
        return virtualNetwork != null;
    }

}
