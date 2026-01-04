package com.github.nalamodikk.common.utils.nbt;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class TagUtils {

    /**
     * 判斷指定物品是否屬於某個標籤。
     * 建議將此方法移至 TagUtils 工具類中。
     */
    public static boolean isItemInTag(ResourceLocation itemId, String tagName) {
        TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(),
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, tagName));

        return BuiltInRegistries.ITEM.getTag(tag)
                .stream()
                .flatMap(holderSet -> holderSet.stream())
                .anyMatch(holder -> holder.value().builtInRegistryHolder().key().location().equals(itemId));
    }

}
