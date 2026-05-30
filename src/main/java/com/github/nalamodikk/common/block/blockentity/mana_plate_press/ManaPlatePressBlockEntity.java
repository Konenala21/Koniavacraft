package com.github.nalamodikk.common.block.blockentity.mana_plate_press;

import com.github.nalamodikk.common.block.blockentity.mana_plate_press.sync.ManaPlatePressSync;
import com.github.nalamodikk.common.block.blockentity.manabase.AbstractManaMachineEntityBlock;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlockEntities;
import com.github.nalamodikk.register.ModRecipes;
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

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Optional;

public class ManaPlatePressBlockEntity extends AbstractManaMachineEntityBlock {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int SLOT_COUNT = 2;

    private static final int MAX_MANA_CAPACITY = 30000;
    private static final int MANA_TRANSFER_RATE = 150;
    private static final int DEFAULT_PRESSING_TIME = 80;
    private static final int INTERVAL_TICK = 5;

    private final ManaPlatePressSync syncHelper = new ManaPlatePressSync();
    private final EnumMap<Direction, IOHandlerUtils.IOType> directionConfig = new EnumMap<>(Direction.class);
    private ManaPlatePressRecipe currentRecipe = null;
    private boolean hasInputChanged = false;
    private boolean lastComputedWorking = false;
    private int stateSyncTicker = 0;

