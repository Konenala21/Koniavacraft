package com.github.nalamodikk.narasystem.nara.screen;


import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.button.TooltipButton;
import com.github.nalamodikk.narasystem.nara.network.server.NaraBindRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import java.util.Collections;
import java.util.List;

public class NaraInitScreen extends Screen {

    private enum Stage {
        SHOWING_LINES, AWAITING_CONFIRM
    }

    private static final ResourceLocation OVERLAY_TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/nara_overlay.png");
    private static final ResourceLocation BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/nara_button.png");
    private static final Component TITLE = Component.translatable("screen.koniava.nara.title");
    private static final Component BIND_LABEL = Component.translatable("screen.koniava.nara.bind");
    private static final Component CANCEL_LABEL = Component.translatable("screen.koniava.nara.cancel");
    private static final List<Component> BIND_TOOLTIP = Collections.singletonList(Component.translatable("tooltip.koniava.nara.bind"));
    private static final List<Component> CANCEL_TOOLTIP = Collections.singletonList(Component.translatable("tooltip.koniava.nara.cancel"));
    private static final Component[] LINES = createLines();

    private static final int BACKGROUND_WIDTH = 256;
    private static final int BACKGROUND_HEIGHT = 190;
    private static final int BUTTON_WIDTH = 90;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_LEFT_OFFSET = 100;
    private static final int BUTTON_RIGHT_OFFSET = 10;
    private static final int BUTTON_VERTICAL_OFFSET = 60;
    private static final int LINE_REVEAL_INTERVAL = 10;
    private static final int BUTTON_EXTRA_DELAY = 20;
    private static final int LINE_SPACING = 12;
    private static final int TITLE_OFFSET_Y = -50;
    private static final int LINE_START_OFFSET = 20;
    private static final int BUTTON_APPEAR_TICK = LINES.length * LINE_REVEAL_INTERVAL + BUTTON_EXTRA_DELAY;

    private final Minecraft minecraft = Minecraft.getInstance(); // 快取客戶端實例以避免重複查詢
    private Stage currentStage = Stage.SHOWING_LINES; // 當前動畫階段
    private int visibleLines = 0; // 已經顯示的敘述行數
    private int ticksElapsed = 0; // 已流逝的遊戲刻數
    private int centerX; // 中央 X 座標快取
    private int centerY; // 中央 Y 座標快取
    private int backgroundX; // 背景圖左上 X
    private int backgroundY; // 背景圖左上 Y
    private int titleY; // 標題 Y 座標
    private int lineStartY; // 文字起始 Y 座標

    private TooltipButton bindButton; // 綁定按鈕實例
    private TooltipButton cancelButton; // 取消按鈕實例
    private boolean buttonsInitialized; // 是否已經建立按鈕

    public NaraInitScreen() {
        super(Component.empty());
    }


    /**
     * 每個遊戲刻更新動畫進度並在適當時機載入互動按鈕。
     */
    @Override
    public void tick() {
        super.tick();
        ticksElapsed++;

        if (currentStage == Stage.SHOWING_LINES) {
            if (visibleLines < LINES.length && ticksElapsed % LINE_REVEAL_INTERVAL == 0) {
                visibleLines++;
            }
            if (visibleLines == LINES.length && ticksElapsed >= BUTTON_APPEAR_TICK) {
                currentStage = Stage.AWAITING_CONFIRM;
                ensureButtonsReady(); // 延後加入按鈕
            }
        }
    }

    /**
     * 確保按鈕已建立並與最新的版面配置同步。
     */
    private void ensureButtonsReady() {
        if (!buttonsInitialized) {
            bindButton = new TooltipButton(
                    0, 0, BUTTON_WIDTH, BUTTON_HEIGHT,
                    BIND_LABEL,
                    BUTTON_TEXTURE, BUTTON_WIDTH, BUTTON_HEIGHT,
                    btn -> {
                        NaraBindRequestPacket.send(true);
                        onClose();
                    },
                    () -> BIND_TOOLTIP
            );

            cancelButton = new TooltipButton(
                    0, 0, BUTTON_WIDTH, BUTTON_HEIGHT,
                    CANCEL_LABEL,
                    BUTTON_TEXTURE, BUTTON_WIDTH, BUTTON_HEIGHT,
                    btn -> {
                        NaraBindRequestPacket.send(false);
                        onClose();
                        var connection = minecraft.getConnection();
                        if (connection != null) {
                            connection.disconnect(Component.translatable("message.koniava.nara.disconnect_message"));
                        }
                    },
                    () -> CANCEL_TOOLTIP
            );

            buttonsInitialized = true;
        }

        updateButtonPositions();
        attachButtonIfMissing(bindButton);
        attachButtonIfMissing(cancelButton);
    }

