package com.github.nalamodikk.common.item.wand;

import com.github.nalamodikk.common.item.wand.core.IWandCore;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record WandCoreData(ItemStack core, List<ItemStack> upgrades) {

    public static final int UPGRADE_SLOTS = 4;

    public static final Codec<WandCoreData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ItemStack.CODEC.fieldOf("core").forGetter(WandCoreData::core),
            ItemStack.CODEC.listOf().fieldOf("upgrades").forGetter(WandCoreData::upgrades)
    ).apply(inst, WandCoreData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WandCoreData> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, WandCoreData::core,
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(UPGRADE_SLOTS)), WandCoreData::upgrades,
            WandCoreData::new
    );

    public static WandCoreData empty() {
        List<ItemStack> slots = new ArrayList<>(UPGRADE_SLOTS);
        for (int i = 0; i < UPGRADE_SLOTS; i++) slots.add(ItemStack.EMPTY);
        return new WandCoreData(ItemStack.EMPTY, slots);
    }

    public boolean hasCore() {
        return !core.isEmpty() && core.getItem() instanceof IWandCore;
    }

    public IWandCore getCore() {
        return core.getItem() instanceof IWandCore c ? c : null;
    }

    public WandCoreData withCore(ItemStack newCore) {
        return new WandCoreData(newCore.copy(), new ArrayList<>(upgrades));
    }

    public WandCoreData withUpgrade(int slot, ItemStack item) {
        List<ItemStack> copy = new ArrayList<>(upgrades);
        while (copy.size() <= slot) copy.add(ItemStack.EMPTY);
        copy.set(slot, item.copy());
        return new WandCoreData(core.copy(), copy);
    }
}