    public ManaPlatePressBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MANA_PLATE_PRESS_BE.get(), pos, blockState, false, 0, MAX_MANA_CAPACITY, INTERVAL_TICK, 0);
        this.maxProgress = DEFAULT_PRESSING_TIME;
        initializeIOConfig();
    }

    public ManaPlatePressSync getSyncHelper() { return syncHelper; }

    private void initializeIOConfig() {
        directionConfig.put(Direction.UP, IOHandlerUtils.IOType.INPUT);
        directionConfig.put(Direction.DOWN, IOHandlerUtils.IOType.OUTPUT);
        directionConfig.put(Direction.NORTH, IOHandlerUtils.IOType.BOTH);
        directionConfig.put(Direction.SOUTH, IOHandlerUtils.IOType.BOTH);
        directionConfig.put(Direction.EAST, IOHandlerUtils.IOType.BOTH);
        directionConfig.put(Direction.WEST, IOHandlerUtils.IOType.BOTH);
    }

    @Override
    public void setIOMap(EnumMap<Direction, IOHandlerUtils.IOType> map) {
        directionConfig.clear();
        directionConfig.putAll(map);
        setChanged();
    }

    @Override
    public EnumMap<Direction, IOHandlerUtils.IOType> getIOMap() {
        return directionConfig;
    }

    @Override
    public void setIOConfig(Direction direction, IOHandlerUtils.IOType type) {
        directionConfig.put(direction, type);
        setChanged();
    }

    @Override
    public IOHandlerUtils.IOType getIOConfig(Direction direction) {
        return directionConfig.getOrDefault(direction, IOHandlerUtils.IOType.DISABLED);
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
                if (slot == INPUT_SLOT) return hasRecipeForItem(stack);
                return slot != OUTPUT_SLOT && super.isItemValid(slot, stack);
            }
        };
    }

    @Override
    protected boolean canExternalInsertToSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == INPUT_SLOT;
    }

    @Override
    protected boolean canExternalExtractFromSlot(int slot, @Nullable Direction side) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    public void tickMachine() {
        if (level == null || level.isClientSide()) return;

        syncHelper.syncFrom(this);

        if (tickCounter % 20 == 0) extractManaFromNeighbors();

        if (hasInputChanged) {
            updateCurrentRecipe();
            hasInputChanged = false;
        }

        processPressing();
        syncWorkingBlockState();

        tickCounter++;
    }

    @Override
    protected boolean canGenerate() {
        if (currentRecipe == null) return false;
        if (manaStorage == null || manaStorage.getManaStored() < currentRecipe.getManaCost()) return false;

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

    private void processPressing() {
        if (!canGenerate()) {
            if (progress > 0) {
                progress = 0;
                setChanged();
            }
            return;
        }

        progress++;
        setChanged();
        if (progress >= maxProgress) {
            completePressing();
            progress = 0;
            setChanged();
        }
    }

    private void completePressing() {
        if (currentRecipe == null) return;
        manaStorage.extractMana(currentRecipe.getManaCost(), ManaAction.EXECUTE);
        itemHandler.extractItem(INPUT_SLOT, currentRecipe.getInputCount(), false);

        ItemStack result = currentRecipe.getResult().copy();
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (currentOutput.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, result);
        } else {
            currentOutput.grow(result.getCount());
            setChanged();
        }
        onGenerate(currentRecipe.getManaCost());
    }

    private void syncWorkingBlockState() {
        boolean working = isWorking();
        stateSyncTicker++;
        boolean changed = working != lastComputedWorking;
        lastComputedWorking = working;
        if (changed || stateSyncTicker >= 100) {
            updateBlockWorkingState(working);
            stateSyncTicker = 0;
        }
    }

    private void updateBlockWorkingState(boolean working) {
        if (level != null && !level.isClientSide()) {
            BlockState currentState = getBlockState();
            if (currentState.hasProperty(ManaPlatePressBlock.WORKING)) {
                BlockState newState = currentState.setValue(ManaPlatePressBlock.WORKING, working);
                if (!currentState.equals(newState)) {
                    level.setBlock(worldPosition, newState, 3);
                }
            }
        }
    }

    private void updateCurrentRecipe() {
        if (level == null || level.isClientSide()) return;
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            currentRecipe = null;
            maxProgress = DEFAULT_PRESSING_TIME;
            progress = 0;
            return;
        }
        if (currentRecipe != null && currentRecipe.getInput().test(input)) return;

        ManaPlatePressRecipe.PlatePressInput recipeInput = new ManaPlatePressRecipe.PlatePressInput(input);
        Optional<RecipeHolder<ManaPlatePressRecipe>> holder = level.getRecipeManager()
                .getRecipeFor(ModRecipes.PLATE_PRESS_TYPE.get(), recipeInput, level);

        if (holder.isPresent()) {
            currentRecipe = holder.get().value();
            maxProgress = currentRecipe.getPressingTime();
        } else {
            currentRecipe = null;
            maxProgress = DEFAULT_PRESSING_TIME;
        }
        progress = 0;
    }

    private void extractManaFromNeighbors() {
        if (level == null || level.isClientSide() || manaStorage == null) return;
        if (manaStorage.getManaStored() >= manaStorage.getMaxManaStored()) return;
        IOHandlerUtils.extractManaFromNeighbors(level, worldPosition, manaStorage, directionConfig, MANA_TRANSFER_RATE);
    }

    private boolean hasRecipeForItem(ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;
        ManaPlatePressRecipe.PlatePressInput input = new ManaPlatePressRecipe.PlatePressInput(stack);
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.PLATE_PRESS_TYPE.get(), input, level)
                .isPresent();
    }

    @Override
    protected void onGenerate(int amount) {}

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ManaPlatePressMenu(id, inv, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.koniava.mana_plate_press");
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        currentRecipe = null;
        directionConfig.clear();
        hasInputChanged = false;
    }

    public int getCurrentMana() { return manaStorage != null ? manaStorage.getManaStored() : 0; }
    public int getMaxMana() { return manaStorage != null ? manaStorage.getMaxManaStored() : 0; }
    // progress 在每次合成完成那一 tick 會歸 0；若此刻還能繼續加工就維持 working，
    // 避免 WORKING blockstate 每個循環閃一下 false，導致 BER 壓印動畫週期性抽動。
    public boolean isWorking() { return progress > 0 || canGenerate(); }
    public int getPressingProgress() { return progress; }
    public int getMaxPressingTime() { return maxProgress; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag ioTag = new CompoundTag();
        for (Direction dir : Direction.values()) {
            ioTag.putString(dir.name(), directionConfig.getOrDefault(dir, IOHandlerUtils.IOType.BOTH).name());
        }
        tag.put("IOConfig", ioTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("IOConfig")) {
            CompoundTag ioTag = tag.getCompound("IOConfig");
            for (Direction dir : Direction.values()) {
                if (ioTag.contains(dir.name())) {
                    try {
                        directionConfig.put(dir, IOHandlerUtils.IOType.valueOf(ioTag.getString(dir.name())));
                    } catch (IllegalArgumentException e) {
                        directionConfig.put(dir, IOHandlerUtils.IOType.BOTH);
                    }
                }
            }
        }
        hasInputChanged = true;
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
