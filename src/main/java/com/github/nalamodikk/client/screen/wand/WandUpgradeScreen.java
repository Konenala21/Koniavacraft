package com.github.nalamodikk.client.screen.wand;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.wand.WandCoreData;
import com.github.nalamodikk.common.item.wand.WandRodItem;
import com.github.nalamodikk.common.item.wand.core.IWandCore;
import com.github.nalamodikk.common.item.wand.upgrade.IWandUpgrade;
import com.github.nalamodikk.common.network.packet.server.wand.WandCoreSwapPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.math.Axis;

import java.util.ArrayList;
import java.util.List;

public class WandUpgradeScreen extends Screen {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/wand_upgrade_gui.png");
    private static final int TEXTURE_W = 512;
    private static final int TEXTURE_H = 512;

    // 整張背景（0,0 起，涵蓋三個區域）
    private static final int BG_W = 473, BG_H = 210;

    // 三個區域在貼圖裡的 x 起點（用來計算螢幕位置）
    private static final int LEFT_X  = 5;
    private static final int LEFT_W  = 93;   // 5→98
    private static final int CTR_X   = 99;
    private static final int CTR_W   = 241;  // 99→340 (3D 預覽)
    private static final int RIGHT_X = 341;
    private static final int RIGHT_W = 132;  // 341→473

    // 槽格 UV
    private static final int SLOT_UV_X = 485, SLOT_UV_Y = 5, SLOT_SIZE = 24;

    // 右列表面板顯示高度（在 RIGHT 面板內）
    private static final int LIST_ITEM_H = 20;

    private final InteractionHand hand;

    // -1 = none, -2 = core slot, 0-3 = upgrade slots
    private int selectedWandSlot = -1;

    // 3D 預覽旋轉 + 縮放狀態
    private float previewRotY = 30f;
    private float previewRotX = -15f;
    private float previewScale = 48f;
    private boolean draggingPreview = false;

    public WandUpgradeScreen(InteractionHand hand) {
        super(Component.translatable("screen.koniava.wand_upgrade"));
        this.hand = hand;
    }

