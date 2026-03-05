package com.github.nalamodikk.biome.region;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Applies deterministic and conflict-safe biome parameter overlays.
 */
public class BiomeParameterOverlayBuilder {
    private final Map<Climate.ParameterPoint, BiomeInjectionEntry> points = new LinkedHashMap<>();
    private int collisionReplaced = 0;
    private int collisionSkipped = 0;

    public void addEntry(BiomeInjectionEntry entry) {
        int count = entry.injectionCount();
        for (int i = 0; i < count; i++) {
            Climate.ParameterPoint point = entry.climate().toPoint(computeOffsetJitter(i, count));
            BiomeInjectionEntry existing = points.get(point);
            if (existing == null) {
                points.put(point, entry);
                continue;
            }

            if (shouldReplace(existing, entry)) {
                points.put(point, entry);
                collisionReplaced++;
            } else {
                collisionSkipped++;
            }
        }
    }

    public int emit(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> consumer) {
        for (Map.Entry<Climate.ParameterPoint, BiomeInjectionEntry> entry : points.entrySet()) {
            consumer.accept(Pair.of(entry.getKey(), entry.getValue().biome()));
        }
        return points.size();
    }

    public int getCollisionReplaced() {
        return collisionReplaced;
    }

    public int getCollisionSkipped() {
        return collisionSkipped;
    }

    private static boolean shouldReplace(BiomeInjectionEntry existing, BiomeInjectionEntry incoming) {
        if (incoming.priority() != existing.priority()) {
            return incoming.priority() > existing.priority();
        }
        if (incoming.weight() != existing.weight()) {
            return incoming.weight() > existing.weight();
        }
        String incomingNs = incoming.sourceNamespace();
        String existingNs = existing.sourceNamespace();
        return incomingNs.compareTo(existingNs) < 0;
    }

    private static long computeOffsetJitter(int index, int count) {
        if (count <= 1) {
            return 0L;
        }
        double centered = index - ((count - 1) / 2.0D);
        return Math.round(centered * 120.0D);
    }
}
