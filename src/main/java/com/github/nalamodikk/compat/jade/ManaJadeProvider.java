package com.github.nalamodikk.compat.jade;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.register.ModCapabilities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class ManaJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    public static final ManaJadeProvider INSTANCE = new ManaJadeProvider();

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_storage");

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        IUnifiedManaHandler mana = accessor.getLevel().getCapability(
                ModCapabilities.MANA, accessor.getPosition(), null);
        if (mana == null) return;
        data.putInt("manaStored", mana.getManaStored());
        data.putInt("maxMana",    mana.getMaxManaStored());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("manaStored")) return;
        int stored = data.getInt("manaStored");
        int max    = data.getInt("maxMana");
        tooltip.add(Component.translatable("jade.koniava.mana_storage", stored, max));
    }

    @Override
    public ResourceLocation getUid() { return UID; }
}
