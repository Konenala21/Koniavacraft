package com.github.nalamodikk.common.block.blockentity.mana_generator;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.BurnProgressWidget;
import com.github.nalamodikk.client.screenAPI.component.EnergyBarWidget;
import com.github.nalamodikk.client.screenAPI.component.ManaBarWidget;
import com.github.nalamodikk.client.screenAPI.framework.AbstractWidget;
import com.github.nalamodikk.client.screenAPI.framework.ButtonWidget;
import com.github.nalamodikk.client.screenAPI.framework.AutoSizedModularScreen;
import com.github.nalamodikk.client.screenAPI.framework.Panel;
import com.github.nalamodikk.common.network.packet.server.OpenUpgradeGuiPacket;
import com.github.nalamodikk.common.network.packet.server.manatool.ToggleModePacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * 🔥 魔力發電機 GUI 界面 (v2 - 自動尺寸)
 */
public class ManaGeneratorScreen extends AutoSizedModularScreen<ManaGeneratorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/mana_generator_gui.png");
    private static final ResourceLocation BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/mana_generator_button_texture.png");
    private static final ResourceLocation UPGRADE_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/upgrade_button.png");

    // 警告相關
    private boolean showWarning = false;
    private long warningStartTime = 0;

    public ManaGeneratorScreen(ManaGeneratorMenu menu, Inventory inv, Component title) {
        // ✨ v2: 自動檢測尺寸並繪製背景
        super(menu, inv, title, TEXTURE, 176, 166);
    }

    @Override
    protected void buildGui(Panel root) {
        // 1. 魔力條 (11, 19)
        root.add(new ManaBarWidget(11, 19, menu::getManaStored, menu::getMaxMana)
                .setSize(7, 47)
                .setDrawBackground(false)); // 關閉 Widget 背景

        // 2. 能量條 (156, 19)
        root.add(new EnergyBarWidget(156, 19, menu::getEnergyStored, menu::getMaxEnergy)
                .setSize(8, 47)
                .setDrawBackground(false)); // 關閉 Widget 背景

        // 3. 燃燒進度 (56, 36)
        root.add(new BurnProgressWidget(56, 36, menu::getBurnTime, menu::getCurrentBurnTime));

        // 4. 模式文字 (置中, y=15)
        root.add(new AbstractWidget(0, 15, imageWidth, 10) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int localX, int localY, int screenX, int screenY) {
                String modeText = menu.getCurrentMode() == 1
                        ? Component.translatable("mode.koniava.energy").getString()
                        : Component.translatable("mode.koniava.mana").getString();
                Component currentMode = Component.translatable("screen.koniava.current_mode", modeText);

                float scale = 0.85f;
                float originalX = (imageWidth - font.width(currentMode)) / 2f;
                
                graphics.pose().pushPose();
                graphics.pose().scale(scale, scale, scale);
                graphics.drawString(font, currentMode, (int)(originalX / scale), 0, 4210752, false);
                graphics.pose().popPose();
            }
        });

        // 5. 診斷資訊 (y=62)
        root.add(new AbstractWidget(0, 62, imageWidth, 20) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int localX, int localY, int screenX, int screenY) {
                if (!menu.hasDiagnosticDisplay()) return;

                boolean isManaMode = menu.getCurrentMode() == 0;
                int rate = isManaMode ? menu.getManaRate() : menu.getEnergyRate();
                long totalOutput = (long) rate * menu.getCurrentBurnTime();

                Component rateText, yieldText;
                if (menu.isWorking()) {
                    String unit = isManaMode ? "Mana/t" : "RF/t";
                    rateText = Component.translatable("gui.koniava.rate", String.format("%d %s", rate, unit));
                    String totalUnit = isManaMode ? "Mana" : "RF";
                    yieldText = Component.translatable("gui.koniava.total_yield", String.format("%,d %s", totalOutput, totalUnit));
                } else {
                    rateText = Component.translatable("gui.koniava.rate", "N/A");
                    yieldText = Component.translatable("gui.koniava.total_yield", "N/A");
                }

                float scale = 0.8f;
                int color = 0x404040;
                
                graphics.pose().pushPose();
                graphics.pose().scale(scale, scale, scale);
                
                float centerX = (imageWidth / 2f) / scale;
                graphics.drawCenteredString(font, rateText, (int) centerX, 0, color);
                graphics.drawCenteredString(font, yieldText, (int) centerX, (int)(10 / scale), color);
                
                graphics.pose().popPose();
            }
        });

        // 6. 模式切換按鈕 (130, 25)
        root.add(new ButtonWidget(130, 25, 20, 20, BUTTON_TEXTURE, 20, 20, btn -> {
            if (this.menu.getBurnTime() > 0) {
                KoniavacraftMod.LOGGER.debug("Cannot toggle mana generator mode while it is running.");
                showWarning = true;
                warningStartTime = System.currentTimeMillis();
                return;
            }
            BlockPos blockPos = this.menu.getBlockEntityPos();
            ToggleModePacket.sendToServer(blockPos);
        }).setTooltip(() -> List.of(Component.translatable("screen.koniava.toggle_mode"))));

        // 7. 升級按鈕 (150, 5)
        root.add(new ButtonWidget(150, 5, 18, 18, UPGRADE_BUTTON_TEXTURE, 18, 18, btn -> {
            BlockPos pos = this.menu.getBlockEntityPos();
            OpenUpgradeGuiPacket.sendToServer(pos);
        }).setTooltip(() -> List.of(Component.translatable("screen.koniava.upgrade_button.tooltip"))));
    }

    // ✨ v2: renderBg 完全刪除，AutoSizedModularScreen 會自動處理

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // 震動警告特效
        if (showWarning && System.currentTimeMillis() - warningStartTime < 3000) {
            renderWarning(graphics);
        }
    }

    private void renderWarning(GuiGraphics graphics) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        float scale = 0.8f;
        poseStack.scale(scale, scale, scale);

        long time = System.currentTimeMillis();
        double shakeFactor = Math.sin(time * 0.015) * Math.cos(time * 0.025);
        int shakeX = (int) (shakeFactor * 3);
        int shakeY = (int) (shakeFactor * 2);

        int warningX = (int) ((this.leftPos + this.imageWidth / 2) / scale) + shakeX;
        int warningY = (int) ((this.topPos + 65) / scale) + shakeY;

        graphics.drawCenteredString(font, Component.translatable("screen.koniava.cannot_toggle")
                .withStyle(ChatFormatting.RED), warningX, warningY, 0xFF0000);

        poseStack.popPose();
    }
}
