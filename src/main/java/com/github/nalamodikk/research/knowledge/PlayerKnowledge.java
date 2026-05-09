package com.github.nalamodikk.research.knowledge;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Per-player research progress:
 *   - which aspects have been discovered (via scanning)
 *   - which research entries have been completed
 *   - current tier (1–12)
 *
 * Serialised as NBT and stored inside {@link ResearchSavedData}.
 */
public class PlayerKnowledge {

    private final Set<ResourceLocation> discoveredAspects = new HashSet<>();
    private final Set<ResourceLocation> completedResearch = new HashSet<>();
    private int currentTier = 1;

    public PlayerKnowledge() {
        // The six primary aspects are always known — no scanning required
        ModAspects.all().stream()
                .filter(Aspect::isPrimary)
                .forEach(a -> discoveredAspects.add(a.getId()));
    }

    // ── Aspect discovery ─────────────────────────────────────────────────────

    public boolean hasDiscovered(Aspect aspect) {
        return discoveredAspects.contains(aspect.getId());
    }

    public boolean hasDiscovered(ResourceLocation aspectId) {
        return discoveredAspects.contains(aspectId);
    }

    /** Returns true if this was a new discovery. */
    public boolean discoverAspect(Aspect aspect) {
        return discoveredAspects.add(aspect.getId());
    }

    public Set<ResourceLocation> getDiscoveredAspects() {
        return Collections.unmodifiableSet(discoveredAspects);
    }

    // ── Research completion ──────────────────────────────────────────────────

    public boolean hasCompleted(ResourceLocation researchId) {
        return completedResearch.contains(researchId);
    }

    /** Returns true if this was newly completed. */
    public boolean completeResearch(ResourceLocation researchId) {
        return completedResearch.add(researchId);
    }

    public Set<ResourceLocation> getCompletedResearch() {
        return Collections.unmodifiableSet(completedResearch);
    }

    // ── Tier ────────────────────────────────────────────────────────────────

    public int getCurrentTier() { return currentTier; }

    public void advanceTier() {
        if (currentTier < 12) currentTier++;
    }

    // ── NBT serialisation ────────────────────────────────────────────────────

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        ListTag aspectList = new ListTag();
        for (ResourceLocation id : discoveredAspects) {
            aspectList.add(StringTag.valueOf(id.toString()));
        }
        tag.put("DiscoveredAspects", aspectList);

        ListTag researchList = new ListTag();
        for (ResourceLocation id : completedResearch) {
            researchList.add(StringTag.valueOf(id.toString()));
        }
        tag.put("CompletedResearch", researchList);

        tag.putInt("Tier", currentTier);
        return tag;
    }

    public static PlayerKnowledge load(CompoundTag tag) {
        PlayerKnowledge knowledge = new PlayerKnowledge();

        ListTag aspectList = tag.getList("DiscoveredAspects", Tag.TAG_STRING);
        for (int i = 0; i < aspectList.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(aspectList.getString(i));
            if (id != null && ModAspects.contains(id)) {
                knowledge.discoveredAspects.add(id);
            }
        }

        ListTag researchList = tag.getList("CompletedResearch", Tag.TAG_STRING);
        for (int i = 0; i < researchList.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(researchList.getString(i));
            if (id != null) knowledge.completedResearch.add(id);
        }

        knowledge.currentTier = Math.clamp(tag.getInt("Tier"), 1, 12);
        return knowledge;
    }
}
