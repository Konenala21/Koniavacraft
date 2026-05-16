package com.github.nalamodikk.research.jei;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.client.ClientResearchCache;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class AspectIngredientHelper implements IIngredientHelper<Aspect> {

    @Override
    public IIngredientType<Aspect> getIngredientType() {
        return AspectIngredientType.INSTANCE;
    }

    @Override
    public String getDisplayName(Aspect aspect) {
        return displayName(aspect).getString();
    }

    @Override
    public Object getUid(Aspect aspect, UidContext context) {
        return aspect.getId();
    }

    @Override
    @SuppressWarnings("removal")
    public String getUniqueId(Aspect aspect, UidContext context) {
        return aspect.getId().toString();
    }

    @Override
    public ResourceLocation getResourceLocation(Aspect aspect) {
        return aspect.getId();
    }

    @Override
    public Aspect copyIngredient(Aspect aspect) {
        return aspect;
    }

    @Override
    public String getErrorInfo(Aspect aspect) {
        return aspect.getId().toString();
    }

    private static Component displayName(Aspect aspect) {
        if (!aspect.isPrimary() && !ClientResearchCache.hasDiscovered(aspect.getId())) {
            return Component.translatable("jei.koniava.aspect_synthesis.unknown_aspect");
        }
        return aspect.getName();
    }
}
