package com.github.nalamodikk.common.item.wand;

import com.github.nalamodikk.common.item.wand.core.IWandCore;
import com.github.nalamodikk.common.item.wand.upgrade.WandUpgradeBehavior;
import com.github.nalamodikk.common.item.wand.upgrade.WandUpgradeItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Stores the installed core plugin and up to UPGRADE_SLOTS upgrade items.
 * Empty slots are absent from the map — ItemStack.EMPTY is never serialized.
 */
public record WandCoreData(ItemStack core, Map<Integer, ItemStack> upgrades) {

    public static final int UPGRADE_SLOTS = 6;

    // Upgrades 序列化：Integer key → String（NBT map 只支援 String key）
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

    // Core 序列化：存在時序列化，不存在時省略
    private static final Codec<ItemStack> CORE_CODEC =
            ItemStack.CODEC.optionalFieldOf("_core").codec().xmap(
                    opt -> opt.orElse(ItemStack.EMPTY),
                    stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack)
            );

    public static final Codec<WandCoreData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            CORE_CODEC.optionalFieldOf("core", ItemStack.EMPTY).forGetter(WandCoreData::core),
            UPGRADES_CODEC.optionalFieldOf("upgrades", new HashMap<>()).forGetter(WandCoreData::upgrades)
    ).apply(inst, WandCoreData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WandCoreData> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, WandCoreData::core,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, ItemStack.STREAM_CODEC),
            WandCoreData::upgrades,
            WandCoreData::new
    );

    // ── Factory ───────────────────────────────────────────────────────────

    public static WandCoreData empty() {
        return new WandCoreData(ItemStack.EMPTY, new HashMap<>());
    }

    // ── Queries ───────────────────────────────────────────────────────────

    public boolean hasCore() {
        return !core.isEmpty() && core.getItem() instanceof IWandCore;
    }

    public IWandCore getCore() {
        return core.getItem() instanceof IWandCore c ? c : null;
    }

    public ItemStack getUpgrade(int slot) {
        return upgrades.getOrDefault(slot, ItemStack.EMPTY);
    }

    public int countUpgrade(WandUpgradeBehavior type) {
        int count = 0;
        for (ItemStack upg : upgrades.values()) {
            if (upg.getItem() instanceof WandUpgradeItem wu && wu.getBehavior() == type) count++;
        }
        return count;
    }

    public int sumUpgradeBonus(WandUpgradeBehavior type) {
        int total = 0;
        for (ItemStack upg : upgrades.values()) {
            if (upg.getItem() instanceof WandUpgradeItem wu && wu.getBehavior() == type) {
                total += type.getBonusForMk(wu.getMk());
            }
        }
        return total;
    }

    // ── Mutators ──────────────────────────────────────────────────────────

    public WandCoreData withCore(ItemStack newCore) {
        return new WandCoreData(newCore.isEmpty() ? ItemStack.EMPTY : newCore.copy(), new HashMap<>(upgrades));
    }

    public WandCoreData withUpgrade(int slot, ItemStack item) {
        Map<Integer, ItemStack> copy = new HashMap<>(upgrades);
        if (item.isEmpty()) copy.remove(slot);
        else copy.put(slot, item.copy());
        return new WandCoreData(core.isEmpty() ? ItemStack.EMPTY : core.copy(), copy);
    }

    // ── equals / hashCode ─────────────────────────────────────────────────────
    // ItemStack has no equals() override — we must provide content-based comparison
    // to prevent equip animation from firing on every inventory sync.

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WandCoreData other)) return false;
        if (!ItemStack.isSameItemSameComponents(this.core, other.core)) return false;
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
        int h = core.isEmpty() ? 0 : Objects.hashCode(core.getItem());
        h = 31 * h + upgrades.size();
        return h;
    }
}
