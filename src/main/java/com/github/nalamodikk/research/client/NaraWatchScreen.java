package com.github.nalamodikk.research.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.network.StartResearchPacket;
import com.github.nalamodikk.research.template.ResearchRegistry;
import com.github.nalamodikk.research.template.ResearchTemplate;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holographic watch research node graph.
 *
 * Layout (background.png pixel measurements):
 *   Panel total:  400×252
 *   Node area:    x=18..271 (254px wide), y=13..215 (203px) — dark purple, interactive
 *   Desc area:    x=272..399 (128px wide), y=4..245          — blue, description only
 *   y=4..12   = light-blue title strip
 *   y=216..251 = light-blue bottom strip
 *
 * Side tabs (outside panel, in empty screen space):
 *   Left   (panelX - TIER_TAB_W):   tier filter  全 / T1 / T2 / T3 / T4
 *   Right  (panelX + PANEL_W):      status filter 全部 / 可解鎖 / 已完成
 *   Bottom (panelY + PANEL_H + 8):  search box
 *
 * Controls:
 *   Scroll wheel             — zoom (0.4×–3.0×) within node area
 *   Left-drag empty area     — pan
 *   Right-drag anywhere      — pan
 *   Left-click AVAILABLE     — 1st click: select + description; 2nd click: receive note + close
 *   Left-click other node    — show description
 *   E / Esc                  — close
 */
