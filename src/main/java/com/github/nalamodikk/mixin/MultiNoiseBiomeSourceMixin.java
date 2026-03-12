package com.github.nalamodikk.mixin;

import com.github.nalamodikk.biome.region.BiomeRegionManager;
import com.github.nalamodikk.biome.region.SimpleBiomeRegion;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Intercepts {@link MultiNoiseBiomeSource#getNoiseBiome} to implement Zoom-Layer
 * region-based custom biome placement.
 *
 * <p>Positions where {@link BiomeRegionManager#getRegionIndex} returns a non-zero value
 * are routed to that region's own {@link Climate.ParameterList}, so custom biomes never
 * compete against vanilla biomes in the 6D climate fitness search.
 *
 * <p>One {@link Climate.ParameterList} per region index is built lazily on first use and
 * cached for the lifetime of this {@link MultiNoiseBiomeSource} instance.
 */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Per-region lazy cache.  Key is the region's uniqueness index (1..N).
     * Value is {@code Optional.empty()} if the region produced no usable entries.
     */
    private final Map<Integer, Optional<Climate.ParameterList<Holder<Biome>>>> koniava$regionLists =
            new ConcurrentHashMap<>();

    @Inject(method = "getNoiseBiome", at = @At("HEAD"), cancellable = true)
    private void koniava$interceptGetNoiseBiome(
            int x, int y, int z,
            Climate.Sampler climateSampler,
            CallbackInfoReturnable<Holder<Biome>> cir) {

        if (!BiomeRegionManager.hasCustomRegions()) return;

        // x/z arrive in biome coordinates (block >> 2); convert back to block coords for the Area
        int regionIndex = BiomeRegionManager.getRegionIndex(x << 2, z << 2);
        if (regionIndex == 0) return;

        Climate.TargetPoint target = climateSampler.sample(x, y, z);
        // Only intercept surface queries; let vanilla handle cave/underground biomes
        if (Climate.unquantizeCoord(target.depth()) > 0.1f) return;
        // Let vanilla handle ocean/coast positions — custom biomes should not bleed into water
        // COAST threshold ≈ -0.11 (NEAR_INLAND starts at -0.11 in VanillaClimateBands)
        if (Climate.unquantizeCoord(target.continentalness()) < -0.11f) return;
        // Let vanilla handle river/valley slots — VALLEY weirdness ≈ [-0.05, 0.05]
        float weirdness = Climate.unquantizeCoord(target.weirdness());
        if (weirdness > -0.06f && weirdness < 0.06f) return;

        Climate.ParameterList<Holder<Biome>> list = getOrBuildListForRegion(regionIndex);
        if (list == null) return;

        Holder<Biome> result = list.findValue(target);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    private Climate.ParameterList<Holder<Biome>> getOrBuildListForRegion(int index) {
        Optional<Climate.ParameterList<Holder<Biome>>> cached = koniava$regionLists.get(index);
        if (cached != null) return cached.orElse(null);

        synchronized (koniava$regionLists) {
            cached = koniava$regionLists.get(index);
            if (cached != null) return cached.orElse(null);

            Climate.ParameterList<Holder<Biome>> list = buildListForRegion(index);
            koniava$regionLists.put(index, Optional.ofNullable(list));
            return list;
        }
    }

    private Climate.ParameterList<Holder<Biome>> buildListForRegion(int index) {
        SimpleBiomeRegion region = BiomeRegionManager.getRegionByIndex(index);
        if (region == null) return null;

        // Build holder map from possibleBiomes() — no @Shadow needed
        Map<ResourceKey<Biome>, Holder<Biome>> holderMap = new HashMap<>();
        for (Holder<Biome> holder : ((BiomeSource) (Object) this).possibleBiomes()) {
            holder.unwrapKey().ifPresent(key -> holderMap.put(key, holder));
        }

        List<Pair<Climate.ParameterPoint, Holder<Biome>>> entries = new ArrayList<>();
        region.forEachEntry(entry -> {
            Holder<Biome> holder = holderMap.get(entry.biome());
            if (holder == null) {
                LOGGER.warn("Koniava region [{}]: holder not found for '{}', was it registered in the biome registry?",
                        index, entry.biome().location());
                return;
            }
            entries.add(Pair.of(entry.climate().toPoint(0L), holder));
        });

        if (entries.isEmpty()) {
            LOGGER.debug("Koniava region [{}]: no usable entries, vanilla biomes will show here", index);
            return null;
        }

        LOGGER.info("Koniava region [{}]: built climate list with {} entries (region id={})",
                index, entries.size(), region.id());
        return new Climate.ParameterList<>(entries);
    }
}
