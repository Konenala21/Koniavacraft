package com.github.nalamodikk.common.block.blockentity.mana_crafting;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UpdatingSlotItemHandler extends SlotItemHandler {
    private final AbstractContainerMenu menu;
    private static final Logger LOGGER = LogManager.getLogger();

    public UpdatingSlotItemHandler(IItemHandler handler, int index, int x, int y, AbstractContainerMenu menu) {
        super(handler, index, x, y);
        this.menu = menu;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        menu.slotsChanged(this.container); // ✅ 強制觸發更新
    }

    @Override
    public boolean mayPickup(Player player) {
        LOGGER.debug("[SlotCheck] mayPickup() called, hasItem = {}", this.hasItem());
        return this.hasItem();
    }


}

