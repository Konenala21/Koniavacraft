package com.github.nalamodikk.common.item.weapon.turret;

import com.github.nalamodikk.common.item.upgrade.IModUpgrade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class TurretUpgradeItem extends Item implements IModUpgrade {

    private final TurretUpgradeBehavior behavior;
    private final int mk;

    public TurretUpgradeItem(TurretUpgradeBehavior behavior, int mk, Properties properties) {
        super(properties);
        this.behavior = behavior;
        this.mk = mk;
    }

    public TurretUpgradeBehavior getBehavior() { return behavior; }
    public int getMk() { return mk; }

    @Override
    public Component getUpgradeDisplayName() { return behavior.getDisplayName(); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        if (mk > 0) lines.add(Component.translatable("tooltip.koniava.wand_upgrade.mk_level", mk));
        lines.add(Component.translatable("tooltip.koniava.wand_upgrade.type", behavior.getDisplayName()));
        lines.add(behavior.getEffectTooltip(mk));
        if (behavior.isControl()) {
            lines.add(Component.translatable("tooltip.koniava.turret_upgrade.boss_resistant")
                    .withStyle(ChatFormatting.DARK_RED));
        }
        if (behavior.isEntityOnly()) {
            lines.add(Component.translatable("tooltip.koniava.turret_upgrade.entity_only")
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("tooltip.koniava.upgrade.compatible",
                Component.translatable("item.koniava.floating_turret").withStyle(ChatFormatting.YELLOW)
        ).withStyle(ChatFormatting.GRAY));
    }
}
