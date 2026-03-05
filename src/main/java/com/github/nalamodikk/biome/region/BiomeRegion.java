package com.github.nalamodikk.biome.region;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * Region-like biome contributor abstraction.
 */
public interface BiomeRegion {
    ResourceLocation id();

    int weight();

    void forEachEntry(Consumer<BiomeInjectionEntry> consumer);
}
