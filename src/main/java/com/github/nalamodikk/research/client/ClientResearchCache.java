package com.github.nalamodikk.research.client;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Client-side cache of the local player's discovered aspects and completed research.
 * Updated by networking packets.
 */
public final class ClientResearchCache {

    private static Set<ResourceLocation> discoveredAspects = new HashSet<>();
    private static Set<ResourceLocation> completedResearch = new HashSet<>();
    private static Set<ResourceLocation> availableResearchOverrides = new HashSet<>();
    private static int currentTier = 1;

    /** Called by networking handlers on the client thread. */
    public static void update(Collection<ResourceLocation> aspects, Collection<ResourceLocation> research, int tier) {
        update(aspects, research, Collections.emptySet(), tier);
    }

    public static void update(Collection<ResourceLocation> aspects, Collection<ResourceLocation> research,
                              Collection<ResourceLocation> availableOverrides, int tier) {
        discoveredAspects = new HashSet<>(aspects);
        completedResearch = new HashSet<>(research);
        availableResearchOverrides = new HashSet<>(availableOverrides);
        currentTier = tier;
    }

    public static void updateAspects(Collection<ResourceLocation> aspects) {
        discoveredAspects = new HashSet<>(aspects);
    }

    public static boolean hasDiscovered(ResourceLocation aspectId) {
        return discoveredAspects.contains(aspectId);
    }

    public static Set<ResourceLocation> getDiscoveredAspects() {
        return Collections.unmodifiableSet(discoveredAspects);
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
