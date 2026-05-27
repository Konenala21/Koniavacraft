package com.github.nalamodikk.common.item.equipment.armor;

import com.github.nalamodikk.common.item.upgrade.IModUpgrade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ArmorCapacityUpgradeItem extends Item implements IModUpgrade {

    public static final int[] BONUS_PER_MK = {1000, 2000, 3500, 5500};

    private final int mk;

    public ArmorCapacityUpgradeItem(int mk, Properties properties) {
        super(properties);
        this.mk = mk;
    }

    public int getMk() { return mk; }

    public int getBonus() {
        return BONUS_PER_MK[Mth.clamp(mk, 0, BONUS_PER_MK.length - 1)];
    }

    @Override
    public Component getUpgradeDisplayName() {
        return Component.translatable("item.koniava.armor_upgrade_capacity");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        if (mk > 0) lines.add(Component.translatable("tooltip.koniava.wand_upgrade.mk_level", mk));
        lines.add(Component.translatable("tooltip.koniava.wand_upgrade.type",
                Component.translatable("item.koniava.armor_upgrade_capacity")));
        lines.add(Component.translatable("tooltip.koniava.armor_upgrade.effect.capacity", getBonus()));
        lines.add(Component.translatable("tooltip.koniava.upgrade.compatible_all_armor")
                .withStyle(ChatFormatting.GRAY));
    }
}
