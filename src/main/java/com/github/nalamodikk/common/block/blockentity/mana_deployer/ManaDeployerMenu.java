package com.github.nalamodikk.common.block.blockentity.mana_deployer;

import com.github.nalamodikk.register.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ManaDeployerMenu extends AbstractContainerMenu {

    private final ManaDeployerBlockEntity blockEntity;

    private static final int DATA_CURRENT_MANA  = 0;
    private static final int DATA_MAX_MANA      = 1;
    private static final int DATA_MODE          = 2;
    private static final int DATA_INTERVAL_TICK = 3;
    private static final int DATA_ENABLED       = 4;

    // Slot boundaries
    private static final int MACHINE_SLOTS = 1;
    private static final int PLAYER_INV_START = MACHINE_SLOTS;
    private static final int PLAYER_INV_END   = PLAYER_INV_START + 27;
    private static final int HOTBAR_START     = PLAYER_INV_END;
    private static final int HOTBAR_END       = HOTBAR_START + 9;

    public ManaDeployerMenu(int containerId, Inventory playerInventory, ManaDeployerBlockEntity blockEntity) {
        super(ModMenuTypes.MANA_DEPLOYER_MENU.get(), containerId);
        this.blockEntity = blockEntity;

        this.addDataSlots(blockEntity.getDeployerData());

        var handler = blockEntity.getItemHandler();
        if (handler == null) throw new IllegalStateException("ManaDeployer itemHandler is null");
        this.addSlot(new SlotItemHandler(handler, 0, 79, 35));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index < MACHINE_SLOTS) {
                // Machine slot -> player inventory
                if (!this.moveItemStackTo(slotStack, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player inventory/hotbar -> machine slot (only if slot is empty)
                if (!this.moveItemStackTo(slotStack, 0, MACHINE_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    // Accessors for Screen

    public int getCurrentMana() {
        return blockEntity.getDeployerData().get(DATA_CURRENT_MANA);
    }

    public int getMaxMana() {
        return blockEntity.getDeployerData().get(DATA_MAX_MANA);
    }

    public ManaDeployerBlockEntity.Mode getMode() {
        return blockEntity.getDeployerData().get(DATA_MODE) == 1
                ? ManaDeployerBlockEntity.Mode.LEFT_CLICK
                : ManaDeployerBlockEntity.Mode.RIGHT_CLICK;
    }

    public int getIntervalTick() {
        return blockEntity.getDeployerData().get(DATA_INTERVAL_TICK);
    }

    public boolean isEnabled() {
        return blockEntity.getDeployerData().get(DATA_ENABLED) != 0;
    }

    public BlockPos getBlockPos() {
        return blockEntity.getBlockPos();
    }
}
