package com.github.nalamodikk.research.client;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Client-side cache of the local player's discovered aspects.
 * Updated by {@link com.github.nalamodikk.research.network.AspectSyncPacket}.
 * Read by {@link com.github.nalamodikk.common.block.blockentity.research.ResearchTableScreen}
 * when building the palette.
 */
public final class ClientResearchCache {

    private static Set<ResourceLocation> discoveredAspects = new HashSet<>();

    /** Called by AspectSyncPacket handler on the client thread. */
    public static void update(Collection<ResourceLocation> aspects) {
        discoveredAspects = new HashSet<>(aspects);
    }

    public static boolean hasDiscovered(ResourceLocation aspectId) {
        return discoveredAspects.contains(aspectId);
    }

    public static Set<ResourceLocation> getDiscoveredAspects() {
        return Collections.unmodifiableSet(discoveredAspects);
    }

    private ClientResearchCache() {}
}
