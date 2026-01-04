package com.github.nalamodikk.common.utils.fuel;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.HashMap;
import java.util.Map;

public class FuelUtils {

    public static int getBurnTime(Item item) {
        return FurnaceBlockEntity.getFuel().getOrDefault(item, 0);
    }

    public static Map<Item, Integer> getAllVanillaFuelItems() {
        Map<Item, Integer> fuelMap = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            int burnTime = getBurnTime(item);
            if (burnTime > 0) {
                fuelMap.put(item, burnTime);
            }
        }
        return fuelMap;
    }
}
