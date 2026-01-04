package com.github.nalamodikk.narasystem.nara.screen;

import com.github.nalamodikk.KoniavacraftMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class NaraIntroScreen extends Screen {
    private static final ResourceLocation CIRCLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/nara_circle.png");
    private static final int INTRO_DURATION = 80; // 播 4 秒（20 tick = 1 秒）
    private static final int TEXTURE_SIZE = 128;
    private static final float HALF_TEXTURE_SIZE = TEXTURE_SIZE / 2F;
    private static final float ROTATION_SPEED = 2F;
    private static final int BACKGROUND_COLOR = 0xFF000000;

    private final Minecraft minecraft = Minecraft.getInstance(); // 快取客戶端實例
    private int ticksElapsed = 0; // 已經播放的刻數
    private int centerX; // 螢幕中央 X 座標
    private int centerY; // 螢幕中央 Y 座標

    public NaraIntroScreen() {
        super(Component.empty());
    }

    /**
     * 初始化材質與版面配置。
     */
    @Override
    protected void init() {
        minecraft.getTextureManager().getTexture(CIRCLE_TEXTURE).setFilter(false, false);
        updateLayoutMetrics();
    }

    /**
     * 每個遊戲刻推進動畫並在播放結束後切換畫面。
     */
    @Override
    public void tick() {
        ticksElapsed++;
        if (ticksElapsed >= INTRO_DURATION) {
            minecraft.setScreen(new NaraInitScreen());
        }
    }

    /**
     * 依照目前刻數計算旋轉角度。
     *
     * @param partialTick 插值用的部分刻數
     * @return 旋轉角度（度數）
     */
    private float computeAngle(float partialTick) {
        return (ticksElapsed + partialTick) * ROTATION_SPEED;
    }

    /**
     * 更新螢幕中心座標快取。
     */
    private void updateLayoutMetrics() {
        centerX = this.width / 2;
        centerY = this.height / 2;
    }

    /**
     * 視窗尺寸改變時更新快取的座標資訊。
     */
    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        updateLayoutMetrics();
    }

    /**
     * 繪製動畫，包括背景遮罩與旋轉的圓形貼圖。
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 背景全黑
        graphics.setColor(1F, 1F, 1F, 1F); // 確保 alpha 正常（非必要，但推薦）

        graphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR); // 自己畫純黑遮罩

        float angle = computeAngle(partialTick);

        PoseStack pose = graphics.pose();
        pose.pushPose();

        pose.translate(centerX, centerY, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.translate(-HALF_TEXTURE_SIZE, -HALF_TEXTURE_SIZE, 0);

        graphics.blit(CIRCLE_TEXTURE, 0, 0, 0, 0, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);

        pose.popPose();

        super.render(graphics, mouseX, mouseY, partialTick);
    }


    /**
     * 覆寫背景繪製以防止基底類別套用模糊效果。
     */
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // ❌ 不要呼叫 super，不要呼叫 renderBlurredBackground
    }

    /**
     * 禁止使用 Esc 鍵提前關閉動畫。
     */
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
