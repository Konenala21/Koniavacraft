package com.github.nalamodikk.client.screen.turret;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.upgrade.EquipmentUpgradeData;
import com.github.nalamodikk.common.item.weapon.FloatingTurretItem;
import com.github.nalamodikk.common.network.packet.server.turret.TurretUpgradeSwapPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TurretUpgradeScreen extends Screen {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/wand_upgrade_gui.png");
    private static final int TEXTURE_W = 512, TEXTURE_H = 512;

    private static final int BG_W = 473, BG_H = 210;
    private static final int LEFT_X  = 5;
    private static final int LEFT_W  = 93;
    private static final int CTR_X   = 99;
    private static final int CTR_W   = 241;
    private static final int RIGHT_X = 341;
    private static final int RIGHT_W = 132;

    private static final int SLOT_UV_X = 485, SLOT_UV_Y = 5, SLOT_SIZE = 24;
    private static final int LIST_ITEM_H = 20;

    private final InteractionHand hand;
    private int selectedSlot = -1;

    private Component pendingTooltip = null;
    private int pendingTooltipX, pendingTooltipY;

    private float previewRotY = 30f;
    private float previewRotX = -15f;
    private float previewScale = 48f;
    private boolean draggingPreview = false;

    public TurretUpgradeScreen(InteractionHand hand) {
        super(Component.translatable("screen.koniava.turret_upgrade"));
        this.hand = hand;
    }

    private ItemStack turret() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null ? mc.player.getItemInHand(hand) : ItemStack.EMPTY;
    }

    private int maxSlots() {
        return turret().getItem() instanceof FloatingTurretItem t ? t.getMaxUpgradeSlots() : 0;
    }

    // ── 座標 ──────────────────────────────────────────────────────────────

    private int startX() { return width  / 2 - BG_W / 2; }
    private int startY() { return height / 2 - BG_H / 2; }
    private int leftX()  { return startX() + LEFT_X; }
    private int ctrX()   { return startX() + CTR_X; }
    private int rightX() { return startX() + RIGHT_X; }

    private int[][] slotPositions() {
        int sp = SLOT_SIZE + 6;
        int bx = leftX() + 10, by = startY() + 30;
        int[][] pos = new int[maxSlots()][2];
        for (int i = 0; i < pos.length; i++) {
            pos[i] = new int[]{bx + (i % 2) * sp, by + (i / 2) * sp};
        }
        return pos;
    }

    // ── Render ──────────────────────────────────────────────────────────────

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x55000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.blit(TEXTURE, startX(), startY(), 0, 0, BG_W, BG_H, TEXTURE_W, TEXTURE_H);

        pendingTooltip = null;
        render3DTurret(g);
        renderLeftPanel(g, leftX(), startY(), mouseX, mouseY);
        renderRightPanel(g, rightX(), startY(), mouseX, mouseY);

        if (pendingTooltip != null) {
            g.renderTooltip(font, pendingTooltip, pendingTooltipX, pendingTooltipY);
        }
    }

    private void render3DTurret(GuiGraphics g) {
        int cx = ctrX() + CTR_W / 2;
        int cy = startY() + BG_H / 2;
        // 用 scissor 裁切到中央面板區內側（四邊各留 6px 不貼到面板邊框）
        int scissorX1 = ctrX() + 6;
        int scissorY1 = startY() + 6;
        int scissorX2 = ctrX() + CTR_W - 6;
        int scissorY2 = startY() + BG_H - 6;
        g.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 200);
        pose.scale(previewScale, -previewScale, previewScale);
        pose.mulPose(Axis.YP.rotationDegrees(previewRotY));
        pose.mulPose(Axis.XP.rotationDegrees(previewRotX));
        Minecraft mc = Minecraft.getInstance();
        mc.getItemRenderer().renderStatic(
                turret(), ItemDisplayContext.FIXED, 15728880, OverlayTexture.NO_OVERLAY,
                pose, mc.renderBuffers().bufferSource(), mc.level, 0);
        mc.renderBuffers().bufferSource().endBatch();
        pose.popPose();
        g.disableScissor();
    }

    private void renderLeftPanel(GuiGraphics g, int x, int y, int mx, int my) {
        EquipmentUpgradeData data = FloatingTurretItem.getData(turret());
        int[][] pos = slotPositions();
        for (int i = 0; i < pos.length; i++) {
            renderSlot(g, pos[i][0], pos[i][1], data.getUpgrade(i), selectedSlot == i, mx, my,
                    Component.translatable("screen.koniava.wand_upgrade.upgrade_slot", i + 1));
        }
        var hintLines = font.split(Component.translatable("screen.koniava.wand_upgrade.hint"), LEFT_W - 6);
        int lineH = font.lineHeight + 1;
        int hintY = y + BG_H - 4 - hintLines.size() * lineH;
        for (var line : hintLines) {
            g.drawString(font, line, x + 3, hintY, 0x444444, false);
            hintY += lineH;
        }
    }

    private void renderSlot(GuiGraphics g, int x, int y, ItemStack stack,
                            boolean selected, int mx, int my, Component label) {
        boolean hovered = mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE;
        g.blit(TEXTURE, x, y, SLOT_UV_X, SLOT_UV_Y, SLOT_SIZE, SLOT_SIZE, TEXTURE_W, TEXTURE_H);
        if (selected)     g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x664488FF);
        else if (hovered) g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x33FFFFFF);
        if (!stack.isEmpty()) {
            g.renderItem(stack, x + 4, y + 4);
            g.renderItemDecorations(font, stack, x + 4, y + 4);
        }
        if (hovered) {
            pendingTooltip = stack.isEmpty() ? label : stack.getHoverName();
            pendingTooltipX = mx;
            pendingTooltipY = my;
        }
    }

    private void renderRightPanel(GuiGraphics g, int x, int y, int mx, int my) {
        if (selectedSlot == -1) {
            drawWrapped(g, Component.translatable("screen.koniava.wand_upgrade.select_slot"),
                    x + 5, y + 8, RIGHT_W - 10, 0x333333);
            return;
        }
        Component header = Component.translatable("screen.koniava.wand_upgrade.compatible_upgrades");
        int listY = drawWrapped(g, header, x + 5, y + 8, RIGHT_W - 10, 0x222222) + 2;
        List<ItemStack> compatible = getCompatibleItems();
        if (compatible.isEmpty()) {
            drawWrapped(g, Component.translatable("screen.koniava.wand_upgrade.none_in_inventory"),
                    x + 5, listY + 3, RIGHT_W - 10, 0x555555);
            return;
        }
        for (ItemStack item : compatible) {
            if (listY + LIST_ITEM_H > y + BG_H - 5) break;
            boolean hovered = mx >= x + 2 && mx < x + RIGHT_W - 2 && my >= listY && my < listY + LIST_ITEM_H;
            if (hovered) g.fill(x + 2, listY, x + RIGHT_W - 2, listY + LIST_ITEM_H, 0x33000066);
            g.renderItem(item, x + 4, listY + 1);
            drawWrapped(g, item.getHoverName(), x + 24, listY + 4, RIGHT_W - 30, 0x111111);
            listY += LIST_ITEM_H;
        }
    }

    // ── 點擊 ──────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int sy = startY();
        EquipmentUpgradeData data = FloatingTurretItem.getData(turret());

        int[][] pos = slotPositions();
        for (int i = 0; i < pos.length; i++) {
            if (isInSlot(mx, my, pos[i][0], pos[i][1])) {
                ItemStack cur = data.getUpgrade(i);
                if (selectedSlot == i) {
                    if (!cur.isEmpty()) TurretUpgradeSwapPacket.sendRemove(hand, i);
                    selectedSlot = -1;
                } else {
                    selectedSlot = i;
                }
                return true;
            }
        }

        if (selectedSlot != -1) {
            Component header = Component.translatable("screen.koniava.wand_upgrade.compatible_upgrades");
            int listY = sy + 8 + font.split(header, RIGHT_W - 10).size() * (font.lineHeight + 1) + 2;
            for (ItemStack item : getCompatibleItems()) {
                if (listY + LIST_ITEM_H > sy + BG_H - 5) break;
                if (mx >= rightX() + 2 && mx < rightX() + RIGHT_W - 2 && my >= listY && my < listY + LIST_ITEM_H) {
                    int invSlot = findInventorySlot(item);
                    if (invSlot >= 0) {
                        TurretUpgradeSwapPacket.sendInstall(hand, selectedSlot, invSlot);
                        selectedSlot = -1;
                    }
                    return true;
                }
                listY += LIST_ITEM_H;
            }
        }

        if (button == 0) draggingPreview = true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingPreview && button == 0) {
            previewRotY += (float) (dx * 0.8f);
            previewRotX += (float) (dy * 0.8f);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        previewScale = Mth.clamp(previewScale + (float) (sy * 5f), 12f, 120f);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) draggingPreview = false;
        return super.mouseReleased(mx, my, button);
    }

    // ── 相容物品（排除已安裝的同類）────────────────────────────────────────

    private List<ItemStack> getCompatibleItems() {
        ItemStack turret = turret();
        if (!(turret.getItem() instanceof FloatingTurretItem item)) return List.of();
        EquipmentUpgradeData data = FloatingTurretItem.getData(turret);
        Set<String> installed = new HashSet<>();
        for (int i = 0; i < maxSlots(); i++) {
            if (i == selectedSlot) continue;
            ItemStack upg = data.getUpgrade(i);
            if (!upg.isEmpty()) installed.add(item.getUpgradeBehaviorKey(upg));
        }
        List<ItemStack> result = new ArrayList<>();
        Inventory inv = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && item.isValidUpgradeItem(s)
                    && !installed.contains(item.getUpgradeBehaviorKey(s))) {
                result.add(s);
            }
        }
        return result;
    }

    private int findInventorySlot(ItemStack match) {
        Inventory inv = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (ItemStack.isSameItemSameComponents(inv.getItem(i), match)) return i;
        }
        return -1;
    }

    private int drawWrapped(GuiGraphics g, Component text, int x, int y, int maxWidth, int color) {
        var lines = font.split(text, maxWidth);
        int lineH = font.lineHeight + 1;
        for (var line : lines) { g.drawString(font, line, x, y, color, false); y += lineH; }
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
