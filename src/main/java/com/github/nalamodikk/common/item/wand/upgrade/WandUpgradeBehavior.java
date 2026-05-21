package com.github.nalamodikk.common.item.wand.upgrade;

import net.minecraft.network.chat.Component;

public enum WandUpgradeBehavior {

    CAPACITY {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.wand_upgrade_capacity"); }
        @Override public int getColor() { return 0xFF44AAFF; }
    },
    EFFICIENCY {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.wand_upgrade_efficiency"); }
        @Override public int getColor() { return 0xFF44DD44; }
    },
    RANGE {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.wand_upgrade_range"); }
        @Override public int getColor() { return 0xFFFFAA22; }
    },
    COOLDOWN {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.wand_upgrade_cooldown"); }
        @Override public int getColor() { return 0xFFFF4466; }
    };

    public abstract Component getDisplayName();
    public abstract int getColor();
}
