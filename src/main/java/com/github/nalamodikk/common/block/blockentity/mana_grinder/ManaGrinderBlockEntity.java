package com.github.nalamodikk.common.block.blockentity.mana_grinder;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlockEntity;
import com.github.nalamodikk.common.block.blockentity.manabase.AbstractManaMachineEntityBlock;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.coreapi.recipe.ProcessingRecipe;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.research.ResearchGate;
import com.github.nalamodikk.register.ModBlockEntities;
import com.github.nalamodikk.register.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import com.github.nalamodikk.common.block.blockentity.mana_grinder.sync.ManaGrinderSyncHelper;
import com.github.nalamodikk.common.utils.upgrade.UpgradeInventory;
import com.github.nalamodikk.common.utils.upgrade.UpgradeType;
import com.github.nalamodikk.common.utils.upgrade.api.IUpgradeableMachine;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * ⚙️ 魔力粉碎機 BlockEntity
 */
public class ManaGrinderBlockEntity extends AbstractManaMachineEntityBlock implements IUpgradeableMachine {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManaGrinderBlockEntity.class);

    // === 📦 槽位定義 ===
    private static final int INPUT_SLOT_1 = 0;
    private static final int INPUT_SLOT_2 = 1;
    private static final int OUTPUT_SLOT_1 = 2;
    private static final int OUTPUT_SLOT_2 = 3;
    private static final int OUTPUT_SLOT_3 = 4;
    private static final int OUTPUT_SLOT_4 = 5;
    private static final int SLOT_COUNT = 6;

    // === 🔧 配置常量 ===
    private static final int MAX_MANA_CAPACITY = 100000;
    private static final int GRINDING_TIME = 200;  // 10 秒
    private static final int INTERVAL_TICK = 1;
    private static final int STATE_SYNC_INTERVAL_TICKS = 100; // 5 秒（20 tick/s）

    // === 📊 同步助手 ===
    private final ManaGrinderSyncHelper syncHelper = new ManaGrinderSyncHelper();

    // === ⚡ 升級系統 ===
    private final UpgradeInventory upgradeInventory = new UpgradeInventory(4);

    // === 📊 狀態變量 ===
    private final EnumMap<Direction, IOHandlerUtils.IOType> directionConfig = new EnumMap<>(Direction.class);
    private ProcessingRecipe currentRecipe = null;
    private boolean hasInputChanged = false;
    private int manaSpent = 0;
    private int stateSyncTicker = 0;
    private boolean lastComputedWorking = false;

    public ManaGrinderBlockEntity(BlockPos pos, BlockState blockState) {
        super(
                ModBlockEntities.MANA_GRINDER_BE.get(),
                pos,
                blockState,
                false,
                0,
                MAX_MANA_CAPACITY,
                INTERVAL_TICK,
                0
        );

        this.maxProgress = GRINDING_TIME;
        initializeIOConfig();
    }
    
    public static int getMaxMana() {
        return MAX_MANA_CAPACITY;
    }

    public ManaGrinderSyncHelper getSyncHelper() {
        return syncHelper;
    }

    // === 🏗️ 初始化 ===

    /**
     * 🔧 初始化 IO 配置
     */
    private void initializeIOConfig() {
        directionConfig.put(Direction.UP, getDefaultIOType(Direction.UP));
        directionConfig.put(Direction.DOWN, getDefaultIOType(Direction.DOWN));
        directionConfig.put(Direction.NORTH, getDefaultIOType(Direction.NORTH));
        directionConfig.put(Direction.SOUTH, getDefaultIOType(Direction.SOUTH));
        directionConfig.put(Direction.EAST, getDefaultIOType(Direction.EAST));
        directionConfig.put(Direction.WEST, getDefaultIOType(Direction.WEST));
    }

    /**
     * 📦 創建物品處理器
     */
    @Override
    protected ItemStackHandler createHandler() {
        return new ItemStackHandler(SLOT_COUNT) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                if (slot < 2) { // 輸入槽變化
                    hasInputChanged = true;
                }
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot == INPUT_SLOT_1 || slot == INPUT_SLOT_2) {
                    // 輸入槽檢查是否有有效配方
                    return canGrind(stack);
                } else if (slot >= OUTPUT_SLOT_1 && slot <= OUTPUT_SLOT_4) {
                    return true; // 允許機器內部輸出插入
                }
                return super.isItemValid(slot, stack);
            }
        };
    }

    @Override
    protected boolean canExternalInsertToSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == INPUT_SLOT_1 || slot == INPUT_SLOT_2;
    }

    @Override
    protected boolean canExternalExtractFromSlot(int slot, @Nullable Direction side) {
        return slot >= OUTPUT_SLOT_1 && slot <= OUTPUT_SLOT_4;
    }

    @Override
    public void tickMachine() {
        if (level == null || level.isClientSide()) return;
        boolean previousComputedWorking = lastComputedWorking;

        if (!ResearchGate.canOperate("mana_grinder", level, ownerId)) {
            currentRecipe = null;
            progress = 0;
            manaSpent = 0;
            lastComputedWorking = false;
            updateBlockWorkingState(false);
            syncHelper.syncFrom(this);
            return;
        }

        // 2. 處理輸入變化
        if (hasInputChanged) {
            updateCurrentRecipe();
            hasInputChanged = false;
        }

        // 3. 嘗試進行研磨
        boolean progressedThisTick = false;
        if (currentRecipe != null && progress < maxProgress) {
            int progressStep = scaleProgressByOwner(1) * getSpeedMultiplier();
            int totalManaCost = currentRecipe.getManaCost() / getEfficiencyMultiplier();
            int newProgress = Math.min(progress + progressStep, maxProgress);
            int targetSpent = maxProgress > 0
                    ? (int) Math.floor((newProgress / (double) maxProgress) * totalManaCost)
                    : totalManaCost;
            int costThisTick = Math.max(0, targetSpent - manaSpent);

            if (manaStorage != null && canConsumeForThisTick(totalManaCost, costThisTick)) {
                if (costThisTick > 0) {
                    manaStorage.extractMana(costThisTick, ManaAction.EXECUTE);
                    manaSpent += costThisTick;
                }
                progress = newProgress;
                progressedThisTick = true;
                setChanged();
            }
        }

        // 4. 完成時輸出結果
        if (currentRecipe != null && progress >= maxProgress) {
            finishGrinding();
        }

        boolean isWorking = progressedThisTick;
        stateSyncTicker++;
        boolean workingChanged = previousComputedWorking != isWorking;
        boolean blockStateMismatch = isBlockStateWorking() != isWorking;
        lastComputedWorking = isWorking;

        if (workingChanged || blockStateMismatch || stateSyncTicker >= STATE_SYNC_INTERVAL_TICKS) {
            updateBlockWorkingState(isWorking);
            stateSyncTicker = 0;
        }

        // 5. 同步數據到 Helper (由 Menu 讀取)；放在最後確保本 tick 即時反映
        syncHelper.syncFrom(this);
    }

    /**
     * 🔍 更新當前配方
     */
    private void updateCurrentRecipe() {
        currentRecipe = null;
        progress = 0;
        maxProgress = GRINDING_TIME;
        manaSpent = 0;

        if (itemHandler == null) return;

        ItemStack input1 = itemHandler.getStackInSlot(INPUT_SLOT_1);
        ItemStack input2 = itemHandler.getStackInSlot(INPUT_SLOT_2);

        if (input1.isEmpty() && input2.isEmpty()) {
            return;
        }

        // 查找配方
        List<ItemStack> inputs = new ArrayList<>(2);
        if (!input1.isEmpty()) inputs.add(input1);
        if (!input2.isEmpty()) inputs.add(input2);

        ProcessingRecipe.ProcessingInput recipeInput = new ProcessingRecipe.ProcessingInput(
                inputs,
                "grinder"
        );

        if (level == null) return;

        ProcessingRecipe matchedRecipe = null;
        for (RecipeHolder<ProcessingRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.PROCESSING_TYPE.get())) {
            if (holder.value().matches(recipeInput, level)) {
                matchedRecipe = holder.value();
                break;
            }
        }

        if (matchedRecipe != null) {
            currentRecipe = matchedRecipe;
            maxProgress = Math.max(1, currentRecipe.getProcessingTime());
            manaSpent = 0;
            if (KoniavacraftMod.IS_DEV) {
                LOGGER.info("Found recipe for mana grinder");
            }
        }
    }

    /**
     * ✅ 完成研磨，輸出結果
     */
    private void finishGrinding() {
        if (currentRecipe == null || itemHandler == null) return;

        // 輸出主產物
        ItemStack mainOutput = currentRecipe.getMainOutput().copy();
        if (!itemHandler.insertItem(OUTPUT_SLOT_1, mainOutput, false).isEmpty()) {
            // 失敗，等待槽位空出
            return;
        }

        // 輸出概率副產物
        for (ProcessingRecipe.ChanceOutput chanceOutput : currentRecipe.getChanceOutputs()) {
            if (Math.random() < chanceOutput.getChance()) {
                ItemStack output = chanceOutput.getOutput().copy();
                for (int slot = OUTPUT_SLOT_2; slot <= OUTPUT_SLOT_4; slot++) {
                    ItemStack result = itemHandler.insertItem(slot, output, false);
                    if (result.isEmpty()) {
                        break;
                    }
                    output = result;
                }
            }
        }

        // 消耗輸入物品（依實際放置的輸入）
        int inputCount = currentRecipe.getInputs().size();
        ItemStack input1 = itemHandler.getStackInSlot(INPUT_SLOT_1);
        ItemStack input2 = itemHandler.getStackInSlot(INPUT_SLOT_2);

        if (inputCount >= 2) {
            if (!input1.isEmpty()) {
                itemHandler.extractItem(INPUT_SLOT_1, 1, false);
            }
            if (!input2.isEmpty()) {
                itemHandler.extractItem(INPUT_SLOT_2, 1, false);
            }
        } else if (inputCount == 1) {
            if (!input1.isEmpty()) {
                itemHandler.extractItem(INPUT_SLOT_1, 1, false);
            } else if (!input2.isEmpty()) {
                itemHandler.extractItem(INPUT_SLOT_2, 1, false);
            }
        }

        // 重置狀態
        progress = 0;
        currentRecipe = null;
        hasInputChanged = true;
        manaSpent = 0;

        if (KoniavacraftMod.IS_DEV) {
            LOGGER.info("Grinding finished, output produced");
        }
    }

    /**
     * 🔍 判斷物品是否可以研磨
     */
    private boolean canGrind(ItemStack stack) {
        if (stack.isEmpty() || level == null || level.isClientSide()) return false;

        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(stack);

        ProcessingRecipe.ProcessingInput recipeInput = new ProcessingRecipe.ProcessingInput(
                inputs,
                "grinder"
        );

        for (RecipeHolder<ProcessingRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.PROCESSING_TYPE.get())) {
            if (holder.value().matches(recipeInput, level)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean canGenerate() {
        return false; // 粉碎機不生成魔力
    }

    // === ⚡ IUpgradeableMachine 實作 ===

    @Override
    public UpgradeInventory getUpgradeInventory() {
        return upgradeInventory;
    }

    @Override
    public boolean supportsUpgradeType(UpgradeType type) {
        return type == UpgradeType.SPEED || type == UpgradeType.EFFICIENCY;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    // === 🔧 IConfigurableBlock 實作 ===

    @Override
    public void setIOConfig(Direction direction, IOHandlerUtils.IOType type) {
        directionConfig.put(direction, type);
        onIOConfigChanged();
    }

    public void markInputChanged() {
        hasInputChanged = true;
    }

    @Override
    public IOHandlerUtils.IOType getIOConfig(Direction direction) {
        return directionConfig.getOrDefault(direction, getDefaultIOType(direction));
    }

    @Override
    public EnumMap<Direction, IOHandlerUtils.IOType> getIOMap() {
        return new EnumMap<>(directionConfig);
    }

    @Override
    public void setIOMap(EnumMap<Direction, IOHandlerUtils.IOType> map) {
        directionConfig.clear();
        for (Direction dir : Direction.values()) {
            directionConfig.put(dir, map.getOrDefault(dir, getDefaultIOType(dir)));
        }
        onIOConfigChanged();
    }

    // === 📦 Menu 支援 ===

    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new ManaGrinderMenu(pContainerId, pPlayerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.koniava.mana_grinder");
    }

    // === 💾 數據保存 ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        if (itemHandler != null) {
            CompoundTag itemsTag = itemHandler.serializeNBT(registries);
            tag.put("Items", itemsTag);
        }

        tag.put("DirectionConfig", serializeIOMap());
        tag.putInt("ManaSpent", manaSpent);
        tag.put("UpgradeInventory", upgradeInventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("Items") && itemHandler != null) {
            CompoundTag itemsTag = tag.getCompound("Items");
            itemHandler.deserializeNBT(registries, itemsTag);
        }

        if (tag.contains("DirectionConfig")) {
            deserializeIOMap(tag.getCompound("DirectionConfig"));
        }

        manaSpent = tag.getInt("ManaSpent");
        if (tag.contains("UpgradeInventory")) {
            upgradeInventory.deserializeNBT(registries, tag.getCompound("UpgradeInventory"));
        }

        hasInputChanged = true;
    }

    private CompoundTag serializeIOMap() {
        CompoundTag tag = new CompoundTag();
        for (Direction direction : Direction.values()) {
            tag.putString(direction.getName(), directionConfig.get(direction).name());
        }
        return tag;
    }

    private void deserializeIOMap(CompoundTag tag) {
        directionConfig.clear();
        initializeIOConfig();

        for (Direction direction : Direction.values()) {
            String key = direction.getName();
            if (!tag.contains(key)) {
                continue;
            }

            if (tag.contains(key, Tag.TAG_STRING)) {
                String typeName = tag.getString(key);
                try {
                    directionConfig.put(direction, IOHandlerUtils.IOType.valueOf(typeName));
                } catch (IllegalArgumentException e) {
                    directionConfig.put(direction, getDefaultIOType(direction));
                }
                continue;
            }

            // 舊版相容：Boolean 配置（true=OUTPUT，false=維持預設）
            if (tag.contains(key, Tag.TAG_BYTE)) {
                boolean oldOutput = tag.getBoolean(key);
                directionConfig.put(direction, oldOutput
                        ? IOHandlerUtils.IOType.OUTPUT
                        : getDefaultIOType(direction));
            }
        }

        // 保底修復：避免歷史版本把整張配置降成全 DISABLED，導致導管完全無法互動
        if (isAllSidesDisabled()) {
            directionConfig.clear();
            initializeIOConfig();
        }
    }

    private IOHandlerUtils.IOType getDefaultIOType(Direction direction) {
        return switch (direction) {
            case UP -> IOHandlerUtils.IOType.INPUT;
            case DOWN -> IOHandlerUtils.IOType.OUTPUT;
            case NORTH, SOUTH, EAST, WEST -> IOHandlerUtils.IOType.BOTH;
        };
    }

    private boolean isAllSidesDisabled() {
        for (Direction direction : Direction.values()) {
            if (directionConfig.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED) != IOHandlerUtils.IOType.DISABLED) {
                return false;
            }
        }
        return true;
    }

    private void updateBlockWorkingState(boolean working) {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState currentState = getBlockState();
        if (!currentState.hasProperty(ManaGrinderBlock.WORKING)) {
            return;
        }
        if (currentState.getValue(ManaGrinderBlock.WORKING) == working) {
            return;
        }
        level.setBlock(worldPosition, currentState.setValue(ManaGrinderBlock.WORKING, working), 3);
    }

    private boolean isBlockStateWorking() {
        BlockState state = getBlockState();
        return state.hasProperty(ManaGrinderBlock.WORKING) && state.getValue(ManaGrinderBlock.WORKING);
    }

    private boolean canConsumeForThisTick(int totalManaCost, int costThisTick) {
        if (manaStorage == null) {
            return false;
        }
        if (totalManaCost <= 0) {
            return true;
        }
        if (costThisTick > 0) {
            return manaStorage.getManaStored() >= costThisTick;
        }
        // 比例扣魔在某些 tick 可能為 0，這裡仍要求至少有 1 點魔力才可推進進度
        return manaStorage.getManaStored() > 0;
    }

    private void onIOConfigChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.invalidateCapabilities(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            notifyNeighborsOfIOChange();
        }
    }

    private void notifyNeighborsOfIOChange() {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState currentState = level.getBlockState(worldPosition);
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(direction);
            level.neighborChanged(neighborPos, currentState.getBlock(), worldPosition);

            BlockEntity neighborBE = level.getBlockEntity(neighborPos);
            if (neighborBE instanceof ArcaneConduitBlockEntity conduit) {
                conduit.onNeighborChanged();
            }
        }
    }
}
