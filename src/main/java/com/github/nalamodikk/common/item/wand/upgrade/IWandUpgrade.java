package com.github.nalamodikk.common.item.wand.upgrade;

import net.minecraft.network.chat.Component;

/**
 * Implemented by items that can be installed in a WandRodItem upgrade slot.
 */
public interface IWandUpgrade {
    Component getUpgradeDisplayName();
}
