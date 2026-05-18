package com.github.nalamodikk.common.block.blockentity.altar;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class AltarTierSavedData extends SavedData {

    private static final String DATA_NAME = KoniavacraftMod.MOD_ID + "_altar_tiers";
    private final Map<Long, Integer> tiers = new HashMap<>();

    public static AltarTierSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(AltarTierSavedData::new, AltarTierSavedData::load, null),
                DATA_NAME
        );
    }

    public void saveTier(BlockPos pos, int tier) {
        if (tier > 0) {
            tiers.put(pos.asLong(), tier);
            setDirty();
        } else {
            clearTier(pos);
        }
    }

    public int peekTier(BlockPos pos) {
        Integer t = tiers.get(pos.asLong());
        return t != null ? t : 0;
    }

    public void clearTier(BlockPos pos) {
        if (tiers.remove(pos.asLong()) != null) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        long[] keys = new long[tiers.size()];
        int[] vals = new int[tiers.size()];
        int i = 0;
        for (Map.Entry<Long, Integer> e : tiers.entrySet()) {
            keys[i] = e.getKey();
            vals[i] = e.getValue();
            i++;
        }
        tag.putLongArray("Keys", keys);
        tag.putIntArray("Vals", vals);
        return tag;
    }

    public static AltarTierSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        AltarTierSavedData data = new AltarTierSavedData();
        long[] keys = tag.getLongArray("Keys");
        int[] vals = tag.getIntArray("Vals");
        for (int i = 0; i < Math.min(keys.length, vals.length); i++) {
            data.tiers.put(keys[i], vals[i]);
        }
        return data;
    }
}
