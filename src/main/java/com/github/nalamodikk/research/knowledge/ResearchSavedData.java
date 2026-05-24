package com.github.nalamodikk.research.knowledge;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.template.ResearchRegistry;
import com.github.nalamodikk.research.template.ResearchTemplate;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * World-level SavedData that stores {@link PlayerKnowledge} for every player.
 *
 * Stored at: world/data/koniava_research.dat
 *
 * Usage:
 * <pre>
 *   ResearchSavedData data = ResearchSavedData.get(serverLevel);
 *   PlayerKnowledge k = data.getOrCreate(player.getUUID());
 *   k.discoverAspect(ModAspects.WATER);
 *   data.setDirty();
 * </pre>
 */
public class ResearchSavedData extends SavedData {

    private static final String SAVE_NAME = "koniava_research";

    private final Map<UUID, PlayerKnowledge> playerData = new HashMap<>();

    // ── Static accessor ──────────────────────────────────────────────────────

    public static ResearchSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ResearchSavedData::new, ResearchSavedData::load),
                SAVE_NAME
        );
    }

    // ── Player data access ───────────────────────────────────────────────────

    public PlayerKnowledge getOrCreate(UUID playerId) {
        return playerData.computeIfAbsent(playerId, id -> new PlayerKnowledge());
    }

    /**
     * Convenience: discover an aspect for a player and mark dirty.
     * Returns true if this was a new discovery.
     */
    public boolean discoverAspect(UUID playerId, Aspect aspect) {
        boolean isNew = getOrCreate(playerId).discoverAspect(aspect);
        if (isNew) setDirty();
        return isNew;
    }

    /**
     * Convenience: complete a research entry for a player and mark dirty.
     * Returns true if this was newly completed.
     */
    public boolean completeResearch(UUID playerId, net.minecraft.resources.ResourceLocation researchId) {
        PlayerKnowledge knowledge = getOrCreate(playerId);
        boolean isNew = knowledge.completeResearch(researchId);
        if (isNew) {
            tryAdvanceTier(knowledge);
            setDirty();
        }
        return isNew;
    }

    private static void tryAdvanceTier(PlayerKnowledge knowledge) {
        int tier = knowledge.getCurrentTier();
        List<ResearchTemplate> tierResearches = ResearchRegistry.allForTier(tier);
        if (tierResearches.isEmpty()) return;
        boolean allDone = tierResearches.stream()
                .allMatch(t -> knowledge.hasCompleted(t.getId()));
        if (allDone) knowledge.advanceTier();
    }

    // ── NBT serialisation ────────────────────────────────────────────────────

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag playersTag = new CompoundTag();
        for (Map.Entry<UUID, PlayerKnowledge> entry : playerData.entrySet()) {
            playersTag.put(entry.getKey().toString(), entry.getValue().save());
        }
        tag.put("Players", playersTag);
        return tag;
    }

    public static ResearchSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ResearchSavedData data = new ResearchSavedData();
        CompoundTag playersTag = tag.getCompound("Players");
        for (String key : playersTag.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                data.playerData.put(uuid, PlayerKnowledge.load(playersTag.getCompound(key)));
            } catch (IllegalArgumentException ignored) {
                // Corrupt key — skip silently
            }
        }
        return data;
    }
}
