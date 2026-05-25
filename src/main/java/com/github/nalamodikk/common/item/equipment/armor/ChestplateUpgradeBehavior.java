package com.github.nalamodikk.common.item.equipment.armor;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public enum ChestplateUpgradeBehavior {

    CAPACITY(new int[]{1500, 3000, 5000, 7500}, "capacity") {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.chestplate_upgrade_capacity"); }
        @Override public int getColor() { return 0xFF44FFAA; }
    },
    ARMOR(new int[]{1, 2, 3, 4}, "armor") {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.chestplate_upgrade_armor"); }
        @Override public int getColor() { return 0xFF8888FF; }
    };

    private final int[] bonusPerMk;
    private final String tooltipKey;

    ChestplateUpgradeBehavior(int[] bonusPerMk, String tooltipKey) {
        this.bonusPerMk = bonusPerMk;
        this.tooltipKey = tooltipKey;
    }

    public int getBonusForMk(int mk) {
        return bonusPerMk[Mth.clamp(mk, 0, bonusPerMk.length - 1)];
    }

    public Component getEffectTooltip(int mk) {
        return Component.translatable("tooltip.koniava.chestplate_upgrade.effect." + tooltipKey, getBonusForMk(mk));
    }

    public abstract Component getDisplayName();
    public abstract int getColor();
}
