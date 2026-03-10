package com.github.nalamodikk.biome.region;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Mutable biome region used by mod code to register biome injection entries.
 */
public class SimpleBiomeRegion implements BiomeRegion {
    private final ResourceLocation id;
    private final int weight;
    private final int uniquenessIndex;
    private final List<BiomeInjectionEntry> entries = new CopyOnWriteArrayList<>();

    /**
     * @param uniquenessIndex region slot index assigned by {@link BiomeRegionManager}; starts at 1
     *                        (0 is reserved for vanilla)
     */
    public SimpleBiomeRegion(ResourceLocation id, int weight, int uniquenessIndex) {
        this.id = id;
        this.weight = weight;
        this.uniquenessIndex = uniquenessIndex;
    }

    /** Returns the zoom-layer slot index for this region (1..N). */
    public int uniquenessIndex() {
        return uniquenessIndex;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public int weight() {
        return weight;
    }

    public void registerEntry(BiomeInjectionEntry entry) {
        entries.add(entry);
    }

    public void clear() {
        entries.clear();
    }

    public boolean hasEntries() {
        return !entries.isEmpty();
    }

    public List<BiomeInjectionEntry> entriesSnapshot() {
        return List.copyOf(entries);
    }

    @Override
    public void forEachEntry(Consumer<BiomeInjectionEntry> consumer) {
        entries.forEach(consumer);
    }
}