public class NaraWatchScreen extends Screen {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID,
                    "textures/gui/nara_watch/background.png");
    private static final ResourceLocation NODE_TEX =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID,
                    "textures/gui/nara_watch/node.png");

    // ── Panel / region constants ──────────────────────────────────────────────

    private static final int PANEL_W = 400, PANEL_H = 252;
    private static final int NA_X = 18,  NA_Y = 13,  NA_W = 254, NA_H = 203;
    private static final int DA_X = 272, DA_Y = 4,   DA_W = 128, DA_H = 242;

    private static final int NODE_SIZE     = 24;
    private static final int NODE_TEX_SIZE = 32;
    private static final int NODE_TEX_OFF  = 4;

    private static final float ZOOM_MIN = 0.4f;
    private static final float ZOOM_MAX = 3.0f;

    // ── Side-tab geometry ─────────────────────────────────────────────────────

    private static final int TIER_TAB_W         = 30;
    private static final int TIER_TAB_H         = 22;
    private static final int TIER_TAB_START_Y   = 20;  // offset from panelY

    private static final int STATUS_TAB_W        = 62;
    private static final int STATUS_TAB_H        = 22;
    private static final int STATUS_TAB_START_Y  = 30;  // offset from panelY

    private static final int TAB_GAP = 3;

    private static final String[] TIER_LABELS   = { "全", "T1", "T2", "T3", "T4" };
    private static final String[] STATUS_LABELS = { "全部", "可解鎖", "已完成" };

    // ── State ─────────────────────────────────────────────────────────────────

    private final Set<ResourceLocation> completed;
    private final int tier;

    private int panelX, panelY;
    private final Map<ResourceLocation, int[]> nodePositions = new LinkedHashMap<>();

    private float   zoom    = 1.0f;
    private float   offsetX = 0f, offsetY = 0f;
    private boolean isPanning = false;
    private double  lastPanMouseX, lastPanMouseY;

    @Nullable private ResearchTemplate selectedNode = null;

    private int activeTierFilter   = 0; // 0=all, 1-4=specific tier
    private int activeStatusFilter = 0; // 0=all, 1=available, 2=completed

    @Nullable private EditBox searchBox;

    // ── Constructor ───────────────────────────────────────────────────────────

    public NaraWatchScreen(Set<ResourceLocation> completed, int tier) {
        super(Component.translatable("gui.koniava.nara_watch.title"));
        this.completed = completed;
        this.tier      = tier;
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        panelX = (width  - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2;

        nodePositions.clear();
        computeLayout(ResearchRegistry.all());
        zoom = 1.0f; offsetX = 0f; offsetY = 0f;
        selectedNode = null;

        // Search box: below the panel
        searchBox = addRenderableWidget(new EditBox(font,
                panelX + NA_X, panelY + PANEL_H + 8, NA_W, 16,
                Component.empty()));
        searchBox.setHint(Component.translatable("gui.koniava.nara_watch.search_hint"));
        searchBox.setMaxLength(64);
    }

    private void computeLayout(Collection<ResearchTemplate> all) {
        Map<ResourceLocation, Integer> depths = new LinkedHashMap<>();
        for (ResearchTemplate t : all) depths.put(t.getId(), 0);

        boolean changed = true;
        while (changed) {
            changed = false;
            for (ResearchTemplate t : all)
                for (ResourceLocation prereq : t.getPrerequisites())
                    if (depths.containsKey(prereq)) {
                        int d = depths.get(prereq) + 1;
                        if (d > depths.getOrDefault(t.getId(), 0)) {
                            depths.put(t.getId(), d);
                            changed = true;
                        }
                    }
        }

        Map<Integer, List<ResourceLocation>> byDepth = new LinkedHashMap<>();
        for (ResearchTemplate t : all)
            byDepth.computeIfAbsent(depths.getOrDefault(t.getId(), 0), k -> new ArrayList<>())
                   .add(t.getId());

        int maxDepth = byDepth.keySet().stream().mapToInt(i -> i).max().orElse(0);
        int padding  = 20;
        int usableH  = NA_H - 2 * padding - NODE_SIZE;
        int rowStep  = maxDepth > 0 ? usableH / maxDepth : 0;

        for (Map.Entry<Integer, List<ResourceLocation>> e : byDepth.entrySet()) {
            int depth = e.getKey();
            List<ResourceLocation> ids = e.getValue();
            int count  = ids.size();
            int y      = padding + depth * rowStep;
            int totalW = count * NODE_SIZE + (count - 1) * 16;
            int startX = (NA_W - totalW) / 2;
            for (int i = 0; i < count; i++)
                nodePositions.put(ids.get(i), new int[]{ startX + i * (NODE_SIZE + 16), y });
        }
    }

    // ── Coordinate helpers ────────────────────────────────────────────────────

    private int sx(float wx) { return panelX + NA_X + Math.round(wx * zoom + offsetX); }
    private int sy(float wy) { return panelY + NA_Y + Math.round(wy * zoom + offsetY); }
    private int scaledNodeSize() { return Math.max(4, Math.round(NODE_SIZE * zoom)); }

    private boolean isInNodeArea(double mx, double my) {
        return mx >= panelX + NA_X && mx < panelX + NA_X + NA_W
            && my >= panelY + NA_Y && my < panelY + NA_Y + NA_H;
    }

    // ── Visibility filter ──────────────────────────────────────────────────────

    private boolean isNodeVisible(ResearchTemplate t) {
        if (activeTierFilter != 0 && t.getTier() != activeTierFilter) return false;
        NodeState state = stateOf(t);
        if (activeStatusFilter == 1 && state != NodeState.AVAILABLE) return false;
        if (activeStatusFilter == 2 && state != NodeState.COMPLETED) return false;
        String q = searchBox != null ? searchBox.getValue().trim() : "";
        if (!q.isEmpty()) {
            String title = Component.translatable(t.getTitleKey()).getString().toLowerCase();
            if (!title.contains(q.toLowerCase())) return false;
        }
        return true;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x88000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick); // dim fill + EditBox widget

        // Deselect if current selection no longer visible
        if (selectedNode != null && !isNodeVisible(selectedNode)) selectedNode = null;

        // Panel background
        g.blit(BACKGROUND, panelX, panelY, 0, 0, PANEL_W, PANEL_H, 512, 512);

        // Title in header strip
        g.drawCenteredString(font, title,
                panelX + NA_X + NA_W / 2, panelY + 5, 0x2233AA);

        // === Nodes (scissored to dark-purple content area) ===
        g.enableScissor(panelX + NA_X, panelY + NA_Y,
                        panelX + NA_X + NA_W, panelY + NA_Y + NA_H);
        renderConnections(g);
        ResearchTemplate hoveredNode = null;
        for (ResearchTemplate t : ResearchRegistry.all()) {
            if (!isNodeVisible(t)) continue;
            int[] pos = nodePositions.get(t.getId());
            if (pos == null) continue;
            int ns = scaledNodeSize();
            int nx = sx(pos[0]), ny = sy(pos[1]);
            if (nx + ns < panelX + NA_X || nx > panelX + NA_X + NA_W ||
                ny + ns < panelY + NA_Y || ny > panelY + NA_Y + NA_H) continue;
            renderNode(g, pos[0], pos[1], t, partialTick);
            if (mouseX >= nx && mouseX < nx + ns && mouseY >= ny && mouseY < ny + ns)
                hoveredNode = t;
        }
        g.disableScissor();

        // === Overdraw: restore non-node areas from background texture ===
        g.blit(BACKGROUND, panelX, panelY,
               0, 0, PANEL_W, NA_Y, 512, 512);
        g.blit(BACKGROUND, panelX, panelY + NA_Y + NA_H,
               0, NA_Y + NA_H, PANEL_W, PANEL_H - NA_Y - NA_H, 512, 512);
        g.blit(BACKGROUND, panelX + NA_X + NA_W, panelY + NA_Y,
               NA_X + NA_W, NA_Y, PANEL_W - NA_X - NA_W, NA_H, 512, 512);
        g.blit(BACKGROUND, panelX, panelY + NA_Y,
               0, NA_Y, NA_X, NA_H, 512, 512);

        // === Side tabs (outside panel, in empty screen space) ===
        renderLeftTabs(g, mouseX, mouseY);
        renderRightTabs(g, mouseX, mouseY);

        // === Description panel ===
        renderDescPanel(g, mouseX, mouseY);

        // === Node hover tooltip (rendered last so it's above everything) ===
        if (hoveredNode != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable(hoveredNode.getTitleKey()));
            NodeState state = stateOf(hoveredNode);
            Component stateLine;
            switch (state) {
                case COMPLETED -> stateLine = Component.translatable("research.koniava.state.completed")
                        .withStyle(ChatFormatting.GREEN);
                case AVAILABLE -> stateLine = Component.translatable("research.koniava.state.available_short")
                        .withStyle(ChatFormatting.YELLOW);
                default        -> stateLine = Component.translatable("research.koniava.state.locked")
                        .withStyle(ChatFormatting.DARK_GRAY);
            }
            tip.add(stateLine);
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    private void renderConnections(GuiGraphics g) {
        int ns = scaledNodeSize();
        for (ResearchTemplate t : ResearchRegistry.all()) {
            if (!isNodeVisible(t)) continue;
            int[] pos = nodePositions.get(t.getId());
            if (pos == null) continue;
            for (ResourceLocation prereqId : t.getPrerequisites()) {
                int[] pp = nodePositions.get(prereqId);
                if (pp == null) continue;
                drawLine(g,
                        sx(pp[0])  + ns / 2, sy(pp[1])  + ns,
                        sx(pos[0]) + ns / 2, sy(pos[1]),
                        completed.contains(prereqId) ? 0xFF55FF55 : 0xFF555555);
            }
        }
    }

    private void renderNode(GuiGraphics g, int worldX, int worldY,
                            ResearchTemplate t, float partialTick) {
        NodeState state    = stateOf(t);
        boolean   selected = t.equals(selectedNode);

        float r, gr, b, a;
        switch (state) {
            case COMPLETED -> { r = 0.2f; gr = 0.9f; b = 0.2f; a = 1f; }
            case AVAILABLE -> {
                float pulse = selected ? 1f :
                        0.6f + 0.4f * (float)(0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 400.0));
                r = 0.3f * pulse; gr = 0.7f * pulse; b = pulse; a = 1f;
            }
            default -> { r = 0.35f; gr = 0.35f; b = 0.35f; a = 0.8f; }
        }

        int nx = sx(worldX), ny = sy(worldY), ns = scaledNodeSize();
        RenderSystem.setShaderColor(r, gr, b, a);
        g.blit(NODE_TEX, nx, ny, ns, ns,
               NODE_TEX_OFF, NODE_TEX_OFF, NODE_SIZE, NODE_SIZE, NODE_TEX_SIZE, NODE_TEX_SIZE);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        if (selected) g.renderOutline(nx - 2, ny - 2, ns + 4, ns + 4, 0xFFFFDD44);
    }

    // Left tabs: tier filter, protrude to the LEFT of the panel
    private void renderLeftTabs(GuiGraphics g, int mouseX, int mouseY) {
        for (int i = 0; i < TIER_LABELS.length; i++) {
            int tx = panelX - TIER_TAB_W;
            int ty = panelY + TIER_TAB_START_Y + i * (TIER_TAB_H + TAB_GAP);
            boolean active  = activeTierFilter == i;
            boolean hovered = mouseX >= tx && mouseX < tx + TIER_TAB_W
                           && mouseY >= ty && mouseY < ty + TIER_TAB_H;
            renderSideTab(g, tx, ty, TIER_TAB_W, TIER_TAB_H, TIER_LABELS[i],
                          active, hovered, false);
        }
    }

    // Right tabs: status filter, protrude to the RIGHT of the panel
    private void renderRightTabs(GuiGraphics g, int mouseX, int mouseY) {
        for (int i = 0; i < STATUS_LABELS.length; i++) {
            int tx = panelX + PANEL_W;
            int ty = panelY + STATUS_TAB_START_Y + i * (STATUS_TAB_H + TAB_GAP);
            boolean active  = activeStatusFilter == i;
            boolean hovered = mouseX >= tx && mouseX < tx + STATUS_TAB_W
                           && mouseY >= ty && mouseY < ty + STATUS_TAB_H;
            renderSideTab(g, tx, ty, STATUS_TAB_W, STATUS_TAB_H, STATUS_LABELS[i],
                          active, hovered, true);
        }
    }

    /**
     * Draws a tab rectangle with the inner edge (touching the panel) left open
     * so tabs appear "attached" to the panel wall.
     *
     * @param rightSide true = tab protrudes rightward (open left edge);
     *                  false = tab protrudes leftward (open right edge)
     */
    private void renderSideTab(GuiGraphics g, int x, int y, int w, int h,
                                String label, boolean active, boolean hovered,
                                boolean rightSide) {
        int bg     = active  ? 0xCC1A2C66
                   : hovered ? 0xCC152150
                   :           0xCC0D1830;
        int border = active  ? 0xFF6699FF : 0xFF334477;
        int textColor = active ? 0xFFFFFF : hovered ? 0xCCDDFF : 0x778899;

        g.fill(x, y, x + w, y + h, bg);
        g.fill(x,         y,         x + w, y + 1,     border); // top
        g.fill(x,         y + h - 1, x + w, y + h,     border); // bottom
        if (rightSide) {
            g.fill(x + w - 1, y, x + w, y + h, border);         // outer (right) edge
            // left edge is open — "attached" to panel's right wall
        } else {
            g.fill(x, y, x + 1, y + h, border);                  // outer (left) edge
            // right edge is open — "attached" to panel's left wall
        }

        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, textColor);
    }

    private void renderDescPanel(GuiGraphics g, int mouseX, int mouseY) {
        int dx   = panelX + DA_X + 4;
        int dy   = panelY + DA_Y + 6;
        int maxW = DA_W - 8;

        if (selectedNode == null) {
            g.drawCenteredString(font,
                    Component.translatable("gui.koniava.nara_watch.hint"),
                    panelX + DA_X + DA_W / 2, panelY + DA_Y + DA_H / 2 - 4, 0x334466);
            return;
        }

        g.drawString(font, Component.translatable(selectedNode.getTitleKey()),
                dx, dy, 0x2233AA, false);
        dy += 11;

        NodeState state = stateOf(selectedNode);
        Component stateLine; int stateColor;
        switch (state) {
            case COMPLETED -> { stateLine = Component.translatable("research.koniava.state.completed");       stateColor = 0x006600; }
            case AVAILABLE -> { stateLine = Component.translatable("research.koniava.state.available_short"); stateColor = 0x997700; }
            default        -> { stateLine = Component.translatable("research.koniava.state.locked");          stateColor = 0x555555; }
        }
        g.drawString(font, stateLine, dx, dy, stateColor, false);
        dy += 13;

        String descKey = "research.koniava." + selectedNode.getId().getPath() + ".description";
        for (FormattedCharSequence line : font.split(Component.translatable(descKey), maxW)) {
            g.drawString(font, line, dx, dy, 0x223355, false);
            dy += 9;
        }

        String loreKey = "research.koniava." + selectedNode.getId().getPath() + ".lore";
        Component loreComp = Component.translatable(loreKey);
        if (!loreComp.getString().equals(loreKey)) {
            dy += 4;
            for (FormattedCharSequence line : font.split(loreComp, maxW)) {
                g.drawString(font, line, dx, dy, 0x445566, false);
                dy += 9;
            }
        }

        if (state == NodeState.AVAILABLE) {
            g.drawString(font, Component.translatable("research.koniava.click_confirm"),
                    dx, panelY + DA_Y + DA_H - 20, 0x885500, false);
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isInNodeArea(mouseX, mouseY))
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);

        float oldZoom = zoom;
        zoom = Math.clamp(zoom + (float) scrollY * 0.15f, ZOOM_MIN, ZOOM_MAX);
        float relX   = (float)(mouseX - panelX - NA_X);
        float relY   = (float)(mouseY - panelY - NA_Y);
        float wx     = (relX - offsetX) / oldZoom;
        float wy     = (relY - offsetY) / oldZoom;
        offsetX = relX - wx * zoom;
        offsetY = relY - wy * zoom;
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Check left tier tabs
            for (int i = 0; i < TIER_LABELS.length; i++) {
                int tx = panelX - TIER_TAB_W;
                int ty = panelY + TIER_TAB_START_Y + i * (TIER_TAB_H + TAB_GAP);
                if (mouseX >= tx && mouseX < tx + TIER_TAB_W
                 && mouseY >= ty && mouseY < ty + TIER_TAB_H) {
                    activeTierFilter = i;
                    if (selectedNode != null && !isNodeVisible(selectedNode)) selectedNode = null;
                    return true;
                }
            }
            // Check right status tabs
            for (int i = 0; i < STATUS_LABELS.length; i++) {
                int tx = panelX + PANEL_W;
                int ty = panelY + STATUS_TAB_START_Y + i * (STATUS_TAB_H + TAB_GAP);
                if (mouseX >= tx && mouseX < tx + STATUS_TAB_W
                 && mouseY >= ty && mouseY < ty + STATUS_TAB_H) {
                    activeStatusFilter = i;
                    if (selectedNode != null && !isNodeVisible(selectedNode)) selectedNode = null;
                    return true;
                }
            }
        }

        ResearchTemplate clicked = getNodeAt(mouseX, mouseY);

        if (button == 0 && clicked != null) {
            if (clicked.equals(selectedNode) && stateOf(clicked) == NodeState.AVAILABLE) {
                StartResearchPacket.sendToServer(clicked.getId());
                this.onClose();
            } else {
                selectedNode = clicked;
            }
            return true;
        }

        if (button == 1 || (button == 0 && clicked == null && isInNodeArea(mouseX, mouseY))) {
            isPanning     = true;
            lastPanMouseX = mouseX;
            lastPanMouseY = mouseY;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (isPanning) {
            offsetX += (float)(mouseX - lastPanMouseX);
            offsetY += (float)(mouseY - lastPanMouseY);
            lastPanMouseX = mouseX;
            lastPanMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isPanning = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // When search box is focused, let it consume letter keys; only block E-close
        if (searchBox == null || !searchBox.isFocused()) {
            assert minecraft != null;
            if (minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                this.onClose();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private NodeState stateOf(ResearchTemplate t) {
        if (completed.contains(t.getId())) return NodeState.COMPLETED;
        if (ClientResearchCache.isLocked(t.getId())) return NodeState.LOCKED;
        if (ClientResearchCache.isForcedAvailable(t.getId())) return NodeState.AVAILABLE;
        if (tier < t.getTier()) return NodeState.LOCKED;
        for (ResourceLocation prereq : t.getPrerequisites())
            if (!completed.contains(prereq)) return NodeState.LOCKED;
        for (Aspect aspect : t.getRequiredAspects())
            if (!ClientResearchCache.hasDiscovered(aspect.getId())) return NodeState.LOCKED;
        return NodeState.AVAILABLE;
    }

    @Nullable
    private ResearchTemplate getNodeAt(double mouseX, double mouseY) {
        if (!isInNodeArea(mouseX, mouseY)) return null;
        int ns = scaledNodeSize();
        for (ResearchTemplate t : ResearchRegistry.all()) {
            if (!isNodeVisible(t)) continue;
            int[] pos = nodePositions.get(t.getId());
            if (pos == null) continue;
            int nx = sx(pos[0]), ny = sy(pos[1]);
            if (mouseX >= nx && mouseX < nx + ns && mouseY >= ny && mouseY < ny + ns)
                return t;
        }
        return null;
    }

    private static void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1, dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) return;
        for (int i = 0; i <= steps; i++)
            g.fill(x1 + dx * i / steps, y1 + dy * i / steps,
                   x1 + dx * i / steps + 1, y1 + dy * i / steps + 1, color);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private enum NodeState { COMPLETED, AVAILABLE, LOCKED }
}
