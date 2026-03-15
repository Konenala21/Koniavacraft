package com.github.nalamodikk.common.screen.block.shared;


import com.github.nalamodikk.common.utils.upgrade.UpgradeInventory;
import com.github.nalamodikk.common.utils.upgrade.UpgradeType;
import com.github.nalamodikk.common.utils.upgrade.api.IUpgradeableMachine;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class FallbackUpgradeMenu extends UpgradeMenu {

    private static final UpgradeInventory FAKE_INVENTORY = new UpgradeInventory(4);

    private static final IUpgradeableMachine DUMMY_MACHINE = new IUpgradeableMachine() {
        @Override
        public UpgradeInventory getUpgradeInventory() {
            return FAKE_INVENTORY;
        }

        @Override
        public int getSpeedMultiplier() {
            return 1;
        }

        @Override
        public int getEfficiencyMultiplier() {
            return 1;
        }

        @Override
        public boolean supportsUpgradeType(UpgradeType type) {
            return false;
        }

        @Override
        public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity() {
            return null;
        }
    };

    public FallbackUpgradeMenu(int id, Inventory playerInv) {
        super(id, playerInv, FAKE_INVENTORY, DUMMY_MACHINE);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
