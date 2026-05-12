package com.github.nalamodikk.common.block.blockentity.research;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.client.AspectSynthesisScreen;
import com.github.nalamodikk.research.client.ClientResearchCache;
import com.github.nalamodikk.research.client.ResearchScreen;
import com.github.nalamodikk.research.template.ResearchRegistry;
import com.github.nalamodikk.research.template.ResearchTemplate;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            KoniavacraftMod.MOD_ID, "textures/gui/research_table_gui.png");
    private static final float GUI_TINT_R = 1.0F;
    private static final float GUI_TINT_G = 1.0F;
    private static final float GUI_TINT_B = 1.0F;

    private Button beginButton;
    private Button synthesisButton;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 181;
    }

    @Override
    protected void init() {
        super.init();

        beginButton = Button.builder(
                        Component.translatable("gui.koniava.research_table.begin"),
                        button -> openResearchPuzzle())
                .bounds(leftPos + 54, topPos + 65, 68, 20)
                .build();
        this.addRenderableWidget(beginButton);

        synthesisButton = Button.builder(
                        Component.translatable("gui.koniava.research_table.synthesis_short"),
                        button -> openSynthesisUI())
                .bounds(leftPos + 78, topPos + 30, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.koniava.research_table.synthesis")))
                .build();
        this.addRenderableWidget(synthesisButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        beginButton.active = menu.isReadyToResearch();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(GUI_TINT_R, GUI_TINT_G, GUI_TINT_B, 1.0F);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderCurrentResearchName(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, this.title, 9, 4, 0xFFFFFF, false);
        graphics.drawString(font, this.playerInventoryTitle, 9, imageHeight - 94, 0x404040, false);
    }

    private void renderCurrentResearchName(GuiGraphics graphics) {
        ResourceLocation researchId = menu.getBlockEntity().getCurrentResearchId();
        if (researchId == null) {
            return;
        }

        ResearchRegistry.get(researchId).ifPresent(template -> {
            Component name = Component.translatable(template.getTitleKey()).withStyle(ChatFormatting.AQUA);
            graphics.drawCenteredString(font, name, leftPos + imageWidth / 2, topPos + 57, 0xFFFFFF);
        });
    }

    private void openSynthesisUI() {
        minecraft.setScreen(new AspectSynthesisScreen(menu, minecraft.player.getInventory(), this));
    }

    private void openResearchPuzzle() {
        ResourceLocation researchId = menu.getBlockEntity().getCurrentResearchId();
        if (researchId == null) {
            return;
        }

        ResearchRegistry.get(researchId).ifPresent(template -> {
            List<Aspect> palette = buildPalette(template);
            this.onClose();
            minecraft.setScreen(new ResearchScreen(template, palette, menu.getBlockEntity().getBlockPos()));
        });
    }

    private static List<Aspect> buildPalette(ResearchTemplate template) {
        Set<Aspect> seen = new LinkedHashSet<>();
        seen.addAll(template.getRequiredAspects());
        for (Aspect aspect : ModAspects.all()) {
            if (aspect.isPrimary() || ClientResearchCache.hasDiscovered(aspect.getId())) {
                seen.add(aspect);
            }
        }
        return List.copyOf(seen);
    }
}
