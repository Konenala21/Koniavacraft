package com.github.nalamodikk.common.screen.block.shared;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.button.TexturedButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 🎨 修正版動態升級界面 - 使用mana_gui_slot.png動態繪製
 *
 * 🤔 概念解釋：為什麼不拉伸背景？
 * - 你說得對！拉伸會讓玩家背包槽位變形
 * - 背景材質的所有元素都會被破壞
 * - 視覺效果會很醜
 *
 * 💡 設計理念：動態槽位繪製
 * - 🎯 使用原有背景：保持4槽位時的完美外觀
 * - 🎨 動態繪製槽位：使用mana_gui_slot.png畫出需要的槽位
 * - 📐 智能背景：超過4槽位時適當延伸背景
 * - 🔧 完美對齊：確保槽位和視覺完全匹配
 */
public class UpgradeScreen extends AbstractContainerScreen<UpgradeMenu> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeScreen.class);
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/upgrade_gui.png");
    private static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/mana_gui_slot.png");

    private TexturedButton upgradeButton;
    private final int upgradeSlotCount;
    private final boolean isExtended;

    public UpgradeScreen(UpgradeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.upgradeSlotCount = menu.getUpgradeSlotCount();
        this.isExtended = upgradeSlotCount > 4;

        // 🎯 設定GUI尺寸
        this.imageWidth = 176;
        this.imageHeight = menu.calculateGUIHeight();

    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        if (isExtended) {
            // 🎨 擴展模式：基礎背景 + 動態槽位
            renderExtendedBackground(guiGraphics);
        } else {
            // 🎯 標準模式：使用原始背景
            guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        }

        // 🔧 繪製升級槽位
        renderUpgradeSlots(guiGraphics);
    }

    /**
     * 🎨 渲染擴展背景
     * 保持玩家背包區域不變形
     */
    private void renderExtendedBackground(GuiGraphics guiGraphics) {
        // 🎯 策略：只延伸中間區域，保持頂部和底部完整

        // 1. 頂部區域（標題 + 原有升級區域）
        int topHeight = 75;
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, topHeight);

        // 2. 中間區域（擴展的升級區域）
        int middleStart = topPos + topHeight;
        int bottomStart = topPos + imageHeight - 90; // 玩家背包需要90像素

        if (bottomStart > middleStart) {
            // 🔧 填充中間區域（使用純色或簡單紋理）
            int middleHeight = bottomStart - middleStart;
            // 使用背景的中間部分重複填充
            for (int y = 0; y < middleHeight; y += 16) {
                int h = Math.min(16, middleHeight - y);
                guiGraphics.blit(TEXTURE, leftPos, middleStart + y, 0, 50, imageWidth, h);
            }
        }

        // 3. 底部區域（玩家背包，完整保留）
        int bottomHeight = 90;
        int sourceY = 166 - bottomHeight;
        guiGraphics.blit(TEXTURE, leftPos, topPos + imageHeight - bottomHeight,
                0, sourceY, imageWidth, bottomHeight);
    }

    /**
     * 🔧 動態繪製升級槽位
     * 使用mana_gui_slot.png繪製所有升級槽位
     */
    private void renderUpgradeSlots(GuiGraphics guiGraphics) {
        if (upgradeSlotCount <= 0) return;

        // 🎨 計算佈局（與Menu中的計算保持一致）
        int slotsPerRow = Math.min(4, upgradeSlotCount);
        int slotSpacing = 18;
        int totalWidth = slotsPerRow * slotSpacing - 2;
        int startX = (176 - totalWidth) / 2-1;
        int startY = 34;

        // 🔧 繪製每個槽位
        for (int i = 0; i < upgradeSlotCount; i++) {
            int row = i / slotsPerRow;
            int col = i % slotsPerRow;

            int x = startX + col * slotSpacing;
            int y = startY + row * slotSpacing;

            // 🎨 繪製槽位背景
            guiGraphics.blit(SLOT_TEXTURE, leftPos + x, topPos + y, 0, 0, 18, 18,18,18);
        }

    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 🎯 基本標題
        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);

        // 📊 擴展資訊（當槽位超過4個時）
        if (isExtended) {
            renderExtendedInfo(graphics);
        }
    }

    /**
     * 📊 渲染擴展資訊
     * 顯示槽位數量和提示
     */
    private void renderExtendedInfo(GuiGraphics graphics) {
        // 🔍 獲取機器名稱
        String machineName = getMachineName();

        // 📊 槽位資訊
        Component info = Component.translatable("screen.koniava.upgrade.machine_info",
                machineName, upgradeSlotCount);

        // 🎨 顯示位置（標題下方，小字體）
        float scale = 0.8f;
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);

        graphics.drawString(this.font, info,
                (int)(10 / scale), (int)(18 / scale), 0x606060, false);

        // 💡 提示文字（底部）
        if (upgradeSlotCount > 6) {
            Component hint = Component.translatable("screen.koniava.upgrade.extended_hint");
            graphics.drawString(this.font, hint,
                    (int)(10 / scale), (int)((imageHeight - 95) / scale), 0x808080, false);
        }

        graphics.pose().popPose();
    }

    /**
     * 🔍 獲取機器名稱
     * 從BlockEntity的本地化名稱獲取
     */
    private String getMachineName() {
        if (menu.getMachine() != null && menu.getMachine().getBlockEntity() != null) {
            BlockEntity be = menu.getMachine().getBlockEntity();
            // 🌐 使用方塊的本地化名稱
            String blockId = be.getBlockState().getBlock().getDescriptionId();
            return Component.translatable(blockId).getString();
        }
        return Component.translatable("screen.koniava.upgrade.unknown_machine").getString();
    }
}
