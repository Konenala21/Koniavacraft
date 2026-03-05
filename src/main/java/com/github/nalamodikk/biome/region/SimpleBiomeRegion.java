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
    private final List<BiomeInjectionEntry> entries = new CopyOnWriteArrayList<>();

    public SimpleBiomeRegion(ResourceLocation id, int weight) {
        this.id = id;
        this.weight = weight;
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

    public List<BiomeInjectionEntry> entriesSnapshot() {
        return List.copyOf(entries);
    }

    @Override
    public void forEachEntry(Consumer<BiomeInjectionEntry> consumer) {
        entries.forEach(consumer);
    }
}