    private ItemStack wand() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null ? mc.player.getItemInHand(hand) : ItemStack.EMPTY;
    }

    // ── 座標計算 ──────────────────────────────────────────────────────────

    private int startX() { return width  / 2 - BG_W / 2; }
    private int startY() { return height / 2 - BG_H / 2; }
    private int leftX()  { return startX() + LEFT_X; }
    private int ctrX()   { return startX() + CTR_X; }
    private int rightX() { return startX() + RIGHT_X; }

    // ── Render ────────────────────────────────────────────────────────────

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x55000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        int sy = startY();

        // 整張背景一次 blit
        g.blit(TEXTURE, startX(), sy, 0, 0, BG_W, BG_H, TEXTURE_W, TEXTURE_H);

        render3DWand(g);
        renderLeftPanel(g, leftX(), sy, mouseX, mouseY);
        renderRightPanel(g, rightX(), sy, mouseX, mouseY);
    }

    // ── 中間：3D 模型 ─────────────────────────────────────────────────────

    private void render3DWand(GuiGraphics g) {
        int cx = ctrX() + CTR_W / 2;
        int cy = startY() + BG_H / 2;

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 200);
        pose.scale(previewScale, -previewScale, previewScale);
        pose.mulPose(Axis.YP.rotationDegrees(previewRotY));
        pose.mulPose(Axis.XP.rotationDegrees(previewRotX));

        Minecraft mc = Minecraft.getInstance();
        mc.getItemRenderer().renderStatic(
                wand(),
                ItemDisplayContext.FIXED,
                15728880,
                OverlayTexture.NO_OVERLAY,
                pose,
                mc.renderBuffers().bufferSource(),
                mc.level,
                0);
        mc.renderBuffers().bufferSource().endBatch();
        pose.popPose();
    }

    // ── 左側：槽位面板 ────────────────────────────────────────────────────

    private void renderLeftPanel(GuiGraphics g, int x, int y, int mx, int my) {
        WandCoreData data = WandRodItem.getData(wand());

        // 核心槽（置中）
        int coreX = x + LEFT_W / 2 - SLOT_SIZE / 2;
        int coreY = y + 20;
        renderWandSlot(g, coreX, coreY, data.core(), selectedWandSlot == -2, mx, my,
                Component.translatable("screen.koniava.wand_upgrade.core_slot"));

        // 升級槽（2 欄，列數依杖柄等級）
        int slotCount = wand().getItem() instanceof WandRodItem rod ? rod.getMaxUpgradeSlots() : 4;
        int spacing = SLOT_SIZE + 6;
        int[] ux = {x + 10, x + 10 + spacing, x + 10, x + 10 + spacing, x + 10, x + 10 + spacing};
        int[] uy = {y + 60, y + 60, y + 60 + spacing, y + 60 + spacing, y + 60 + spacing * 2, y + 60 + spacing * 2};
        for (int i = 0; i < slotCount; i++) {
            ItemStack upg = data.getUpgrade(i);
            renderWandSlot(g, ux[i], uy[i], upg, selectedWandSlot == i, mx, my,
                    Component.translatable("screen.koniava.wand_upgrade.upgrade_slot", i + 1));
        }

        var hintLines = font.split(
                Component.translatable("screen.koniava.wand_upgrade.hint"), LEFT_W - 6);
        int lineH = font.lineHeight + 1;
        int hintY = y + BG_H - 4 - hintLines.size() * lineH;
        for (var line : hintLines) {
            g.drawString(font, line, x + 3, hintY, 0x444444, false);
            hintY += lineH;
        }

    }

    private void renderWandSlot(GuiGraphics g, int x, int y, ItemStack stack,
                                 boolean selected, int mx, int my, Component label) {
        boolean hovered = mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE;

        g.blit(TEXTURE, x, y, SLOT_UV_X, SLOT_UV_Y, SLOT_SIZE, SLOT_SIZE, TEXTURE_W, TEXTURE_H);

        if (selected)      g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x664488FF);
        else if (hovered)  g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x33FFFFFF);

        if (!stack.isEmpty()) {
            g.renderItem(stack, x + 4, y + 4);
            g.renderItemDecorations(font, stack, x + 4, y + 4);
        }
        if (hovered) {
            Component tooltip = stack.isEmpty() ? label : stack.getHoverName();
            g.renderTooltip(font, tooltip, mx, my);
        }
    }

    // ── 右側：相容物品列表 ────────────────────────────────────────────────

    private void renderRightPanel(GuiGraphics g, int x, int y, int mx, int my) {
        if (selectedWandSlot == -1) {
            drawWrapped(g, Component.translatable("screen.koniava.wand_upgrade.select_slot"),
                    x + 5, y + 8, RIGHT_W - 10, 0x333333);
            return;
        }

        Component header = selectedWandSlot == -2
                ? Component.translatable("screen.koniava.wand_upgrade.compatible_cores")
                : Component.translatable("screen.koniava.wand_upgrade.compatible_upgrades");
        int listY = drawWrapped(g, header, x + 5, y + 8, RIGHT_W - 10, 0x222222) + 2;

        List<ItemStack> compatible = getCompatibleItems();
        if (compatible.isEmpty()) {
            drawWrapped(g, Component.translatable("screen.koniava.wand_upgrade.none_in_inventory"),
                    x + 5, listY + 3, RIGHT_W - 10, 0x555555);
            return;
        }

        for (ItemStack item : compatible) {
            if (listY + LIST_ITEM_H > y + BG_H - 5) break;
            boolean hovered = mx >= x + 2 && mx < x + RIGHT_W - 2
                    && my >= listY && my < listY + LIST_ITEM_H;

            if (hovered) g.fill(x + 2, listY, x + RIGHT_W - 2, listY + LIST_ITEM_H, 0x33000066);

            g.renderItem(item, x + 4, listY + 1);
            drawWrapped(g, item.getHoverName(), x + 24, listY + 4, RIGHT_W - 30, 0x111111);
            listY += LIST_ITEM_H;
        }
    }

    // ── 點擊處理 ──────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int sy = startY();
        WandCoreData data = WandRodItem.getData(wand());

        // 左面板：核心槽
        int coreX = leftX() + LEFT_W / 2 - SLOT_SIZE / 2;
        int coreY = sy + 20;
        if (isInSlot(mx, my, coreX, coreY)) { handleWandSlotClick(-2, data.core()); return true; }

        // 左面板：升級槽
        int slotCount = wand().getItem() instanceof WandRodItem rod ? rod.getMaxUpgradeSlots() : 4;
        int spacing = SLOT_SIZE + 6;
        int[] ux = {leftX() + 10, leftX() + 10 + spacing, leftX() + 10, leftX() + 10 + spacing, leftX() + 10, leftX() + 10 + spacing};
        int[] uy = {sy + 60, sy + 60, sy + 60 + spacing, sy + 60 + spacing, sy + 60 + spacing * 2, sy + 60 + spacing * 2};
        for (int i = 0; i < slotCount; i++) {
            if (isInSlot(mx, my, ux[i], uy[i])) {
                ItemStack upg = data.getUpgrade(i);
                handleWandSlotClick(i, upg);
                return true;
            }
        }

        // 右面板：列表項目
        if (selectedWandSlot != -1) {
            int listY = sy + 18;
            for (ItemStack item : getCompatibleItems()) {
                if (listY + LIST_ITEM_H > sy + BG_H - 5) break;
                if (mx >= rightX() + 2 && mx < rightX() + RIGHT_W - 2
                        && my >= listY && my < listY + LIST_ITEM_H) {
                    int invSlot = findInventorySlot(item);
                    if (invSlot >= 0) {
                        WandCoreSwapPacket.sendInstall(hand, selectedWandSlot == -2 ? -1 : selectedWandSlot, invSlot);
                        selectedWandSlot = -1;
                    }
                    return true;
                }
                listY += LIST_ITEM_H;
            }
        }

        // 任何未被攔截的左鍵都可以開始拖曳預覽
        if (button == 0) draggingPreview = true;

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double deltaX, double deltaY) {
        if (draggingPreview && button == 0) {
            previewRotY += (float) (deltaX * 0.8f);
            previewRotX = Mth.clamp(previewRotX + (float) (deltaY * 0.8f), -85f, 85f);
            return true;
        }
        return super.mouseDragged(mx, my, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        previewScale = Mth.clamp(previewScale + (float) (scrollY * 5f), 12f, 120f);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) draggingPreview = false;
        return super.mouseReleased(mx, my, button);
    }

    private void handleWandSlotClick(int slotId, ItemStack current) {
        if (selectedWandSlot == slotId) {
            if (!current.isEmpty()) {
                WandCoreSwapPacket.sendRemove(hand, slotId == -2 ? -1 : slotId);
            }
            selectedWandSlot = -1;
        } else {
            selectedWandSlot = slotId;
        }
    }

    // ── 工具 ──────────────────────────────────────────────────────────────

    private List<ItemStack> getCompatibleItems() {
        List<ItemStack> result = new ArrayList<>();
        Inventory inv = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            boolean ok = selectedWandSlot == -2
                    ? s.getItem() instanceof IWandCore
                    : s.getItem() instanceof IWandUpgrade;
            if (ok) result.add(s);
        }
        return result;
    }

    private int findInventorySlot(ItemStack match) {
        Inventory inv = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i) == match) return i;
        }
        return -1;
    }

    private int drawWrapped(GuiGraphics g, Component text, int x, int y, int maxWidth, int color) {
        var lines = font.split(text, maxWidth);
        int lineH = font.lineHeight + 1;
        for (var line : lines) {
            g.drawString(font, line, x, y, color, false);
            y += lineH;
        }
        return y;
    }

    private boolean isInSlot(double mx, double my, int x, int y) {
        return mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
