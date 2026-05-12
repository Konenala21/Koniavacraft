package com.github.nalamodikk.research.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.common.block.blockentity.research.ResearchTableMenu;
import com.github.nalamodikk.research.network.AspectSynthesisPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class AspectSynthesisScreen extends AbstractContainerScreen<ResearchTableMenu> {

    private static final ResourceLocation HEX_CELL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/research/hex_cell.png");
    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/research/aspect_synthesis.png");
    private static final int GUI_W = 177;
    private static final int GUI_H = 256;
    private static final int TEXTURE_W = 256;
    private static final int TEXTURE_H = 256;
    private static final int CELL_SIZE = 24;
    private static final int PALETTE_CELL_SIZE = 16;
    private static final int PALETTE_STEP = 18;
    private static final int PALETTE_PER_ROW = 9;
    private static final int COUNT_TEXT_COLOR = 0xFFFFFF;
    private static final int COUNT_BG_COLOR = 0x66000000;
    private static final float COUNT_SCALE = 0.5F;
    private static final float GUI_TINT_R = 1.0F;
    private static final float GUI_TINT_G = 1.0F;
    private static final float GUI_TINT_B = 1.0F;
    private static final int INPUT_ONE_X = 24;
    private static final int INPUT_TWO_X = 80;
    private static final int RESULT_X = 137;
    private static final int INPUT_Y = 34;
    private static final int QUILL_FRAME_X = 177;
    private static final int QUILL_FRAME_Y = 36;
    private static final int QUILL_SLOT_X = 177;
    private static final int QUILL_SLOT_Y = 36;
    private static final int QUILL_SLOT_SIZE = 18;
    private static final int PALETTE_X = 8;
    private static final int PALETTE_Y = 85;
    private static final int PLAYER_TITLE_X = 4;
    private static final int PLAYER_TITLE_Y = 161;
    private static final int PLAYER_MAIN_START_X = 9;
    private static final int PLAYER_MAIN_START_Y = 172;
    private static final int PLAYER_HOTBAR_START_Y = 230;
    private static final Field SLOT_X_FIELD;
    private static final Field SLOT_Y_FIELD;

    static {
        try {
            SLOT_X_FIELD = Slot.class.getDeclaredField("x");
            SLOT_Y_FIELD = Slot.class.getDeclaredField("y");
            SLOT_X_FIELD.setAccessible(true);
            SLOT_Y_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Screen parent;
    @Nullable
    private Aspect input1;
    @Nullable
    private Aspect input2;
    @Nullable
    private Aspect result;

    private int guiLeft;
    private int guiTop;
    private boolean synthesisLayoutApplied;

    public AspectSynthesisScreen(ResearchTableMenu menu, Inventory playerInventory, Screen parent) {
        super(menu, playerInventory, Component.translatable("gui.koniava.research_table.synthesis"));
        this.parent = parent;
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = leftPos;
        this.guiTop = topPos;
        applySynthesisLayout();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderPalette(graphics, mouseX, mouseY);
        renderTooltips(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(graphics);
        renderQuillSlotFrame(graphics);
        renderInputs(graphics, mouseX, mouseY);
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
        restoreResearchLayout();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, this.playerInventoryTitle, PLAYER_TITLE_X, PLAYER_TITLE_Y, 0x404040, false);
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

        AspectSynthesisPacket.sendToServer(menu.getBlockEntity().getBlockPos(), input1.getId(), input2.getId());
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

    private void renderQuillSlotFrame(GuiGraphics graphics) {
        int x = guiLeft + QUILL_FRAME_X;
        int y = guiTop + QUILL_FRAME_Y;
        int size = QUILL_SLOT_SIZE;

        graphics.fill(x - 2, y + 1, x - 1, y + size + 2, 0xAA000000);
        graphics.fill(x - 1, y + 2, x, y + size + 1, 0x77000000);
        graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF1B1B1B);
        graphics.fill(x, y, x + size, y + size, 0xFF6C6C6C);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF2B2B2B);
        graphics.fill(x - 1, y - 1, x + size + 1, y, 0xFF9A9A9A);
        graphics.fill(x - 1, y - 1, x, y + size + 1, 0xFF9A9A9A);
        graphics.fill(x + size, y, x + size + 1, y + size + 1, 0xFF5A5A5A);
        graphics.fill(x, y + size, x + size, y + size + 1, 0xFF5A5A5A);
    }

    private void applySynthesisLayout() {
        if (synthesisLayoutApplied || menu.slots.size() < 38) {
            return;
        }

        Slot quillSlot = menu.slots.get(1);
        setSlotPosition(quillSlot, QUILL_SLOT_X, QUILL_SLOT_Y);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                Slot slot = menu.slots.get(2 + row * 9 + col);
                setSlotPosition(slot, PLAYER_MAIN_START_X + col * 18, PLAYER_MAIN_START_Y + row * 18);
            }
        }

        for (int col = 0; col < 9; col++) {
            Slot slot = menu.slots.get(29 + col);
            setSlotPosition(slot, PLAYER_MAIN_START_X + col * 18, PLAYER_HOTBAR_START_Y);
        }

        synthesisLayoutApplied = true;
    }

    private void restoreResearchLayout() {
        if (!synthesisLayoutApplied || menu.slots.size() < 38) {
            return;
        }

        Slot quillSlot = menu.slots.get(1);
        setSlotPosition(quillSlot, 116, 33);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                Slot slot = menu.slots.get(2 + row * 9 + col);
                setSlotPosition(slot, 9 + col * 18, 99 + row * 18);
            }
        }

        for (int col = 0; col < 9; col++) {
            Slot slot = menu.slots.get(29 + col);
            setSlotPosition(slot, 9 + col * 18, 157);
        }

        synthesisLayoutApplied = false;
    }

    private static void setSlotPosition(Slot slot, int x, int y) {
        try {
            SLOT_X_FIELD.setInt(slot, x);
            SLOT_Y_FIELD.setInt(slot, y);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to reposition slot", e);
        }
    }

    private void renderInputs(GuiGraphics graphics, int mouseX, int mouseY) {
        renderAspectSlot(graphics, input1, guiLeft + INPUT_ONE_X, guiTop + INPUT_Y, mouseX, mouseY, CELL_SIZE, 0);
        renderAspectSlot(graphics, input2, guiLeft + INPUT_TWO_X, guiTop + INPUT_Y, mouseX, mouseY, CELL_SIZE, 0);
        renderAspectSlot(graphics, result, guiLeft + RESULT_X, guiTop + INPUT_Y, mouseX, mouseY, CELL_SIZE, 0);
    }

    private void renderAspectSlot(GuiGraphics graphics, @Nullable Aspect aspect, int x, int y,
                                  int mouseX, int mouseY, int size, int count) {
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
            if (count > 0) {
                String countText = Integer.toString(count);
                int textWidth = Math.max(1, (int) Math.ceil(font.width(countText) * COUNT_SCALE));
                int textHeight = Math.max(1, (int) Math.ceil(8 * COUNT_SCALE));
                int countX = x + size - textWidth - 1;
                int countY = y + size - textHeight - 1;
                graphics.fill(countX - 1, countY - 1, countX + textWidth + 1, countY + textHeight + 1, COUNT_BG_COLOR);
                graphics.pose().pushPose();
                graphics.pose().translate(countX, countY, 0.0F);
                graphics.pose().scale(COUNT_SCALE, COUNT_SCALE, 1.0F);
                graphics.drawString(font, countText, 0, 0, COUNT_TEXT_COLOR, false);
                graphics.pose().popPose();
            }
        }
    }

    private void renderPalette(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Aspect> discoveredAspects = collectDiscoveredAspects();
        int startX = guiLeft + PALETTE_X;
        int startY = guiTop + PALETTE_Y;

        for (int i = 0; i < discoveredAspects.size(); i++) {
            Aspect aspect = discoveredAspects.get(i);
            int x = startX + (i % PALETTE_PER_ROW) * PALETTE_STEP;
            int y = startY + (i / PALETTE_PER_ROW) * PALETTE_STEP;
            renderAspectSlot(graphics, aspect, x, y, mouseX, mouseY, PALETTE_CELL_SIZE,
                    ClientResearchCache.getAspectCount(aspect.getId()));
        }
    }

    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        Aspect hoveredSynthesis = getSynthesisAspectAt(mouseX, mouseY);
        if (hoveredSynthesis != null) {
            graphics.renderTooltip(font, hoveredSynthesis.getName(), mouseX, mouseY);
            return;
        }

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
        List<Aspect> discoveredAspects = collectDiscoveredAspects();
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

    @Nullable
    private Aspect getSynthesisAspectAt(int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, guiLeft + INPUT_ONE_X, guiTop + INPUT_Y, CELL_SIZE, CELL_SIZE)) {
            return input1;
        }
        if (isInside(mouseX, mouseY, guiLeft + INPUT_TWO_X, guiTop + INPUT_Y, CELL_SIZE, CELL_SIZE)) {
            return input2;
        }
        if (isInside(mouseX, mouseY, guiLeft + RESULT_X, guiTop + INPUT_Y, CELL_SIZE, CELL_SIZE)) {
            return result;
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
