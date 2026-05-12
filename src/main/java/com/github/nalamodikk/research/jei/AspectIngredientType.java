package com.github.nalamodikk.research.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import mezz.jei.api.ingredients.IIngredientType;
import net.minecraft.resources.ResourceLocation;

public final class AspectIngredientType implements IIngredientType<Aspect> {

    public static final AspectIngredientType INSTANCE = new AspectIngredientType();
    public static final String UID = KoniavacraftMod.MOD_ID + ":aspect";
    public static final Codec<Aspect> CODEC = ResourceLocation.CODEC.flatXmap(
            id -> {
                Aspect aspect = ModAspects.get(id);
                return aspect != null
                        ? DataResult.success(aspect)
                        : DataResult.error(() -> "Unknown aspect id: " + id);
            },
            aspect -> DataResult.success(aspect.getId())
    );

    private AspectIngredientType() {
    }

    @Override
    public Class<? extends Aspect> getIngredientClass() {
        return Aspect.class;
    }

    @Override
    public String getUid() {
        return UID;
    }
}