    /**
     * 更新按鈕的螢幕座標。
     */
    private void updateButtonPositions() {
        if (bindButton != null) {
            bindButton.setPosition(centerX - BUTTON_LEFT_OFFSET, centerY + BUTTON_VERTICAL_OFFSET);
        }
        if (cancelButton != null) {
            cancelButton.setPosition(centerX + BUTTON_RIGHT_OFFSET, centerY + BUTTON_VERTICAL_OFFSET);
        }
    }

    /**
     * 在必要時將按鈕加入畫面避免重複建立。
     *
     * @param button 欲加到畫面的按鈕
     */
    private void attachButtonIfMissing(TooltipButton button) {
        if (button != null && !this.renderables.contains(button)) {
            addRenderableWidget(button);
        }
    }

    /**
     * 建立逐行敘述用的多語系文字陣列。
     *
     * @return 文字陣列
     */
    private static Component[] createLines() {
        String modVersion = "unknown";
        var modFile = ModList.get().getModFileById(KoniavacraftMod.MOD_ID);
        if (modFile != null && !modFile.getMods().isEmpty()) {
            modVersion = modFile.getMods().getFirst().getVersion().toString();
        }
        return new Component[]{
                Component.translatable("screen.koniava.nara.line1"),
                Component.translatable("screen.koniava.nara.line2"),
                Component.translatable("screen.koniava.nara.line3"),
                Component.translatable("screen.koniava.nara.line4"),
                Component.translatable("screen.koniava.nara.line5", modVersion),
                Component.translatable("screen.koniava.nara.line6")
        };
    }

    /**
     * 更新快取的版面配置，避免在每幀重複計算。
     */
    private void updateLayoutMetrics() {
        centerX = this.width / 2;
        centerY = this.height / 2;
        backgroundX = centerX - BACKGROUND_WIDTH / 2;
        backgroundY = centerY - BACKGROUND_HEIGHT / 2;
        titleY = centerY + TITLE_OFFSET_Y;
        lineStartY = titleY + LINE_START_OFFSET;
        updateButtonPositions();
    }

    /**
     * 初始化渲染所需的材質與版面數值。
     */
    @Override
    protected void init() {
        minecraft.getTextureManager()
                .getTexture(OVERLAY_TEXTURE)
                .setFilter(false, false); // 關閉 linear filtering
        updateLayoutMetrics();
        if (buttonsInitialized && currentStage == Stage.AWAITING_CONFIRM) {
            ensureButtonsReady();
        }
    }

    /**
     * 覆寫背景繪製以避免不必要的模糊效果。
     */
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // ❌ 不要呼叫 super，不要呼叫 renderBlurredBackground
    }

    /**
     * 覆寫視窗調整事件，重建按鈕與版面配置。
     */
    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        this.rebuildWidgets();
        updateLayoutMetrics();
        if (buttonsInitialized && currentStage == Stage.AWAITING_CONFIRM) {
            ensureButtonsReady();
        }
    }


    /**
     * 繪製畫面上的背景、文字與互動元素。
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.setColor(1F, 1F, 1F, 1F); // 保證透明正常

        // 背景遮罩
        graphics.fill(0, 0, this.width, this.height, 0xFF000000); // 自己畫純黑遮罩

        // 背景貼圖
        graphics.blit(OVERLAY_TEXTURE, backgroundX, backgroundY, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);

        // 文字
        graphics.drawCenteredString(this.font, TITLE, centerX, titleY, 0xFFFFFF);
        for (int i = 0; i < visibleLines; i++) {
            graphics.drawCenteredString(this.font, LINES[i], centerX, lineStartY + i * LINE_SPACING, 0xAAAAAA);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }


    /**
     * 禁止使用 Esc 鍵直接關閉畫面。
     */
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    /**
     * 關閉畫面時回到遊戲主畫面。
     */
    @Override
    public void onClose() {
        minecraft.setScreen(null);
    }
}
