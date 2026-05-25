package com.github.nalamodikk.common.item.equipment.armor;

import com.github.nalamodikk.common.item.equipment.ManaArmorItem;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

public class ManaAlloyChestplateItem extends ManaArmorItem {

    public static final int BASE_MAX_MANA = 7500;
    public static final int BASE_ARMOR    = 6;
    public static final int MAX_UPGRADE_SLOTS = 4;

    public ManaAlloyChestplateItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, BASE_MAX_MANA, BASE_ARMOR, MAX_UPGRADE_SLOTS, properties);
    }

    @Override
    public void recalculateMaxMana(ItemStack armor) {
        int max = BASE_MAX_MANA;
        for (ItemStack upg : getData(armor).upgrades().values()) {
            if (upg.getItem() instanceof ChestplateUpgradeItem cu
                    && cu.getBehavior() == ChestplateUpgradeBehavior.CAPACITY) {
                max += cu.getBehavior().getBonusForMk(cu.getMk());
            }
        }
        armor.set(ModDataComponents.MAX_MANA, max);
        int stored = getMana(armor);
        if (stored > max) armor.set(ModDataComponents.MANA_STORED, max);
    }

    @Override
    public boolean isValidUpgradeItem(ItemStack stack) {
        return stack.getItem() instanceof ChestplateUpgradeItem;
    }

    @Override
    public String getUpgradeBehaviorKey(ItemStack upgradeStack) {
        if (upgradeStack.getItem() instanceof ChestplateUpgradeItem cu)
            return cu.getBehavior().name();
        return "";
    }

    @Override
    protected int getArmorBonus(ItemStack stack) {
        int bonus = 0;
        for (ItemStack upg : getData(stack).upgrades().values()) {
            if (upg.getItem() instanceof ChestplateUpgradeItem cu
                    && cu.getBehavior() == ChestplateUpgradeBehavior.ARMOR) {
                bonus += cu.getBehavior().getBonusForMk(cu.getMk());
            }
        }
        return bonus;
    }
}
