package com.github.nalamodikk.research.knowledge;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerKnowledge {

    private static final int MIN_TIER = 1;
    private static final int MAX_TIER = 12;
    private static final int PRIMARY_ASPECT_START_AMOUNT = 5;

    private final Map<ResourceLocation, Integer> discoveredAspects = new HashMap<>();
    private final Set<ResourceLocation> completedResearch = new HashSet<>();
    private final Set<ResourceLocation> availableResearchOverrides = new HashSet<>();
    private final Set<ResourceLocation> lockedResearch = new HashSet<>();
    private final Set<ResourceLocation> scannedItems = new HashSet<>();
    private final Set<String> seenTutorials = new HashSet<>();
    private final Set<String> pendingTutorials = new HashSet<>();
    private int currentTier = MIN_TIER;

    public enum ResearchState {
        LOCKED,
        FORCED_AVAILABLE,
        COMPLETED
    }

    public PlayerKnowledge() {
        addPrimaryAspects();
    }

    public boolean hasDiscovered(Aspect aspect) {
        return discoveredAspects.containsKey(aspect.getId());
    }

    public boolean hasDiscovered(ResourceLocation aspectId) {
        return discoveredAspects.containsKey(aspectId);
    }

    public int getAspectCount(ResourceLocation aspectId) {
        return discoveredAspects.getOrDefault(aspectId, 0);
    }

    public boolean discoverAspect(Aspect aspect, int amount) {
        int current = discoveredAspects.getOrDefault(aspect.getId(), 0);
        discoveredAspects.put(aspect.getId(), Math.max(0, current + amount));
        return current == 0 && !aspect.isPrimary();
    }

    public boolean discoverAspect(Aspect aspect) {
        return discoverAspect(aspect, 1);
    }

    public void setAspectAmount(ResourceLocation id, int amount) {
        if (amount < 0) {
            discoveredAspects.remove(id);
        } else {
            discoveredAspects.put(id, amount);
        }
    }

    public Set<ResourceLocation> getDiscoveredAspects() {
        return Collections.unmodifiableSet(discoveredAspects.keySet());
    }

    public Map<ResourceLocation, Integer> getDiscoveredAspectsMap() {
        return Collections.unmodifiableMap(discoveredAspects);
    }

    public boolean hasSeenTutorial(String tutorialId) {
        return seenTutorials.contains(tutorialId);
    }

    public boolean markTutorialSeen(String tutorialId) {
        pendingTutorials.remove(tutorialId);
        return seenTutorials.add(tutorialId);
    }

    public void addPendingTutorial(String tutorialId) {
        if (!seenTutorials.contains(tutorialId)) {
            pendingTutorials.add(tutorialId);
        }
    }

    public Set<String> getPendingTutorials() {
        return Collections.unmodifiableSet(pendingTutorials);
    }

    public boolean recordItemScan(ResourceLocation itemId) {
        return scannedItems.add(itemId);
    }

    public Set<ResourceLocation> getScannedItems() {
        return Collections.unmodifiableSet(scannedItems);
    }

    public boolean hasCompleted(ResourceLocation researchId) {
        return completedResearch.contains(researchId);
    }

    public boolean completeResearch(ResourceLocation researchId) {
        availableResearchOverrides.remove(researchId);
        lockedResearch.remove(researchId);
        return completedResearch.add(researchId);
    }

    public void setResearchState(ResourceLocation id, boolean completed) {
        setResearchState(id, completed ? ResearchState.COMPLETED : ResearchState.LOCKED);
    }

    public void setResearchState(ResourceLocation id, ResearchState state) {
        completedResearch.remove(id);
        availableResearchOverrides.remove(id);
        lockedResearch.remove(id);

        switch (state) {
            case COMPLETED -> completedResearch.add(id);
            case FORCED_AVAILABLE -> availableResearchOverrides.add(id);
            case LOCKED -> lockedResearch.add(id);
        }
    }

    public ResearchState getResearchState(ResourceLocation id) {
        if (lockedResearch.contains(id)) {
            return ResearchState.LOCKED;
        }
        if (completedResearch.contains(id)) {
            return ResearchState.COMPLETED;
        }
        if (availableResearchOverrides.contains(id)) {
            return ResearchState.FORCED_AVAILABLE;
        }
        return ResearchState.LOCKED;
    }

    public boolean isResearchForcedAvailable(ResourceLocation id) {
        return availableResearchOverrides.contains(id);
    }

    public boolean isResearchLocked(ResourceLocation id) {
        return lockedResearch.contains(id);
    }

    public int clearNonPrimaryAspects() {
        int removed = 0;
        var iterator = discoveredAspects.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ResourceLocation, Integer> entry = iterator.next();
            Aspect aspect = ModAspects.get(entry.getKey());
            if (aspect == null || !aspect.isPrimary()) {
                iterator.remove();
                removed++;
            }
        }
        addPrimaryAspects();
        return removed;
    }

    public Set<ResourceLocation> getCompletedResearch() {
        return Collections.unmodifiableSet(completedResearch);
    }

    public Set<ResourceLocation> getAvailableResearchOverrides() {
        return Collections.unmodifiableSet(availableResearchOverrides);
    }

    public Set<ResourceLocation> getLockedResearch() {
        return Collections.unmodifiableSet(lockedResearch);
    }

    public int getCurrentTier() {
        return currentTier;
    }

    public void setTier(int tier) {
        this.currentTier = clampTier(tier);
    }

    public void advanceTier() {
        setTier(currentTier + 1);
    }

    public void clearProgress() {
        discoveredAspects.clear();
        completedResearch.clear();
        availableResearchOverrides.clear();
        lockedResearch.clear();
        scannedItems.clear();
        seenTutorials.clear();
        pendingTutorials.clear();
        currentTier = MIN_TIER;
        addPrimaryAspects();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        CompoundTag aspectTag = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> entry : discoveredAspects.entrySet()) {
            aspectTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put("DiscoveredAspectsQuantities", aspectTag);

        ListTag researchList = new ListTag();
        for (ResourceLocation id : completedResearch) {
            researchList.add(StringTag.valueOf(id.toString()));
        }
        tag.put("CompletedResearch", researchList);

        ListTag availableOverrideList = new ListTag();
        for (ResourceLocation id : availableResearchOverrides) {
            availableOverrideList.add(StringTag.valueOf(id.toString()));
        }
        tag.put("AvailableResearchOverrides", availableOverrideList);

        ListTag lockedList = new ListTag();
        for (ResourceLocation id : lockedResearch) {
            lockedList.add(StringTag.valueOf(id.toString()));
        }
        tag.put("LockedResearch", lockedList);

        ListTag scannedList = new ListTag();
        for (ResourceLocation id : scannedItems) {
            scannedList.add(StringTag.valueOf(id.toString()));
        }
        tag.put("ScannedItems", scannedList);

        ListTag tutorialList = new ListTag();
        for (String id : seenTutorials) {
            tutorialList.add(StringTag.valueOf(id));
        }
        tag.put("SeenTutorials", tutorialList);

        ListTag pendingList = new ListTag();
        for (String id : pendingTutorials) {
            pendingList.add(StringTag.valueOf(id));
        }
        tag.put("PendingTutorials", pendingList);

        tag.putInt("Tier", currentTier);
        return tag;
    }

    public static PlayerKnowledge load(CompoundTag tag) {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.discoveredAspects.clear();

        if (tag.contains("DiscoveredAspectsQuantities", Tag.TAG_COMPOUND)) {
            loadAspectQuantities(tag.getCompound("DiscoveredAspectsQuantities"), knowledge);
        } else {
            loadLegacyAspects(tag.getList("DiscoveredAspects", Tag.TAG_STRING), knowledge);
        }

        loadResearch(tag.getList("CompletedResearch", Tag.TAG_STRING), knowledge);
        loadAvailableResearchOverrides(tag, knowledge);
        loadLockedResearch(tag, knowledge);
        loadScannedItems(tag.getList("ScannedItems", Tag.TAG_STRING), knowledge);
        loadSeenTutorials(tag.getList("SeenTutorials", Tag.TAG_STRING), knowledge);
        loadPendingTutorials(tag.getList("PendingTutorials", Tag.TAG_STRING), knowledge);
        knowledge.addPrimaryAspects();
        knowledge.currentTier = clampTier(tag.getInt("Tier"));
        return knowledge;
    }

    private static void loadAspectQuantities(CompoundTag aspectTag, PlayerKnowledge knowledge) {
        for (String key : aspectTag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) {
                knowledge.discoveredAspects.put(id, Math.max(0, aspectTag.getInt(key)));
            }
        }
    }

    private static void loadLegacyAspects(ListTag aspectList, PlayerKnowledge knowledge) {
        for (int i = 0; i < aspectList.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(aspectList.getString(i));
            if (id != null) {
                knowledge.discoveredAspects.put(id, 1);
            }
        }
    }

    private static void loadResearch(ListTag researchList, PlayerKnowledge knowledge) {
        for (int i = 0; i < researchList.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(researchList.getString(i));
            if (id != null) {
                knowledge.completedResearch.add(id);
            }
        }
    }

    private static void loadAvailableResearchOverrides(CompoundTag tag, PlayerKnowledge knowledge) {
        ListTag availableList = tag.contains("AvailableResearchOverrides", Tag.TAG_LIST)
                ? tag.getList("AvailableResearchOverrides", Tag.TAG_STRING)
                : tag.getList("AvailableResearch", Tag.TAG_STRING);

        for (int i = 0; i < availableList.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(availableList.getString(i));
            if (id != null && !knowledge.completedResearch.contains(id)) {
                knowledge.availableResearchOverrides.add(id);
            }
        }
    }

    private static void loadLockedResearch(CompoundTag tag, PlayerKnowledge knowledge) {
        ListTag lockedList = tag.contains("LockedResearch", Tag.TAG_LIST)
                ? tag.getList("LockedResearch", Tag.TAG_STRING)
                : tag.getList("LockedResearchIds", Tag.TAG_STRING);

        for (int i = 0; i < lockedList.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(lockedList.getString(i));
            if (id != null && !knowledge.completedResearch.contains(id)) {
                knowledge.lockedResearch.add(id);
            }
        }
    }

    private static void loadScannedItems(ListTag scannedList, PlayerKnowledge knowledge) {
        for (int i = 0; i < scannedList.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(scannedList.getString(i));
            if (id != null) {
                knowledge.scannedItems.add(id);
            }
        }
    }

    private static void loadSeenTutorials(ListTag tutorialList, PlayerKnowledge knowledge) {
        for (int i = 0; i < tutorialList.size(); i++) {
            String id = tutorialList.getString(i);
            if (!id.isEmpty()) {
                knowledge.seenTutorials.add(id);
            }
        }
    }

    private static void loadPendingTutorials(ListTag pendingList, PlayerKnowledge knowledge) {
        for (int i = 0; i < pendingList.size(); i++) {
            String id = pendingList.getString(i);
            // Only restore pending if not already seen
            if (!id.isEmpty() && !knowledge.seenTutorials.contains(id)) {
                knowledge.pendingTutorials.add(id);
            }
        }
    }

    private void addPrimaryAspects() {
        for (Aspect aspect : ModAspects.all()) {
            if (aspect.isPrimary()) {
                discoveredAspects.putIfAbsent(aspect.getId(), PRIMARY_ASPECT_START_AMOUNT);
            }
        }
    }

    private static int clampTier(int tier) {
        return Math.max(MIN_TIER, Math.min(MAX_TIER, tier));
    }
}
