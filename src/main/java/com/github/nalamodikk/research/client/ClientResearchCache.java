package com.github.nalamodikk.research.client;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;

/**
 * Client-side cache of the local player's discovered aspects and completed research.
 * Updated by networking packets.
 */
public final class ClientResearchCache {

    private static Map<ResourceLocation, Integer> discoveredAspects = new HashMap<>();
    private static Set<ResourceLocation> completedResearch = new HashSet<>();
    private static Set<ResourceLocation> availableResearchOverrides = new HashSet<>();
    private static int currentTier = 1;

    /** Called by networking handlers on the client thread. */
    public static void update(Collection<ResourceLocation> aspects, Collection<ResourceLocation> research, int tier) {
        update(aspects, research, Collections.emptySet(), tier);
    }

    public static void update(Map<ResourceLocation, Integer> aspects, Collection<ResourceLocation> research, int tier) {
        update(aspects, research, Collections.emptySet(), tier);
    }

    public static void update(Collection<ResourceLocation> aspects, Collection<ResourceLocation> research,
                              Collection<ResourceLocation> availableOverrides, int tier) {
        Map<ResourceLocation, Integer> mappedAspects = new HashMap<>();
        for (ResourceLocation aspect : aspects) {
            mappedAspects.put(aspect, 1);
        }
        update(mappedAspects, research, availableOverrides, tier);
    }

    public static void update(Map<ResourceLocation, Integer> aspects, Collection<ResourceLocation> research,
                              Collection<ResourceLocation> availableOverrides, int tier) {
        discoveredAspects = new HashMap<>(aspects);
        completedResearch = new HashSet<>(research);
        availableResearchOverrides = new HashSet<>(availableOverrides);
        currentTier = tier;
    }

    public static void updateAspects(Collection<ResourceLocation> aspects) {
        Map<ResourceLocation, Integer> mappedAspects = new HashMap<>();
        for (ResourceLocation aspect : aspects) {
            mappedAspects.put(aspect, 1);
        }
        discoveredAspects = mappedAspects;
    }

    public static void updateAspects(Map<ResourceLocation, Integer> aspects) {
        discoveredAspects = new HashMap<>(aspects);
    }

    public static boolean hasDiscovered(ResourceLocation aspectId) {
        return discoveredAspects.containsKey(aspectId);
    }

    public static int getAspectCount(ResourceLocation aspectId) {
        return discoveredAspects.getOrDefault(aspectId, 0);
    }

    public static Set<ResourceLocation> getDiscoveredAspects() {
        return Collections.unmodifiableSet(discoveredAspects.keySet());
    }

    public static Map<ResourceLocation, Integer> getDiscoveredAspectCounts() {
        return Collections.unmodifiableMap(discoveredAspects);
    }

    public static boolean hasCompleted(ResourceLocation researchId) {
        return completedResearch.contains(researchId);
    }

    public static Set<ResourceLocation> getCompletedResearch() {
        return Collections.unmodifiableSet(completedResearch);
    }

    public static boolean isForcedAvailable(ResourceLocation researchId) {
        return availableResearchOverrides.contains(researchId);
    }

    public static Set<ResourceLocation> getAvailableResearchOverrides() {
        return Collections.unmodifiableSet(availableResearchOverrides);
    }

    public static int getCurrentTier() {
        return currentTier;
    }

    private ClientResearchCache() {}
}
