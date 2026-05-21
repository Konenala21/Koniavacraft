package com.github.nalamodikk.common.item.wand;

import com.github.nalamodikk.common.item.wand.core.IWandCore;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

public class WandRodItem extends Item {

    public WandRodItem(Properties properties) {
        super(properties.component(ModDataComponents.WAND_CORE_DATA, WandCoreData.empty()));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        WandCoreData data = stack.getOrDefault(ModDataComponents.WAND_CORE_DATA, WandCoreData.empty());

        if (!data.hasCore()) {
            if (ctx.getPlayer() != null && !ctx.getLevel().isClientSide) {
                ctx.getPlayer().displayClientMessage(
                        Component.translatable("message.koniava.wand.no_core"), true);
            }
            return InteractionResult.FAIL;
        }

        IWandCore core = data.getCore();
        return core.coreUseOn(ctx, stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        WandCoreData data = stack.getOrDefault(ModDataComponents.WAND_CORE_DATA, WandCoreData.empty());
        if (data.hasCore()) {
            lines.add(Component.translatable("tooltip.koniava.wand.core",
                    data.getCore().getCoreDisplayName()));
        } else {
            lines.add(Component.translatable("tooltip.koniava.wand.no_core"));
        }
    }

    public static WandCoreData getData(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.WAND_CORE_DATA, WandCoreData.empty());
    }

    public static void setData(ItemStack stack, WandCoreData data) {
        stack.set(ModDataComponents.WAND_CORE_DATA, data);
    }
}
