package com.github.nalamodikk.common.utils.logic;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.mana_generator.recipe.loader.ManaGenFuelRateLoader;
import com.github.nalamodikk.common.utils.nbt.TagUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * 燃料工具類，用於查詢燃料燃燒時間與自訂魔力倍率。
 * 適用於 GUI、機器邏輯與資料讀取。
 */
public class FuelRegistryHelper {

    /**
     * 用於快取已查詢過的燃料倍率，減少重複查詢。
     */
    private static final Map<Item, ManaGenFuelRateLoader.FuelRate> FUEL_RATE_CACHE = new WeakHashMap<>();

    /**
     * 檢查一個物品是否為 Minecraft 內建的可燃燒燃料。
     */
    public static boolean isFuel(@NotNull ItemStack stack) {
        return getBurnTime(stack) > 0;
    }

    /**
     * 取得該物品的燃燒時間（tick 為單位），沒有則回傳 0。
     */
    public static int getBurnTime(@NotNull ItemStack stack) {
        return FurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
    }

    /**
     * 直接由 Item 查詢燃燒時間（會包成 1 個 ItemStack 再查）。
     */
    public static int getBurnTime(@NotNull Item item) {
        return getBurnTime(new ItemStack(item));
    }



    /**
     * 取得所有自定義 + 系統可識別的有效燃料（ManaFuel JSON / tag / 系統爐）。
     */
    public static Map<Item, ManaGenFuelRateLoader.FuelRate> getAllRecognizedFuelItems() {
        Map<Item, ManaGenFuelRateLoader.FuelRate> map = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ManaGenFuelRateLoader.FuelRate rate = getFuelRateFor(item);
            if (rate.getBurnTime() > 0) {
                map.put(item, rate);
            }
        }
        return map;
    }

    /**
     * 取得目前所有定義過燃料倍率的項目（供開發或 GUI 用）。
     */
    public static Set<String> getAllDefinedFuelIds() {
        return ManaGenFuelRateLoader.getAllDefinedFuelIds();
    }

    /**
     * 取得指定 Item 的 FuelRate（優先使用快取）。
     * 若找不到，將根據內建燃燒時間給予預設倍率。
     */
    public static ManaGenFuelRateLoader.FuelRate getFuelRateFor(@NotNull Item item) {
        ManaGenFuelRateLoader.FuelRate rate = FUEL_RATE_CACHE.computeIfAbsent(item,
                it -> ManaGenFuelRateLoader.getFuelRateForItem(
                        ResourceLocation.fromNamespaceAndPath(
                                it.builtInRegistryHolder().key().location().getNamespace(),
                                it.builtInRegistryHolder().key().location().getPath()
                        )));

        if (KoniavacraftMod.IS_DEV && !hasCustomFuelRate(item)) {
            KoniavacraftMod.LOGGER.debug("⚠️ 未定義燃料倍率: {}", item);
        }

        return rate;
    }


    /**
     * 確認某物品是否有自訂燃料倍率。
     */
    public static boolean hasCustomFuelRate(@NotNull Item item) {
        ResourceLocation id = item.builtInRegistryHolder().key().location();
        return ManaGenFuelRateLoader.getAllDefinedFuelIds().contains(id.toString());
    }

    /**
     * 判斷某 Item 是否屬於指定燃料 Tag（作為轉接）。
     */
    public static boolean isFuelInTag(@NotNull Item item, @NotNull String tagId) {
        return TagUtils.isItemInTag(item.builtInRegistryHolder().key().location(), tagId);
    }
}
