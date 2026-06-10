package com.github.nalamodikk.space.ship;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * 飛船燃料分級的能量換算。固態 tier：任何爐子燃料都能當燃料(energy = 燃燒 tick × FUEL_K)，
 * 所以煤/木炭/木板/熔岩桶...都行，燒越久的密度越高，天然形成低階梯度。
 * 中階 = 魔力網路(走 capability，不經這裡)。高階(曲速燃料)之後在 #2 加專屬物品 + 更高換算。
 */
public final class ShipFuels {
    private static final int FUEL_K = 6; // 1 tick 燃燒值 = 6 點燃料能量(煤 1600t → 9600，約 10 顆滿一槽)

    private ShipFuels() {}

    /** 這個物品當飛船燃料值多少能量；不是燃料回 0。 */
    public static int energyOf(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        int burn = stack.getBurnTime(RecipeType.SMELTING);
        return burn <= 0 ? 0 : burn * FUEL_K;
    }

    /**
     * 這個流體每 mb 值多少能量；不是燃料回 0。給輸入方塊吃流體燃料用。
     * 目前只認熔岩(120/mb，對齊熔岩桶換算)；未來新流體燃料在這裡加。
     */
    public static int fluidEnergyOf(Fluid fluid) {
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) return 120;
        return 0;
    }
}
