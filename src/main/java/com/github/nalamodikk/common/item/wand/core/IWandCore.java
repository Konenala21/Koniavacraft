package com.github.nalamodikk.common.item.wand.core;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Implemented by Item classes that act as wand cores.
 * The WandRodItem delegates useOn() (block) and use() (air) to whichever core is installed.
 */
public interface IWandCore {

    /** Right-click on a block. */
    InteractionResult coreUseOn(UseOnContext ctx, ItemStack wandStack);

    /**
     * Right-click in air. Default: do nothing (most cores only act on blocks).
     * The spell core overrides this to cast a skill.
     */
    default InteractionResultHolder<ItemStack> coreUse(Level level, Player player, InteractionHand hand, ItemStack wandStack) {
        return InteractionResultHolder.pass(wandStack);
    }

    Component getCoreDisplayName();
}
