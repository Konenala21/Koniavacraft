package com.github.nalamodikk.common.item.wand.core;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Implemented by Item classes that act as wand cores.
 * The WandRodItem delegates useOn() to whichever core is installed.
 */
public interface IWandCore {

    InteractionResult coreUseOn(UseOnContext ctx, ItemStack wandStack);

    Component getCoreDisplayName();
}
