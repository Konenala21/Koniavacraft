package com.github.nalamodikk.research.knowledge;

import com.github.nalamodikk.research.aspect.Aspect;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
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
        boolean isNew = getOrCreate(playerId).completeResearch(researchId);
        if (isNew) setDirty();
        return isNew;
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
