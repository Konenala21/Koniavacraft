package com.github.nalamodikk.common.item.equipment.armor;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public enum LeggingsUpgradeBehavior {

    CAPACITY(new int[]{1500, 2500, 4000, 7000}, "capacity", null, null) {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.leggings_upgrade_capacity"); }
        @Override public int getColor() { return 0xFF44FFAA; }
    },
    MULTI_JUMP(new int[]{1, 2, 3, 4}, "multi_jump",
            new double[]{0.42, 0.52, 0.62, 0.72},
            new int[]{14, 7, 5, 4}) {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.leggings_upgrade_multi_jump"); }
        @Override public int getColor() { return 0xFF00EEFF; }
    };

    private final int[] bonusPerMk;
    private final String tooltipKey;
    private final double[] jumpVelocityPerMk;
    private final int[] manaCostPerMk;

    LeggingsUpgradeBehavior(int[] bonusPerMk, String tooltipKey,
                             double[] jumpVelocityPerMk, int[] manaCostPerMk) {
        this.bonusPerMk = bonusPerMk;
        this.tooltipKey = tooltipKey;
        this.jumpVelocityPerMk = jumpVelocityPerMk;
        this.manaCostPerMk = manaCostPerMk;
    }

    public int getBonusForMk(int mk) {
        return bonusPerMk[Mth.clamp(mk, 0, bonusPerMk.length - 1)];
    }

    public double getJumpVelocity(int mk) {
        if (jumpVelocityPerMk == null) return 0;
        return jumpVelocityPerMk[Mth.clamp(mk, 0, jumpVelocityPerMk.length - 1)];
    }

    public int getJumpManaCost(int mk) {
        if (manaCostPerMk == null) return 0;
        return manaCostPerMk[Mth.clamp(mk, 0, manaCostPerMk.length - 1)];
    }

    public Component getEffectTooltip(int mk) {
        return Component.translatable("tooltip.koniava.leggings_upgrade.effect." + tooltipKey, getBonusForMk(mk));
    }

    public abstract Component getDisplayName();
    public abstract int getColor();
}
