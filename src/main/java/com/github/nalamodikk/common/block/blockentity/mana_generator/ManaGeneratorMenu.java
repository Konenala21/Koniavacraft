package com.github.nalamodikk.common.block.blockentity.mana_generator;

import com.github.nalamodikk.register.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;


public class ManaGeneratorMenu extends AbstractContainerMenu {
    private final ManaGeneratorBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public ManaGeneratorMenu(int id, Inventory inv, ManaGeneratorBlockEntity blockEntity) {
        super(ModMenuTypes.MANA_GENERATOR_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.addDataSlots(blockEntity.getContainerData());

        IItemHandler blockInventory = blockEntity.getInventory();
        this.addSlot(new SlotItemHandler(blockInventory, 0, 80, 40) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return true; // 允許所有物品放入
            }

        });

        layoutPlayerInventorySlots(inv, 8, 84);
    }

    private void layoutPlayerInventorySlots(Inventory playerInventory, int leftCol, int topRow) {
        // 玩家物品欄槽位 (3行)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, leftCol + col * 18, topRow + row * 18));
            }
        }

        // 玩家快捷欄槽位 (1行)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, leftCol + col * 18, topRow + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, blockEntity.getBlockState().getBlock());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();
            if (index < 1) { // 如果是在方塊槽位
                if (!this.moveItemStackTo(stackInSlot, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) { // 如果是在玩家槽位
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    public int getMaxMana() {
        return ManaGeneratorBlockEntity.getMaxMana();
    }

    public int getMaxEnergy() {
        return ManaGeneratorBlockEntity.getMaxEnergy();
    }


    public BlockPos getBlockEntityPos() {
        return this.blockEntity.getBlockPos();
    }

    public int getCurrentMode() {
        return blockEntity.getCurrentMode();
    }

    // 建議將這些獲取數據的方法改為直接從 Helper 獲取緩存值
    public int getManaStored() {
        return blockEntity.getManaStorage() != null ? blockEntity.getManaStorage().getManaStored() : 0;
    }

    public int getEnergyStored() {
        return blockEntity.getEnergyStorage() != null ? blockEntity.getEnergyStorage().getEnergyStored() : 0;
    }

    public int getBurnTime() {
        return blockEntity.getBurnTime();
    }

    public int getCurrentBurnTime() {
        return blockEntity.getCurrentBurnTime();
    }

    // 便利方法，方便Screen獲取數據

    public boolean isWorking() {
        return blockEntity.isWorking();
    }

    // 💡 新增 Getter 方法
    public boolean hasDiagnosticDisplay() {
        return blockEntity.hasDiagnosticDisplay();
    }

    public int getManaRate() {
        return blockEntity.getManaRate();
    }

    public int getEnergyRate() {
        return blockEntity.getEnergyRate();
    }

}
