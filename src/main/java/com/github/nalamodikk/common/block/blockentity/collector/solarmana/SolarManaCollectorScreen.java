package com.github.nalamodikk.common.block.blockentity.collector.solarmana;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.ImageWidget;
import com.github.nalamodikk.client.screenAPI.component.ManaBarWidget;
import com.github.nalamodikk.client.screenAPI.component.ResearchLockWidget;
import com.github.nalamodikk.client.screenAPI.framework.AbstractWidget;
import com.github.nalamodikk.client.screenAPI.framework.ButtonWidget;
import com.github.nalamodikk.client.screenAPI.framework.AutoSizedModularScreen;
import com.github.nalamodikk.client.screenAPI.framework.Panel;
import com.github.nalamodikk.common.network.packet.server.OpenUpgradeGuiPacket;
import com.github.nalamodikk.register.client.ModKeyMappings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * 🔆 太陽能魔力收集器 GUI 界面 (v2 - 自動尺寸)
 */
public class SolarManaCollectorScreen extends AutoSizedModularScreen<SolarManaCollectorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/solar_mana_collector_gui.png");
    private static final ResourceLocation WIDGET_TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/upgrade_button.png");
    private boolean debugKeyHeld = false;

    public SolarManaCollectorScreen(SolarManaCollectorMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        // ✨ v2: 自動檢測尺寸並繪製背景
        super(pMenu, pPlayerInventory, pTitle, TEXTURE, 176, 166);
    }

    @Override
    protected void buildGui(Panel root) {
        // 0. 研究鎖定遮罩
        root.add(new ResearchLockWidget(0, 0, imageWidth, imageHeight, "solar_mana_collector"));

        // 1. 魔力條 (11, 19)
        root.add(new ManaBarWidget(11, 19, menu::getManaStored, menu::getMaxMana)
                .setSize(7, 47)
                .setDrawBackground(false)); // 關閉 Widget 背景

        // 2. 太陽圖示 (69, 24)
        root.add(new ImageWidget(69, 24, 39, 38, TEXTURE, 176, 2) {
            @Override
            public List<Component> getTooltip() {
                boolean isDaytime = menu.isDaytime();
                boolean isGenerating = menu.isGenerating();

                if (isGenerating) {
                    List<Component> lines = new java.util.ArrayList<>();
                    lines.add(Component.translatable("tooltip.koniava.solar.generating"));
                    lines.add(Component.translatable("tooltip.koniava.debug.hint",
                            ModKeyMappings.DEBUG_DETAILS.getTranslatedKeyMessage()));
                    appendDebugLines(lines);
                    return lines;
                } else if (!isDaytime) {
                    List<Component> lines = new java.util.ArrayList<>();
                    lines.add(Component.translatable("tooltip.koniava.solar.nighttime"));
                    lines.add(Component.translatable("tooltip.koniava.debug.hint",
                            ModKeyMappings.DEBUG_DETAILS.getTranslatedKeyMessage()));
                    appendDebugLines(lines);
                    return lines;
                } else {
                    List<Component> lines = new java.util.ArrayList<>();
                    lines.add(Component.translatable("tooltip.koniava.solar.blocked"));
                    if (!menu.hasSkyLight()) {
                        lines.add(Component.translatable("tooltip.koniava.solar.reason.no_sky_light"));
                    }
                    if (!menu.isOpenToSky()) {
                        lines.add(Component.translatable("tooltip.koniava.solar.reason.blocked"));
                    }
                    if (menu.isRaining()) {
                        lines.add(Component.translatable("tooltip.koniava.solar.reason.raining"));
                    }
                    if (menu.isThundering()) {
                        lines.add(Component.translatable("tooltip.koniava.solar.reason.thundering"));
                    }
                    if (lines.size() == 1) {
                        lines.add(Component.translatable("tooltip.koniava.solar.reason.unknown"));
                    }
                    lines.add(Component.translatable("tooltip.koniava.debug.hint",
                            ModKeyMappings.DEBUG_DETAILS.getTranslatedKeyMessage()));
                    appendDebugLines(lines);
                    return lines;
                }
            }

            private void appendDebugLines(List<Component> lines) {
                if (!isDebugKeyDown()) {
                    return;
                }
                lines.add(Component.translatable("tooltip.koniava.solar.debug.daytime",
                        boolText(menu.isDaytime())));
                lines.add(Component.translatable("tooltip.koniava.solar.debug.overworld",
                        boolText(menu.isOverworld())));
                lines.add(Component.translatable("tooltip.koniava.solar.debug.skylight",
                        boolText(menu.hasSkyLight())));
                lines.add(Component.translatable("tooltip.koniava.solar.debug.open_to_sky",
                        boolText(menu.isOpenToSky())));
                lines.add(Component.translatable("tooltip.koniava.solar.debug.raining",
                        boolText(menu.isRaining())));
                lines.add(Component.translatable("tooltip.koniava.solar.debug.thundering",
                        boolText(menu.isThundering())));
            }

            private boolean isDebugKeyDown() {
                if (debugKeyHeld) {
                    return true;
                }
                if (ModKeyMappings.DEBUG_DETAILS.isDown()) {
                    return true;
                }
                long window = net.minecraft.client.Minecraft.getInstance().getWindow().getWindow();
                com.mojang.blaze3d.platform.InputConstants.Key key = ModKeyMappings.DEBUG_DETAILS.getKey();
                if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM) {
                    if (com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, key.getValue())) {
                        return true;
                    }
                } else if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE) {
                    if (org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, key.getValue()) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                        return true;
                    }
                }
                if (org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                    return true;
                }
                if (org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                    return true;
                }
                return net.minecraft.client.gui.screens.Screen.hasShiftDown();
            }

            private Component boolText(boolean value) {
                return Component.translatable(value
                        ? "tooltip.koniava.boolean.true"
                        : "tooltip.koniava.boolean.false");
            }

            @Override
            protected void renderWidget(GuiGraphics graphics, int localX, int localY, int mouseX, int mouseY) {
                if (!menu.isGenerating()) {
                    this.setColor(0.5f, 0.5f, 0.5f, 1.0f);
                } else {
                    this.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                }
                super.renderWidget(graphics, localX, localY, mouseX, mouseY);
            }
        });

        // 3. 升級資訊文字 (22, 20)
        root.add(new AbstractWidget(22, 20, 100, 20) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int localX, int localY, int screenX, int screenY) {
                int speedLevel = menu.getSpeedLevel();
                int efficiencyLevel = menu.getEfficiencyLevel();

                int speedColor = speedLevel > 0 ? 0xFFFFFF : 0x666666;
                int effColor = efficiencyLevel > 0 ? 0xFFFFFF : 0x666666;

                Component speedLabel = Component.translatable("screen.koniava.upgrade.speed", speedLevel);
                Component efficiencyLabel = Component.translatable("screen.koniava.upgrade.efficiency", efficiencyLevel);

                float scale = 0.8f;
                graphics.pose().pushPose();
                graphics.pose().scale(scale, scale, 1.0f);
                
                graphics.drawString(font, speedLabel, 0, 0, speedColor, false);
                graphics.drawString(font, efficiencyLabel, 0, (int)(10 / scale), effColor, false);

                graphics.pose().popPose();
            }
        });
        
        // 4. 升級按鈕 (150, 5)
        root.add(new ButtonWidget(150, 5, 18, 18, WIDGET_TEXTURE, 18, 18, btn -> {
            BlockPos pos = this.menu.getBlockEntity().getBlockPos();
            OpenUpgradeGuiPacket.sendToServer(pos);
        }).setTooltip(() -> List.of(
            Component.translatable("screen.koniava.upgrade_button.tooltip")
        )));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isDebugKeyCode(keyCode)) {
            debugKeyHeld = true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (isDebugKeyCode(keyCode)) {
            debugKeyHeld = false;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private boolean isDebugKeyCode(int keyCode) {
        com.mojang.blaze3d.platform.InputConstants.Key key = ModKeyMappings.DEBUG_DETAILS.getKey();
        if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM && key.getValue() == keyCode) {
            return true;
        }
        return keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
    }

    // ✨ v2: renderBg 完全刪除，AutoSizedModularScreen 會自動處理
}
