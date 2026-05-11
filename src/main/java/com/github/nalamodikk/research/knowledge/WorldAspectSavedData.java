package com.github.nalamodikk.research.knowledge;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists the dynamically generated aspect mappings for a specific world.
 * This ensures that " आयरन सोर्ड" (Iron Sword) always has the same aspects in the same world,
 * but might differ between seeds.
 */
public class WorldAspectSavedData extends SavedData {
    private static final String DATA_NAME = KoniavacraftMod.MOD_ID + "_dynamic_aspects";

    private static final String GENOME_SEED_KEY = "GenomeSeed";
    private static final String ITEM_MAPPINGS_KEY = "ItemMappings";
    private static final String BLOCK_MAPPINGS_KEY = "BlockMappings";
    private static final String ENTITY_MAPPINGS_KEY = "EntityMappings";

    private long genomeSeed;
    private boolean genomeInitialized;

    private final Map<ResourceLocation, List<Aspect>> itemToAspects = new HashMap<>();
    private final Map<ResourceLocation, List<Aspect>> blockToAspects = new HashMap<>();
    private final Map<ResourceLocation, List<Aspect>> entityToAspects = new HashMap<>();

    public WorldAspectSavedData() {}

    public void initializeGenome(long levelSeed) {
        if (genomeInitialized) {
            return;
        }
        genomeSeed = mix(levelSeed ^ KoniavacraftMod.MOD_ID.hashCode());
        genomeInitialized = true;
        setDirty();
    }

    public long getGenomeSeed() {
        return genomeSeed;
    }

    public void putMapping(ResourceLocation id, List<Aspect> aspects) {
        putItemMapping(id, aspects);
    }

    public List<Aspect> getMapping(ResourceLocation id) {
        return getItemMapping(id);
    }

    public void putItemMapping(ResourceLocation id, List<Aspect> aspects) {
        put(itemToAspects, id, aspects);
    }

    public List<Aspect> getItemMapping(ResourceLocation id) {
        return itemToAspects.get(id);
    }

    public void putBlockMapping(ResourceLocation id, List<Aspect> aspects) {
        put(blockToAspects, id, aspects);
    }

    public List<Aspect> getBlockMapping(ResourceLocation id) {
        return blockToAspects.get(id);
    }

    public void putEntityMapping(ResourceLocation id, List<Aspect> aspects) {
        put(entityToAspects, id, aspects);
    }

    public List<Aspect> getEntityMapping(ResourceLocation id) {
        return entityToAspects.get(id);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong(GENOME_SEED_KEY, genomeSeed);
        tag.put(ITEM_MAPPINGS_KEY, saveMappings(itemToAspects));
        tag.put(BLOCK_MAPPINGS_KEY, saveMappings(blockToAspects));
        tag.put(ENTITY_MAPPINGS_KEY, saveMappings(entityToAspects));
        return tag;
    }

    public static WorldAspectSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        WorldAspectSavedData data = new WorldAspectSavedData();
        if (tag.contains(GENOME_SEED_KEY)) {
            data.genomeSeed = tag.getLong(GENOME_SEED_KEY);
            data.genomeInitialized = true;
        }
        loadMappings(data.itemToAspects, tag, ITEM_MAPPINGS_KEY);
        loadMappings(data.blockToAspects, tag, BLOCK_MAPPINGS_KEY);
        loadMappings(data.entityToAspects, tag, ENTITY_MAPPINGS_KEY);
        loadMappings(data.itemToAspects, tag, "Mappings");
        return data;
    }

    public static WorldAspectSavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        WorldAspectSavedData data = storage.computeIfAbsent(new SavedData.Factory<>(
                WorldAspectSavedData::new,
                WorldAspectSavedData::load,
                null
        ), DATA_NAME);
        data.initializeGenome(level.getSeed());
        return data;
    }

    private void put(Map<ResourceLocation, List<Aspect>> mappings, ResourceLocation id, List<Aspect> aspects) {
        mappings.put(id, new ArrayList<>(aspects));
        setDirty();
    }

    private static CompoundTag saveMappings(Map<ResourceLocation, List<Aspect>> mappings) {
        CompoundTag mapTag = new CompoundTag();
        for (Map.Entry<ResourceLocation, List<Aspect>> entry : mappings.entrySet()) {
            ListTag list = new ListTag();
            for (Aspect aspect : entry.getValue()) {
                list.add(StringTag.valueOf(aspect.getId().toString()));
            }
            mapTag.put(entry.getKey().toString(), list);
        }
        return mapTag;
    }

    private static void loadMappings(Map<ResourceLocation, List<Aspect>> mappings, CompoundTag tag, String keyName) {
        if (!tag.contains(keyName)) {
            return;
        }
        CompoundTag mapTag = tag.getCompound(keyName);
        for (String key : mapTag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.parse(key);
            ListTag list = mapTag.getList(key, Tag.TAG_STRING);
            List<Aspect> aspects = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                Aspect aspect = ModAspects.get(ResourceLocation.parse(list.getString(i)));
                if (aspect != null) {
                    aspects.add(aspect);
                }
            }
            mappings.put(id, aspects);
        }
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }
}
