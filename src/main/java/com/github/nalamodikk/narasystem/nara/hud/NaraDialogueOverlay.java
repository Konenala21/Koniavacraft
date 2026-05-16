package com.github.nalamodikk.narasystem.nara.hud;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraft.network.chat.Component;

import java.util.List;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class NaraDialogueOverlay {

    // 立繪路徑（等使用者提供圖片）
    private static final ResourceLocation PORTRAIT = ResourceLocation.fromNamespaceAndPath(
            KoniavacraftMod.MOD_ID, "textures/gui/nara/konenala21.png");
    private static final ResourceLocation PORTRAIT_MAD = ResourceLocation.fromNamespaceAndPath(
            KoniavacraftMod.MOD_ID, "textures/gui/nara/konenala21_mad.png");

    // 顏色常數
    private static final int COLOR_BLUE_ARROW = 0xFF5599FF;
    private static final int COLOR_TEAL       = 0xFF00FFCC;
    private static final int COLOR_TEAL_DIM   = 0x8800FFCC;
    private static final int COLOR_BG         = 0xCC0A0A0F;
    private static final int COLOR_CHOICE_BG  = 0xAA001A14;
    private static final int COLOR_WHITE      = 0xFFFFFFFF;
    private static final int COLOR_GREY       = 0xFFAAAAAA;

    // HUD 尺寸
    private static final int PORTRAIT_W  = 72;
    private static final int PORTRAIT_H  = 108;
    private static final int NAME_H      = 18;
    private static final int BOX_W       = 280;
    private static final int BOX_H       = PORTRAIT_H + NAME_H;
    private static final int CHOICE_H    = 18;
    private static final int PADDING     = 8;
    private static final int MARGIN      = 16;

    // 游標閃爍
    private static int cursorTick = 0;
    private static boolean cursorVisible = true;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!NaraDialogueManager.isActive()) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        int key = event.getKey();

        // F 鍵確認選項或推進對話，同時清空副手切換的 click queue
        if (key == GLFW.GLFW_KEY_F) {
            if (NaraDialogueManager.getDialogueState() == NaraDialogueManager.DialogueState.CHOOSING) {
                NaraDialogueManager.confirmSelectedChoice();
            } else {
                NaraDialogueManager.onPlayerClick();
            }
            // 清掉 vanilla 副手換手 keybind 的 pending clicks
            Minecraft mc = Minecraft.getInstance();
            while (mc.options.keySwapOffhand.consumeClick()) {}
            return;
        }

        // 數字鍵 1-9 直接選對應選項
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            NaraDialogueManager.selectChoice(key - GLFW.GLFW_KEY_1);
            return;
        }

        // Enter 推進對話
        if (key == GLFW.GLFW_KEY_ENTER) {
            NaraDialogueManager.onPlayerClick();
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!NaraDialogueManager.isActive()) return;
        if (NaraDialogueManager.getDialogueState() != NaraDialogueManager.DialogueState.CHOOSING) return;
        // 向上滾 = -1，向下滾 = +1
        int delta = event.getScrollDeltaY() > 0 ? -1 : 1;
        NaraDialogueManager.scrollChoice(delta);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        // 玩家死亡時暫停對話，等重生後由 server 重新觸發
        if (mc.player != null && mc.player.isDeadOrDying()) return;
        NaraDialogueManager.tick();
        cursorTick++;
        if (cursorTick >= 10) { cursorTick = 0; cursorVisible = !cursorVisible; }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!NaraDialogueManager.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;

        GuiGraphics g = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        int totalW = PORTRAIT_W + PADDING + BOX_W;
        int hudX = (sw - totalW) / 2;
        int hudY = (sh - BOX_H) / 2;

        renderPortraitArea(g, hudX, hudY);
        renderTextBox(g, mc, hudX + PORTRAIT_W + PADDING, hudY);
        renderChoices(g, mc, hudX, hudY + BOX_H + 4, sw);
    }

    private static void renderPortraitArea(GuiGraphics g, int x, int y) {
        // 名牌背景
        g.fill(x, y, x + PORTRAIT_W, y + NAME_H, COLOR_BG);
        drawBorder(g, x, y, PORTRAIT_W, NAME_H, COLOR_TEAL_DIM);

        // 名字
        Minecraft mc = Minecraft.getInstance();
        boolean nameRevealed = NaraDialogueManager.getPortraitState() != NaraDialogueManager.PortraitState.HIDDEN;
        int nameColor = nameRevealed ? COLOR_TEAL : COLOR_GREY;
        g.drawCenteredString(mc.font, NaraDialogueManager.getDisplayName(), x + PORTRAIT_W / 2, y + 3, nameColor);

        int pY = y + NAME_H;

        // 立繪背景
        g.fill(x, pY, x + PORTRAIT_W, pY + PORTRAIT_H, COLOR_BG);
        drawBorder(g, x, pY, PORTRAIT_W, PORTRAIT_H, COLOR_TEAL_DIM);

        // 立繪渲染
        ResourceLocation currentPortrait = NaraDialogueManager.isPortraitMad() ? PORTRAIT_MAD : PORTRAIT;
        NaraDialogueManager.PortraitState ps = NaraDialogueManager.getPortraitState();
        if (ps == NaraDialogueManager.PortraitState.HIDDEN) {
            // 純黑剪影
            g.setColor(0f, 0f, 0f, 1f);
            g.blit(currentPortrait, x, pY, 0, 0, PORTRAIT_W, PORTRAIT_H, PORTRAIT_W, PORTRAIT_H);
            g.setColor(1f, 1f, 1f, 1f);
        } else if (ps == NaraDialogueManager.PortraitState.REVEALING) {
            // 漸變從黑到彩色
            float progress = (float) NaraDialogueManager.getRevealTicks() / NaraDialogueManager.getRevealDuration();
            g.setColor(progress, progress, progress, 1f);
            g.blit(currentPortrait, x, pY, 0, 0, PORTRAIT_W, PORTRAIT_H, PORTRAIT_W, PORTRAIT_H);
            g.setColor(1f, 1f, 1f, 1f);
        } else {
            // 完整顯示
            g.blit(currentPortrait, x, pY, 0, 0, PORTRAIT_W, PORTRAIT_H, PORTRAIT_W, PORTRAIT_H);
        }

        // 脈衝外框（SHOWN 時亮邊）
        if (ps == NaraDialogueManager.PortraitState.SHOWN) {
            float pulse = (float)(0.6 + 0.4 * Math.sin(System.currentTimeMillis() / 500.0));
            int alpha = (int)(pulse * 255) << 24;
            int glowColor = (alpha & 0xFF000000) | (COLOR_TEAL & 0x00FFFFFF);
            drawBorder(g, x, pY, PORTRAIT_W, PORTRAIT_H, glowColor);
        }
    }

    private static void renderTextBox(GuiGraphics g, Minecraft mc, int x, int y) {
        int w = BOX_W;
        int h = BOX_H;

        // 背景
        g.fill(x, y, x + w, y + h, COLOR_BG);
        drawBorder(g, x, y, w, h, COLOR_TEAL_DIM);

        // 文字
        String shown = NaraDialogueManager.getShownText();
        boolean typing = NaraDialogueManager.getDialogueState() == NaraDialogueManager.DialogueState.TYPING;
        String display = shown + (typing && cursorVisible ? "▌" : "");

        int textX = x + PADDING;
        int textY = y + PADDING;
        int maxWidth = w - PADDING * 2;

        // 多行自動換行
        mc.font.getSplitter().splitLines(display, maxWidth, net.minecraft.network.chat.Style.EMPTY)
                .forEach(line -> {});

        for (var line : mc.font.split(net.minecraft.network.chat.Component.literal(display), maxWidth)) {
            g.drawString(mc.font, line, textX, textY, COLOR_WHITE, false);
            textY += mc.font.lineHeight + 2;
        }

        // 操作提示（每行都顯示）
        NaraDialogueManager.DialogueState state = NaraDialogueManager.getDialogueState();
        if (state == NaraDialogueManager.DialogueState.WAITING) {
            g.drawString(mc.font, Component.translatable("nara.hud.hint.continue"), x + PADDING, y + h - mc.font.lineHeight - 4, COLOR_TEAL, true);
            if (cursorVisible) g.drawString(mc.font, "▶", x + w - 12, y + h - 12, COLOR_TEAL, false);
        } else if (state == NaraDialogueManager.DialogueState.TYPING) {
            g.drawString(mc.font, Component.translatable("nara.hud.hint.skip"), x + PADDING, y + h - mc.font.lineHeight - 4, COLOR_GREY, true);
        }
    }

    private static void renderChoices(GuiGraphics g, Minecraft mc, int x, int y, int screenWidth) {
        if (NaraDialogueManager.getDialogueState() != NaraDialogueManager.DialogueState.CHOOSING) return;

        List<NaraChoice> choices = NaraDialogueManager.getCurrentChoices();
        if (choices.isEmpty()) return;

        // 倒數進度條
        int timeout = NaraDialogueManager.getChoiceTimeoutTicks();
        if (timeout > 0) {
            int elapsed = NaraDialogueManager.getChoiceTimerTicks();
            float progress = 1f - (float) elapsed / timeout;
            int barW = PORTRAIT_W + PADDING + BOX_W;
            int barH = 3;
            g.fill(x, y, x + barW, y + barH, 0x44FFFFFF);
            g.fill(x, y, x + (int)(barW * progress), y + barH, COLOR_TEAL);
            y += barH + 2;
        }

        // 操作提示（加陰影提高可讀性）
        g.drawString(mc.font, Component.translatable("nara.hud.hint.choose"), x, y, COLOR_TEAL, true);
        y += mc.font.lineHeight + 3;

        // 選項按鈕
        int selected = NaraDialogueManager.getSelectedChoiceIndex();
        for (int i = 0; i < choices.size(); i++) {
            NaraChoice choice = choices.get(i);
            int btnW = PORTRAIT_W + PADDING + BOX_W;
            int btnH = CHOICE_H;
            boolean isSelected = (i == selected);

            g.fill(x, y, x + btnW, y + btnH, isSelected ? 0xCC001A30 : COLOR_CHOICE_BG);
            drawBorder(g, x, y, btnW, btnH, isSelected ? COLOR_BLUE_ARROW : COLOR_TEAL_DIM);

            // 藍色箭頭（當前游標）
            if (isSelected) {
                g.drawString(mc.font, "▶", x + PADDING, y + 3, COLOR_BLUE_ARROW, false);
            }

            g.drawString(mc.font, "[" + (i + 1) + "] " + choice.label().getString(),
                    x + PADDING + (isSelected ? 10 : 0), y + 3, COLOR_WHITE, false);
            y += btnH + 2;
        }
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x,         y,         x + w,     y + 1,     color); // top
        g.fill(x,         y + h - 1, x + w,     y + h,     color); // bottom
        g.fill(x,         y,         x + 1,     y + h,     color); // left
        g.fill(x + w - 1, y,         x + w,     y + h,     color); // right
    }
}
