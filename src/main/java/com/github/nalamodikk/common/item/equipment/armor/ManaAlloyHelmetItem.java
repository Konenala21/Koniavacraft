package com.github.nalamodikk.common.item.equipment.armor;

import com.github.nalamodikk.common.item.equipment.ManaArmorItem;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

public class ManaAlloyHelmetItem extends ManaArmorItem {

    public static final int BASE_MAX_MANA = 5000;
    public static final int BASE_ARMOR    = 2;
    public static final int MAX_UPGRADE_SLOTS = 3;

    public ManaAlloyHelmetItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, BASE_MAX_MANA, BASE_ARMOR, MAX_UPGRADE_SLOTS, properties);
    }

    @Override
    public void recalculateMaxMana(ItemStack armor) {
        int max = BASE_MAX_MANA;
        for (ItemStack upg : getData(armor).upgrades().values()) {
            if (upg.getItem() instanceof HelmetUpgradeItem hu
                    && hu.getBehavior() == HelmetUpgradeBehavior.CAPACITY) {
                max += hu.getBehavior().getBonusForMk(hu.getMk());
            }
        }
        armor.set(ModDataComponents.MAX_MANA, max);
        int stored = getMana(armor);
        if (stored > max) armor.set(ModDataComponents.MANA_STORED, max);
    }

    @Override
    public boolean isValidUpgradeItem(ItemStack stack) {
        return stack.getItem() instanceof HelmetUpgradeItem;
    }

    @Override
    public String getUpgradeBehaviorKey(ItemStack upgradeStack) {
        if (upgradeStack.getItem() instanceof HelmetUpgradeItem hu)
            return hu.getBehavior().name();
        return "";
    }

    @Override
    protected int getArmorBonus(ItemStack stack) {
        int bonus = 0;
        for (ItemStack upg : getData(stack).upgrades().values()) {
            if (upg.getItem() instanceof HelmetUpgradeItem hu
                    && hu.getBehavior() == HelmetUpgradeBehavior.ARMOR) {
                bonus += hu.getBehavior().getBonusForMk(hu.getMk());
            }
        }
        return bonus;
    }
}
