    package com.github.nalamodikk.common.block.blockentity.collector.solarmana;

    import com.github.nalamodikk.common.block.blockentity.collector.solarmana.manager.SolarUpgradeManager;
    import com.github.nalamodikk.common.block.blockentity.collector.solarmana.sync.SolarCollectorSyncHelper;
    import com.github.nalamodikk.common.block.blockentity.mana_generator.logic.OutputHandler;
    import com.github.nalamodikk.common.block.blockentity.manabase.AbstractManaCollectorBlock;
    import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
    import com.github.nalamodikk.common.capability.ManaStorage;
    import com.github.nalamodikk.common.capability.mana.ManaAction;
    import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
    import com.github.nalamodikk.common.utils.SkyUtils;
    import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
    import com.github.nalamodikk.common.utils.nbt.NbtUtils;
import com.github.nalamodikk.common.utils.upgrade.UpgradeInventory;
    import com.github.nalamodikk.common.utils.upgrade.UpgradeType;
    import com.github.nalamodikk.common.utils.upgrade.api.IUpgradeableMachine;
    import com.github.nalamodikk.research.ResearchGate;
    import com.github.nalamodikk.register.ModBlockEntities;
    import com.github.nalamodikk.register.ModCapabilities;
    import com.mojang.logging.LogUtils;
    import net.minecraft.core.BlockPos;
    import net.minecraft.core.Direction;
    import net.minecraft.core.HolderLookup;
    import net.minecraft.core.particles.ParticleTypes;
    import net.minecraft.nbt.CompoundTag;
    import net.minecraft.network.Connection;
    import net.minecraft.network.chat.Component;
    import net.minecraft.network.protocol.Packet;
    import net.minecraft.network.protocol.game.ClientGamePacketListener;
    import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
    import net.minecraft.server.level.ServerLevel;
    import net.minecraft.world.MenuProvider;
    import net.minecraft.world.entity.player.Inventory;
    import net.minecraft.world.entity.player.Player;
    import net.minecraft.world.inventory.AbstractContainerMenu;
    import net.minecraft.world.level.Level;
    import net.minecraft.world.level.LightLayer;
    import net.minecraft.world.level.block.entity.BlockEntity;
    import net.minecraft.world.level.block.state.BlockState;
    import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
    import net.neoforged.neoforge.capabilities.Capabilities;
    import net.neoforged.neoforge.energy.IEnergyStorage;
    import org.slf4j.Logger;

    import java.util.EnumMap;

    /**
     * 🌞 太陽能魔力收集器 - 完整修復版
     *
     * 🎯 修復內容：
     * - ✅ 修復重登世界物品消失問題
     * - ✅ 正確的 Menu 創建邏輯
     * - ✅ 改善數據持久化
     * - ✅ 優化同步時機
     *
     * 💡 設計理念：
     * - 🔧 主類保持簡潔，委派給管理器
     * - 📊 數據同步穩定可靠
     * - 💾 數據持久化防護性強
     * - ⚡ 性能優化適度
     */
    public class SolarManaCollectorBlockEntity extends AbstractManaCollectorBlock implements IConfigurableBlock, MenuProvider, IUpgradeableMachine {
        private static final Logger LOGGER = LogUtils.getLogger();
        private static final int MAX_MANA = 80000;

        // === 🔧 核心組件 ===
        private final SolarCollectorSyncHelper syncHelper = new SolarCollectorSyncHelper();
        private final SolarUpgradeManager upgradeManager;

        // === 📊 狀態數據 ===
        private boolean generating = false;
        private final EnumMap<Direction, IOHandlerUtils.IOType> ioMap = new EnumMap<>(Direction.class);

        // === ⚡ 性能優化 ===
        private int stateCheckInterval = 20; // 🔧 每 20 tick 檢查一次狀態（1秒）
        private int lastSyncedMana = 0; // 🔧 上次同步的魔力值
        private boolean lastSyncedDaytime = true; // 🔧 上次同步的日夜狀態
        private boolean lastSyncedOverworld = true;
        private boolean lastSyncedHasSkyLight = true;
        private boolean lastSyncedOpenToSky = true;
        private boolean lastSyncedRaining = false;
        private boolean lastSyncedThundering = false;

        // === ⚡ 性能緩存 ===
        private final EnumMap<Direction, BlockCapabilityCache<IUnifiedManaHandler, Direction>> manaCaches = new EnumMap<>(Direction.class);
        private final EnumMap<Direction, BlockCapabilityCache<IEnergyStorage, Direction>> energyCaches = new EnumMap<>(Direction.class);

        public SolarManaCollectorBlockEntity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.SOLAR_MANA_COLLECTOR_BE.get(), pos, state, MAX_MANA,
                    SolarUpgradeManager.BASE_INTERVAL, SolarUpgradeManager.BASE_OUTPUT);
            this.upgradeManager = new SolarUpgradeManager(this);
            ioMap.put(Direction.DOWN, IOHandlerUtils.IOType.OUTPUT);
            // 其他方向預設為 DISABLED
            for (Direction dir : Direction.values()) {
                if (!ioMap.containsKey(dir)) {
                    ioMap.put(dir, IOHandlerUtils.IOType.DISABLED);
                }
            }
            LOGGER.debug("🌞 太陽能收集器初始化：位置 {}", pos);
        }


        // === 📊 公開接口 ===

        public SolarCollectorSyncHelper getSyncHelper() {
            return syncHelper;
        }

        // === ⚡ 核心邏輯 ===


        public boolean isDaytime() {
            if (level == null) return true; // 只有這裡可以有預設值
            // 🔧 統一使用與 canGenerate() 相同的判定邏輯
            return level.isDay();
        }

        // 🏗️ 分離關注點版本 - 符合你的架構偏好

        @Override
        public void tickMachine() {
            if (!ResearchGate.canOperate("solar_mana_collector", level, ownerId)) {
                if (generating) {
                    generating = false;
                    syncHelper.syncFrom(this);
                    setChanged();
                }
                return;
            }

            long gameTime = level.getGameTime();

            // 🔄 狀態管理：降低檢查頻率（每 20 tick = 1秒）
            if (gameTime % stateCheckInterval == 0) {
                updateGeneratingState();
            }

            // 📊 數據同步：只在有變化時執行
            handleDataSync();

            // ⚡ 魔力生成：按間隔執行
            handleManaGeneration();
        }

        // 🔄 狀態更新邏輯
        private void updateGeneratingState() {
            boolean oldGenerating = this.generating;
            boolean canGenerate = canGenerate();
            boolean hasSpace = !manaStorage.isFull();

            // 🎯 真正的生成狀態：能發電 + 有空間
            this.generating = canGenerate && hasSpace;

            // 🔧 狀態變化時通知客戶端並同步
            if (oldGenerating != this.generating && level instanceof ServerLevel) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                setChanged();

                // ⚡ 狀態變化時立即同步（確保 GUI 即時更新）
                syncHelper.syncFrom(this);
                lastSyncedMana = manaStorage.getManaStored();
                lastSyncedDaytime = isDaytime();
                lastSyncedOverworld = isOverworld();
                updateSyncedSkyFlags((ServerLevel) level);

                LOGGER.debug("Solar collector generating state changed: {} -> {}, canGenerate={}, hasSpace={}",
                        oldGenerating, this.generating, canGenerate, hasSpace);
            }
        }

        // 📊 數據同步邏輯（優化版）
        private void handleDataSync() {
            if (!(level instanceof ServerLevel)) return;
            ServerLevel server = (ServerLevel) level;

            // ⚡ 優化：只在數據真正變化時才同步
            int currentMana = manaStorage.getManaStored();
            boolean manaChanged = currentMana != lastSyncedMana;
            boolean daytimeChanged = isDaytime() != lastSyncedDaytime;
            boolean isOverworld = isOverworld(server);
            boolean hasSkyLight = hasSkyLight(server);
            boolean openToSky = isOpenToSky(server);
            boolean isRaining = isRainingAt(server);
            boolean isThundering = isThundering(server);
            boolean skyStateChanged = isOverworld != lastSyncedOverworld
                || hasSkyLight != lastSyncedHasSkyLight
                || openToSky != lastSyncedOpenToSky
                || isRaining != lastSyncedRaining
                || isThundering != lastSyncedThundering;

            // 檢查魔力變化是否顯著（變化超過 1% 或每 20 tick 強制同步一次）
            boolean shouldSync = daytimeChanged || skyStateChanged || (manaChanged && (
                Math.abs(currentMana - lastSyncedMana) > MAX_MANA / 100 || // 變化超過 1%
                level.getGameTime() % 20 == 0 // 或每秒強制同步一次
            ));

            if (shouldSync) {
                syncHelper.syncFrom(this);
                lastSyncedMana = currentMana;
                lastSyncedDaytime = isDaytime();
                lastSyncedOverworld = isOverworld;
                lastSyncedHasSkyLight = hasSkyLight;
                lastSyncedOpenToSky = openToSky;
                lastSyncedRaining = isRaining;
                lastSyncedThundering = isThundering;
            }
        }

        // ⚡ 魔力生成邏輯
        private void handleManaGeneration() {
            // 檢查生成間隔
            int interval = upgradeManager.getUpgradedInterval();
            if (level.getGameTime() % interval != 0) return;

            // 只有在真正發電時才執行生成
            if (!this.generating) return;

            // 執行實際生成
            performManaGeneration();
        }

        // 🎯 執行實際的魔力生成
        private void performManaGeneration() {
            int amount = scaleByOwner(upgradeManager.getUpgradedOutput());
            int inserted = manaStorage.insertMana(amount, ManaAction.EXECUTE);

            if (inserted > 0 && level instanceof ServerLevel server) {
                // 🔌 處理輸出
                handleManaOutput(server);

                // 🎨 視覺效果
                createGenerationEffects(server);
            }
        }

        // 🔌 魔力輸出處理
        private void handleManaOutput(ServerLevel server) {
            boolean didOutput = OutputHandler.tryOutput(server, worldPosition, manaStorage, null, ioMap, manaCaches, energyCaches, OutputHandler.getBaseManaOutputPerTick());

            if (didOutput) {
                // 魔力值變化，通知更新
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                setChanged();
            }
        }

        // 🎨 生成視覺效果
        private void createGenerationEffects(ServerLevel server) {
            server.sendParticles(
                    ParticleTypes.ENCHANT,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.1,
                    worldPosition.getZ() + 0.5,
                    2, 0.2, 0.1, 0.2, 0.0
            );
        }


        //是否可以發電方法 - 使用高度圖的極速穩定方案
        @Override
        protected boolean canGenerate() {
            if (!(level instanceof ServerLevel server)) return false;

            return shouldGenerate(server.isDay(),
                isOverworld(server),
                hasSkyLight(server),
                isOpenToSky(server),
                isRainingAt(server),
                isThundering(server));
        }

        static boolean shouldGenerate(boolean isDaytime,
                                      boolean isOverworld,
                                      boolean hasSkyLight,
                                      boolean openToSky,
                                      boolean isRaining,
                                      boolean isThundering) {
            return isDaytime
                && isOverworld
                && hasSkyLight
                && openToSky
                && !isRaining
                && !isThundering;
        }

        public boolean hasSkyLight() {
            if (level == null) return false;
            return hasSkyLight(level);
        }

        public boolean isOverworld() {
            if (level == null) return false;
            return isOverworld(level);
        }

        public boolean isOpenToSky() {
            if (level == null) return false;
            return isOpenToSky(level);
        }

        public boolean isRainingAtCollector() {
            if (level == null) return false;
            return isRainingAt(level);
        }

        public boolean isThundering() {
            if (level == null) return false;
            return isThundering(level);
        }

        private boolean isOverworld(Level level) {
            return "minecraft:overworld".equals(level.dimension().location().toString());
        }

        private boolean hasSkyLight(Level level) {
            int skyLight = level.getBrightness(LightLayer.SKY, worldPosition.above());
            return skyLight > 0 || level.dimensionType().hasSkyLight();
        }

        private boolean isOpenToSky(Level level) {
            return level.canSeeSky(worldPosition.above());
        }

        private boolean isRainingAt(Level level) {
            return level.isRainingAt(worldPosition.above());
        }

        private boolean isThundering(Level level) {
            return level.isThundering();
        }

        private void updateSyncedSkyFlags(Level level) {
            lastSyncedOverworld = isOverworld(level);
            lastSyncedHasSkyLight = hasSkyLight(level);
            lastSyncedOpenToSky = isOpenToSky(level);
            lastSyncedRaining = isRainingAt(level);
            lastSyncedThundering = isThundering(level);
        }






        // === 💾 數據持久化 ===

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);

            try {
                // 🔧 委派給組件保存 - 傳入正確的 registries
                NbtUtils.write(tag, "Mana", manaStorage, registries);
                upgradeManager.saveToNBT(tag, registries); // ✅ 傳入 registries
                NbtUtils.writeEnumIOTypeMap(tag, "IOMap", ioMap);

                // ❌ 不保存 generating 狀態 - 這是衍生狀態，應該每次重新計算
                // tag.putBoolean("Generating", generating);

                // 🔍 調試日誌
//                LOGGER.debug("🌞 保存太陽能收集器: 位置 {}, 魔力 {}, 升級管理器已保存",
//                        worldPosition, manaStorage.getManaStored());

            } catch (Exception e) {
                LOGGER.error("Failed to save solar mana collector at {}", worldPosition, e);
            }
        }


        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);

            try {
                // 🔧 委派給組件加載 - 傳入正確的 registries
                NbtUtils.read(tag, "Mana", manaStorage, registries);
                if (!tag.contains("Mana") && tag.contains("ManaStorage")) {
                    manaStorage.deserializeNBT(registries, tag.getCompound("ManaStorage"));
                }
                upgradeManager.loadFromNBT(tag, registries); // ✅ 使用修復後的方法
                if (!tag.contains("SolarUpgradeManager") && tag.contains("UpgradeInventory")) {
                    upgradeManager.getUpgradeInventory().deserializeNBT(registries, tag.getCompound("UpgradeInventory"));
                    upgradeManager.markEffectsDirty();
                }
                EnumMap<Direction, IOHandlerUtils.IOType> loadedIoMap = NbtUtils.readEnumIOTypeMap(tag, "IOMap");
                if (loadedIoMap.isEmpty()) {
                    applyDefaultIOMap();
                } else {
                    setIOMap(loadedIoMap);
                }

                // ✅ 不從 NBT 載入 generating 狀態 - 將在 onLoad() 時重新計算
                // generating = tag.getBoolean("Generating");
                generating = false; // 初始化為 false，等待第一次 tick 更新

                // 🆕 關鍵修復：僅伺服器端同步數據，避免客戶端覆寫同步狀態
                if (level != null && !level.isClientSide()) {
                    syncHelper.syncFrom(this);
                }

                LOGGER.debug("Loaded solar mana collector at {} with mana {} and upgrade data.",
                        worldPosition, manaStorage.getManaStored());

            } catch (Exception e) {
                LOGGER.error("Failed to load solar mana collector at {}", worldPosition, e);
                generating = false;
            }
        }

        private void applyDefaultIOMap() {
            ioMap.clear();
            ioMap.put(Direction.DOWN, IOHandlerUtils.IOType.OUTPUT);
            for (Direction dir : Direction.values()) {
                if (!ioMap.containsKey(dir)) {
                    ioMap.put(dir, IOHandlerUtils.IOType.DISABLED);
                }
            }
        }

        private void ensureUpgradeDataSync() {
            if (level == null || level.isClientSide()) return;

            // 檢查實際升級數量
            int actualSpeed = upgradeManager.getUpgradeInventory().getUpgradeCount(UpgradeType.SPEED);
            int actualEff = upgradeManager.getUpgradeInventory().getUpgradeCount(UpgradeType.EFFICIENCY);

            // 檢查同步數據
            int syncSpeed = syncHelper.getSpeedLevel();
            int syncEff = syncHelper.getEfficiencyLevel();

            // 如果不一致，強制同步
            if (actualSpeed != syncSpeed || actualEff != syncEff) {
                LOGGER.debug("🔄 檢測到升級數據不一致，強制同步: 實際速度={}, 同步速度={}, 實際效率={}, 同步效率={}",
                        actualSpeed, syncSpeed, actualEff, syncEff);

                syncHelper.syncFrom(this);
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                setChanged();
            }
        }


        // === 🎮 GUI 接口 ===

        @Override
        public Component getDisplayName() {
            return Component.translatable("block.koniava.solar_mana_collector");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
            // 🎯 創建 Menu 前確保數據已同步
            syncHelper.syncFrom(this);

            if (!level.isClientSide()) {
                // 伺服器端再次確保同步
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }

            return new SolarManaCollectorMenu(id, playerInventory, this);
        }

        // === 🔌 能力系統 ===

        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel serverLevel) {
                initializeCapabilityCaches(serverLevel);

                // ✅ 關鍵修復：載入後立即重新計算發電狀態
                updateGeneratingState();

                // 🆕 強制刷新同步狀態，避免重開世界後鎖定在舊狀態
                forceInitialSync(serverLevel);

                // 🆕 通知客戶端更新
                serverLevel.scheduleTick(worldPosition, getBlockState().getBlock(), 1);

                LOGGER.debug("Solar collector server load complete at {}. generating={}, canGenerate={}",
                        worldPosition, generating, canGenerate());
            } else if (level != null && level.isClientSide()) {
                // 🆕 客戶端載入時的日誌
                LOGGER.debug("Solar collector client load complete at {}.", worldPosition);
            }
        }

        private void forceInitialSync(ServerLevel serverLevel) {
            lastSyncedMana = -1;
            lastSyncedDaytime = !isDaytime();
            lastSyncedOverworld = !isOverworld(serverLevel);
            lastSyncedHasSkyLight = !hasSkyLight(serverLevel);
            lastSyncedOpenToSky = !isOpenToSky(serverLevel);
            lastSyncedRaining = !isRainingAt(serverLevel);
            lastSyncedThundering = !isThundering(serverLevel);
            syncHelper.syncFrom(this);
        }

        private void initializeCapabilityCaches(ServerLevel serverLevel) {
            for (Direction dir : Direction.values()) {
                BlockPos targetPos = worldPosition.relative(dir);
                Direction inputSide = dir.getOpposite();

                LOGGER.debug("Initializing capability cache. direction={}, targetPos={}, inputSide={}",
                        dir, targetPos, inputSide);

                manaCaches.put(dir, BlockCapabilityCache.create(
                        ModCapabilities.MANA,
                        serverLevel,
                        targetPos,
                        inputSide,
                        () -> !this.isRemoved(),
                        () -> {
                            LOGGER.debug("Mana capability cache invalidated for direction={}", dir);
                        }
                ));

                energyCaches.put(dir, BlockCapabilityCache.create(
                        Capabilities.EnergyStorage.BLOCK,
                        serverLevel,
                        targetPos,
                        inputSide,
                        () -> !this.isRemoved(),
                        () -> {
                            LOGGER.debug("Energy capability cache invalidated for direction={}", dir);
                        }
                ));
            }

            LOGGER.debug("Solar collector capability caches initialized at {}.", worldPosition);
        }
        // === 🔧 升級系統接口 ===

        @Override
        public UpgradeInventory getUpgradeInventory() {
            return upgradeManager.getUpgradeInventory();
        }

        @Override
        public boolean supportsUpgradeType(UpgradeType type) {
            return type == UpgradeType.SPEED || type == UpgradeType.EFFICIENCY;
        }

        @Override
        public BlockEntity getBlockEntity() {
            return this;
        }

        // === 📊 數據接口 ===

        public ManaStorage getManaStorage() {
            return manaStorage;
        }

        public int getManaStored() {
            return this.manaStorage.getManaStored();
        }

        public boolean isCurrentlyGenerating() {
            return this.generating;
        }

        public void setCurrentlyGenerating(boolean value) {
            this.generating = value;
        }

        public static int getMaxMana() {
            return MAX_MANA;
        }

        // === 🔧 配置接口 ===


        @Override
        public void setIOConfig(Direction direction, IOHandlerUtils.IOType type) {
            ioMap.put(direction, type);
            setChanged(); // 🔄 配置變更時保存
        }

        @Override
        public IOHandlerUtils.IOType getIOConfig(Direction direction) {
            return ioMap.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);
        }

        @Override
        public EnumMap<Direction, IOHandlerUtils.IOType> getIOMap() {
            return ioMap;
        }

        @Override
        public void setIOMap(EnumMap<Direction, IOHandlerUtils.IOType> map) {
            ioMap.clear();
            ioMap.putAll(map);
            setChanged(); // 🔄 配置變更時保存
        }

        /**
         * 🌐 獲取同步標籤 - 發送給客戶端的完整數據
         * 當客戶端載入區塊時會調用此方法
         */
        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            CompoundTag tag = super.getUpdateTag(registries);

            // 🔧 包含所有需要同步的數據
            saveAdditional(tag, registries);

            LOGGER.debug("🌐 伺服器準備同步標籤: 位置={}, 升級數據已包含", worldPosition);
            return tag;
        }


        /**
         * 🌐 處理收到的同步標籤 - 客戶端接收數據
         * 客戶端收到同步數據時會調用此方法
         */
        @Override
        public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
            // 🔧 載入所有同步的數據
            loadAdditional(tag, registries);

            LOGGER.debug("🌐 客戶端收到同步標籤: 位置={}, 升級數據已更新", worldPosition);
        }

        /**
         * 🌐 獲取更新封包 - 用於區塊更新時同步
         * 當調用 level.sendBlockUpdated 時會使用此方法
         */
        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        /**
         * 🌐 處理數據封包 - 客戶端處理區塊更新
         * 客戶端收到區塊更新封包時會調用此方法
         */
        @Override
        public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
            CompoundTag tag = pkt.getTag();
            if (tag != null) {
                handleUpdateTag(tag, registries);
                LOGGER.debug("🌐 客戶端處理數據封包: 位置={}", worldPosition);
            }
        }

    }
