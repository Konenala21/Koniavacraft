package com.github.nalamodikk.research.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.research.ResearchTableBlockEntity;
import com.github.nalamodikk.common.item.research.ResearchNoteItem;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.grid.GridCell;
import com.github.nalamodikk.research.grid.HexCoord;
import com.github.nalamodikk.research.grid.ResearchGrid;
import com.github.nalamodikk.research.grid.ResearchGridGenerator;
import com.github.nalamodikk.research.client.ClientResearchCache;
import com.github.nalamodikk.research.network.ResearchAspectPlacePacket;
import com.github.nalamodikk.research.network.ResearchCompletePacket;
import com.github.nalamodikk.research.template.ResearchTemplate;
import net.minecraft.core.BlockPos;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Research minigame screen — 12×12 hexagonal grid.
 *
 * Interaction:
 *  - Click palette to select an aspect.
 *  - Left-click or left-drag on EMPTY cells → place selected aspect.
 *  - Right-click or right-drag on PLACED cells → remove aspect.
 *  - Puzzle complete when all FIXED nodes are connected via canConnectTo chain.
 */
public class ResearchScreen extends Screen {

    private static final ResourceLocation HEX_CELL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/research/hex_cell.png");

    private static final int CELL_W        = 18;
    private static final int CELL_H        = 18;
    private static final int COL_SPACING   = 14;
    private static final int ROW_SPACING   = 16;
    private static final int PALETTE_CELL  = 20;
    private static final int PALETTE_GAP   = 12;
    private static final int PALETTE_COLUMNS = 10;
    private static final int PALETTE_ROWS = 2;
    private static final int PALETTE_PAGE_SIZE = PALETTE_COLUMNS * PALETTE_ROWS;
    private static final int CELL_HIT_MARGIN = 2;
    private static final float COUNT_SCALE = 0.55F;
    private static final int COUNT_BG_COLOR = 0xAA000000;
    private static final int COUNT_TEXT_COLOR = 0xFFFFFF;

    private final ResearchTemplate template;
    private final BlockPos tablePos;
    private ResearchGrid grid;
    private final List<Aspect> palette;

    @Nullable private Aspect selectedAspect = null;
    private boolean completed = false;

    // Drag state
    private boolean isDragging  = false;
    private int     dragButton  = -1;
    private final Set<HexCoord> draggedCells = new HashSet<>();
    private int palettePage = 0;

    // Layout
    private int gridStartX, gridStartY;
    private int paletteStartX, paletteY;

    public ResearchScreen(ResearchTemplate template, List<Aspect> discoveredAspects, BlockPos tablePos) {
        super(Component.translatable(template.getTitleKey()));
        this.template = template;
        this.tablePos = tablePos;
        this.palette  = List.copyOf(discoveredAspects);
        // Deterministic seed: same research → same grid layout every time
        this.grid     = ResearchGridGenerator.generate(
                template.getRequiredAspects(), template.getHoleRatio(),
                new Random(template.getId().hashCode()));
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        int gridPixelW = (ResearchGrid.COLS - 1) * COL_SPACING + CELL_W;
        int gridPixelH = (ResearchGrid.ROWS - 1) * ROW_SPACING + ROW_SPACING / 2 + CELL_H;
        int paletteW   = PALETTE_COLUMNS * PALETTE_CELL;

        gridStartX    = (width  - gridPixelW) / 2;
        gridStartY    = (height - gridPixelH) / 2 - PALETTE_GAP;
        paletteStartX = (width  - paletteW)   / 2;
        paletteY      = gridStartY + gridPixelH + PALETTE_GAP;
        palettePage   = 0;

        // Restore saved puzzle state from the block entity (already synced to client)
        assert minecraft != null;
        if (minecraft.level != null) {
            var be = minecraft.level.getBlockEntity(tablePos);
            if (be instanceof ResearchTableBlockEntity table
                    && template.getId().equals(table.getActiveResearchId())) {
                for (var entry : table.getSavedPlacements().entrySet()) {
                    String[] parts = entry.getKey().split(",");
                    if (parts.length != 2) continue;
                    int q, r;
                    try { q = Integer.parseInt(parts[0]); r = Integer.parseInt(parts[1]); }
                    catch (NumberFormatException ignored) { continue; }
                    ResourceLocation aspectRl = ResourceLocation.tryParse(entry.getValue());
                    if (aspectRl == null) continue;
                    Aspect aspect = ModAspects.get(aspectRl);
                    HexCoord coord = new HexCoord(q, r);
                    GridCell cell  = grid.getCell(coord);
                    // Guard: only restore into cells that are still EMPTY (layout is deterministic
                    // but old saves from before the fix might have stale positions)
                    if (aspect != null && cell != null && cell.isEmpty()) {
                        grid = grid.placeAspect(coord, aspect);
                    }
                }
            }
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Suppress vanilla blur shader
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.setColor(1f, 1f, 1f, 1f);
        g.fill(0, 0, width, height, 0x88000000);

        int panelX = gridStartX - 10;
        int panelY = gridStartY - 22;
        int panelW = (ResearchGrid.COLS - 1) * COL_SPACING + CELL_W + 20;
        int panelH = paletteY + PALETTE_CELL + 14 - panelY;
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xE00A0A1E);
        g.renderOutline(panelX, panelY, panelW, panelH, 0xFF3D1F6E);

        g.drawCenteredString(font, title, width / 2, gridStartY - 14, 0xFFFFFF);

        renderGrid(g, mouseX, mouseY);
        renderConnections(g);
        renderPaletteIcons(g, mouseX, mouseY);
        renderDragCursor(g, mouseX, mouseY);
        renderSideItemIcons(g, mouseX, mouseY);

        if (completed) {
            g.drawCenteredString(font,
                    Component.translatable("research.koniava.complete"),
                    width / 2, gridStartY + (paletteY - gridStartY) / 2, 0x55FF55);
        }

        super.render(g, mouseX, mouseY, partialTick);

        // All tooltips rendered LAST so they always appear above every icon
        renderAllTooltips(g, mouseX, mouseY);
    }

