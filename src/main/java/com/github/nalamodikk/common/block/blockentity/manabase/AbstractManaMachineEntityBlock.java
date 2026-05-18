package com.github.nalamodikk.common.block.blockentity.manabase;

import com.github.nalamodikk.common.sync.annotation.Sync;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.compat.energy.ModNeoNalaEnergyStorage;
import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.util.UUID;

/**
 * 機器方塊實體的抽象基底類別。
 * 可選擇性支援魔力儲存、能量儲存、流體儲存、物品儲存等功能，
 * 並可透過覆寫對應方法客製化運作邏輯。
 */
public abstract class AbstractManaMachineEntityBlock extends BlockEntity implements MenuProvider , IConfigurableBlock {
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 魔力儲存（可選） */
    private static final int MAX_MANA = 200000;

    @Nullable
    @Sync(readOnly = true)
    protected final  ManaStorage manaStorage;

    /** 能量儲存（可選） */
    @Nullable
    @Sync(readOnly = true)
    protected final ModNeoNalaEnergyStorage energyStorage;

    /** 物品儲存槽（可選） */
    @Nullable
    protected final ItemStackHandler itemHandler;

    /** 流體儲存槽（可選） */
    @Nullable
    protected final FluidTank fluidTank;

    /** 是否具有能量儲存能力 */
    protected final boolean hasEnergy;


    /** 每次生產的魔力量 */
    protected int manaPerCycle;

    /** 當前進度 */
    @Sync(readOnly = true)
    protected int progress = 0;

    /** 所需最大進度 */
    @Sync(readOnly = true)
    protected int maxProgress = 100;

    /** 每 tick 所需能量（如需支援能耗） */
    protected int energyPerTick = 20;

    /** 內部 tick 計數器 */
    protected int tickCounter = 0;

    /** 每次生成魔力的間隔 tick 數 */
    protected int intervalTick;

    /** 最大存儲能量*/
    protected final int maxEnergy;
    @Nullable
    protected UUID ownerId;

    /**
     * 建構子
     *
     * @param type BlockEntity 類型
     * @param pos 方塊座標
     * @param state 方塊狀態
     * @param hasEnergy 是否支援能量儲存
     * @param maxMana 最大魔力儲存值（若不需支援魔力可填 0）
     * @param intervalTick 每幾 tick 執行一次生產行為
     * @param manaPerCycle 每次執行時生產的魔力量
     */
    public AbstractManaMachineEntityBlock(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean hasEnergy,int maxEnergy, int maxMana, int intervalTick, int manaPerCycle) {
        super(type, pos, state);
        this.hasEnergy = hasEnergy;
        this.manaStorage = maxMana > 0 ? new ManaStorage(maxMana, this::markStorageChanged) : null;
        this.energyStorage = hasEnergy ? new ModNeoNalaEnergyStorage(BigInteger.valueOf(maxEnergy), this::markStorageChanged) : null;
        this.itemHandler = createHandler();
        this.fluidTank = createFluidTank();
        this.intervalTick = intervalTick;
        this.manaPerCycle = manaPerCycle;
        this.maxEnergy = maxEnergy;

    }

    private void markStorageChanged() {
        if (level != null && !level.isClientSide) {
            super.setChanged();
        }
    }

    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
    public int getEnergyPerTick() { return energyPerTick; }
    public boolean hasEnergy() { return hasEnergy; }
    public @Nullable ManaStorage getManaStorage() { return manaStorage; }
    public @Nullable ModNeoNalaEnergyStorage getEnergyStorage() { return energyStorage; }
    public @Nullable ItemStackHandler getItemHandler() { return itemHandler; }
    public @Nullable IItemHandler getItemHandlerForSide(@Nullable Direction side) {
        if (itemHandler == null) {
            return null;
        }
        if (side != null && getIOConfig(side) == IOHandlerUtils.IOType.DISABLED) {
            return null;
        }
        return new MachineSidedItemHandler(this, side);
    }
    public @Nullable FluidTank getFluidTank() { return fluidTank; }
    public int getMaxEnergyCapacity() {return maxEnergy;}
    public void setOwnerId(@Nullable UUID ownerId) { this.ownerId = ownerId; }
    public @Nullable UUID getOwnerId() { return ownerId; }
    public float getOwnerGenerationMultiplier() {
        // 平衡調整：機器產能不再受 RPG 配點影響
        return 1.0f;
    }

