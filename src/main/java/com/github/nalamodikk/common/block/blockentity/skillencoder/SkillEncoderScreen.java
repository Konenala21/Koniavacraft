package com.github.nalamodikk.common.block.blockentity.skillencoder;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.network.packet.server.skill.EncodeSkillPacket;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.client.ClientResearchCache;
import com.github.nalamodikk.research.skill.AspectRoles;
import com.github.nalamodikk.research.skill.SkillEncoding;
import com.github.nalamodikk.research.skill.SkillRole;
import com.github.nalamodikk.research.skill.StoredSkill;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Skill Core Encoding Bench screen. Mirrors the Aspect Synthesis bench's
 * aspect palette (discovered aspects as tinted hex cells, paginated) and adds
 * manual role slots: pick an aspect, click a carrier/effect/modifier cell to
 * place it (the role dictionary rejects illegal placements), then Encode writes
 * the recipe onto the spell core in the slot.
 *
 * Layout coordinates are matched to assets/.../gui/skill_encoder_gui.png: the upper
 * grey grid is the aspect palette, the lower grid is the player inventory, the
 * detached top-right cell is the core slot, and the role/target widgets sit in
 * the open area above the palette.
 */
public class SkillEncoderScreen extends AbstractContainerScreen<SkillEncoderMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/skill_encoder_gui.png");
    private static final ResourceLocation HEX_CELL =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/research/hex_cell.png");

    private static final int GUI_W = 233;   // panel spans x 0..232; scrap icon at 235+ stays hidden
    private static final int GUI_H = 256;
    private static final int TEX = 256;
    private static final int CELL = 18;

    private static final int CARRIER_X = 64, CARRIER_Y = 36;
    private static final int EFFECT_X = 64, EFFECT_Y = 58;
    private static final int MODIFIER_X = 64, MODIFIER_Y = 78;
    private static final int SLOT_ROW_X = 64, SLOT_ROW_Y = 98, SLOT_CELL = 16;
    private static final int PALETTE_X = 50, PALETTE_Y = 122;
    // core slot icon lives in the texture's scrap area (right of the visible GUI)
    private static final int CORE_ICON_U = 235, CORE_ICON_V = 8, CORE_ICON_SRC = 17;
    private static final int PALETTE_COLS = 9, PALETTE_ROWS = 2, PALETTE_STEP = 18;
    private static final int PALETTE_PAGE = PALETTE_COLS * PALETTE_ROWS;

    private final Aspect[] effects = new Aspect[3];
    private final Aspect[] modifiers = new Aspect[2];
    @Nullable private Aspect carrier;
    @Nullable private Aspect selected;
    private int targetSlot;
    private int palettePage;

    private EditBox nameField;
    private EditBox searchField;
    private ItemStack lastCore = ItemStack.EMPTY;

    public SkillEncoderScreen(SkillEncoderMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
    }

    @Override
    protected void init() {
        super.init();
        this.nameField = new EditBox(font, leftPos + 8, topPos + 16, 150, 14,
                Component.translatable("gui.koniava.skill_encoder.name"));
        this.nameField.setMaxLength(32);
        this.nameField.setHint(Component.translatable("gui.koniava.skill_encoder.name"));
        addRenderableWidget(this.nameField);

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.koniava.skill_encoder.encode"), b -> onEncode())
                .bounds(leftPos + 120, topPos + 40, 50, 20)
                .build());

        this.searchField = new EditBox(font, leftPos + 140, topPos + 78, 85, 14,
                Component.translatable("gui.koniava.skill_encoder.search"));
        this.searchField.setMaxLength(32);
        this.searchField.setHint(Component.translatable("gui.koniava.skill_encoder.search"));
        this.searchField.setResponder(s -> palettePage = 0);   // reset paging on new query
        addRenderableWidget(this.searchField);
    }

    /** When the core in the slot changes (placed/removed/just encoded), reflect it. */
    @Override
    protected void containerTick() {
        super.containerTick();
        ItemStack core = menu.getCore();
        if (!ItemStack.matches(core, lastCore)) {
            lastCore = core.copy();
            loadFromCore(targetSlot);
        }
    }

    /** Load the stored skill at {@code slot} into the editor, or clear it if empty. */
    private void loadFromCore(int slot) {
        clearEditor();
        ItemStack core = menu.getCore();
        if (core.isEmpty()) return;
        List<StoredSkill> skills = SkillEncoding.getSkills(core);
        if (slot < 0 || slot >= skills.size()) return;

        StoredSkill s = skills.get(slot);
        carrier = ModAspects.get(s.carrier());
        for (int i = 0; i < effects.length && i < s.effects().size(); i++)
            effects[i] = ModAspects.get(s.effects().get(i));
        for (int i = 0; i < modifiers.length && i < s.modifiers().size(); i++)
            modifiers[i] = ModAspects.get(s.modifiers().get(i));
        if (nameField != null) nameField.setValue(s.name());
    }

    private void clearEditor() {
        carrier = null;
        Arrays.fill(effects, null);
        Arrays.fill(modifiers, null);
        if (nameField != null) nameField.setValue("");
    }

    private void onEncode() {
        List<ResourceLocation> eff = new ArrayList<>();
        for (Aspect a : effects) if (a != null) eff.add(a.getId());
        List<ResourceLocation> mod = new ArrayList<>();
        for (Aspect a : modifiers) if (a != null) mod.add(a.getId());

        if (carrier == null || eff.isEmpty()) {
            status(Component.translatable("message.koniava.encode.incomplete"));
            return;
        }
        EncodeSkillPacket.send(nameField.getValue(), carrier.getId(), eff, mod, targetSlot);
    }

    private void status(Component message) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(message, true);
        }
    }

    // ── render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderPalette(g, mouseX, mouseY);
        // our palette is drawn after super.render, so re-draw the hovered item's
        // vanilla tooltip on top (unless an aspect tooltip takes precedence).
        if (!renderTooltips(g, mouseX, mouseY)) {
            this.renderTooltip(g, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        g.blit(BACKGROUND, leftPos, topPos, 0, 0, GUI_W, GUI_H, TEX, TEX);

        // core slot well: blit the scrap-area icon (17x17) scaled to an 18x18 cell.
        // Offset -1,-1 so the vanilla 16x16 slot item + hover highlight sit centred
        // inside the drawn 18x18 well (the slot's hover box starts at slot.x-1).
        g.blit(BACKGROUND, leftPos + SkillEncoderMenu.CORE_SLOT_X - 1, topPos + SkillEncoderMenu.CORE_SLOT_Y - 1,
                18, 18, (float) CORE_ICON_U, (float) CORE_ICON_V, CORE_ICON_SRC, CORE_ICON_SRC, TEX, TEX);

        g.drawString(font, Component.translatable("gui.koniava.skill_encoder.carrier"),
                leftPos + 10, topPos + CARRIER_Y + 5, 0x3A2F4A, false);
        g.drawString(font, Component.translatable("gui.koniava.skill_encoder.effects"),
                leftPos + 10, topPos + EFFECT_Y + 5, 0x3A2F4A, false);
        g.drawString(font, Component.translatable("gui.koniava.skill_encoder.modifiers"),
                leftPos + 10, topPos + MODIFIER_Y + 5, 0x3A2F4A, false);
        g.drawString(font, Component.translatable("gui.koniava.skill_encoder.slot"),
                leftPos + 10, topPos + SLOT_ROW_Y + 4, 0x3A2F4A, false);
        g.drawString(font, Component.translatable("gui.koniava.skill_encoder.search"),
                leftPos + 140, topPos + 67, 0x3A2F4A, false);

        renderRoleCells(g, mouseX, mouseY);
        renderTargetSlots(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // titles drawn in renderBg; suppress default offset labels
    }

    private void renderRoleCells(GuiGraphics g, int mouseX, int mouseY) {
        renderAspectCell(g, carrier, leftPos + CARRIER_X, topPos + CARRIER_Y, CELL, mouseX, mouseY, true, 0);
        for (int i = 0; i < effects.length; i++)
            renderAspectCell(g, effects[i], leftPos + EFFECT_X + i * CELL, topPos + EFFECT_Y, CELL, mouseX, mouseY, true, 0);
        for (int i = 0; i < modifiers.length; i++)
            renderAspectCell(g, modifiers[i], leftPos + MODIFIER_X + i * CELL, topPos + MODIFIER_Y, CELL, mouseX, mouseY, true, 0);
    }

    private void renderTargetSlots(GuiGraphics g, int mouseX, int mouseY) {
        int stored = menu.getCore().isEmpty() ? 0 : SkillEncoding.getSkills(menu.getCore()).size();
        for (int i = 0; i < SkillEncoding.MAX_SLOTS; i++) {
            int x = leftPos + SLOT_ROW_X + i * SLOT_CELL;
            int y = topPos + SLOT_ROW_Y;
            int border = i == targetSlot ? 0xFFEE7722 : 0xFF1B1B1B;
            g.fill(x, y, x + SLOT_CELL, y + SLOT_CELL, border);
            g.fill(x + 1, y + 1, x + SLOT_CELL - 1, y + SLOT_CELL - 1,
                    i < stored ? 0xFF5A4A2A : 0xFF2B2B2B);
            String n = Integer.toString(i + 1);
            g.drawString(font, n, x + (SLOT_CELL - font.width(n)) / 2, y + 4, 0xCFCFCF, false);
        }
    }

    private void renderPalette(GuiGraphics g, int mouseX, int mouseY) {
        List<Aspect> aspects = visiblePalette();
        for (int i = 0; i < aspects.size(); i++) {
            int x = leftPos + PALETTE_X + (i % PALETTE_COLS) * PALETTE_STEP;
            int y = topPos + PALETTE_Y + (i / PALETTE_COLS) * PALETTE_STEP;
            // palette sits over the texture's slot wells: don't draw our own well
            Aspect a = aspects.get(i);
            renderAspectCell(g, a, x, y, 16, mouseX, mouseY, false,
                    ClientResearchCache.getAspectCount(a.getId()));
        }
        int pages = pageCount();
        if (pages > 1) {
            g.drawCenteredString(font,
                    Component.translatable("gui.koniava.page_indicator", palettePage + 1, pages),
                    leftPos + PALETTE_X + (PALETTE_COLS * PALETTE_STEP) / 2,
                    topPos + PALETTE_Y + PALETTE_ROWS * PALETTE_STEP + 1, 0x666666);
        }
    }

    private void renderAspectCell(GuiGraphics g, @Nullable Aspect aspect, int x, int y, int size,
                                  int mouseX, int mouseY, boolean drawWell, int count) {
        boolean hovered = mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
        if (drawWell) {
            g.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF1B1B1B);
            g.fill(x, y, x + size, y + size, 0xFF2B2B2B);
        }
        if (aspect != null) {
            int color = aspect.getColor();
            RenderSystem.setShaderColor(ch(color, 16), ch(color, 8), ch(color, 0), 1.0F);
            g.blit(HEX_CELL, x, y, 0, 0, size, size, size, size);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            String label = aspect.getId().getPath().substring(0, 1).toUpperCase();
            g.drawString(font, label, x + size / 3, y + size / 3, 0xFFFFFF, true);
            if (count > 0) drawCount(g, x, y, size, count);
        }
        if (aspect != null && aspect == selected) {
            g.renderOutline(x - 1, y - 1, size + 2, size + 2, 0xFFEE7722);
        }
        if (hovered) g.fill(x, y, x + size, y + size, 0x44FFFFFF);
    }

    /** Owned-amount badge in the bottom-right corner, scaled down like the synthesis bench. */
    private void drawCount(GuiGraphics g, int x, int y, int size, int count) {
        String text = Integer.toString(count);
        float scale = 0.5F;
        int textW = Math.max(1, (int) Math.ceil(font.width(text) * scale));
        int textH = Math.max(1, (int) Math.ceil(8 * scale));
        int cx = x + size - textW - 1;
        int cy = y + size - textH - 1;
        g.fill(cx - 1, cy - 1, cx + textW + 1, cy + textH + 1, 0x66000000);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(font, text, 0, 0, 0xFFFFFF, false);
        g.pose().popPose();
    }

    private boolean renderTooltips(GuiGraphics g, int mouseX, int mouseY) {
        Aspect a = paletteAspectAt(mouseX, mouseY);
        if (a == null) a = roleAspectAt(mouseX, mouseY);
        if (a != null) {
            g.renderTooltip(font, a.getName(), mouseX, mouseY);
            return true;
        }
        return false;
    }

    // ── input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        Aspect clickedPalette = paletteAspectAt((int) mx, (int) my);
        if (clickedPalette != null) {
            selected = (selected == clickedPalette) ? null : clickedPalette;
            return true;
        }
        if (handleRoleClick((int) mx, (int) my)) return true;
        if (handleTargetSlotClick((int) mx, (int) my)) return true;
        return super.mouseClicked(mx, my, button);
    }

    private boolean handleRoleClick(int mx, int my) {
        if (inCell(mx, my, leftPos + CARRIER_X, topPos + CARRIER_Y, CELL)) {
            carrier = place(carrier, SkillRole.CARRIER);
            return true;
        }
        for (int i = 0; i < effects.length; i++) {
            if (inCell(mx, my, leftPos + EFFECT_X + i * CELL, topPos + EFFECT_Y, CELL)) {
                effects[i] = place(effects[i], SkillRole.EFFECT);
                return true;
            }
        }
        for (int i = 0; i < modifiers.length; i++) {
            if (inCell(mx, my, leftPos + MODIFIER_X + i * CELL, topPos + MODIFIER_Y, CELL)) {
                modifiers[i] = place(modifiers[i], SkillRole.MODIFIER);
                return true;
            }
        }
        return false;
    }

    /** Place the selected aspect into a role cell if legal; clear it if no selection. */
    @Nullable
    private Aspect place(@Nullable Aspect current, SkillRole role) {
        if (selected == null) return null; // clicking with nothing selected clears the cell
        if (!AspectRoles.hasRole(selected, role)) {
            status(Component.translatable("message.koniava.encode.wrong_role"));
            return current;
        }
        return selected;
    }

    private boolean handleTargetSlotClick(int mx, int my) {
        for (int i = 0; i < SkillEncoding.MAX_SLOTS; i++) {
            if (inCell(mx, my, leftPos + SLOT_ROW_X + i * SLOT_CELL, topPos + SLOT_ROW_Y, SLOT_CELL)) {
                targetSlot = i;
                loadFromCore(i);   // show what is already encoded in this slot
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (overPalette(mx, my) && pageCount() > 1) {
            if (sy < 0) palettePage = Math.min(pageCount() - 1, palettePage + 1);
            else if (sy > 0) palettePage = Math.max(0, palettePage - 1);
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    // ── palette data ────────────────────────────────────────────────────────

    private List<Aspect> usableDiscovered() {
        String query = searchField == null ? "" : searchField.getValue().trim().toLowerCase();
        List<Aspect> out = new ArrayList<>();
        for (Aspect a : ModAspects.all()) {
            if (!AspectRoles.isUsable(a)) continue;
            if (!(a.isPrimary() || ClientResearchCache.hasDiscovered(a.getId()))) continue;
            if (!query.isEmpty() && !matches(a, query)) continue;
            out.add(a);
        }
        return out;
    }

    /** Matches the search query against the aspect's display name and id path. */
    private static boolean matches(Aspect a, String query) {
        if (a.getId().getPath().toLowerCase().contains(query)) return true;
        return a.getName().getString().toLowerCase().contains(query);
    }

    private List<Aspect> visiblePalette() {
        List<Aspect> all = usableDiscovered();
        int from = palettePage * PALETTE_PAGE;
        if (from >= all.size()) return List.of();
        return all.subList(from, Math.min(all.size(), from + PALETTE_PAGE));
    }

    private int pageCount() {
        return Math.max(1, (usableDiscovered().size() + PALETTE_PAGE - 1) / PALETTE_PAGE);
    }

    @Nullable
    private Aspect paletteAspectAt(int mx, int my) {
        List<Aspect> aspects = visiblePalette();
        for (int i = 0; i < aspects.size(); i++) {
            int x = leftPos + PALETTE_X + (i % PALETTE_COLS) * PALETTE_STEP;
            int y = topPos + PALETTE_Y + (i / PALETTE_COLS) * PALETTE_STEP;
            if (inCell(mx, my, x, y, 16)) return aspects.get(i);
        }
        return null;
    }

    @Nullable
    private Aspect roleAspectAt(int mx, int my) {
        if (inCell(mx, my, leftPos + CARRIER_X, topPos + CARRIER_Y, CELL)) return carrier;
        for (int i = 0; i < effects.length; i++)
            if (inCell(mx, my, leftPos + EFFECT_X + i * CELL, topPos + EFFECT_Y, CELL)) return effects[i];
        for (int i = 0; i < modifiers.length; i++)
            if (inCell(mx, my, leftPos + MODIFIER_X + i * CELL, topPos + MODIFIER_Y, CELL)) return modifiers[i];
        return null;
    }

    private boolean overPalette(double mx, double my) {
        return mx >= leftPos + PALETTE_X && mx < leftPos + PALETTE_X + PALETTE_COLS * PALETTE_STEP
                && my >= topPos + PALETTE_Y && my < topPos + PALETTE_Y + PALETTE_ROWS * PALETTE_STEP;
    }

    private static boolean inCell(int mx, int my, int x, int y, int size) {
        return mx >= x && mx < x + size && my >= y && my < y + size;
    }

    private static float ch(int color, int shift) {
        return ((color >> shift) & 0xFF) / 255.0F;
    }
}
