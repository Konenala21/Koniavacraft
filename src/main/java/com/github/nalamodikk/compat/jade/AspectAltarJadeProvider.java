package com.github.nalamodikk.compat.jade;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.List;

public class AspectAltarJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    public static final AspectAltarJadeProvider INSTANCE = new AspectAltarJadeProvider();

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "aspect_altar");

    // Server: 把 BlockEntity 資料打包送給客戶端
    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof AspectAltarBlockEntity altar)) return;

        data.putBoolean("formed", altar.isFormed());

        List<ItemStack> pedestalItems = altar.getPedestalItems();
        data.putInt("pedestalTotal", pedestalItems.size());
        ListTag pedestalTag = new ListTag();
        for (ItemStack item : pedestalItems) {
            if (!item.isEmpty()) {
                pedestalTag.add(item.save(accessor.getLevel().registryAccess()));
            }
        }
        data.put("pedestals", pedestalTag);
    }

    // Client: 從資料渲染 Tooltip
    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();

        boolean formed = data.getBoolean("formed");
        tooltip.add(Component.translatable(
                formed ? "jade.koniava.altar.formed" : "jade.koniava.altar.not_formed"));

        if (data.contains("pedestals")) {
            int totalPedestals = data.getInt("pedestalTotal");
            ListTag pedestalTag = data.getList("pedestals", Tag.TAG_COMPOUND);
            int filledCount = 0;
            for (int i = 0; i < pedestalTag.size(); i++) {
                ItemStack item = ItemStack.parseOptional(
                        accessor.getLevel().registryAccess(), pedestalTag.getCompound(i));
                if (!item.isEmpty()) filledCount++;
            }
            if (totalPedestals > 0) {
                tooltip.add(Component.translatable("jade.koniava.altar.pedestals", filledCount, totalPedestals));
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
