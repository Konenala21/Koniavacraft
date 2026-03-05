// 🔧 Koniavacraft NoiseGeneratorSettings Mixin - 簡潔版
package com.github.nalamodikk.mixin;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.biome.lib.SurfaceRuleRegistry;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * 🎯 Mixin to inject Koniavacraft's custom terrain rules into Surface Rules
 *
 * 簡潔版：在原版規則前面插入自定義規則，讓原版邏輯自然繼續
 */
@Mixin(NoiseGeneratorSettings.class)
public class NoiseGeneratorSettingsMixin {

    @Shadow
    @Final
    private NoiseSettings noiseSettings;

    /**
     * 🔧 在原版規則前面插入 Koniavacraft 的地形規則
     *
     * 策略：修改返回值，不完全攔截
     */
    @Inject(method = "surfaceRule", at = @At("RETURN"), cancellable = true)
    private void injectKoniavaSurfaceRules(CallbackInfoReturnable<SurfaceRules.RuleSource> cir) {
        try {
            SurfaceRuleRegistry.RuleCategory category = resolveCategory();
            if (category == null) {
                return;
            }

            SurfaceRules.RuleSource vanillaRules = cir.getReturnValue();
            SurfaceRules.RuleSource beforeVanilla = SurfaceRuleRegistry.build(
                    category,
                    SurfaceRuleRegistry.RuleStage.BEFORE_VANILLA
            );
            SurfaceRules.RuleSource afterVanilla = SurfaceRuleRegistry.build(
                    category,
                    SurfaceRuleRegistry.RuleStage.AFTER_VANILLA
            );

            if (beforeVanilla == null && afterVanilla == null) {
                return;
            }

            List<SurfaceRules.RuleSource> sequence = new ArrayList<>();
            if (beforeVanilla != null) {
                sequence.add(beforeVanilla);
            }
            sequence.add(vanillaRules);
            if (afterVanilla != null) {
                sequence.add(afterVanilla);
            }

            cir.setReturnValue(SurfaceRules.sequence(sequence.toArray(new SurfaceRules.RuleSource[0])));

        } catch (Exception e) {
            // 🚨 如果出錯，記錄錯誤但不修改返回值，保持原版規則
            KoniavacraftMod.LOGGER.error("❌ Koniavacraft 地形規則注入失敗: {}", e.getMessage(), e);
        }
    }

    private SurfaceRuleRegistry.RuleCategory resolveCategory() {
        if (this.noiseSettings.equals(NoiseSettingsAccessor.getOverworldNoiseSettings())) {
            return SurfaceRuleRegistry.RuleCategory.OVERWORLD;
        }
        if (this.noiseSettings.equals(NoiseSettingsAccessor.getNetherNoiseSettings())) {
            return SurfaceRuleRegistry.RuleCategory.NETHER;
        }
        if (this.noiseSettings.equals(NoiseSettingsAccessor.getEndNoiseSettings())) {
            return SurfaceRuleRegistry.RuleCategory.END;
        }
        return null;
    }
}
