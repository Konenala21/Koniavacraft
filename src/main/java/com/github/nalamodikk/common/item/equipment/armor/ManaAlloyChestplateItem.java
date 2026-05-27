package com.github.nalamodikk.common.item.equipment.armor;

import com.github.nalamodikk.common.item.equipment.ManaArmorItem;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

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
            if (upg.getItem() instanceof ArmorCapacityUpgradeItem acu) {
                max += acu.getBonus();
            }
        }
        armor.set(ModDataComponents.MAX_MANA, max);
        int stored = getMana(armor);
        if (stored > max) armor.set(ModDataComponents.MANA_STORED, max);
    }

    @Override
    public boolean isValidUpgradeItem(ItemStack stack) {
        return stack.getItem() instanceof ChestplateUpgradeItem
                || stack.getItem() instanceof ArmorCapacityUpgradeItem
                || stack.getItem() instanceof ArmorDefenseUpgradeItem;
    }

    @Override
    public String getUpgradeBehaviorKey(ItemStack upgradeStack) {
        if (upgradeStack.getItem() instanceof ChestplateUpgradeItem cu)
            return cu.getBehavior().name();
        if (upgradeStack.getItem() instanceof ArmorCapacityUpgradeItem)
            return "CAPACITY";
        if (upgradeStack.getItem() instanceof ArmorDefenseUpgradeItem)
            return "DEFENSE";
        return "";
    }

    /** 取得已安裝的護盾插件（無則 null）。 */
    @org.jetbrains.annotations.Nullable
    public static ChestplateUpgradeItem getShieldUpgrade(ItemStack chest) {
        for (ItemStack upg : getData(chest).upgrades().values()) {
            if (upg.getItem() instanceof ChestplateUpgradeItem cu && cu.getBehavior().isShield()) {
                return cu;
            }
        }
        return null;
    }

    public static int getShieldEnergy(ItemStack chest) {
        return chest.getOrDefault(ModDataComponents.SHIELD_ENERGY, 0);
    }

    public static void setShieldEnergy(ItemStack chest, int value) {
        chest.set(ModDataComponents.SHIELD_ENERGY, Math.max(0, value));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, lines, flag);
        ChestplateUpgradeItem shield = getShieldUpgrade(stack);
        if (shield == null) {
            lines.add(Component.translatable("tooltip.koniava.chestplate.mana_shield_none")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else if (shield.getBehavior().isShieldAbsorb()) {
            int cap = shield.getBehavior().getBonusForMk(shield.getMk());
            lines.add(Component.translatable("tooltip.koniava.chestplate.shield_absorb_status",
                    getShieldEnergy(stack), cap).withStyle(ChatFormatting.AQUA));
        } else {
            lines.add(Component.translatable("tooltip.koniava.chestplate.shield_reduction_status",
                    shield.getBehavior().getBonusForMk(shield.getMk())).withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    protected int getArmorBonus(ItemStack stack) {
        int bonus = 0;
        for (ItemStack upg : getData(stack).upgrades().values()) {
            if (upg.getItem() instanceof ArmorDefenseUpgradeItem adu) {
                bonus += adu.getBonus();
            }
        }
        return bonus;
    }
}
