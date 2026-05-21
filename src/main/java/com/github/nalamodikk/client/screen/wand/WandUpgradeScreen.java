package com.github.nalamodikk.client.screen.wand;

import com.github.nalamodikk.common.item.wand.WandCoreData;
import com.github.nalamodikk.common.item.wand.WandRodItem;
import com.github.nalamodikk.common.item.wand.core.IWandCore;
import com.github.nalamodikk.common.item.wand.upgrade.IWandUpgrade;
import com.github.nalamodikk.common.network.packet.server.wand.WandCoreSwapPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class WandUpgradeScreen extends Screen {

    private static final int SLOT_SIZE = 18;
    private static final int PANEL_W = 120;
    private static final int PANEL_H = 140;
    private static final int LIST_ITEM_H = 20;

    private final ItemStack wandStack;
    private final InteractionHand hand;

    // -1 = none, -2 = core slot, 0-3 = upgrade slots
    private int selectedWandSlot = -1;

    public WandUpgradeScreen(ItemStack wandStack, InteractionHand hand) {
        super(Component.translatable("screen.koniava.wand_upgrade"));
        this.wandStack = wandStack;
        this.hand = hand;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);

        int lx = width / 2 - PANEL_W - 4;
        int rx = width / 2 + 4;
        int py = height / 2 - PANEL_H / 2;

        renderWandPanel(g, lx, py, mouseX, mouseY);
        renderListPanel(g, rx, py, mouseX, mouseY);

        super.render(g, mouseX, mouseY, partialTick);
    }

    // ── 左側：杖柄槽位面板 ─────────────────────────────────────────────────

    private void renderWandPanel(GuiGraphics g, int x, int y, int mx, int my) {
        g.fill(x, y, x + PANEL_W, y + PANEL_H, 0xCC222222);
        g.renderOutline(x, y, PANEL_W, PANEL_H, 0xFF666666);
        g.drawString(font, title, x + 5, y + 5, 0xFFFFFF, false);

        WandCoreData data = WandRodItem.getData(wandStack);

        // 核心槽 (置中)
        int coreX = x + PANEL_W / 2 - SLOT_SIZE / 2;
        int coreY = y + 25;
        boolean coreSelected = selectedWandSlot == -2;
        renderWandSlot(g, coreX, coreY, data.core(), coreSelected, mx, my,
                Component.translatable("screen.koniava.wand_upgrade.core_slot"));

        // 升級槽 (2x2)
        int[] ux = {x + 20, x + 20 + SLOT_SIZE + 8, x + 20, x + 20 + SLOT_SIZE + 8};
        int[] uy = {y + 60, y + 60, y + 60 + SLOT_SIZE + 8, y + 60 + SLOT_SIZE + 8};
        for (int i = 0; i < WandCoreData.UPGRADE_SLOTS; i++) {
            ItemStack upg = i < data.upgrades().size() ? data.upgrades().get(i) : ItemStack.EMPTY;
            boolean sel = selectedWandSlot == i;
            renderWandSlot(g, ux[i], uy[i], upg, sel, mx, my,
                    Component.translatable("screen.koniava.wand_upgrade.upgrade_slot", i + 1));
        }

        // 提示
        g.drawString(font, Component.translatable("screen.koniava.wand_upgrade.hint"),
                x + 5, y + PANEL_H - 12, 0x888888, false);
    }

    private void renderWandSlot(GuiGraphics g, int x, int y, ItemStack stack,
                                 boolean selected, int mx, int my, Component label) {
        boolean hovered = mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE;
        int bg = selected ? 0xFF446688 : (hovered ? 0xFF444444 : 0xFF333333);
        g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bg);
        g.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, selected ? 0xFF88BBFF : 0xFF888888);

        if (!stack.isEmpty()) {
            g.renderItem(stack, x + 1, y + 1);
            g.renderItemDecorations(font, stack, x + 1, y + 1);
        }
        if (hovered) {
            g.renderTooltip(font, label, mx, my);
        }
    }

    // ── 右側：相容物品列表面板 ──────────────────────────────────────────────

    private void renderListPanel(GuiGraphics g, int x, int y, int mx, int my) {
        g.fill(x, y, x + PANEL_W, y + PANEL_H, 0xCC1A1A1A);
        g.renderOutline(x, y, PANEL_W, PANEL_H, 0xFF555555);

        if (selectedWandSlot == -1) {
            g.drawString(font,
                    Component.translatable("screen.koniava.wand_upgrade.select_slot"),
                    x + 5, y + 5, 0x888888, false);
            return;
        }

        Component header = selectedWandSlot == -2
                ? Component.translatable("screen.koniava.wand_upgrade.compatible_cores")
                : Component.translatable("screen.koniava.wand_upgrade.compatible_upgrades");
        g.drawString(font, header, x + 5, y + 5, 0xCCCCCC, false);

        List<ItemStack> compatible = getCompatibleItems();
        if (compatible.isEmpty()) {
            g.drawString(font,
                    Component.translatable("screen.koniava.wand_upgrade.none_in_inventory"),
                    x + 5, y + 20, 0x666666, false);
            return;
        }

        int listY = y + 18;
        for (int i = 0; i < compatible.size(); i++) {
            if (listY + LIST_ITEM_H > y + PANEL_H - 5) break;
            ItemStack item = compatible.get(i);
            boolean hovered = mx >= x + 2 && mx < x + PANEL_W - 2
                    && my >= listY && my < listY + LIST_ITEM_H;

            if (hovered) g.fill(x + 2, listY, x + PANEL_W - 2, listY + LIST_ITEM_H, 0xFF334455);

            g.renderItem(item, x + 4, listY + 1);
            g.drawString(font, item.getHoverName(), x + 24, listY + 6, 0xFFFFFF, false);
            listY += LIST_ITEM_H;
        }
    }

    // ── 點擊處理 ───────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int lx = width / 2 - PANEL_W - 4;
        int rx = width / 2 + 4;
        int py = height / 2 - PANEL_H / 2;

        WandCoreData data = WandRodItem.getData(wandStack);

        // 左面板：點擊槽位
        int coreX = lx + PANEL_W / 2 - SLOT_SIZE / 2;
        int coreY = py + 25;
        if (isInSlot(mx, my, coreX, coreY)) {
            handleWandSlotClick(-2, data.core());
            return true;
        }

        int[] ux = {lx + 20, lx + 20 + SLOT_SIZE + 8, lx + 20, lx + 20 + SLOT_SIZE + 8};
        int[] uy = {py + 60, py + 60, py + 60 + SLOT_SIZE + 8, py + 60 + SLOT_SIZE + 8};
        for (int i = 0; i < WandCoreData.UPGRADE_SLOTS; i++) {
            if (isInSlot(mx, my, ux[i], uy[i])) {
                ItemStack upg = i < data.upgrades().size() ? data.upgrades().get(i) : ItemStack.EMPTY;
                handleWandSlotClick(i, upg);
                return true;
            }
        }

        // 右面板：點擊列表項目
        if (selectedWandSlot != -1) {
            int listY = py + 18;
            List<ItemStack> compatible = getCompatibleItems();
            for (ItemStack item : compatible) {
                if (listY + LIST_ITEM_H > py + PANEL_H - 5) break;
                if (mx >= rx + 2 && mx < rx + PANEL_W - 2 && my >= listY && my < listY + LIST_ITEM_H) {
                    int invSlot = findInventorySlot(item);
                    if (invSlot >= 0) {
                        int wandSlot = selectedWandSlot == -2 ? -1 : selectedWandSlot;
                        WandCoreSwapPacket.sendInstall(hand, wandSlot, invSlot);
                        selectedWandSlot = -1;
                    }
                    return true;
                }
                listY += LIST_ITEM_H;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    private void handleWandSlotClick(int wandSlotId, ItemStack current) {
        if (selectedWandSlot == wandSlotId) {
            // 再次點擊已選槽位：如果有物品則取出
            if (!current.isEmpty()) {
                int packetSlot = wandSlotId == -2 ? -1 : wandSlotId;
                WandCoreSwapPacket.sendRemove(hand, packetSlot);
            }
            selectedWandSlot = -1;
        } else {
            selectedWandSlot = wandSlotId;
        }
    }

    // ── 工具 ───────────────────────────────────────────────────────────────

    private List<ItemStack> getCompatibleItems() {
        List<ItemStack> result = new ArrayList<>();
        Inventory inv = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            boolean compatible = selectedWandSlot == -2
                    ? s.getItem() instanceof IWandCore
                    : s.getItem() instanceof IWandUpgrade;
            if (compatible) result.add(s);
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

    private boolean isInSlot(double mx, double my, int x, int y) {
        return mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
