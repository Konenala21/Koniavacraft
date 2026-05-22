package com.github.nalamodikk.common.item.wand.upgrade;

import net.minecraft.network.chat.Component;

public enum WandUpgradeBehavior {

    /** +2000 MAX_MANA per slot */
    CAPACITY(2000) {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.wand_upgrade_capacity"); }
        @Override public int getColor() { return 0xFF44AAFF; }
        @Override public Component getEffectTooltip() { return Component.translatable("tooltip.koniava.wand_upgrade.effect.capacity", bonusPerSlot); }
    },
    /** -15% mana cost per slot (FORMATION core) */
    EFFICIENCY(15) {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.wand_upgrade_efficiency"); }
        @Override public int getColor() { return 0xFF44DD44; }
        @Override public Component getEffectTooltip() { return Component.translatable("tooltip.koniava.wand_upgrade.effect.efficiency", bonusPerSlot); }
    },
    /** +2 block interaction range per slot */
    RANGE(2) {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.wand_upgrade_range"); }
        @Override public int getColor() { return 0xFFFFAA22; }
        @Override public Component getEffectTooltip() { return Component.translatable("tooltip.koniava.wand_upgrade.effect.range", bonusPerSlot); }
    },
    /** -3 cooldown ticks per slot (FORMATION core) */
    COOLDOWN(3) {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.wand_upgrade_cooldown"); }
        @Override public int getColor() { return 0xFFFF4466; }
        @Override public Component getEffectTooltip() { return Component.translatable("tooltip.koniava.wand_upgrade.effect.cooldown", bonusPerSlot); }
    };

    public final int bonusPerSlot;

    WandUpgradeBehavior(int bonusPerSlot) {
        this.bonusPerSlot = bonusPerSlot;
    }

    public abstract Component getDisplayName();
    public abstract int getColor();
    public abstract Component getEffectTooltip();
}
