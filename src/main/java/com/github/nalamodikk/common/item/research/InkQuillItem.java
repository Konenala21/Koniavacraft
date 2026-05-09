package com.github.nalamodikk.common.item.research;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Scribing tool consumed when completing a research puzzle in the Research Table.
 * Has durability — each completed research uses one charge.
 */
public class InkQuillItem extends Item {

    public InkQuillItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.koniava.ink_quill.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }

    /** Damage the quill by one use. Returns true if it broke. */
    public static boolean useCharge(ItemStack stack, Player player) {
        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        return stack.isEmpty();
    }
}
