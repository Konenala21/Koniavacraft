package com.github.nalamodikk.common.screen.player;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.player.equipment.EquipmentType;
import com.github.nalamodikk.common.player.equipment.slot.SpecificEquipmentSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;

public class ExtraEquipmentScreen extends AbstractContainerScreen<ExtraEquipmentMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/extra_equipment.png");
        private static final int PLAYER_MODEL_X = 30;
        private static final int PLAYER_MODEL_Y = 54;
        private static final int PLAYER_MODEL_SIZE = 30;

        private static final int SLOT_UV_X = 235;
        private static final int SLOT_UV_Y = 1;
        private static final int SLOT_SIZE = 18;
        private static final int EXTRA_SLOT_BASE_X = 79;   // slot.x(80) - 1
        private static final int EXTRA_SLOT_BASE_Y = 23;   // slot.y(24) - 1
        private static final int EXTRA_SLOTS_PER_COL = 5;
        private static final int VANILLA_SLOT_BASE_X = 60; // slot.x(61) - 1
        private static final int VANILLA_SLOT_BASE_Y = 22; // slot.y(23) - 1
        private static final int VANILLA_SLOT_COUNT = 4;

        private float xMouse;
        private float yMouse;

        public ExtraEquipmentScreen(ExtraEquipmentMenu menu, Inventory playerInventory, Component title) {
            super(menu, playerInventory, title);
            this.imageWidth = 256;
            this.imageHeight = 256;
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
            graphics.blit(TEXTURE, leftPos, topPos, 0, 0, SLOT_UV_X - 1, imageHeight);

            for (int i = 0; i < VANILLA_SLOT_COUNT; i++) {
                graphics.blit(TEXTURE,
                        leftPos + VANILLA_SLOT_BASE_X,
                        topPos + VANILLA_SLOT_BASE_Y + i * SLOT_SIZE,
                        SLOT_UV_X, SLOT_UV_Y, SLOT_SIZE, SLOT_SIZE);
            }

            for (int i = 0; i < ExtraEquipmentMenu.EQUIPMENT_SLOT_COUNT; i++) {
                int col = i / EXTRA_SLOTS_PER_COL;
                int row = i % EXTRA_SLOTS_PER_COL;
                graphics.blit(TEXTURE,
                        leftPos + EXTRA_SLOT_BASE_X + col * SLOT_SIZE,
                        topPos + EXTRA_SLOT_BASE_Y + row * SLOT_SIZE,
                        SLOT_UV_X, SLOT_UV_Y, SLOT_SIZE, SLOT_SIZE);
            }

            renderPlayerModelWithEyeTracking(graphics,
                    leftPos + PLAYER_MODEL_X,
                    topPos + PLAYER_MODEL_Y,
                    PLAYER_MODEL_SIZE,
                    xMouse,
                    yMouse,
                    this.minecraft.player);
        }

        @Override
        protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
            graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
            graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0x404040, false);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.xMouse = (float)mouseX;
            this.yMouse = (float)mouseY;

            this.renderBackground(graphics, mouseX, mouseY, partialTick);
            super.render(graphics, mouseX, mouseY, partialTick);
            this.renderTooltip(graphics, mouseX, mouseY);
        }

        @Override
        protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
            if (hoveredSlot instanceof SpecificEquipmentSlot equipSlot && !equipSlot.hasItem()) {
                EquipmentType type = equipSlot.getAllowedType();
                graphics.renderComponentTooltip(this.font, List.of(
                        type.getDisplayName().copy().withStyle(ChatFormatting.WHITE),
                        Component.translatable("tooltip.koniava.equipment.slot_install_hint")
                                 .withStyle(ChatFormatting.GRAY)
                ), mouseX, mouseY);
                return;
            }
            super.renderTooltip(graphics, mouseX, mouseY);
        }


    /**
     * 🔥 進階版：只有眼睛跟隨滑鼠，身體保持靜止 🔥
     * 最自然的頭部追蹤效果，就像真人在看滑鼠位置
     */
    public static void renderPlayerModelWithEyeTracking(GuiGraphics graphics, int x, int y, int size, float mouseX, float mouseY, LivingEntity entity) {
        if (entity == null) return;

        // 調大裁剪區域的寬度
        int x1 = x - size;      // 左邊界往左擴大（原本是 size/2）
        int y1 = y - size;
        int x2 = x + size;      // 右邊界往右擴大（原本是 size/2）
        int y2 = y + size;

        // 開啟裁剪，防止人物渲染超出邊界
        graphics.enableScissor(x1, y1, x2, y2);

        // 使用原版方法渲染
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics,
                x1, y1, x2, y2,  // 渲染區域邊界
                size,            // 縮放大小
                0.0625F,         // Y偏移
                mouseX, mouseY,  // 滑鼠位置
                entity           // 實體
        );

        // 關閉裁剪
        graphics.disableScissor();
    }
}


