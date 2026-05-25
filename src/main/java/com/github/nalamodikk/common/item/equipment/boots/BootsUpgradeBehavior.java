package com.github.nalamodikk.common.item.equipment.boots;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public enum BootsUpgradeBehavior {

    ARMOR(new int[]{1, 2, 3, 4}, "armor") {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.boots_upgrade_armor"); }
        @Override public int getColor() { return 0xFF8888FF; }
    },
    DASH_DISTANCE(new int[]{1, 2, 3, 4}, "dash_distance") {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.boots_upgrade_dash_distance"); }
        @Override public int getColor() { return 0xFFFFAA22; }
    },
    MANA_EFFICIENCY(new int[]{1, 2, 3, 5}, "mana_efficiency") {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.boots_upgrade_mana_efficiency"); }
        @Override public int getColor() { return 0xFF44AAFF; }
    },
    CAPACITY(new int[]{1000, 2000, 3000, 4000}, "capacity") {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.boots_upgrade_capacity"); }
        @Override public int getColor() { return 0xFF44FFAA; }
    };

    private final int[] bonusPerMk;
    private final String tooltipKey;

    BootsUpgradeBehavior(int[] bonusPerMk, String tooltipKey) {
        this.bonusPerMk = bonusPerMk;
        this.tooltipKey = tooltipKey;
    }

    public int getBonusForMk(int mk) {
        return bonusPerMk[Mth.clamp(mk, 0, bonusPerMk.length - 1)];
    }

    public Component getEffectTooltip(int mk) {
        return Component.translatable("tooltip.koniava.boots_upgrade.effect." + tooltipKey, getBonusForMk(mk));
    }

    public abstract Component getDisplayName();
    public abstract int getColor();
}
