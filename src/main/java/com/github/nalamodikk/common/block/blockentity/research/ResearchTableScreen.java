package com.github.nalamodikk.common.block.blockentity.research;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.client.ResearchScreen;
import com.github.nalamodikk.research.template.ResearchRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Research Table GUI — shows the two item slots (note + quill) and a
 * "Begin Research" button that opens the puzzle screen when both are present.
 */
public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            KoniavacraftMod.MOD_ID, "textures/gui/research_table_gui.png");

    private Button beginButton;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = 176;
        this.imageHeight = 181;
    }

    @Override
    protected void init() {
        super.init();

        beginButton = Button.builder(
                        Component.translatable("gui.koniava.research_table.begin"),
                        btn -> openResearchPuzzle())
                .bounds(leftPos + 54, topPos + 65, 68, 20)
                .build();
        this.addRenderableWidget(beginButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        beginButton.active = menu.isReadyToResearch();
    }

    // ── Render ───────────────────────────────────────────────────────────────

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);

        // Show research name between slots and button if note is present
        ResourceLocation researchId = menu.getBlockEntity().getCurrentResearchId();
        if (researchId != null) {
            ResearchRegistry.get(researchId).ifPresent(template -> {
                var name = Component.translatable(template.getTitleKey())
                        .withStyle(net.minecraft.ChatFormatting.AQUA);
                g.drawCenteredString(font, name, leftPos + imageWidth / 2, topPos + 57, 0xFFFFFF);
            });
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, this.title, 9, 4, 0xFFFFFF, false);
        g.drawString(font, this.playerInventoryTitle, 9, imageHeight - 94, 0x404040, false);
    }

    private void openResearchPuzzle() {
        ResourceLocation researchId = menu.getBlockEntity().getCurrentResearchId();
        if (researchId == null) return;

        ResearchRegistry.get(researchId).ifPresent(template -> {
            // Build palette from the research's required aspects + their direct components.
            // This gives players exactly the aspects they need to bridge fixed nodes.
            var palette = buildPalette(template);
            this.onClose();
            minecraft.setScreen(new ResearchScreen(template, palette,
                    menu.getBlockEntity().getBlockPos()));
        });
    }

    /**
     * Palette = all registered aspects (primaries first, then compounds).
     * Required aspects appear first so they're easy to find; remaining aspects follow.
     */
    private static List<com.github.nalamodikk.research.aspect.Aspect> buildPalette(
            com.github.nalamodikk.research.template.ResearchTemplate template) {
        var seen = new java.util.LinkedHashSet<com.github.nalamodikk.research.aspect.Aspect>();
        seen.addAll(template.getRequiredAspects()); // required aspects first for visibility
        // Primary aspects always shown; compound aspects only if discovered via scanning
        ModAspects.all().stream()
                .filter(a -> a.isPrimary()
                        || com.github.nalamodikk.research.client.ClientResearchCache
                                .hasDiscovered(a.getId()))
                .forEach(seen::add);
        return List.copyOf(seen);
    }
}
