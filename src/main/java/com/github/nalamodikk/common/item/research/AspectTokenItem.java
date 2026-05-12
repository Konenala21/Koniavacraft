package com.github.nalamodikk.common.item.research;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.register.ModDataComponents;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class AspectTokenItem extends Item {

    public AspectTokenItem(Properties properties) {
        super(properties);
    }

    public static ItemStack forAspect(Aspect aspect) {
        return forAspect(aspect, false);
    }

    public static ItemStack forAspect(Aspect aspect, boolean hidden) {
        ItemStack stack = new ItemStack(ModItems.ASPECT_TOKEN.get());
        stack.set(ModDataComponents.ASPECT_ID, aspect.getId());
        stack.set(ModDataComponents.ASPECT_HIDDEN, hidden);
        return stack;
    }

    public static ResourceLocation getAspectId(ItemStack stack) {
        return stack.get(ModDataComponents.ASPECT_ID);
    }

    public static boolean isHidden(ItemStack stack) {
        Boolean hidden = stack.get(ModDataComponents.ASPECT_HIDDEN);
        return hidden != null && hidden;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (isHidden(stack)) {
            return Component.translatable("jei.koniava.aspect_synthesis.unknown_aspect");
        }
        ResourceLocation id = getAspectId(stack);
        if (id != null) {
            Aspect aspect = ModAspects.get(id);
            if (aspect != null) {
                return aspect.getName();
            }
            return Component.literal(id.toString());
        }
        return Component.translatable("item.koniava.aspect_token");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        if (isHidden(stack)) {
            lines.add(Component.translatable("jei.koniava.aspect_synthesis.unknown_aspect"));
            return;
        }
        ResourceLocation id = getAspectId(stack);
        if (id != null) {
            Aspect aspect = ModAspects.get(id);
            if (aspect != null) {
                lines.add(aspect.getName());
                lines.add(Component.literal(id.toString()));
            }
        }
    }
}
