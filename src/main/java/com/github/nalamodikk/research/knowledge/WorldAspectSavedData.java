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

    // Item ID -> List of Aspects
    private final Map<ResourceLocation, List<Aspect>> itemToAspects = new HashMap<>();

    public WorldAspectSavedData() {}

    public void putMapping(ResourceLocation id, List<Aspect> aspects) {
        itemToAspects.put(id, new ArrayList<>(aspects));
        setDirty();
    }

    public List<Aspect> getMapping(ResourceLocation id) {
        return itemToAspects.get(id);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag mapTag = new CompoundTag();
        for (var entry : itemToAspects.entrySet()) {
            ListTag list = new ListTag();
            for (Aspect a : entry.getValue()) {
                list.add(StringTag.valueOf(a.getId().toString()));
            }
            mapTag.put(entry.getKey().toString(), list);
        }
        tag.put("Mappings", mapTag);
        return tag;
    }

    public static WorldAspectSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        WorldAspectSavedData data = new WorldAspectSavedData();
        if (tag.contains("Mappings")) {
            CompoundTag mapTag = tag.getCompound("Mappings");
            for (String key : mapTag.getAllKeys()) {
                ResourceLocation itemId = ResourceLocation.parse(key);
                ListTag list = mapTag.getList(key, Tag.TAG_STRING);
                List<Aspect> aspects = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    Aspect a = ModAspects.get(ResourceLocation.parse(list.getString(i)));
                    if (a != null) aspects.add(a);
                }
                data.itemToAspects.put(itemId, aspects);
            }
        }
        return data;
    }

    public static WorldAspectSavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(new SavedData.Factory<>(
                WorldAspectSavedData::new,
                WorldAspectSavedData::load,
                null
        ), DATA_NAME);
    }
}
