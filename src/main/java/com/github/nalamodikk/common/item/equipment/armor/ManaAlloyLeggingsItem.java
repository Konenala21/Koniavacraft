package com.github.nalamodikk.common.item.equipment.armor;

import com.github.nalamodikk.common.item.equipment.ManaArmorItem;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

public class ManaAlloyLeggingsItem extends ManaArmorItem {

    public static final int BASE_MAX_MANA = 7000;
    public static final int BASE_ARMOR    = 5;
    public static final int MAX_UPGRADE_SLOTS = 4;

    public ManaAlloyLeggingsItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.LEGGINGS, BASE_MAX_MANA, BASE_ARMOR, MAX_UPGRADE_SLOTS, properties);
    }

    @Override
    public void recalculateMaxMana(ItemStack armor) {
        int max = BASE_MAX_MANA;
        for (ItemStack upg : getData(armor).upgrades().values()) {
            if (upg.getItem() instanceof LeggingsUpgradeItem lu
                    && lu.getBehavior() == LeggingsUpgradeBehavior.CAPACITY) {
                max += lu.getBehavior().getBonusForMk(lu.getMk());
            }
        }
        armor.set(ModDataComponents.MAX_MANA, max);
        int stored = getMana(armor);
        if (stored > max) armor.set(ModDataComponents.MANA_STORED, max);
    }

    @Override
    public boolean isValidUpgradeItem(ItemStack stack) {
        return stack.getItem() instanceof LeggingsUpgradeItem;
    }

    @Override
    public String getUpgradeBehaviorKey(ItemStack upgradeStack) {
        if (upgradeStack.getItem() instanceof LeggingsUpgradeItem lu)
            return lu.getBehavior().name();
        return "";
    }

    @Override
    protected int getArmorBonus(ItemStack stack) {
        int bonus = 0;
        for (ItemStack upg : getData(stack).upgrades().values()) {
            if (upg.getItem() instanceof LeggingsUpgradeItem lu
                    && lu.getBehavior() == LeggingsUpgradeBehavior.ARMOR) {
                bonus += lu.getBehavior().getBonusForMk(lu.getMk());
            }
        }
        return bonus;
    }
}
