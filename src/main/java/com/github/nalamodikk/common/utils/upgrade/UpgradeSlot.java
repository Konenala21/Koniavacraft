package com.github.nalamodikk.common.utils.upgrade;

import com.github.nalamodikk.common.item.UpgradeItem;
import com.github.nalamodikk.common.utils.upgrade.api.IUpgradeableMachine;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class UpgradeSlot extends Slot {
    private final IUpgradeableMachine machine;

    public UpgradeSlot(Container container, int index, int x, int y, IUpgradeableMachine machine) {
        super(container, index, x, y);
        this.machine = machine;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (!(stack.getItem() instanceof UpgradeItem upgradeItem)) {
            return false;
        }
        return machine.supportsUpgradeType(upgradeItem.getUpgradeType());
    }
}
