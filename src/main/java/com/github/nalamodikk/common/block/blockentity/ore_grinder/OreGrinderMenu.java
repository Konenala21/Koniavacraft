package com.github.nalamodikk.common.block.blockentity.ore_grinder;

import com.github.nalamodikk.common.block.blockentity.ore_grinder.sync.OreGrinderSyncHelper;
import com.github.nalamodikk.register.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * 🎛️ 粉碎機 Menu
 */
public class OreGrinderMenu extends AbstractContainerMenu {

    private final OreGrinderBlockEntity blockEntity;
    private final ItemStackHandler itemHandler;
    private final OreGrinderSyncHelper syncHelper;

    public OreGrinderMenu(int containerId, Inventory playerInventory, OreGrinderBlockEntity blockEntity) {
        super(ModMenuTypes.ORE_GRINDER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.itemHandler = blockEntity.getItemHandler();
        this.syncHelper = blockEntity.getSyncHelper();

        // 註冊數據同步
        this.addDataSlots(syncHelper.getContainerData());

        if (this.itemHandler == null) {
            throw new IllegalArgumentException("OreGrinder 必須有 ItemHandler");
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
            this.addSlot(new SlotItemHandler(itemHandler, i, 44 + i * 18, 35));
        }

        // 輸出槽 (2-5) - 2x2 網格
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int slotIndex = 2 + row * 2 + col;
                int x = 116 + col * 18;
                int y = 35 + row * 18;
                this.addSlot(new SlotItemHandler(itemHandler, slotIndex, x, y));
            }
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 140 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 198));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return true;
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
        return OreGrinderBlockEntity.getMaxMana(); // 靜態最大值
    }

    public boolean isWorking() {
        return getProgress() > 0;
    }
}
