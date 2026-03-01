package com.github.nalamodikk.common.block.blockentity.mana_infuser;

import com.github.nalamodikk.common.block.blockentity.mana_infuser.sync.ManaInfuserSyncHelper;
import com.github.nalamodikk.common.block.blockentity.manabase.AbstractManaMachineEntityBlock;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlockEntities;
import com.github.nalamodikk.register.ModRecipes;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Optional;

public class ManaInfuserBlockEntity extends AbstractManaMachineEntityBlock {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int SLOT_COUNT = 2;

    private static final int MAX_MANA_CAPACITY = 50000;
    private static final int MANA_TRANSFER_RATE = 200;
    private static final int INFUSION_TIME = 60;
    private static final int MANA_PER_CYCLE = 0;
    private static final int INTERVAL_TICK = 5;
    private static final int STATE_SYNC_INTERVAL_TICKS = 100; // 5 秒（20 tick/s）

    private final ManaInfuserSyncHelper syncHelper = new ManaInfuserSyncHelper();
    private final EnumMap<Direction, IOHandlerUtils.IOType> directionConfig = new EnumMap<>(Direction.class);
    private ManaInfuserRecipe currentRecipe = null;
    private boolean hasInputChanged = false;
    private boolean needsSync = false;
    private int stateSyncTicker = 0;
    private boolean lastComputedWorking = false;

