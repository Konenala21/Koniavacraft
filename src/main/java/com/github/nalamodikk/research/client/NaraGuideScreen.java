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
 * Layout (reuses nara_watch background.png, 400x252):
 *   Chapter list:  x=18..99   (82px), y=13..215
 *   Content area:  x=108..399 (284px), y=13..215
 *   Bottom bar:    y=220..245  — prev/next page + page indicator
 */
public class NaraGuideScreen extends Screen {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID,
                    "textures/gui/nara_watch/background_2.png");

    // Pixel positions measured from background_2.png (512x512 texture, panel 400x252)
    // Dark column: texture x=5..75 (w=70), y=8..218 (h=210)
    private static final int PANEL_W = 400, PANEL_H = 252;
    private static final int CL_X = 5,  CL_Y = 8, CL_W = 70, CL_H = 210;
    private static final int CA_X = 79, CA_Y = 8, CA_W = 317, CA_H = 210;

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

        // Title strip
        g.drawCenteredString(font, title,
                panelX + CA_X + CA_W / 2, panelY + 5, 0x2233AA);

        renderChapterList(g, mouseX, mouseY);
        renderContent(g);
        renderBottomBar(g, mouseX, mouseY);
    }

    private void renderChapterList(GuiGraphics g, int mouseX, int mouseY) {
        int entryH = 20;
        g.enableScissor(panelX + CL_X, panelY + CL_Y,
                        panelX + CL_X + CL_W, panelY + CL_Y + CL_H);
        for (int i = 0; i < CHAPTER_KEYS.length; i++) {
            int ex = panelX + CL_X;
            int ey = panelY + CL_Y + i * (entryH + 2);
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
            List<FormattedCharSequence> wrapped = font.split(label, CL_W - 4);
            int textY = ey + (entryH - wrapped.size() * 9) / 2;
            for (FormattedCharSequence line : wrapped) {
                g.drawString(font, line, ex + 4, textY, col, false);
                textY += 9;
            }
        }
        g.disableScissor();
    }

    private void renderContent(GuiGraphics g) {
        if (activeChapter >= PAGES.length) return;
        String[] page = PAGES[activeChapter][activePage];

        int x = panelX + CA_X + 6;
        int y = panelY + CA_Y + 6;
        int maxW = CA_W - 12;

        // Page title
        Component titleComp = Component.translatable(page[0]);
        g.drawString(font, titleComp, x, y, 0x2233AA, false);
        y += 13;

        // Separator line
        g.fill(x, y, x + maxW, y + 1, 0x44334466);
        y += 6;

        // Body text
        Component bodyComp = Component.translatable(page[1]);
        for (FormattedCharSequence line : font.split(bodyComp, maxW)) {
            g.drawString(font, line, x, y, 0x223355, false);
            y += 10;
        }
    }

    private void renderBottomBar(GuiGraphics g, int mouseX, int mouseY) {
        if (activeChapter >= PAGES.length) return;
        int pageCount = PAGES[activeChapter].length;

        int barY = panelY + CL_Y + CL_H + 6;
        int cx   = panelX + PANEL_W / 2;

        // Page indicator
        String indicator = (activePage + 1) + " / " + pageCount;
        g.drawCenteredString(font, Component.literal(indicator), cx, barY + 7, 0x556688);

        // Prev button
        boolean hasPrev = activePage > 0 || activeChapter > 0;
        renderNavBtn(g, mouseX, mouseY, panelX + CA_X, barY, "◀", hasPrev);

        // Next button
        boolean hasNext = activePage < pageCount - 1 || activeChapter < PAGES.length - 1;
        renderNavBtn(g, mouseX, mouseY, panelX + CA_X + CA_W - 36, barY, "▶", hasNext);

        // Back to watch button (bottom-right of chapter list area)
        int backX = panelX + CL_X;
        int backY = panelY + PANEL_H - 30;
        boolean backHov = mouseX >= backX && mouseX < backX + CL_W
                       && mouseY >= backY && mouseY < backY + 18;
        g.fill(backX, backY, backX + CL_W, backY + 18,
               backHov ? 0xCC152150 : 0xCC0D1830);
        g.fill(backX, backY, backX + CL_W, backY + 1, 0xFF334477);
        g.fill(backX, backY + 17, backX + CL_W, backY + 18, 0xFF334477);
        g.drawCenteredString(font,
                Component.translatable("gui.koniava.nara_guide.back"),
                backX + CL_W / 2, backY + 5, backHov ? 0xCCDDFF : 0x778899);
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
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return super.mouseClicked(mx, my, btn);

        // Chapter list clicks
        int entryH = 20;
        for (int i = 0; i < CHAPTER_KEYS.length; i++) {
            int ex = panelX + CL_X, ey = panelY + CL_Y + i * (entryH + 2);
            if (mx >= ex && mx < ex + CL_W && my >= ey && my < ey + entryH) {
                if (activeChapter != i) { activeChapter = i; activePage = 0; }
                return true;
            }
        }

        int pageCount = activeChapter < PAGES.length ? PAGES[activeChapter].length : 1;
        int barY = panelY + CL_Y + CL_H + 6;

        // Prev
        int prevX = panelX + CA_X;
        if (mx >= prevX && mx < prevX + 36 && my >= barY && my < barY + 18) {
            if (activePage > 0) {
                activePage--;
            } else if (activeChapter > 0) {
                activeChapter--;
                activePage = PAGES[activeChapter].length - 1;
            }
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
            return true;
        }

        // Back button
        int backX = panelX + CL_X, backY = panelY + PANEL_H - 30;
        if (mx >= backX && mx < backX + CL_W && my >= backY && my < backY + 18) {
            this.onClose();
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
