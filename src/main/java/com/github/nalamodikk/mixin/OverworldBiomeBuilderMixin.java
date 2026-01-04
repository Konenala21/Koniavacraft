package com.github.nalamodikk.mixin;

import com.github.nalamodikk.biome.UniversalBiomeInjector;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * 使用 UniversalBiomeInjector 的 Mixin
 */
@Mixin(OverworldBiomeBuilder.class)
public class OverworldBiomeBuilderMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "addBiomes", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/biome/OverworldBiomeBuilder;addOffCoastBiomes(Ljava/util/function/Consumer;)V"))
    private void injectCustomBiomes(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> consumer, CallbackInfo ci) {
        LOGGER.info("🔧 OverworldBiomeBuilderMixin: 開始注入自訂生物群落...");

        try {
            // 🌟 使用 UniversalBiomeInjector（已經修正深度問題）
            UniversalBiomeInjector.injectBiomes(consumer);

            LOGGER.info("🎉 自訂生物群落注入完成！");

        } catch (Exception e) {
            LOGGER.error("❌ 自訂生物群落注入失敗！", e);
        }
    }
}