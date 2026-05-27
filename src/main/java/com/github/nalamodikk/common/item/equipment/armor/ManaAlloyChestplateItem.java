package com.github.nalamodikk.common.item.equipment.armor;

import com.github.nalamodikk.common.event.ChestplateManaShieldHandler;
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, lines, flag);
        int mana = getMana(stack);
        if (mana > 0) {
            lines.add(Component.translatable("tooltip.koniava.chestplate.mana_shield",
                    (int)(ChestplateManaShieldHandler.SHIELD_RATE * 100)).withStyle(ChatFormatting.AQUA));
        } else {
            lines.add(Component.translatable("tooltip.koniava.chestplate.mana_shield_empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
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
