package com.github.nalamodikk.common.item;

import com.github.nalamodikk.common.utils.upgrade.UpgradeType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class UpgradeItem extends Item {
    private final UpgradeType type;
    private final int mk;

    public UpgradeItem(UpgradeType type, Properties props) {
        this(type, 0, props);
    }

    public UpgradeItem(UpgradeType type, int mk, Properties props) {
        super(props);
        this.type = type;
        this.mk = mk;
    }

    public UpgradeType getUpgradeType() { return type; }
    public int getMk() { return mk; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (mk > 0) {
            tooltipComponents.add(Component.translatable("tooltip.koniava.upgrade.mk_level", mk)
                    .withStyle(ChatFormatting.GRAY));
        }
        if (type == com.github.nalamodikk.common.utils.upgrade.UpgradeType.MANA_OUTPUT) {
            tooltipComponents.add(Component.translatable(
                    "tooltip.koniava.mana_output_upgrade.effect.mk" + mk)
                    .withStyle(ChatFormatting.GREEN));
        }
        if (type == com.github.nalamodikk.common.utils.upgrade.UpgradeType.ENERGY_OUTPUT) {
            tooltipComponents.add(Component.translatable(
                    "tooltip.koniava.energy_output_upgrade.effect.mk" + mk)
                    .withStyle(ChatFormatting.YELLOW));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
