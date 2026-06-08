package com.github.nalamodikk.common.block.blockentity.mana_charger;

import com.github.nalamodikk.register.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ManaChargerMenu extends AbstractContainerMenu {

    private final ManaChargerBlockEntity blockEntity;

    public ManaChargerMenu(int containerId, Inventory playerInventory, ManaChargerBlockEntity blockEntity) {
        super(ModMenuTypes.MANA_CHARGER_MENU.get(), containerId);
        this.blockEntity = blockEntity;

        addDataSlots(blockEntity.data);

        // Item slot — centred in the 24x24 gray area at x=73~96, y=29~52 in the texture
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), ManaChargerBlockEntity.ITEM_SLOT, 76, 32));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
    }

    private void addPlayerHotbar(Inventory inv) {
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
    }

    public int getCurrentMana() { return blockEntity.data.get(0); }
    public int getMaxMana()     { return blockEntity.data.get(1); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index == 0) {
            // Charger slot → player inventory
            if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // Player inventory → charger slot (only if item is chargeable)
            if (stack.has(com.github.nalamodikk.register.ModDataComponents.MAX_MANA)) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(blockEntity, player); // 標準寫法；影子機器靠 ContainerShipShadowMixin 放行
    }
}
