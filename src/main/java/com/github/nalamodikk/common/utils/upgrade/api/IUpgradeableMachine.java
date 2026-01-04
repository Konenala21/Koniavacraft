package com.github.nalamodikk.common.utils.upgrade.api;

import com.github.nalamodikk.common.utils.upgrade.UpgradeInventory;
import com.github.nalamodikk.common.utils.upgrade.UpgradeType;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IUpgradeableMachine {
    UpgradeInventory getUpgradeInventory();

    default int getSpeedMultiplier() {
        return Math.min(1 + getUpgradeInventory().getUpgradeCount(UpgradeType.SPEED), 5);
    }

    default int getEfficiencyMultiplier() {
        return Math.min(1 + getUpgradeInventory().getUpgradeCount(UpgradeType.EFFICIENCY), 5);
    }
    default int getUpgradeCount(UpgradeType type) {
        return getUpgradeInventory().getUpgradeCount(type);
    }

    BlockEntity getBlockEntity();

}