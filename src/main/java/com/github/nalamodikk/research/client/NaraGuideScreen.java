package com.github.nalamodikk.research.client;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Holographic guide screen accessible from the NaraWatchScreen.
 *
 * Layout (background_2.png 512x512, panel 400x252):
 *   Chapter list: x=18..79  (w=62), entries start at y=20
 *   Content area: x=80..395 (w=316), y=8..217
 *   Bottom bar:   y = CL_Y + CL_H + 6
 */
public class NaraGuideScreen extends Screen {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID,
                    "textures/gui/nara_watch/background_2.png");

    // Panel / region constants (verified against texture pixels)
    private static final int PANEL_W = 400, PANEL_H = 252;
    private static final int CL_X = 18, CL_Y = 8,  CL_W = 62, CL_H = 210;
    private static final int CA_X = 80, CA_Y = 8,  CA_W = 316, CA_H = 210;

    // Chapter entries start below title area
    private static final int CL_ENTRY_TOP = 12;

    // Scrollbar strip on right side of content area
    private static final int SCROLL_TRACK_W = 5;
    private static final int SCROLL_PAD     = 6; // reserved width for scroll track

    // ── Guide content ─────────────────────────────────────────────────────────
    // [chapter][page] = { titleKey, bodyKey }
    private static final String[][][] PAGES = {
        // Chapter 0: 本源系統
        {
            {"guide.koniava.aspect.intro.title",    "guide.koniava.aspect.intro.body"},
            {"guide.koniava.aspect.scanning.title", "guide.koniava.aspect.scanning.body"},
            {"guide.koniava.aspect.types.title",    "guide.koniava.aspect.types.body"},
        },
        // Chapter 1: 研究系統
        {
            {"guide.koniava.research.overview.title", "guide.koniava.research.overview.body"},
            {"guide.koniava.research.table.title",    "guide.koniava.research.table.body"},
        },
        // Chapter 2: 魔力基礎設施
        {
            {"guide.koniava.mana.generator.title", "guide.koniava.mana.generator.body"},
            {"guide.koniava.mana.conduit.title",   "guide.koniava.mana.conduit.body"},
            {"guide.koniava.mana.machines.title",  "guide.koniava.mana.machines.body"},
        },
        // Chapter 3: 祭壇系統
        {
            {"guide.koniava.altar.overview.title", "guide.koniava.altar.overview.body"},
            {"guide.koniava.altar.usage.title",    "guide.koniava.altar.usage.body"},
            {"guide.koniava.altar.upgrade.title",  "guide.koniava.altar.upgrade.body"},
        },
    };

    private static final String[] CHAPTER_KEYS = {
        "guide.koniava.chapter.aspect",
        "guide.koniava.chapter.research",
        "guide.koniava.chapter.mana",
        "guide.koniava.chapter.altar",
    };

    // ── State ─────────────────────────────────────────────────────────────────

    @Nullable private final Screen parent;
    private int panelX, panelY;
    private int activeChapter = 0;
    private int activePage    = 0;
    private int scrollOffset  = 0;  // pixels scrolled in content area
    private int totalContentH = 0;  // computed each frame

    public NaraGuideScreen(@Nullable Screen parent) {
        super(Component.translatable("gui.koniava.nara_guide.title"));
        this.parent = parent;
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        panelX = (width  - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
        g.fill(0, 0, width, height, 0x88000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        super.render(g, mouseX, mouseY, pt);

        g.blit(BACKGROUND, panelX, panelY, 0, 0, PANEL_W, PANEL_H, 512, 512);

        g.drawCenteredString(font, title,
                panelX + PANEL_W / 2, panelY + 5, 0x2233AA);

        renderChapterList(g, mouseX, mouseY);
        renderContent(g, mouseX, mouseY);
        renderBottomBar(g, mouseX, mouseY);
    }

    private void renderChapterList(GuiGraphics g, int mouseX, int mouseY) {
        int entryH = 20;
        g.enableScissor(panelX + CL_X, panelY + CL_Y,
                        panelX + CL_X + CL_W, panelY + CL_Y + CL_H);

        for (int i = 0; i < CHAPTER_KEYS.length; i++) {
            int ex = panelX + CL_X;
            int ey = panelY + CL_ENTRY_TOP + i * (entryH + 4);
            boolean active  = activeChapter == i;
            boolean hovered = !active && mouseX >= ex && mouseX < ex + CL_W
                           && mouseY >= ey && mouseY < ey + entryH;

            int bg     = active  ? 0xCC1A2C66 : hovered ? 0xCC152150 : 0x00000000;
            int border = active  ? 0xFF6699FF : 0x00000000;
            int col    = active  ? 0xFFFFFF   : hovered ? 0xCCDDFF   : 0x778899;

            if (bg != 0) g.fill(ex, ey, ex + CL_W, ey + entryH, bg);
            if (border != 0) {
                g.fill(ex, ey, ex + CL_W, ey + 1, border);
                g.fill(ex, ey + entryH - 1, ex + CL_W, ey + entryH, border);
            }

            Component label = Component.translatable(CHAPTER_KEYS[i]);
            List<FormattedCharSequence> wrapped = font.split(label, CL_W - 6);
            int textY = ey + (entryH - wrapped.size() * 9) / 2;
            for (FormattedCharSequence line : wrapped) {
                g.drawString(font, line, ex + 5, textY, col, false);
                textY += 9;
            }
        }

        g.disableScissor();
    }

    private void renderContent(GuiGraphics g, int mouseX, int mouseY) {
        if (activeChapter >= PAGES.length) return;
        String[] page = PAGES[activeChapter][activePage];

        int clipX1 = panelX + CA_X;
        int clipY1 = panelY + CA_Y;
        int clipX2 = clipX1 + CA_W;
        int clipY2 = clipY1 + CA_H;

        int x    = clipX1 + 6;
        int maxW = CA_W - 12 - SCROLL_PAD;

        // Pre-compute line lists so we know total height before rendering
        Component titleComp = Component.translatable(page[0]);
        Component bodyComp  = Component.translatable(page[1]);
        List<FormattedCharSequence> bodyLines = font.split(bodyComp, maxW);

        // title(9) + gap(4) + separator(1) + gap(5) = 19px header, then 10px per line
        totalContentH = 19 + bodyLines.size() * 10 + 4;

        int viewH   = CA_H - 4;
        int maxScroll = Math.max(0, totalContentH - viewH);
        scrollOffset  = Math.max(0, Math.min(scrollOffset, maxScroll));

        // Clip to content area
        g.enableScissor(clipX1, clipY1, clipX2, clipY2);

        int y = clipY1 + 6 - scrollOffset;

        // Page title
        g.drawString(font, titleComp, x, y, 0x2233AA, false);
        y += 13;

        // Separator line
        g.fill(x, y, x + maxW, y + 1, 0x44334466);
        y += 6;

        // Body lines
        for (FormattedCharSequence line : bodyLines) {
            g.drawString(font, line, x, y, 0x223355, false);
            y += 10;
        }

        g.disableScissor();

        // Scrollbar (only when content overflows)
        if (maxScroll > 0) {
            int trackX = clipX2 - SCROLL_TRACK_W - 1;
            int trackY1 = clipY1 + 3;
            int trackY2 = clipY2 - 3;
            int trackH  = trackY2 - trackY1;

            // Track background
            g.fill(trackX, trackY1, trackX + SCROLL_TRACK_W, trackY2, 0x22334477);

            // Thumb
            int thumbH = Math.max(12, trackH * viewH / totalContentH);
            int thumbY = trackY1 + (trackH - thumbH) * scrollOffset / maxScroll;
            boolean thumbHov = mouseX >= trackX && mouseX < trackX + SCROLL_TRACK_W
                            && mouseY >= thumbY  && mouseY < thumbY + thumbH;
            g.fill(trackX, thumbY, trackX + SCROLL_TRACK_W, thumbY + thumbH,
                   thumbHov ? 0xCC4455AA : 0xAA334477);
        }
    }

    private void renderBottomBar(GuiGraphics g, int mouseX, int mouseY) {
        if (activeChapter >= PAGES.length) return;
        int pageCount = PAGES[activeChapter].length;

        int barY = panelY + CL_Y + CL_H + 6;
        int cx   = panelX + PANEL_W / 2;

        // Back button
        int backX = panelX + CL_X;
        boolean backHov = mouseX >= backX && mouseX < backX + CL_W
                       && mouseY >= barY   && mouseY < barY + 18;
        g.fill(backX, barY, backX + CL_W, barY + 18,
               backHov ? 0xCC152150 : 0xCC0D1830);
        g.fill(backX, barY, backX + CL_W, barY + 1, 0xFF334477);
        g.fill(backX, barY + 17, backX + CL_W, barY + 18, 0xFF334477);
        g.drawCenteredString(font,
                Component.translatable("gui.koniava.nara_guide.back"),
                backX + CL_W / 2, barY + 5, backHov ? 0xCCDDFF : 0x778899);

        // Page indicator
        String indicator = (activePage + 1) + " / " + pageCount;
        g.drawCenteredString(font, Component.literal(indicator), cx, barY + 5, 0x556688);

        // Prev button (4px gap from back button)
        boolean hasPrev = activePage > 0 || activeChapter > 0;
        renderNavBtn(g, mouseX, mouseY, panelX + CA_X + 4, barY, "◀", hasPrev);

        // Next button
        boolean hasNext = activePage < pageCount - 1 || activeChapter < PAGES.length - 1;
        renderNavBtn(g, mouseX, mouseY, panelX + CA_X + CA_W - 36, barY, "▶", hasNext);
    }

    private void renderNavBtn(GuiGraphics g, int mouseX, int mouseY,
                               int x, int y, String label, boolean enabled) {
        int w = 36, h = 18;
        boolean hov = enabled && mouseX >= x && mouseX < x + w
                              && mouseY >= y && mouseY < y + h;
        int bg  = !enabled ? 0x440D1830 : hov ? 0xCC152150 : 0xCC0D1830;
        int bdr = !enabled ? 0x22334477 : 0xFF334477;
        int col = !enabled ? 0x445566   : hov ? 0xCCDDFF : 0x778899;
        g.fill(x, y, x + w, y + h, bg);
        g.fill(x, y, x + w, y + 1, bdr);
        g.fill(x, y + h - 1, x + w, y + h, bdr);
        g.fill(x, y, x + 1, y + h, bdr);
        g.fill(x + w - 1, y, x + w, y + h, bdr);
        g.drawCenteredString(font, Component.literal(label), x + w / 2, y + 5, col);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        // Scroll only when mouse is over content area
        int cax = panelX + CA_X, cay = panelY + CA_Y;
        if (mx >= cax && mx < cax + CA_W && my >= cay && my < cay + CA_H) {
            int viewH     = CA_H - 4;
            int maxScroll = Math.max(0, totalContentH - viewH);
            scrollOffset  = Math.max(0, Math.min(maxScroll,
                    scrollOffset - (int) (scrollY * 12)));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return super.mouseClicked(mx, my, btn);

        // Chapter list clicks
        int entryH = 20;
        for (int i = 0; i < CHAPTER_KEYS.length; i++) {
            int ex = panelX + CL_X;
            int ey = panelY + CL_ENTRY_TOP + i * (entryH + 4);
            if (mx >= ex && mx < ex + CL_W && my >= ey && my < ey + entryH) {
                if (activeChapter != i) {
                    activeChapter = i;
                    activePage    = 0;
                    scrollOffset  = 0;
                }
                return true;
            }
        }

        int pageCount = activeChapter < PAGES.length ? PAGES[activeChapter].length : 1;
        int barY = panelY + CL_Y + CL_H + 6;

        // Back button
        int backX = panelX + CL_X;
        if (mx >= backX && mx < backX + CL_W && my >= barY && my < barY + 18) {
            this.onClose();
            return true;
        }

        // Prev
        int prevX = panelX + CA_X + 4;
        if (mx >= prevX && mx < prevX + 36 && my >= barY && my < barY + 18) {
            if (activePage > 0) {
                activePage--;
            } else if (activeChapter > 0) {
                activeChapter--;
                activePage = PAGES[activeChapter].length - 1;
            }
            scrollOffset = 0;
            return true;
        }

        // Next
        int nextX = panelX + CA_X + CA_W - 36;
        if (mx >= nextX && mx < nextX + 36 && my >= barY && my < barY + 18) {
            if (activePage < pageCount - 1) {
                activePage++;
            } else if (activeChapter < PAGES.length - 1) {
                activeChapter++;
                activePage = 0;
            }
            scrollOffset = 0;
            return true;
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
