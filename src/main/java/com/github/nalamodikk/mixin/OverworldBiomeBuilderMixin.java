package com.github.nalamodikk.mixin;

import com.github.nalamodikk.biome.region.BiomeRegionManager;
import com.github.nalamodikk.biome.region.VanillaBiomeParameterReader;
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
 * 使用 BiomeRegionManager 的 Mixin
 */
@Mixin(OverworldBiomeBuilder.class)
public class OverworldBiomeBuilderMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DISABLE_BIOME_INJECTION_PROPERTY = "koniava.disableBiomeInjection";

    @Inject(method = "addBiomes", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/biome/OverworldBiomeBuilder;addOffCoastBiomes(Ljava/util/function/Consumer;)V"))
    private void injectCustomBiomes(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> consumer, CallbackInfo ci) {
        if (Boolean.getBoolean(DISABLE_BIOME_INJECTION_PROPERTY)) {
            return;
        }

        // 若正在建立 VanillaBiomeParameterReader 快取，跳過以防止遞迴
        if (VanillaBiomeParameterReader.IS_READING_VANILLA.get()) {
            return;
        }

        LOGGER.debug("Injecting custom overworld biomes.");

        try {
            BiomeRegionManager.injectBiomes(consumer);

            LOGGER.debug("Custom overworld biome injection completed.");

        } catch (Exception e) {
            LOGGER.error("Custom overworld biome injection failed.", e);
        }
    }
}
