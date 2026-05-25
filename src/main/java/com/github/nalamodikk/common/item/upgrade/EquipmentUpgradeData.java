package com.github.nalamodikk.common.item.upgrade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public record EquipmentUpgradeData(Map<Integer, ItemStack> upgrades) {

    private static final Codec<Map<Integer, ItemStack>> UPGRADES_CODEC =
            Codec.unboundedMap(Codec.STRING, ItemStack.CODEC).xmap(
                    raw -> {
                        Map<Integer, ItemStack> m = new HashMap<>();
                        raw.forEach((k, v) -> { if (!v.isEmpty()) m.put(Integer.parseInt(k), v); });
                        return m;
                    },
                    map -> {
                        Map<String, ItemStack> raw = new HashMap<>();
                        map.forEach((k, v) -> { if (!v.isEmpty()) raw.put(String.valueOf(k), v); });
                        return raw;
                    }
            );

    public static final Codec<EquipmentUpgradeData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            UPGRADES_CODEC.optionalFieldOf("upgrades", new HashMap<>()).forGetter(EquipmentUpgradeData::upgrades)
    ).apply(inst, EquipmentUpgradeData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EquipmentUpgradeData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, ItemStack.STREAM_CODEC),
            EquipmentUpgradeData::upgrades,
            EquipmentUpgradeData::new
    );

    public static EquipmentUpgradeData empty() {
        return new EquipmentUpgradeData(new HashMap<>());
    }

    public ItemStack getUpgrade(int slot) {
        return upgrades.getOrDefault(slot, ItemStack.EMPTY);
    }

    public EquipmentUpgradeData withUpgrade(int slot, ItemStack item) {
        Map<Integer, ItemStack> copy = new HashMap<>(upgrades);
        if (item.isEmpty()) copy.remove(slot);
        else copy.put(slot, item.copy());
        return new EquipmentUpgradeData(copy);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EquipmentUpgradeData other)) return false;
        if (this.upgrades.size() != other.upgrades.size()) return false;
        for (Map.Entry<Integer, ItemStack> entry : this.upgrades.entrySet()) {
            ItemStack otherStack = other.upgrades.get(entry.getKey());
            if (otherStack == null) return false;
            if (!ItemStack.isSameItemSameComponents(entry.getValue(), otherStack)) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return upgrades.size();
    }
}