    /** Icons only — no tooltips (tooltips are rendered later via renderAllTooltips). */
    private void renderSideItemIcons(GuiGraphics g, int mouseX, int mouseY) {
        assert minecraft != null;
        if (minecraft.level == null) return;
        var be = minecraft.level.getBlockEntity(tablePos);
        if (!(be instanceof ResearchTableBlockEntity table)) return;

        var note  = table.getInventory().getStackInSlot(ResearchTableBlockEntity.NOTE_SLOT);
        var quill = table.getInventory().getStackInSlot(ResearchTableBlockEntity.QUILL_SLOT);

        int panelX = gridStartX - 10;
        int panelY = gridStartY - 22;
        int panelW = (ResearchGrid.COLS - 1) * COL_SPACING + CELL_W + 20;
        int slotY  = panelY + 4;

        if (!note.isEmpty())  renderItemSlot(g, note,  panelX - 26,        slotY);
        if (!quill.isEmpty()) renderItemSlot(g, quill, panelX + panelW + 8, slotY);
    }

    private void renderItemSlot(GuiGraphics g, net.minecraft.world.item.ItemStack stack, int x, int y) {
        // Slot background
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF444466);
        g.fill(x, y, x + 16, y + 16, 0xFF222233);
        // Item + decorations (count, durability bar)
        g.renderItem(stack, x, y);
        g.renderItemDecorations(font, stack, x, y);
    }

    /**
     * Single method that renders ALL tooltips after every icon has been drawn.
     * Priority: grid cell > palette > side items (only one tooltip shown at a time).
     */
    private void renderAllTooltips(GuiGraphics g, int mouseX, int mouseY) {
        // 1. Grid cell tooltip
        for (int col = 0; col < ResearchGrid.COLS; col++) {
            for (int row = 0; row < ResearchGrid.ROWS; row++) {
                int cx = cellX(col), cy = cellY(col, row);
                if (isInsideCellHitbox(mouseX, mouseY, cx, cy)) {
                    GridCell cell = grid.getCell(HexCoord.fromOffset(col, row));
                    if (cell != null && cell.hasAspect()) {
                        List<Component> lines = new ArrayList<>();
                        lines.add(cell.getAspect().getName().copy()
                                .withStyle(cell.isFixed()
                                        ? net.minecraft.ChatFormatting.AQUA
                                        : net.minecraft.ChatFormatting.WHITE));
                        if (cell.isFixed()) {
                            lines.add(Component.translatable("research.koniava.cell.fixed")
                                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                        }
                        g.renderComponentTooltip(font, lines, mouseX, mouseY);
                    }
                    return; // stop after checking cell (even if empty)
                }
            }
        }

        // 2. Palette tooltip
        List<Aspect> visibleAspects = getVisiblePaletteAspects();
        for (int i = 0; i < visibleAspects.size(); i++) {
            int px = paletteStartX + (i % PALETTE_COLUMNS) * PALETTE_CELL;
            int py = paletteY + (i / PALETTE_COLUMNS) * PALETTE_CELL;
            if (mouseX >= px && mouseX < px + PALETTE_CELL
                    && mouseY >= py && mouseY < py + PALETTE_CELL) {
                Aspect aspect = visibleAspects.get(i);
                List<Component> tip = new ArrayList<>();
                tip.add(aspect.getName().copy().withStyle(net.minecraft.ChatFormatting.AQUA));
                if (!aspect.isPrimary()) {
                    List<Aspect> comps = aspect.getComponents();
                    tip.add(Component.literal(
                            comps.get(0).getName().getString()
                            + " + "
                            + comps.get(1).getName().getString())
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                }
                g.renderComponentTooltip(font, tip, mouseX, mouseY);
                return;
            }
        }

        // 3. Side item tooltips
        assert minecraft != null;
        if (minecraft.level == null) return;
        var be = minecraft.level.getBlockEntity(tablePos);
        if (!(be instanceof ResearchTableBlockEntity table)) return;

        var note  = table.getInventory().getStackInSlot(ResearchTableBlockEntity.NOTE_SLOT);
        var quill = table.getInventory().getStackInSlot(ResearchTableBlockEntity.QUILL_SLOT);
        int panelX = gridStartX - 10;
        int panelY = gridStartY - 22;
        int panelW = (ResearchGrid.COLS - 1) * COL_SPACING + CELL_W + 20;
        int slotY  = panelY + 4;

        if (!note.isEmpty()) {
            int sx = panelX - 26;
            if (mouseX >= sx && mouseX < sx + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                g.renderTooltip(font, note.getHoverName(), mouseX, mouseY);
                return;
            }
        }
        if (!quill.isEmpty()) {
            int sx = panelX + panelW + 8;
            if (mouseX >= sx && mouseX < sx + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                g.renderComponentTooltip(font,
                        java.util.List.of(
                                quill.getHoverName(),
                                Component.literal(
                                        (quill.getMaxDamage() - quill.getDamageValue())
                                        + " / " + quill.getMaxDamage())
                                        .withStyle(net.minecraft.ChatFormatting.GRAY)),
                        mouseX, mouseY);
            }
        }
    }

    private void renderGrid(GuiGraphics g, int mouseX, int mouseY) {
        for (int col = 0; col < ResearchGrid.COLS; col++) {
            for (int row = 0; row < ResearchGrid.ROWS; row++) {
                HexCoord coord = HexCoord.fromOffset(col, row);
                GridCell cell  = grid.getCell(coord);
                if (cell == null) continue;
                renderCell(g, cell, coord, cellX(col), cellY(col, row), mouseX, mouseY);
            }
        }
    }

    private void renderCell(GuiGraphics g, GridCell cell, HexCoord coord,
                            int x, int y, int mouseX, int mouseY) {
        float r, gr, b, a;

        boolean hovered = isInsideCellHitbox(mouseX, mouseY, x, y);

        if (cell.isHole()) {
            r = 0.08f; gr = 0.05f; b = 0.12f; a = 0.95f;
        } else if (cell.isFixed()) {
            int c = cell.getAspect().getColor();
            r = channel(c, 16); gr = channel(c, 8); b = channel(c, 0); a = 1f;
        } else if (cell.isPlaced()) {
            int c = cell.getAspect().getColor();
            r = channel(c, 16) * 0.75f; gr = channel(c, 8) * 0.75f; b = channel(c, 0) * 0.75f; a = 1f;
        } else { // EMPTY
            // Show preview tint when dragging with a selected aspect
            boolean preview = hovered && isDragging && dragButton == 0
                    && selectedAspect != null && cell.isEmpty();
            if (preview) {
                int c = selectedAspect.getColor();
                r = channel(c, 16) * 0.5f; gr = channel(c, 8) * 0.5f; b = channel(c, 0) * 0.5f; a = 0.9f;
            } else {
                r = hovered ? 0.85f : 0.45f;
                gr = hovered ? 0.85f : 0.45f;
                b = hovered ? 0.85f : 0.45f;
                a = hovered ? 1f : 0.7f;
            }
        }

        RenderSystem.setShaderColor(r, gr, b, a);
        g.blit(HEX_CELL_TEXTURE, x, y, 0, 0, CELL_W, CELL_H, CELL_W, CELL_H);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        if (cell.hasAspect()) {
            Aspect aspect = cell.getAspect();
            if (aspect.getId().getPath().equals("qian") || aspect.getId().getPath().equals("kun") || 
                aspect.getId().getPath().equals("zhen") || aspect.getId().getPath().equals("xun") ||
                aspect.getId().getPath().equals("kan") || aspect.getId().getPath().equals("li") || 
                aspect.getId().getPath().equals("gen") || aspect.getId().getPath().equals("dui")) {
                com.github.nalamodikk.client.screenAPI.utils.TrigramRenderer.render(g, aspect, x + CELL_W / 2, y + CELL_H / 2, 10, 0xFFFFFFFF);
            } else {
                String label = aspect.getId().getPath().substring(0, 1).toUpperCase();
                g.drawString(font, label, x + 5, y + 5, 0xFFFFFF, true);
            }
        }
    }

    private void renderConnections(GuiGraphics g) {
        for (int col = 0; col < ResearchGrid.COLS; col++) {
            for (int row = 0; row < ResearchGrid.ROWS; row++) {
                HexCoord coord = HexCoord.fromOffset(col, row);
                GridCell cell  = grid.getCell(coord);
                if (cell == null || !cell.hasAspect()) continue;

                for (HexCoord nb : coord.neighbors()) {
                    if (!grid.inBounds(nb)) continue;
                    GridCell nbCell = grid.getCell(nb);
                    if (nbCell == null || !nbCell.hasAspect()) continue;
                    if (!cell.getAspect().canConnectTo(nbCell.getAspect())) continue;

                    drawLine(g,
                            cellX(col) + CELL_W / 2, cellY(col, row) + CELL_H / 2,
                            cellX(nb.toOffsetCol()) + CELL_W / 2,
                            cellY(nb.toOffsetCol(), nb.toOffsetRow()) + CELL_H / 2,
                            0xAA55FF55);
                }
            }
        }
    }

    /** Icons only — no tooltips (tooltips are rendered later via renderAllTooltips). */
    private void renderPaletteIcons(GuiGraphics g, int mouseX, int mouseY) {
        List<Aspect> visibleAspects = getVisiblePaletteAspects();
        for (int i = 0; i < visibleAspects.size(); i++) {
            Aspect aspect = visibleAspects.get(i);
            int px = paletteStartX + (i % PALETTE_COLUMNS) * PALETTE_CELL;
            int py = paletteY + (i / PALETTE_COLUMNS) * PALETTE_CELL;
            boolean hover    = mouseX >= px && mouseX < px + PALETTE_CELL
                            && mouseY >= py && mouseY < py + PALETTE_CELL;
            boolean selected = aspect.equals(selectedAspect);

            int   c = aspect.getColor();
            float a = (selected || hover) ? 1f : 0.7f;
            RenderSystem.setShaderColor(channel(c, 16), channel(c, 8), channel(c, 0), a);
            g.blit(HEX_CELL_TEXTURE, px, py, 0, 0, CELL_W, CELL_H, CELL_W, CELL_H);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            if (selected) g.renderOutline(px - 1, py - 1, CELL_W + 2, CELL_H + 2, 0xFFFFFFFF);

            g.drawString(font, aspect.getId().getPath().substring(0, 1).toUpperCase(),
                    px + 5, py + 5, 0xFFFFFF, true);
            renderAspectCount(g, aspect, px, py, PALETTE_CELL);
        }

        if (getPalettePageCount() > 1) {
            g.drawCenteredString(font,
                    Component.translatable("gui.koniava.page_indicator", palettePage + 1, getPalettePageCount()),
                    width / 2, paletteY + PALETTE_ROWS * PALETTE_CELL + 6, 0xAAAAAA);
        }
    }

    /** Draw selected aspect floating under cursor while dragging on the grid. */
    private void renderDragCursor(GuiGraphics g, int mouseX, int mouseY) {
        if (!isDragging || dragButton != 0 || selectedAspect == null) return;
        HexCoord coord = getCellAt(mouseX, mouseY);
        if (coord == null) return;
        GridCell cell = grid.getCell(coord);
        if (cell == null || !cell.isEmpty()) return;

        // Small floating label above cursor
        String label = selectedAspect.getId().getPath().substring(0, 1).toUpperCase();
        g.drawString(font, "▸ " + label, mouseX + 8, mouseY - 10, 0xFFFFFF, true);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (completed) {
            return false;
        }

        // Palette click
        List<Aspect> visibleAspects = getVisiblePaletteAspects();
        for (int i = 0; i < visibleAspects.size(); i++) {
            int px = paletteStartX + (i % PALETTE_COLUMNS) * PALETTE_CELL;
            int py = paletteY + (i / PALETTE_COLUMNS) * PALETTE_CELL;
            if (mouseX >= px && mouseX < px + PALETTE_CELL
                    && mouseY >= py && mouseY < py + PALETTE_CELL) {
                selectedAspect = visibleAspects.get(i).equals(selectedAspect) ? null : visibleAspects.get(i);
                return true;
            }
        }

        // Grid click — also starts a drag
        HexCoord coord = getCellAt(mouseX, mouseY);
        if (coord != null) {
            if (button == 0 && selectedAspect != null
                    && ClientResearchCache.getAspectCount(selectedAspect.getId()) <= 0) {
                showStatusMessage(Component.translatable(
                        "message.koniava.research.not_enough_aspect",
                        selectedAspect.getName()));
                return true;
            }
            isDragging = true;
            dragButton = button;
            draggedCells.clear();
            applyDragAction(coord, button);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isOverPaletteArea(mouseX, mouseY)) {
            int pageCount = getPalettePageCount();
            if (pageCount > 1) {
                if (scrollY < 0) {
                    palettePage = Math.min(pageCount - 1, palettePage + 1);
                    return true;
                }
                if (scrollY > 0) {
                    palettePage = Math.max(0, palettePage - 1);
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (completed || !isDragging || button != dragButton) return false;

        HexCoord coord = getCellAt(mouseX, mouseY);
        if (coord != null && !draggedCells.contains(coord)) {
            applyDragAction(coord, button);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == dragButton) {
            isDragging = false;
            dragButton = -1;
            draggedCells.clear();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ── Placement logic ───────────────────────────────────────────────────────

    private void applyDragAction(HexCoord coord, int button) {
        if (completed) {
            return;
        }

        GridCell cell = grid.getCell(coord);
        if (cell == null || cell.isHole() || cell.isFixed()) return;

        if (button == 0 && selectedAspect != null && cell.isEmpty()) {
            if (ClientResearchCache.getAspectCount(selectedAspect.getId()) <= 0) {
                showStatusMessage(Component.translatable(
                        "message.koniava.research.not_enough_aspect",
                        selectedAspect.getName()));
                return;
            }
            grid = grid.placeAspect(coord, selectedAspect);
            draggedCells.add(coord);
            ResearchAspectPlacePacket.sendToServer(
                    tablePos, template.getId(), coord.q(), coord.r(), selectedAspect);
            checkCompletion();
        } else if (button == 1 && cell.isPlaced()) {
            grid = grid.removeAspect(coord);
            draggedCells.add(coord);
            ResearchAspectPlacePacket.sendToServer(
                    tablePos, template.getId(), coord.q(), coord.r(), null);
        }
    }

    private void showStatusMessage(Component message) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(message, true);
        }
    }

    private void checkCompletion() {
        if (!completed && grid.isComplete()) {
            completed = true;
            isDragging = false;
            dragButton = -1;
            draggedCells.clear();
            ResearchCompletePacket.sendToServer(template.getId(), tablePos);
            KoniavacraftMod.LOGGER.info("Research completed: {}", template.getId());
        }
    }

    /**
     * Called by ResearchCellRevertPacket when the server rejects a placement.
     * Rolls the grid back to the authoritative server state for that cell.
     */
    public void revertCell(BlockPos pos, ResourceLocation resId, int q, int r, @Nullable Aspect serverAspect) {
        if (!pos.equals(tablePos) || !resId.equals(template.getId())) return;
        HexCoord coord = new HexCoord(q, r);
        GridCell cell = grid.getCell(coord);
        if (cell == null || cell.isHole() || cell.isFixed()) return;

        if (serverAspect == null) {
            if (cell.isPlaced()) grid = grid.removeAspect(coord);
        } else {
            if (cell.isEmpty()) {
                grid = grid.placeAspect(coord, serverAspect);
            } else if (cell.isPlaced() && !cell.getAspect().getId().equals(serverAspect.getId())) {
                grid = grid.removeAspect(coord);
                grid = grid.placeAspect(coord, serverAspect);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns the HexCoord at screen position (x, y), or null if none. */
    @Nullable
    private HexCoord getCellAt(double x, double y) {
        HexCoord best = null;
        double bestDist = Double.MAX_VALUE;
        for (int col = 0; col < ResearchGrid.COLS; col++) {
            for (int row = 0; row < ResearchGrid.ROWS; row++) {
                int cx = cellX(col), cy = cellY(col, row);
                if (isInsideCellHitbox(x, y, cx, cy)) {
                    double centerX = cx + CELL_W / 2.0;
                    double centerY = cy + CELL_H / 2.0;
                    double dx = x - centerX;
                    double dy = y - centerY;
                    double dist = dx * dx + dy * dy;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = HexCoord.fromOffset(col, row);
                    }
                }
            }
        }
        return best;
    }

    private List<Aspect> getVisiblePaletteAspects() {
        List<Aspect> aspects = new ArrayList<>();
        for (Aspect aspect : palette) {
            aspects.add(aspect);
        }
        int from = palettePage * PALETTE_PAGE_SIZE;
        if (from >= aspects.size()) {
            return List.of();
        }
        int to = Math.min(aspects.size(), from + PALETTE_PAGE_SIZE);
        return aspects.subList(from, to);
    }

    private int getPalettePageCount() {
        return Math.max(1, (palette.size() + PALETTE_PAGE_SIZE - 1) / PALETTE_PAGE_SIZE);
    }

    private boolean isOverPaletteArea(double mouseX, double mouseY) {
        int widthPixels = PALETTE_COLUMNS * PALETTE_CELL;
        int heightPixels = PALETTE_ROWS * PALETTE_CELL;
        return mouseX >= paletteStartX && mouseX < paletteStartX + widthPixels
                && mouseY >= paletteY && mouseY < paletteY + heightPixels;
    }

    private int cellX(int col) { return gridStartX + col * COL_SPACING; }

    private int cellY(int col, int row) {
        return gridStartY + row * ROW_SPACING + (col % 2 == 1 ? ROW_SPACING / 2 : 0);
    }

    /** Extract a normalised [0,1] colour channel from a packed RGB int. */
    private static float channel(int color, int shift) {
        return ((color >> shift) & 0xFF) / 255f;
    }

    private static boolean isInsideCellHitbox(double mouseX, double mouseY, int cellX, int cellY) {
        int minX = cellX + CELL_HIT_MARGIN;
        int minY = cellY + CELL_HIT_MARGIN;
        int maxX = cellX + CELL_W - CELL_HIT_MARGIN;
        int maxY = cellY + CELL_H - CELL_HIT_MARGIN;
        return mouseX >= minX && mouseX < maxX
                && mouseY >= minY && mouseY < maxY;
    }

    private void renderAspectCount(GuiGraphics g, Aspect aspect, int x, int y, int size) {
        int count = ClientResearchCache.getAspectCount(aspect.getId());
        if (count <= 0) {
            return;
        }

        String countText = Integer.toString(count);
        int textWidth = Math.max(1, (int) Math.ceil(font.width(countText) * COUNT_SCALE));
        int textHeight = Math.max(1, (int) Math.ceil(8 * COUNT_SCALE));
        int countX = x + size - textWidth - 1;
        int countY = y + size - textHeight - 1;

        g.fill(countX - 1, countY - 1, countX + textWidth + 1, countY + textHeight + 1, COUNT_BG_COLOR);
        g.pose().pushPose();
        g.pose().translate(countX, countY, 0.0F);
        g.pose().scale(COUNT_SCALE, COUNT_SCALE, 1.0F);
        g.drawString(font, countText, 0, 0, COUNT_TEXT_COLOR, false);
        g.pose().popPose();
    }

    private static void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1, dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) return;
        for (int i = 0; i <= steps; i++) {
            g.fill(x1 + dx * i / steps, y1 + dy * i / steps,
                   x1 + dx * i / steps + 1, y1 + dy * i / steps + 1, color);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Allow E key (inventory key) and Escape to close the screen
        assert this.minecraft != null;
        if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
