package com.github.nalamodikk.common.item.equipment.armor;

import com.github.nalamodikk.client.renderer.armor.ManaAlloyArmorClientExtension;
import com.github.nalamodikk.common.event.HelmetNightVisionHandler;
import com.github.nalamodikk.common.item.equipment.ManaArmorItem;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

public class ManaAlloyHelmetItem extends ManaArmorItem {

    public static final int BASE_MAX_MANA = 5000;
    public static final int BASE_ARMOR    = 2;
    public static final int MAX_UPGRADE_SLOTS = 3;

    public ManaAlloyHelmetItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, BASE_MAX_MANA, BASE_ARMOR, MAX_UPGRADE_SLOTS, properties);
    }

    /** 用自訂 bedrock 模型取代 vanilla 頭盔:回傳空模型隱藏 vanilla,實際模型由 ManaAlloyArmorLayer 畫。 */
    @Override
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ManaAlloyArmorClientExtension.INSTANCE);
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
        return stack.getItem() instanceof HelmetUpgradeItem
                || stack.getItem() instanceof ArmorCapacityUpgradeItem
                || stack.getItem() instanceof ArmorDefenseUpgradeItem;
    }

    @Override
    public String getUpgradeBehaviorKey(ItemStack upgradeStack) {
        if (upgradeStack.getItem() instanceof HelmetUpgradeItem hu)
            return hu.getBehavior().name();
        if (upgradeStack.getItem() instanceof ArmorCapacityUpgradeItem)
            return "CAPACITY";
        if (upgradeStack.getItem() instanceof ArmorDefenseUpgradeItem)
            return "DEFENSE";
        return "";
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, lines, flag);
        if (HelmetNightVisionHandler.hasNightVisionUpgrade(stack)) {
            boolean active = Boolean.TRUE.equals(stack.get(ModDataComponents.NIGHT_VISION_ACTIVE));
            if (active) {
                lines.add(Component.translatable("tooltip.koniava.helmet.night_vision.on").withStyle(ChatFormatting.AQUA));
            } else {
                lines.add(Component.translatable("tooltip.koniava.helmet.night_vision.off").withStyle(ChatFormatting.GRAY));
            }
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
