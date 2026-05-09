package com.github.nalamodikk.common.item.research;

import com.github.nalamodikk.register.ModDataComponents;
import com.github.nalamodikk.research.template.ResearchRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A research note with a {@link ModDataComponents#RESEARCH_ID} component
 * identifying which research it represents.
 *
 * Obtained from the Nara Holographic Watch by clicking a research entry.
 * Must be combined with an {@link InkQuillItem} in the Research Table to start the puzzle.
 */
public class ResearchNoteItem extends Item {

    public ResearchNoteItem(Properties properties) {
        super(properties);
    }

    /** Create a stack pre-filled with the given research ID. */
    public static ItemStack forResearch(ResourceLocation researchId) {
        ItemStack stack = new ItemStack(com.github.nalamodikk.register.ModItems.RESEARCH_NOTE.get());
        stack.set(ModDataComponents.RESEARCH_ID, researchId);
        return stack;
    }

    public static ResourceLocation getResearchId(ItemStack stack) {
        return stack.get(ModDataComponents.RESEARCH_ID);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> lines, TooltipFlag flag) {
        ResourceLocation id = getResearchId(stack);
        if (id == null) {
            lines.add(Component.translatable("item.koniava.research_note.blank")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            return;
        }
        ResearchRegistry.get(id).ifPresentOrElse(
                template -> lines.add(Component.translatable(template.getTitleKey())
                        .withStyle(net.minecraft.ChatFormatting.AQUA)),
                () -> lines.add(Component.literal(id.toString())
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY))
        );
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation id = getResearchId(stack);
        if (id != null) {
            return Component.translatable("item.koniava.research_note.named",
                    Component.translatable("research." + id.getNamespace() + "." + id.getPath()));
        }
        return super.getName(stack);
    }
}
