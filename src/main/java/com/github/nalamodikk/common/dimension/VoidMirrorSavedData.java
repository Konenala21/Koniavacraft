package com.github.nalamodikk.common.dimension;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VoidMirrorSavedData extends SavedData {

    private static final String DATA_NAME = KoniavacraftMod.MOD_ID + "_void_mirror";
    private final Set<UUID> clearedPlayers = new HashSet<>();

    public static VoidMirrorSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(VoidMirrorSavedData::new, VoidMirrorSavedData::load, null),
                DATA_NAME
        );
    }

    public boolean isCleared(UUID id) {
        return clearedPlayers.contains(id);
    }

    public void markCleared(UUID id) {
        if (clearedPlayers.add(id)) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (UUID id : clearedPlayers) {
            list.add(StringTag.valueOf(id.toString()));
        }
        tag.put("ClearedPlayers", list);
        return tag;
    }

    public static VoidMirrorSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        VoidMirrorSavedData data = new VoidMirrorSavedData();
        ListTag list = tag.getList("ClearedPlayers", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            try {
                data.clearedPlayers.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }
}
