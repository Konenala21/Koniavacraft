package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 曲速引擎結構的輸入方塊：把三種來源轉成統一「能量」緩衝，曲速飛行時 ShipEntity 從這裡抽。
 * 來源：魔力網路(MANA INPUT capability 直接灌能量) / 燃料物品(item slot 燒成能量) / 燃料流體(fluid tank 燒成能量，
 * 給未來只能當流體、不能放世界的燃料)。
 */
public class ManaWarpInputBlockEntity extends BlockEntity {
    public static final int CAPACITY = 1_000_000;
    public static final int FLUID_CAPACITY = 8000;

    private final ManaStorage energy = new ManaStorage(CAPACITY, this::setChanged);
    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot, ItemStack stack) { return ShipFuels.energyOf(stack) > 0; }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final FluidTank fluid = new FluidTank(FLUID_CAPACITY, fs -> ShipFuels.fluidEnergyOf(fs.getFluid()) > 0) {
        @Override protected void onContentsChanged() { setChanged(); }
    };

    public ManaWarpInputBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_WARP_INPUT_BE.get(), pos, state);
    }

    public ManaStorage getEnergy() { return energy; }
    public ItemStackHandler getItems() { return items; }
    public FluidTank getFluidTank() { return fluid; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaWarpInputBlockEntity be) {
        be.convertFuel();
    }

    /** 把物品/流體燃料燒成能量緩衝(魔力網路那條走 capability 直接進 energy，不在這裡)。 */
    private void convertFuel() {
        ItemStack stack = items.getStackInSlot(0);
        int ie = ShipFuels.energyOf(stack);
        if (ie > 0 && CAPACITY - energy.getManaStored() >= ie) {
            energy.receiveMana(ie, ManaAction.EXECUTE);
            items.extractItem(0, 1, false);
        }
        FluidStack fs = fluid.getFluid();
        if (!fs.isEmpty()) {
            int perMb = ShipFuels.fluidEnergyOf(fs.getFluid());
            if (perMb > 0) {
                int burn = Math.min(fs.getAmount(), 10); // 每 tick 最多燒 10mb
                if (CAPACITY - energy.getManaStored() >= perMb * burn) {
                    energy.receiveMana(perMb * burn, ManaAction.EXECUTE);
                    fluid.drain(burn, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.put("Energy", energy.serializeNBT(reg));
        tag.put("Items", items.serializeNBT(reg));
        tag.put("Fluid", fluid.writeToNBT(reg, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        if (tag.contains("Energy")) energy.deserializeNBT(reg, tag.getCompound("Energy"));
        if (tag.contains("Items")) items.deserializeNBT(reg, tag.getCompound("Items"));
        if (tag.contains("Fluid")) fluid.readFromNBT(reg, tag.getCompound("Fluid"));
    }
}
