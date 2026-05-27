package com.github.nalamodikk.common.item.equipment.armor;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public enum ChestplateUpgradeBehavior {

    CAPACITY(new int[]{1500, 3000, 5000, 7500}, "capacity") {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.chestplate_upgrade_capacity"); }
        @Override public int getColor() { return 0xFF44FFAA; }
    },
    // 護盾類型一：純被動免費減免，Mk3 額外把實際受到傷害的 50% 在 3 秒內回血
    SHIELD_REDUCTION(new int[]{40, 50, 60, 60}, "shield_reduction") {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.chestplate_upgrade_shield_reduction"); }
        @Override public int getColor() { return 0xFF66CCFF; }
        @Override public boolean isShieldReduction() { return true; }
        @Override public Component getExtraTooltip(int mk) {
            return mk >= 3 ? Component.translatable("tooltip.koniava.chestplate_upgrade.shield_reduction.heal") : null;
        }
    },
    // 護盾類型二：100% 吸收盾，打空自動燒魔力補盾，Mk3 盾有值時每次受擊額外免傷 20
    SHIELD_ABSORB(new int[]{100, 150, 200, 200}, "shield_absorb") {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.chestplate_upgrade_shield_absorb"); }
        @Override public int getColor() { return 0xFF4488FF; }
        @Override public boolean isShieldAbsorb() { return true; }
        @Override public Component getExtraTooltip(int mk) {
            return mk >= 3 ? Component.translatable("tooltip.koniava.chestplate_upgrade.shield_absorb.flat") : null;
        }
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

    public boolean isShieldReduction() { return false; }
    public boolean isShieldAbsorb() { return false; }
    public boolean isShield() { return isShieldReduction() || isShieldAbsorb(); }

    @Nullable
    public Component getExtraTooltip(int mk) { return null; }

    public abstract Component getDisplayName();
    public abstract int getColor();
}