    public ManaInfuserBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MANA_INFUSER.get(), pos, blockState, false, 0, MAX_MANA_CAPACITY, INTERVAL_TICK, 0);
        this.maxProgress = INFUSION_TIME;
        initializeIOConfig();
    }
    
    public ManaInfuserSyncHelper getSyncHelper() {
        return syncHelper;
    }

    private void initializeIOConfig() {
        directionConfig.put(Direction.UP, IOHandlerUtils.IOType.INPUT);
        directionConfig.put(Direction.DOWN, IOHandlerUtils.IOType.OUTPUT);
        directionConfig.put(Direction.NORTH, IOHandlerUtils.IOType.BOTH);
        directionConfig.put(Direction.SOUTH, IOHandlerUtils.IOType.BOTH);
        directionConfig.put(Direction.EAST, IOHandlerUtils.IOType.BOTH);
        directionConfig.put(Direction.WEST, IOHandlerUtils.IOType.BOTH);
    }

    @Override
    protected ItemStackHandler createHandler() {
        return new ItemStackHandler(SLOT_COUNT) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                hasInputChanged = true;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot == INPUT_SLOT) {
                    return hasRecipeForItem(stack);
                } else if (slot == OUTPUT_SLOT) {
                    return false;
                }
                return super.isItemValid(slot, stack);
            }
        };
    }

    @Override
    public void tickMachine() {
        if (level == null || level.isClientSide()) return;

        syncHelper.syncFrom(this);

        if (tickCounter % 20 == 0) {
            extractManaFromNeighbors();
        }

        if (hasInputChanged) {
            updateCurrentRecipe();
            hasInputChanged = false;
        }

        processInfusion();
        syncWorkingBlockState();
        
        if (needsSync) {
            syncToClient();
            needsSync = false;
        }

        tickCounter++;
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected boolean canGenerate() {
        if (currentRecipe == null) return false;
        if (manaStorage != null && manaStorage.getManaStored() < currentRecipe.getManaCost()) return false;

        ItemStack input = itemHandler != null ? itemHandler.getStackInSlot(INPUT_SLOT) : ItemStack.EMPTY;
        if (input.getCount() < currentRecipe.getInputCount()) return false;

        ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (!output.isEmpty()) {
            ItemStack result = currentRecipe.getResult();
            if (!ItemStack.isSameItemSameComponents(output, result) ||
                    output.getCount() + result.getCount() > output.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    private void processInfusion() {
        if (!canGenerate()) {
            if (progress > 0) {
                progress = 0;
                setChanged();
            }
            return;
        }

        progress += scaleProgressByOwner(1);
        setChanged();
        if (progress >= maxProgress) {
            completeInfusion();
            progress = 0;
            setChanged();
        }
    }

    private void syncWorkingBlockState() {
        boolean working = isWorking();
        stateSyncTicker++;
        boolean workingChanged = working != lastComputedWorking;
        lastComputedWorking = working;
        if (workingChanged || stateSyncTicker >= STATE_SYNC_INTERVAL_TICKS) {
            updateBlockWorkingState(working);
            stateSyncTicker = 0;
        }
    }

    private void updateBlockWorkingState(boolean working) {
        if (level != null && !level.isClientSide()) {
            BlockState currentState = getBlockState();
            if (currentState.hasProperty(ManaInfuserBlock.WORKING)) {
                BlockState newState = currentState.setValue(ManaInfuserBlock.WORKING, working);
                if (!currentState.equals(newState)) {
                    level.setBlock(worldPosition, newState, 3);
                }
            }
        }
    }

    private void completeInfusion() {
        if (currentRecipe == null) return;
        manaStorage.extractMana(currentRecipe.getManaCost(), ManaAction.EXECUTE);
        itemHandler.extractItem(INPUT_SLOT, currentRecipe.getInputCount(), false);

        ItemStack result = currentRecipe.getResult().copy();
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (currentOutput.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, result);
        } else {
            currentOutput.grow(result.getCount());
        }
        onGenerate(currentRecipe.getManaCost());
    }

    private void updateCurrentRecipe() {
        if (level == null || level.isClientSide()) return;
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            currentRecipe = null;
            maxProgress = INFUSION_TIME;
            progress = 0;
            return;
        }
        if (currentRecipe != null && currentRecipe.getInput().test(input)) return;

        ManaInfuserRecipe.ManaInfuserInput recipeInput = new ManaInfuserRecipe.ManaInfuserInput(input);
        Optional<RecipeHolder<ManaInfuserRecipe>> recipeHolder = level.getRecipeManager()
                .getRecipeFor(ModRecipes.MANA_INFUSER_TYPE.get(), recipeInput, level);

        if (recipeHolder.isPresent()) {
            currentRecipe = recipeHolder.get().value();
            maxProgress = currentRecipe.getInfusionTime();
        } else {
            currentRecipe = null;
            maxProgress = INFUSION_TIME;
        }
        progress = 0;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        currentRecipe = null;
        directionConfig.clear();
        hasInputChanged = false;
    }

    private void extractManaFromNeighbors() {
        if (level == null || level.isClientSide() || manaStorage == null) return;
        if (manaStorage.getManaStored() >= manaStorage.getMaxManaStored()) return;

        IOHandlerUtils.extractManaFromNeighbors(level, worldPosition, manaStorage, directionConfig, MANA_TRANSFER_RATE);
    }

    private boolean hasRecipeForItem(ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;
        ManaInfuserRecipe.ManaInfuserInput input = new ManaInfuserRecipe.ManaInfuserInput(stack);
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.MANA_INFUSER_TYPE.get(), input, level)
                .isPresent();
    }

    @Override
    protected void onGenerate(int amount) {
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ManaInfuserMenu(id, inv, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.koniava.mana_infuser");
    }

    // === 🔧 配置相關 ===

    public void onNeighborChanged() {
        // no-op
    }

    public void toggleIOMode(Direction direction) {
        IOHandlerUtils.IOType currentType = directionConfig.get(direction);
        IOHandlerUtils.IOType nextType = IOHandlerUtils.nextIOType(currentType);
        directionConfig.put(direction, nextType);
        setChanged();
        syncToClient();
    }

    public IOHandlerUtils.IOType getIOMode(Direction direction) {
        return directionConfig.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);
    }

    @Override
    public void setIOConfig(Direction direction, IOHandlerUtils.IOType type) {
        directionConfig.put(direction, type);
        setChanged();
        needsSync = true;
    }

    @Override
    public IOHandlerUtils.IOType getIOConfig(Direction direction) {
        return directionConfig.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);
    }

    @Override
    public EnumMap<Direction, IOHandlerUtils.IOType> getIOMap() {
        return new EnumMap<>(directionConfig);
    }

    @Override
    public void setIOMap(EnumMap<Direction, IOHandlerUtils.IOType> map) {
        directionConfig.clear();
        directionConfig.putAll(map);
        setChanged();
        needsSync = true;
    }

    public int getCurrentMana() {
        return manaStorage != null ? manaStorage.getManaStored() : 0;
    }

    public int getMaxMana() {
        return manaStorage != null ? manaStorage.getMaxManaStored() : 0;
    }

    public boolean isWorking() {
        return progress > 0;
    }

    public int getInfusionProgress() {
        return progress;
    }

    public int getMaxInfusionTime() {
        return maxProgress;
    }

    @Nullable
    public ManaInfuserRecipe getCurrentRecipe() {
        return currentRecipe;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag ioTag = new CompoundTag();
        for (Direction dir : Direction.values()) {
            ioTag.putString(dir.name(), directionConfig.get(dir).name());
        }
        tag.put("IOConfig", ioTag);
        tag.putBoolean("HasRecipe", currentRecipe != null);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("IOConfig")) {
            CompoundTag ioTag = tag.getCompound("IOConfig");
            for (Direction dir : Direction.values()) {
                if (ioTag.contains(dir.name())) {
                    try {
                        IOHandlerUtils.IOType type = IOHandlerUtils.IOType.valueOf(ioTag.getString(dir.name()));
                        directionConfig.put(dir, type);
                    } catch (IllegalArgumentException e) {
                        directionConfig.put(dir, IOHandlerUtils.IOType.BOTH);
                    }
                }
            }
        }
        if (tag.getBoolean("HasRecipe")) hasInputChanged = true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        loadAdditional(tag, lookupProvider);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        handleUpdateTag(pkt.getTag(), lookupProvider);
    }
}
