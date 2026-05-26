package com.github.nalamodikk.common.block.blockentity.mana_plate_press;

import com.github.nalamodikk.common.block.blockentity.mana_plate_press.sync.ManaPlatePressSync;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModMenuTypes;
import com.github.nalamodikk.register.ModRecipes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ManaPlatePressMenu extends AbstractContainerMenu {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    private static final int INPUT_SLOT_X = 48;
    private static final int INPUT_SLOT_Y = 35;
    private static final int OUTPUT_SLOT_X = 122;
    private static final int OUTPUT_SLOT_Y = 34;

    private static final int INVENTORY_START_X = 8;
    private static final int INVENTORY_START_Y = 84;
    private static final int HOTBAR_START_Y = 142;

    private final ManaPlatePressBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ManaPlatePressSync syncHelper;

    public ManaPlatePressMenu(int id, Inventory playerInventory, net.minecraft.network.FriendlyByteBuf buf) {
        this(id, playerInventory, (ManaPlatePressBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ManaPlatePressMenu(int id, Inventory playerInventory, ManaPlatePressBlockEntity blockEntity) {
        super(ModMenuTypes.MANA_PLATE_PRESS_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.access = blockEntity != null
                ? ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos())
                : ContainerLevelAccess.NULL;
        this.syncHelper = blockEntity.getSyncHelper();

        this.addDataSlots(syncHelper.getContainerData());

        if (blockEntity != null) {
            IItemHandler itemHandler = blockEntity.getItemHandler();
            if (itemHandler != null) {
                addMachineSlots(itemHandler);
            }
        }
        addPlayerSlots(playerInventory);
    }

    private void addMachineSlots(IItemHandler itemHandler) {
        this.addSlot(new SlotItemHandler(itemHandler, INPUT_SLOT, INPUT_SLOT_X, INPUT_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return blockEntity != null && hasRecipeForItem(stack);
            }
        });

        this.addSlot(new SlotItemHandler(itemHandler, OUTPUT_SLOT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });
    }

    private void addPlayerSlots(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        INVENTORY_START_X + col * 18, INVENTORY_START_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, INVENTORY_START_X + col * 18, HOTBAR_START_Y));
        }
    }

    private boolean hasRecipeForItem(ItemStack stack) {
        if (blockEntity == null || blockEntity.getLevel() == null) return false;
        ManaPlatePressRecipe.PlatePressInput input = new ManaPlatePressRecipe.PlatePressInput(stack);
        return blockEntity.getLevel().getRecipeManager()
                .getRecipeFor(ModRecipes.PLATE_PRESS_TYPE.get(), input, blockEntity.getLevel())
                .isPresent();
    }

    public int getCurrentMana() { return syncHelper.getCurrentMana(); }
    public int getMaxMana() { return syncHelper.getMaxMana(); }
    public int getPressingProgress() { return syncHelper.getProgress(); }
    public int getMaxPressingTime() { return syncHelper.getMaxProgress(); }
    public boolean isWorking() { return syncHelper.isWorking(); }

    public int getManaPercentage() {
        int max = getMaxMana();
        return max > 0 ? (getCurrentMana() * 100) / max : 0;
    }

    public int getProgressPercentage() {
        int max = getMaxPressingTime();
        return max > 0 ? (getPressingProgress() * 100) / max : 0;
    }

    public net.minecraft.core.BlockPos getBlockEntityPos() { return blockEntity.getBlockPos(); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();
            if (index < 2) {
                if (!this.moveItemStackTo(slotStack, 2, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (hasRecipeForItem(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, INPUT_SLOT, INPUT_SLOT + 1, false)) return ItemStack.EMPTY;
                } else {
                    return ItemStack.EMPTY;
                }
            }
            if (slotStack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.MANA_PLATE_PRESS.get());
    }
}
