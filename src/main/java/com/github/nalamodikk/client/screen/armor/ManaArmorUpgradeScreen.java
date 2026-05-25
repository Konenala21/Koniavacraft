package com.github.nalamodikk.client.screen.armor;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.equipment.ManaArmorItem;
import com.github.nalamodikk.common.item.upgrade.EquipmentUpgradeData;
import com.github.nalamodikk.common.network.packet.server.armor.ArmorUpgradeSwapPacket;
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

public class ManaArmorUpgradeScreen extends Screen {

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

    private final EquipmentSlot equipmentSlot;
    private int selectedSlot = -1;

    private Component pendingTooltip = null;
    private int pendingTooltipX, pendingTooltipY;

    public ManaArmorUpgradeScreen(EquipmentSlot equipmentSlot) {
        super(Component.translatable("screen.koniava.armor_upgrade"));
        this.equipmentSlot = equipmentSlot;
    }

    private ItemStack armor() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null ? mc.player.getItemBySlot(equipmentSlot) : ItemStack.EMPTY;
    }

    private ManaArmorItem armorItem() {
        ItemStack s = armor();
        return s.getItem() instanceof ManaArmorItem m ? m : null;
    }

    private int startX() { return width  / 2 - BG_W / 2; }
    private int startY() { return height / 2 - BG_H / 2; }
    private int leftX()  { return startX() + LEFT_X; }
    private int ctrX()   { return startX() + CTR_X; }
    private int rightX() { return startX() + RIGHT_X; }

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

    private void renderPlayerPreview(GuiGraphics g, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int x1 = ctrX(), y1 = startY(), x2 = ctrX() + CTR_W, y2 = startY() + BG_H;
        g.enableScissor(x1, y1, x2, y2);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, x1, y1, x2, y2, 40, 0f, mouseX, mouseY, mc.player);
        g.disableScissor();
    }

    private void renderLeftPanel(GuiGraphics g, int x, int y, int mx, int my) {
        ManaArmorItem item = armorItem();
        if (item == null) return;
        ItemStack armorStack = armor();
        EquipmentUpgradeData data = ManaArmorItem.getData(armorStack);

        int iconX = x + LEFT_W / 2 - 8;
        g.renderItem(armorStack, iconX, y + 14);

        int slots = item.getMaxUpgradeSlots();
        int[][] positions = slotPositions(slots, x + 10, y + 44);
        for (int i = 0; i < slots; i++) {
            ItemStack upg = data.getUpgrade(i);
            renderSlot(g, positions[i][0], positions[i][1], upg, selectedSlot == i, mx, my,
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

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        ManaArmorItem item = armorItem();
        if (item == null) return super.mouseClicked(mx, my, button);

        int sy = startY();
        EquipmentUpgradeData data = ManaArmorItem.getData(armor());
        int slots = item.getMaxUpgradeSlots();
        int[][] positions = slotPositions(slots, leftX() + 10, sy + 44);

        for (int i = 0; i < slots; i++) {
            if (isInSlot(mx, my, positions[i][0], positions[i][1])) {
                if (selectedSlot == i) {
                    if (!data.getUpgrade(i).isEmpty()) ArmorUpgradeSwapPacket.sendRemove(equipmentSlot, i);
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
            for (ItemStack s : getCompatibleItems()) {
                if (listY + LIST_ITEM_H > sy + BG_H - 5) break;
                if (mx >= rightX() + 2 && mx < rightX() + RIGHT_W - 2
                        && my >= listY && my < listY + LIST_ITEM_H) {
                    int invSlot = findInventorySlot(s);
                    if (invSlot >= 0) {
                        ArmorUpgradeSwapPacket.sendInstall(equipmentSlot, selectedSlot, invSlot);
                        selectedSlot = -1;
                    }
                    return true;
                }
                listY += LIST_ITEM_H;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private List<ItemStack> getCompatibleItems() {
        ManaArmorItem item = armorItem();
        if (item == null) return List.of();

        Set<String> installed = new HashSet<>();
        EquipmentUpgradeData data = ManaArmorItem.getData(armor());
        for (int i = 0; i < item.getMaxUpgradeSlots(); i++) {
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

    /** Returns [count][2] array of {x, y} slot positions for the given count. */
    private static int[][] slotPositions(int count, int baseX, int baseY) {
        int sp = SLOT_SIZE + 6;
        return switch (count) {
            case 3 -> new int[][]{{baseX, baseY}, {baseX + sp, baseY}, {baseX + sp * 2, baseY}};
            case 4 -> new int[][]{{baseX, baseY}, {baseX + sp, baseY}, {baseX, baseY + sp}, {baseX + sp, baseY + sp}};
            default -> {
                int[][] pos = new int[count][2];
                for (int i = 0; i < count; i++) pos[i] = new int[]{baseX + (i % 2) * sp, baseY + (i / 2) * sp};
                yield pos;
            }
        };
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
