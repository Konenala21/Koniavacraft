package com.github.nalamodikk.common.item.wand.upgrade;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class WandUpgradeItem extends Item implements IWandUpgrade {

    private final WandUpgradeBehavior behavior;
    private final int mk;

    public WandUpgradeItem(WandUpgradeBehavior behavior, int mk, Properties properties) {
        super(properties);
        this.behavior = behavior;
        this.mk = mk;
    }

    public WandUpgradeBehavior getBehavior() { return behavior; }
    public int getMk() { return mk; }

    @Override
    public Component getUpgradeDisplayName() { return behavior.getDisplayName(); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        if (mk > 0) {
            lines.add(Component.translatable("tooltip.koniava.wand_upgrade.mk_level", mk));
        }
        lines.add(Component.translatable("tooltip.koniava.wand_upgrade.type", behavior.getDisplayName()));
        lines.add(behavior.getEffectTooltip(mk));
    }
}
