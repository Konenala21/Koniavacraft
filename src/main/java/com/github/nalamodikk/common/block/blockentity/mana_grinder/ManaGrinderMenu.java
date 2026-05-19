package com.github.nalamodikk.common.block.blockentity.mana_grinder;

import com.github.nalamodikk.common.block.blockentity.mana_grinder.sync.ManaGrinderSyncHelper;
import com.github.nalamodikk.common.coreapi.recipe.ProcessingRecipe;
import com.github.nalamodikk.register.ModRecipes;
import com.github.nalamodikk.register.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.List;

/**
 * 🎛️ 魔力粉碎機 Menu
 */
public class ManaGrinderMenu extends AbstractContainerMenu {

    private final ManaGrinderBlockEntity blockEntity;
    private final ItemStackHandler itemHandler;
    private final ManaGrinderSyncHelper syncHelper;

    public ManaGrinderMenu(int containerId, Inventory playerInventory, ManaGrinderBlockEntity blockEntity) {
        super(ModMenuTypes.MANA_GRINDER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.itemHandler = blockEntity.getItemHandler();
        this.syncHelper = blockEntity.getSyncHelper();

        // 註冊數據同步
        this.addDataSlots(syncHelper.getContainerData());

        if (this.itemHandler == null) {
            throw new IllegalArgumentException("ManaGrinder 必須有 ItemHandler");
        }

        // 添加機器槽位
        addSlots();
        // 添加玩家物品欄
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addSlots() {
        if (itemHandler == null) return;

        // 輸入槽 (0-1)
        for (int i = 0; i < 2; i++) {
            this.addSlot(new SlotItemHandler(itemHandler, i, 30 + i * 18, 35));
        }

        // 輸出槽 (2-5) - 2x2 網格
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int slotIndex = 2 + row * 2 + col;
                int x = 114 + col * 18;
                int y = 26 + row * 18;
                this.addSlot(new SlotItemHandler(itemHandler, slotIndex, x, y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
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
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        int machineSlots = 6;

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (pIndex < machineSlots) {
                if (!this.moveItemStackTo(slotStack, machineSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (hasRecipeForItem(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, 0, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return blockEntity.getLevel() != null
            && net.minecraft.world.phys.Vec3.atCenterOf(blockEntity.getBlockPos())
                   .distanceToSqr(pPlayer.position()) < 64.0;
    }

    private boolean hasRecipeForItem(ItemStack stack) {
        if (blockEntity.getLevel() == null) return false;
        List<ItemStack> inputs = List.of(stack);
        ProcessingRecipe.ProcessingInput input = new ProcessingRecipe.ProcessingInput(inputs, "grinder");
        return blockEntity.getLevel().getRecipeManager()
                .getAllRecipesFor(ModRecipes.PROCESSING_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .anyMatch(recipe -> recipe.matches(input, blockEntity.getLevel()));
    }

    // === 📊 數據同步 (改為從 syncHelper 讀取，確保 Client 端有效) ===

    public int getProgress() {
        return syncHelper.getProgress();
    }

    public int getMaxProgress() {
        return syncHelper.getMaxProgress();
    }

    public int getProgressPercentage() {
        int max = getMaxProgress();
        if (max == 0) return 0;
        return (getProgress() * 100) / max;
    }

    public int getCurrentMana() {
        return syncHelper.getMana();
    }

    public int getMaxMana() {
        return ManaGrinderBlockEntity.getMaxMana(); // 靜態最大值
    }

    public boolean isWorking() {
        return getProgress() > 0;
    }

    public net.minecraft.core.BlockPos getBlockEntityPos() {
        return blockEntity.getBlockPos();
    }
}
