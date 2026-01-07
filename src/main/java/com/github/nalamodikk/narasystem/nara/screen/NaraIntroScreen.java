package com.github.nalamodikk.narasystem.nara.screen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.TypewriterTextWidget;
import com.github.nalamodikk.client.screenAPI.framework.Panel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class NaraIntroScreen extends Screen {
    private static final ResourceLocation CIRCLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/nara_circle.png");
    private static final int INTRO_DURATION = 120; // 播 6 秒
    private static final int TEXTURE_SIZE = 128;
    private static final float HALF_TEXTURE_SIZE = TEXTURE_SIZE / 2F;
    private static final float ROTATION_SPEED = 2F;
    private static final int BACKGROUND_COLOR = 0xFF000000;

    private final Minecraft minecraft = Minecraft.getInstance();
    private int ticksElapsed = 0;
    private int centerX;
    private int centerY;

    private Panel rootPanel;
    private TypewriterTextWidget textWidget;

    public NaraIntroScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        minecraft.getTextureManager().getTexture(CIRCLE_TEXTURE).setFilter(false, false);
        updateLayoutMetrics();

        // 初始化 Widget 容器
        this.rootPanel = new Panel(0, 0, this.width, this.height);
        
        // 加入打字機文字 (置於螢幕下方)
        this.textWidget = new TypewriterTextWidget(centerX, centerY + 80, 
                Component.translatable("screen.koniava.nara.intro_welcome"), 2, 0x00FFCC, true);
        this.rootPanel.add(textWidget);
    }

    @Override
    public void tick() {
        ticksElapsed++;
        if (ticksElapsed >= INTRO_DURATION) {
            minecraft.setScreen(new NaraInitScreen());
        }
    }

    private float computeAngle(float partialTick) {
        return (ticksElapsed + partialTick) * ROTATION_SPEED;
    }

    private void updateLayoutMetrics() {
        centerX = this.width / 2;
        centerY = this.height / 2;
        if (rootPanel != null) {
            rootPanel.setSize(this.width, this.height);
        }
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        updateLayoutMetrics();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);

        float angle = computeAngle(partialTick);

        PoseStack pose = graphics.pose();
        pose.pushPose();

        pose.translate(centerX, centerY, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.translate(-HALF_TEXTURE_SIZE, -HALF_TEXTURE_SIZE, 0);

        graphics.blit(CIRCLE_TEXTURE, 0, 0, 0, 0, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);

        pose.popPose();

        // 繪製 Widgets
        if (rootPanel != null) {
            rootPanel.render(graphics, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
