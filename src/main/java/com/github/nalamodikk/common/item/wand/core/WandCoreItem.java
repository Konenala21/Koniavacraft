package com.github.nalamodikk.common.item.wand.core;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

public class WandCoreItem extends Item implements IWandCore {

    private final WandCoreBehavior behavior;

    public WandCoreItem(WandCoreBehavior behavior, Properties properties) {
        super(properties);
        this.behavior = behavior;
    }

    public WandCoreBehavior getBehavior() {
        return behavior;
    }

    @Override
    public InteractionResult coreUseOn(UseOnContext ctx, ItemStack wandStack) {
        return behavior.useOn(ctx, wandStack);
    }

    @Override
    public Component getCoreDisplayName() {
        return behavior.getDisplayName();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.koniava.wand_core.type", behavior.getDisplayName()));
        lines.add(behavior.getDescription());
        Component compatible = Component.translatable("item.koniava.wand_rod")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(", ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("item.koniava.wand_rod_advanced")
                        .withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("tooltip.koniava.upgrade.compatible", compatible)
                .withStyle(ChatFormatting.GRAY));
    }
}
