package com.github.nalamodikk.common.loot.jei;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.loot.BossLootEntry;
import com.github.nalamodikk.common.loot.BossLootRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * JEI plugin for the "Boss Drops" page. Reads {@link BossLootRegistry}, so it
 * stays in lockstep with the chest loot tables and grows automatically as
 * bosses are added. Right-clicking any boss drop (it is an OUTPUT) opens the
 * page; each boss icon is also registered as a catalyst.
 */
@JeiPlugin
public class BossLootJEIPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "boss_loot_jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new BossLootCategory(guiHelper));
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        registration.addRecipes(BossLootCategory.RECIPE_TYPE, List.copyOf(BossLootRegistry.all()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (BossLootEntry entry : BossLootRegistry.all()) {
            registration.addRecipeCatalyst(entry.icon(), BossLootCategory.RECIPE_TYPE);
        }
    }
}
