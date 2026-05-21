package com.github.nalamodikk.common.item.wand.upgrade;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class WandUpgradeItem extends Item implements IWandUpgrade {

    private final WandUpgradeBehavior behavior;

    public WandUpgradeItem(WandUpgradeBehavior behavior, Properties properties) {
        super(properties);
        this.behavior = behavior;
    }

    public WandUpgradeBehavior getBehavior() { return behavior; }

    @Override
    public Component getUpgradeDisplayName() { return behavior.getDisplayName(); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.koniava.wand_upgrade.type", behavior.getDisplayName()));
    }
}
