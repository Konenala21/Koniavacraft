package com.github.nalamodikk.research.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.network.AspectSynthesisPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AspectSynthesisScreen extends AbstractContainerScreen<AspectSynthesisMenu> {

    private static final ResourceLocation HEX_CELL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/research/hex_cell.png");
    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/research/aspect_synthesis.png");
    private static final int GUI_W = 177;
    private static final int GUI_H = 168;
    private static final int TEXTURE_W = 256;
    private static final int TEXTURE_H = 256;
    private static final int CELL_SIZE = 24;
    private static final int PALETTE_CELL_SIZE = 16;
    private static final int PALETTE_STEP = 18;
    private static final int PALETTE_PER_ROW = 9;
    private static final float GUI_TINT_R = 1.0F;
    private static final float GUI_TINT_G = 1.0F;
    private static final float GUI_TINT_B = 1.0F;
    private static final int INPUT_ONE_X = 27;
    private static final int INPUT_TWO_X = 84;
    private static final int RESULT_X = 141;
    private static final int INPUT_Y = 38;
    private static final int PALETTE_X = 8;
    private static final int PALETTE_Y = 86;

    private final Screen parent;
    private final BlockPos tablePos;
    private final List<Aspect> discoveredAspects;

    @Nullable
    private Aspect input1;
    @Nullable
    private Aspect input2;
    @Nullable
    private Aspect result;

    private int guiLeft;
    private int guiTop;

    public AspectSynthesisScreen(Screen parent, BlockPos tablePos, Inventory playerInventory) {
        super(new AspectSynthesisMenu(0), playerInventory, Component.translatable("gui.koniava.research_table.synthesis"));
        this.parent = parent;
        this.tablePos = tablePos;
        this.discoveredAspects = collectDiscoveredAspects();
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (width - GUI_W) / 2;
        this.guiTop = (height - GUI_H) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderPanel(graphics);
        renderInputs(graphics, mouseX, mouseY);
        renderPalette(graphics, mouseX, mouseY);
        renderTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handlePaletteClick(mouseX, mouseY, button)) {
            return true;
        }
        if (clearInputIfClicked(mouseX, mouseY, guiLeft + INPUT_ONE_X, guiTop + INPUT_Y, true)) {
            return true;
        }
        if (clearInputIfClicked(mouseX, mouseY, guiLeft + INPUT_TWO_X, guiTop + INPUT_Y, false)) {
            return true;
        }
        if (isInside(mouseX, mouseY, guiLeft + RESULT_X, guiTop + INPUT_Y, CELL_SIZE, CELL_SIZE)) {
            tryCombine();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    private static List<Aspect> collectDiscoveredAspects() {
        List<Aspect> aspects = new ArrayList<>();
        for (Aspect aspect : ModAspects.all()) {
            if (aspect.isPrimary() || ClientResearchCache.hasDiscovered(aspect.getId())) {
                aspects.add(aspect);
            }
        }
        return aspects;
    }

    private void tryCombine() {
        if (input1 == null || input2 == null) {
            showStatusMessage(Component.translatable("message.koniava.synthesis.need_inputs"));
            return;
        }
        if (result == null) {
            showStatusMessage(Component.translatable("message.koniava.synthesis.no_recipe"));
            return;
        }

        AspectSynthesisPacket.sendToServer(tablePos, input1.getId(), input2.getId());
    }

    private void showStatusMessage(Component message) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(message, true);
        }
    }

    private void renderPanel(GuiGraphics graphics) {
        RenderSystem.setShaderColor(GUI_TINT_R, GUI_TINT_G, GUI_TINT_B, 1.0F);
        graphics.blit(BACKGROUND_TEXTURE, guiLeft, guiTop, 0, 0, GUI_W, GUI_H, TEXTURE_W, TEXTURE_H);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderInputs(GuiGraphics graphics, int mouseX, int mouseY) {
        renderAspectSlot(graphics, input1, guiLeft + INPUT_ONE_X, guiTop + INPUT_Y, mouseX, mouseY, CELL_SIZE);
        renderAspectSlot(graphics, input2, guiLeft + INPUT_TWO_X, guiTop + INPUT_Y, mouseX, mouseY, CELL_SIZE);
        renderAspectSlot(graphics, result, guiLeft + RESULT_X, guiTop + INPUT_Y, mouseX, mouseY, CELL_SIZE);
    }

    private void renderAspectSlot(GuiGraphics graphics, @Nullable Aspect aspect, int x, int y,
                                  int mouseX, int mouseY, int size) {
        boolean hovered = mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
        if (aspect != null) {
            int color = aspect.getColor();
            RenderSystem.setShaderColor(channel(color, 16), channel(color, 8), channel(color, 0), 1.0F);
            graphics.blit(HEX_CELL_TEXTURE, x, y, 0, 0, size, size, size, size);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        if (hovered) {
            graphics.fill(x, y, x + size, y + size, 0x44FFFFFF);
        }
        if (aspect != null) {
            String label = aspect.getId().getPath().substring(0, 1).toUpperCase();
            graphics.drawString(font, label, x + size / 3, y + size / 3, 0xFFFFFF, true);
        }
    }

    private void renderPalette(GuiGraphics graphics, int mouseX, int mouseY) {
        int startX = guiLeft + PALETTE_X;
        int startY = guiTop + PALETTE_Y;

        for (int i = 0; i < discoveredAspects.size(); i++) {
            Aspect aspect = discoveredAspects.get(i);
            int x = startX + (i % PALETTE_PER_ROW) * PALETTE_STEP;
            int y = startY + (i / PALETTE_PER_ROW) * PALETTE_STEP;
            renderAspectSlot(graphics, aspect, x, y, mouseX, mouseY, PALETTE_CELL_SIZE);
        }
    }

    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        Aspect hovered = getPaletteAspectAt(mouseX, mouseY);
        if (hovered != null) {
            graphics.renderTooltip(font, hovered.getName(), mouseX, mouseY);
        }
    }

    private boolean handlePaletteClick(double mouseX, double mouseY, int button) {
        Aspect clicked = getPaletteAspectAt((int) mouseX, (int) mouseY);
        if (clicked == null) {
            return false;
        }

        if (button == 0) {
            input1 = clicked;
        } else if (button == 1) {
            input2 = clicked;
        } else {
            return false;
        }
        updateResult();
        return true;
    }

    private boolean clearInputIfClicked(double mouseX, double mouseY, int x, int y, boolean firstInput) {
        if (mouseX < x || mouseX >= x + CELL_SIZE || mouseY < y || mouseY >= y + CELL_SIZE) {
            return false;
        }

        if (firstInput) {
            input1 = null;
        } else {
            input2 = null;
        }
        updateResult();
        return true;
    }

    @Nullable
    private Aspect getPaletteAspectAt(int mouseX, int mouseY) {
        int startX = guiLeft + PALETTE_X;
        int startY = guiTop + PALETTE_Y;

        for (int i = 0; i < discoveredAspects.size(); i++) {
            int x = startX + (i % PALETTE_PER_ROW) * PALETTE_STEP;
            int y = startY + (i / PALETTE_PER_ROW) * PALETTE_STEP;
            if (mouseX >= x && mouseX < x + PALETTE_CELL_SIZE
                    && mouseY >= y && mouseY < y + PALETTE_CELL_SIZE) {
                return discoveredAspects.get(i);
            }
        }
        return null;
    }

    private void updateResult() {
        result = null;
        if (input1 == null || input2 == null) {
            return;
        }

        for (Aspect aspect : ModAspects.all()) {
            List<Aspect> components = aspect.getComponents();
            if (!aspect.isPrimary() && components.size() >= 2
                    && ((components.get(0).equals(input1) && components.get(1).equals(input2))
                    || (components.get(0).equals(input2) && components.get(1).equals(input1)))) {
                result = aspect;
                return;
            }
        }
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static float channel(int color, int shift) {
        return ((color >> shift) & 0xFF) / 255.0F;
    }
}