    protected int scaleByOwner(int base) {
        if (base <= 0) {
            return 0;
        }
        return Math.max(0, Math.round(base * getOwnerGenerationMultiplier()));
    }

    protected int scaleProgressByOwner(int base) {
        if (base <= 0) {
            return 0;
        }
        return Math.max(1, Math.round(base * getOwnerGenerationMultiplier()));
    }

    boolean canReceiveItemsFromSide(@Nullable Direction side) {
        if (side == null) {
            return true;
        }
        IOHandlerUtils.IOType type = getIOConfig(side);
        return type == IOHandlerUtils.IOType.INPUT || type == IOHandlerUtils.IOType.BOTH;
    }

    boolean canOutputItemsToSide(@Nullable Direction side) {
        if (side == null) {
            return true;
        }
        IOHandlerUtils.IOType type = getIOConfig(side);
        return type == IOHandlerUtils.IOType.OUTPUT || type == IOHandlerUtils.IOType.BOTH;
    }

    protected boolean canExternalInsertToSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return true;
    }

    protected boolean canExternalExtractFromSlot(int slot, @Nullable Direction side) {
        return true;
    }

    /**
     * 建立物品槽，可由子類覆寫。
     * 若不需要可覆寫為 null。
     */
    protected ItemStackHandler createHandler() {
        return null; // 預設為無物品槽
    }

    /**
     * 建立流體槽，可由子類覆寫。
     * 若不需要可覆寫為 null。
     */
    protected FluidTank createFluidTank() {
        return null; // 預設為無流體槽
    }

    /**
     * 是否應該在當前 tick 執行伺服器邏輯。
     */
    protected boolean shouldRunTick() {
        return !level.isClientSide;
    }

    /**
     * 伺服器端 tick 執行邏輯。
     */
    public void tickServer() {
        if (shouldRunTick()) {
            tickMachine();
        }
    }

    /**
     * 客戶端動畫、粒子處理邏輯（選用）。
     */
    public void tickClient() {
        // 客戶端動畫邏輯（由子類擴充）
    }

    /**
     * NeoForge 標準 tick 委派邏輯。
     */
    public static void tick(Level level, BlockPos pos, BlockState state, AbstractManaMachineEntityBlock blockEntity) {
        if (level.isClientSide) {
            blockEntity.tickClient();
        } else {
            blockEntity.tickServer();
        }
    }

    @Override
    public abstract AbstractContainerMenu createMenu(int id, Inventory inv, Player player);

    @Override
    public abstract Component getDisplayName();

    /**
     * 機器主要運作邏輯，由 tickServer 呼叫。
     */
    public void tickMachine() {}

    /**
     * 判斷是否符合生成魔力的條件。
     */
    protected abstract boolean canGenerate();

    /**
     * 可由子類自定義每次產生的魔力量（支援 buff、modifier 等機制）
     * 可讓子類覆蓋修改產量
     */
    protected int computeManaAmount() {
        return manaPerCycle;
    }

    /**
     * 生成魔力後觸發（播粒子、音效等）
     */
    protected void onGenerate(int amount) {
        // 可由子類覆寫
    }


    public void drops(Level level, BlockPos pos) {
        if (itemHandler != null) {
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Block.popResource(level, pos, stack);
                }
            }
        }
    }

    public void onRemovedFromWorld(Level level, BlockPos pos) {
        this.drops(level, pos);
        level.invalidateCapabilities(pos);
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (manaStorage != null) {
            builder.set(com.github.nalamodikk.register.ModDataComponents.MANA_STORED, manaStorage.getManaStored());
            builder.set(com.github.nalamodikk.register.ModDataComponents.MAX_MANA, manaStorage.getMaxManaStored());
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        if (manaStorage != null) {
            Integer stored = input.get(com.github.nalamodikk.register.ModDataComponents.MANA_STORED);
            if (stored != null) {
                manaStorage.setMana(stored);
            }
        }
    }
// 在 AbstractManaMachineEntityBlock 類中添加這些方法

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // 🔮 保存魔力儲存（使用 ManaStorage 的序列化）
        if (manaStorage != null) {
            CompoundTag manaTag = manaStorage.serializeNBT(registries);
            tag.put("ManaStorage", manaTag);
        }

        // ⚡ 保存能量儲存（使用你的 ModNeoNalaEnergyStorage 序列化）
        if (energyStorage != null) {
            CompoundTag energyTag = energyStorage.serializeNBT(registries);
            tag.put("EnergyStorage", energyTag);
        }

        // 📦 保存物品槽位
        if (itemHandler != null) {
            CompoundTag itemsTag = itemHandler.serializeNBT(registries);
            tag.put("Items", itemsTag);
        }

        // 🌊 保存流體槽
        if (fluidTank != null) {
            CompoundTag fluidTag = new CompoundTag();
            fluidTank.writeToNBT(registries, fluidTag);
            tag.put("FluidTank", fluidTag);
        }

        // 📊 保存進度和基本狀態
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("TickCounter", tickCounter);
        tag.putInt("ManaPerCycle", manaPerCycle);
        tag.putInt("EnergyPerTick", energyPerTick);
        tag.putInt("IntervalTick", intervalTick);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }

        // 🔍 調試日誌

    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // 🔮 載入魔力儲存（使用 ManaStorage 的反序列化）
        if (tag.contains("ManaStorage") && manaStorage != null) {
            CompoundTag manaTag = tag.getCompound("ManaStorage");
            manaStorage.deserializeNBT(registries, manaTag);
        }

        // ⚡ 載入能量儲存（使用你的 ModNeoNalaEnergyStorage 反序列化）
        if (tag.contains("EnergyStorage") && energyStorage != null) {
            CompoundTag energyTag = tag.getCompound("EnergyStorage");
            energyStorage.deserializeNBT(registries, energyTag);
        }

        // 📦 載入物品槽位
        if (tag.contains("Items") && itemHandler != null) {
            CompoundTag itemsTag = tag.getCompound("Items");
            itemHandler.deserializeNBT(registries, itemsTag);
        }

        // 🌊 載入流體槽
        if (tag.contains("FluidTank") && fluidTank != null) {
            CompoundTag fluidTag = tag.getCompound("FluidTank");
            fluidTank.readFromNBT(registries, fluidTag);
        }

        // 📊 載入進度和基本狀態
        if (tag.contains("Progress"))     progress     = tag.getInt("Progress");
        if (tag.contains("MaxProgress"))  maxProgress  = tag.getInt("MaxProgress");
        if (tag.contains("TickCounter"))  tickCounter  = tag.getInt("TickCounter");
        if (tag.contains("ManaPerCycle")) manaPerCycle = tag.getInt("ManaPerCycle");
        if (tag.contains("EnergyPerTick")) energyPerTick = tag.getInt("EnergyPerTick");
        if (tag.contains("IntervalTick")) intervalTick = tag.getInt("IntervalTick");
        if (tag.hasUUID("Owner")) {
            ownerId = tag.getUUID("Owner");
        }

        // 確保值的有效性
        if (maxProgress <= 0) maxProgress = 100;
        if (intervalTick <= 0) intervalTick = 1;

        // 🔍 調試日誌
    }

}
