package com.github.nalamodikk.common.item;

import com.github.nalamodikk.common.utils.upgrade.UpgradeType;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class UpgradeItem extends Item {
    private final UpgradeType type;

    public UpgradeItem(UpgradeType type, Properties props) {
        super(props);
        this.type = type;
    }

    public UpgradeType getUpgradeType() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        if (Screen.hasShiftDown()) {
            tooltipComponents.add(Component.translatable("tooltip.koniava.upgrade." + type.getSerializedName()));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.koniava.upgrade.hold_shift"));
        }
    }

}