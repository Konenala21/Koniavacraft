// 🔧 NoiseSettings Accessor - 獲取 protected 常量
package com.github.nalamodikk.mixin;

import net.minecraft.world.level.levelgen.NoiseSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 🎯 Accessor Mixin - 獲取 NoiseSettings 的 protected 常量
 */
@Mixin(NoiseSettings.class)
public interface NoiseSettingsAccessor {

    /**
     * 🌍 獲取主世界噪音設定
     */
    @Accessor("OVERWORLD_NOISE_SETTINGS")
    static NoiseSettings getOverworldNoiseSettings() {
        throw new AssertionError();
    }

    /**
     * 🔥 獲取地獄噪音設定
     */
    @Accessor("NETHER_NOISE_SETTINGS")
    static NoiseSettings getNetherNoiseSettings() {
        throw new AssertionError();
    }

    /**
     * 🌌 獲取終界噪音設定
     */
    @Accessor("END_NOISE_SETTINGS")
    static NoiseSettings getEndNoiseSettings() {
        throw new AssertionError();
    }
}