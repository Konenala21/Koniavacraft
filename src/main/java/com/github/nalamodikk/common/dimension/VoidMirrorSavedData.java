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
    // boss 死亡後待清的裂縫（owner UUID），裂縫 tick 時自我 discard。
    // 用這方式而不是 die() 即時刪除是因為 die() 時 overworld 那塊 chunk 通常已卸載，
    // 直接 getEntities 找不到入口裂縫。讓裂縫自己 tick 檢查可解決 chunk 載入問題。
    private final Set<UUID> pendingCrackRemoval = new HashSet<>();

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

    public boolean hasPendingCrackRemoval(UUID id) {
        return pendingCrackRemoval.contains(id);
    }

    public void markCrackRemovalPending(UUID id) {
        if (pendingCrackRemoval.add(id)) setDirty();
    }

    public void clearCrackRemovalPending(UUID id) {
        if (pendingCrackRemoval.remove(id)) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (UUID id : clearedPlayers) {
            list.add(StringTag.valueOf(id.toString()));
        }
        tag.put("ClearedPlayers", list);
        ListTag pendingList = new ListTag();
        for (UUID id : pendingCrackRemoval) pendingList.add(StringTag.valueOf(id.toString()));
        tag.put("PendingCrackRemoval", pendingList);
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
        ListTag pendingList = tag.getList("PendingCrackRemoval", Tag.TAG_STRING);
        for (int i = 0; i < pendingList.size(); i++) {
            try {
                data.pendingCrackRemoval.add(UUID.fromString(pendingList.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }
}
