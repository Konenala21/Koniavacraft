package com.github.nalamodikk.common.item.research;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Scribing tool consumed when completing a research puzzle in the Research Table.
 * Has durability — each placement in the puzzle uses 1-5 charges.
 * When fully depleted, the quill LOCKS instead of breaking.
 * Refill by combining with an Ink Sac in the crafting grid.
 */
public class InkQuillItem extends Item {

    public InkQuillItem(Properties properties) {
        super(properties);
    }

    // ── Static helpers ───────────────────────────────────────────────────────

    public static boolean isOutOfInk(ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage();
    }

    /**
     * Applies damage to the quill. When the quill would break, locks it instead.
     * @return true if the quill just became locked (caller should show message)
     */
    public static boolean applyDamage(ItemStack stack, int amount) {
        int next = stack.getDamageValue() + amount;
        if (next >= stack.getMaxDamage()) {
            stack.setDamageValue(stack.getMaxDamage());
            return true; // just locked
        }
        stack.setDamageValue(next);
        return false;
    }

    // ── Item overrides ────────────────────────────────────────────────────────

    @Override
    public boolean isDamageable(ItemStack stack) {
        return !isOutOfInk(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> lines, TooltipFlag flag) {
        if (isOutOfInk(stack)) {
            lines.add(Component.translatable("item.koniava.ink_quill.out_of_ink")
                    .withStyle(ChatFormatting.RED));
            lines.add(Component.translatable("item.koniava.ink_quill.refill_hint")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable("item.koniava.ink_quill.tooltip")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isOutOfInk(stack);
    }
}
