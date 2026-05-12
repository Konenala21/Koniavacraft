package com.github.nalamodikk.research.jei.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.utils.gui.GuiRenderUtils;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.client.ClientResearchCache;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AspectIngredientRenderer implements IIngredientRenderer<Aspect> {

    private static final int SIZE = 16;
    private static final int TEXTURE_SIZE = 32;
    private static final ResourceLocation HEX_CELL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/research/hex_cell.png");
    private static final ResourceLocation UNKNOWN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/research/unknow_aspect.png");

    @Override
    public void render(GuiGraphics graphics, Aspect aspect) {
        if (!aspect.isPrimary() && !ClientResearchCache.hasDiscovered(aspect.getId())) {
            renderScaledTexture(graphics, UNKNOWN_TEXTURE);
            return;
        }

        int color = aspect.getColor();
        renderScaledTexture(graphics, HEX_CELL_TEXTURE, channel(color, 16), channel(color, 8), channel(color, 0), 1.0f);
    }

    @Override
    public @NotNull List<Component> getTooltip(Aspect aspect, TooltipFlag tooltipFlag) {
        return List.of(displayName(aspect));
    }

    @Override
    public int getWidth() {
        return SIZE;
    }

    @Override
    public int getHeight() {
        return SIZE;
    }

    private static Component displayName(Aspect aspect) {
        if (!aspect.isPrimary() && !ClientResearchCache.hasDiscovered(aspect.getId())) {
            return Component.translatable("jei.koniava.aspect_synthesis.unknown_aspect");
        }
        return aspect.getName();
    }

    private static void renderScaledTexture(GuiGraphics graphics, ResourceLocation texture) {
        renderScaledTexture(graphics, texture, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderScaledTexture(GuiGraphics graphics, ResourceLocation texture,
                                            float red, float green, float blue, float alpha) {
        graphics.pose().pushPose();
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        if (texture.equals(HEX_CELL_TEXTURE)) {
            GuiRenderUtils renderUtils = new GuiRenderUtils(graphics.pose());
            renderUtils.blitWithColor(
                    texture,
                    0, 0,
                    TEXTURE_SIZE, TEXTURE_SIZE,
                    0f, 0f, 1f, 1f,
                    red, green, blue, alpha
            );
        } else {
            graphics.blit(texture, 0, 0, 0, 0, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
        }
        graphics.pose().popPose();
    }

    private static float channel(int color, int shift) {
        return ((color >> shift) & 0xFF) / 255.0F;
    }
}
