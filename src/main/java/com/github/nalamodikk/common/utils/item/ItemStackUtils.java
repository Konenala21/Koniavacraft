package com.github.nalamodikk.common.utils.item;

import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ItemStackUtils {

    public static ItemStack findHeldWand(Player player) {
        for (ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
            if (!stack.isEmpty() && stack.getItem() instanceof BasicTechWandItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
