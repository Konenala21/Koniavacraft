package com.github.nalamodikk.common.item.weapon.turret;

import com.github.nalamodikk.register.ModMobEffects;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

public enum TurretUpgradeBehavior {

    CAPACITY(new int[]{3000, 6000, 10000, 15000}, "capacity", false) {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.turret_upgrade_capacity"); }
        @Override public int getColor() { return 0xFF44FFAA; }
    },
    HEALING(new int[]{1, 2, 3, 4}, "healing", false) {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.turret_upgrade_healing"); }
        @Override public int getColor() { return 0xFFFF6699; }
    },
    HEALTH(new int[]{50, 100, 200}, "health", true) {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.turret_upgrade_health"); }
        @Override public int getColor() { return 0xFF66DD66; }
    },
    DEFENSE(new int[]{5, 10}, "defense", true) {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.turret_upgrade_defense"); }
        @Override public int getColor() { return 0xFF8888FF; }
    },
    AUTO_AIM(new int[]{0}, "auto_aim", false) {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.turret_upgrade_auto_aim"); }
        @Override public int getColor() { return 0xFFFFAA22; }
    },
    NO_BLOCK_DAMAGE(new int[]{0}, "no_block_damage", false) {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.turret_upgrade_no_block_damage"); }
        @Override public int getColor() { return 0xFFAAAAAA; }
    },
    PLAYER_LOCK(new int[]{0}, "player_lock", true) {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.turret_upgrade_player_lock"); }
        @Override public int getColor() { return 0xFFFF4466; }
    },
    PROTECT(new int[]{0, 1}, "protect", true) {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.turret_upgrade_protect"); }
        @Override public int getColor() { return 0xFFFFCC44; }
    },
    // ── 控制彈（萬能控制前置）：每種獨立冷卻，命中套用對應效果 ──────────────────
    SLOW(new int[]{0}, "slow", false) {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.turret_upgrade_slow"); }
        @Override public int getColor() { return 0xFF66CCEE; }
        @Override public boolean isControl() { return true; }
        @Override public Holder<MobEffect> getControlEffect() { return MobEffects.MOVEMENT_SLOWDOWN; }
        @Override public int getControlDuration() { return 60; } // 3s
        @Override public int getControlCooldown() { return 60; } // 3s
    },
    ROOT(new int[]{0}, "root", false) {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.turret_upgrade_root"); }
        @Override public int getColor() { return 0xFF9944AA; }
        @Override public boolean isControl() { return true; }
        @Override public Holder<MobEffect> getControlEffect() { return ModMobEffects.ROOT; }
        @Override public int getControlDuration() { return 20; } // 1s
        @Override public int getControlCooldown() { return 60; } // 3s
    },
    LEVITATE(new int[]{0}, "levitate", false) {
        @Override public Component getDisplayName() { return Component.translatable("item.koniava.turret_upgrade_levitate"); }
        @Override public int getColor() { return 0xFFCCAAFF; }
        @Override public boolean isControl() { return true; }
        @Override public Holder<MobEffect> getControlEffect() { return MobEffects.LEVITATION; }
        @Override public int getControlDuration() { return 40; } // 2s
        @Override public int getControlCooldown() { return 100; } // 5s
    };

    private final int[] bonusPerMk;
    private final String tooltipKey;
    private final boolean entityOnly;

    TurretUpgradeBehavior(int[] bonusPerMk, String tooltipKey, boolean entityOnly) {
        this.bonusPerMk = bonusPerMk;
        this.tooltipKey = tooltipKey;
        this.entityOnly = entityOnly;
    }

    public int getMaxMk() { return bonusPerMk.length - 1; }

    public boolean isEntityOnly() { return entityOnly; }

    public int getBonusForMk(int mk) {
        return bonusPerMk[Mth.clamp(mk, 0, bonusPerMk.length - 1)];
    }

    public Component getEffectTooltip(int mk) {
        return Component.translatable("tooltip.koniava.turret_upgrade.effect." + tooltipKey, getBonusForMk(mk));
    }

    // ── 控制彈（預設非控制）──────────────────────────────────────────────────
    public boolean isControl() { return false; }
    public Holder<MobEffect> getControlEffect() { return null; }
    public int getControlDuration() { return 0; }
    public int getControlCooldown() { return 0; }

    public abstract Component getDisplayName();
    public abstract int getColor();
}
