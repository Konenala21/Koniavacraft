package com.github.nalamodikk.client.screen.boots;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.equipment.boots.BootsUpgradeBehavior;
import com.github.nalamodikk.common.item.equipment.boots.BootsUpgradeItem;
import com.github.nalamodikk.common.item.equipment.boots.ManaSprintBootsItem;
import com.github.nalamodikk.common.item.upgrade.EquipmentUpgradeData;
import com.github.nalamodikk.common.network.packet.server.boots.BootsUpgradeSwapPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BootsUpgradeScreen extends Screen {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/wand_upgrade_gui.png");
    private static final int TEXTURE_W = 512;
    private static final int TEXTURE_H = 512;

    private static final int BG_W  = 473;
    private static final int BG_H  = 210;

    private static final int LEFT_X  = 5;
    private static final int LEFT_W  = 93;
    private static final int CTR_X   = 99;
    private static final int CTR_W   = 241;
    private static final int RIGHT_X = 341;
    private static final int RIGHT_W = 132;

    private static final int SLOT_UV_X = 485, SLOT_UV_Y = 5, SLOT_SIZE = 24;
    private static final int LIST_ITEM_H = 20;

    // -1 = none, 0-4 = upgrade slot index
    private int selectedSlot = -1;

    // Deferred tooltip — set during panel rendering, drawn last to avoid being covered
    private Component pendingTooltip = null;
    private int pendingTooltipX, pendingTooltipY;

    public BootsUpgradeScreen() {
        super(Component.translatable("screen.koniava.boots_upgrade"));
    }

    private ItemStack boots() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null ? mc.player.getItemBySlot(EquipmentSlot.FEET) : ItemStack.EMPTY;
    }

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
        g.blit(TEXTURE, startX(), sy, 0, 0, BG_W, BG_H, TEXTURE_W, TEXTURE_H);

        pendingTooltip = null;
        renderPlayerPreview(g, mouseX, mouseY);
        renderLeftPanel(g, leftX(), sy, mouseX, mouseY);
        renderRightPanel(g, rightX(), sy, mouseX, mouseY);

        if (pendingTooltip != null) {
            g.renderTooltip(font, pendingTooltip, pendingTooltipX, pendingTooltipY);
        }
    }

    // ── 中間：玩家 3D 預覽 ────────────────────────────────────────────────

    private void renderPlayerPreview(GuiGraphics g, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int x1 = ctrX();
        int y1 = startY();
        int x2 = ctrX() + CTR_W;
        int y2 = startY() + BG_H;

        g.enableScissor(x1, y1, x2, y2);
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                g, x1, y1, x2, y2, 40, 0f, mouseX, mouseY, mc.player);
        g.disableScissor();
    }

    // ── 左側：升級槽 ─────────────────────────────────────────────────────

    private void renderLeftPanel(GuiGraphics g, int x, int y, int mx, int my) {
        ItemStack bootsStack = boots();
        EquipmentUpgradeData data = ManaSprintBootsItem.getData(bootsStack);

        int iconX = x + LEFT_W / 2 - 8;
        int iconY = y + 14;
        g.renderItem(bootsStack, iconX, iconY);

        int slots = ManaSprintBootsItem.MAX_UPGRADE_SLOTS;
        int spacing = SLOT_SIZE + 6;
        int[] ux = {x + 10, x + 10 + spacing, x + 10, x + 10 + spacing, x + 10};
        int[] uy = {y + 44, y + 44, y + 44 + spacing, y + 44 + spacing, y + 44 + spacing * 2};
        for (int i = 0; i < slots; i++) {
            ItemStack upg = data.getUpgrade(i);
            renderSlot(g, ux[i], uy[i], upg, selectedSlot == i, mx, my,
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

    // ── 右側：相容升級列表 ────────────────────────────────────────────────

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
            boolean hovered = mx >= x + 2 && mx < x + RIGHT_W - 2
                    && my >= listY && my < listY + LIST_ITEM_H;
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
        EquipmentUpgradeData data = ManaSprintBootsItem.getData(boots());

        int slots = ManaSprintBootsItem.MAX_UPGRADE_SLOTS;
        int spacing = SLOT_SIZE + 6;
        int baseX = leftX() + 10, baseY = sy + 44;
        int[] ux = {baseX, baseX + spacing, baseX, baseX + spacing, baseX};
        int[] uy = {baseY, baseY, baseY + spacing, baseY + spacing, baseY + spacing * 2};
        for (int i = 0; i < slots; i++) {
            if (isInSlot(mx, my, ux[i], uy[i])) {
                if (selectedSlot == i) {
                    if (!data.getUpgrade(i).isEmpty()) BootsUpgradeSwapPacket.sendRemove(i);
                    selectedSlot = -1;
                } else {
                    selectedSlot = i;
                }
                return true;
            }
        }

        if (selectedSlot != -1) {
            Component header = Component.translatable("screen.koniava.wand_upgrade.compatible_upgrades");
            int listY = sy + 8 + font.split(header, RIGHT_W - 10).size() * (font.lineHeight + 1) + 2 + 3;
            for (ItemStack item : getCompatibleItems()) {
                if (listY + LIST_ITEM_H > sy + BG_H - 5) break;
                if (mx >= rightX() + 2 && mx < rightX() + RIGHT_W - 2
                        && my >= listY && my < listY + LIST_ITEM_H) {
                    int invSlot = findInventorySlot(item);
                    if (invSlot >= 0) {
                        BootsUpgradeSwapPacket.sendInstall(selectedSlot, invSlot);
                        selectedSlot = -1;
                    }
                    return true;
                }
                listY += LIST_ITEM_H;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    // ── 工具 ──────────────────────────────────────────────────────────────

    private List<ItemStack> getCompatibleItems() {
        // Collect behavior types already installed in OTHER slots
        Set<BootsUpgradeBehavior> installed = new HashSet<>();
        EquipmentUpgradeData data = ManaSprintBootsItem.getData(boots());
        for (int i = 0; i < ManaSprintBootsItem.MAX_UPGRADE_SLOTS; i++) {
            if (i == selectedSlot) continue;
            ItemStack upg = data.getUpgrade(i);
            if (!upg.isEmpty() && upg.getItem() instanceof BootsUpgradeItem bu) {
                installed.add(bu.getBehavior());
            }
        }

        List<ItemStack> result = new ArrayList<>();
        Inventory inv = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof BootsUpgradeItem bu
                    && !installed.contains(bu.getBehavior())) {
                result.add(s);
            }
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
