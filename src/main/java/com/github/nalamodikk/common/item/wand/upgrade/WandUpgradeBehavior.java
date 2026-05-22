package com.github.nalamodikk.common.item.wand.upgrade;

import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;

public enum WandUpgradeBehavior {

    CAPACITY(new int[]{2000, 3500, 5000, 7000}, "capacity") {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.wand_upgrade_capacity"); }
        @Override public int getColor() { return 0xFF44AAFF; }
    },
    EFFICIENCY(new int[]{15, 20, 27, 35}, "efficiency") {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.wand_upgrade_efficiency"); }
        @Override public int getColor() { return 0xFF44DD44; }
    },
    RANGE(new int[]{2, 3, 4, 6}, "range") {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.wand_upgrade_range"); }
        @Override public int getColor() { return 0xFFFFAA22; }
    },
    COOLDOWN(new int[]{3, 5, 7, 10}, "cooldown") {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.wand_upgrade_cooldown"); }
        @Override public int getColor() { return 0xFFFF4466; }
    };

    private final int[] bonusPerMk;
    private final String tooltipKey;
    public final int bonusPerSlot;

    WandUpgradeBehavior(int[] bonusPerMk, String tooltipKey) {
        this.bonusPerMk = bonusPerMk;
        this.tooltipKey = tooltipKey;
        this.bonusPerSlot = bonusPerMk[0];
    }

    public int getBonusForMk(int mk) {
        return bonusPerMk[Mth.clamp(mk, 0, bonusPerMk.length - 1)];
    }

    public Component getEffectTooltip(int mk) {
        return Component.translatable("tooltip.koniava.wand_upgrade.effect." + tooltipKey, getBonusForMk(mk));
    }

    public Component getEffectTooltip() {
        return getEffectTooltip(0);
    }

    public abstract Component getDisplayName();
    public abstract int getColor();
}
