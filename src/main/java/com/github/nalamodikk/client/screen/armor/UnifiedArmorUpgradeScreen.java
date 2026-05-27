package com.github.nalamodikk.client.screen.armor;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.equipment.ManaArmorItem;
import com.github.nalamodikk.common.item.upgrade.EquipmentUpgradeData;
import com.github.nalamodikk.common.network.packet.server.armor.ArmorUpgradeSwapPacket;
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

/**
 * Layout (same panel positions as original, texture dividers align):
 *
 *  ┌────────────────────────────────────────────────────────────┐
 *  │ [<護腿|胸甲>]  [    3D player preview    ]  [ compatible ] │
 *  │              │                             │  items list  │
 *  │  piece name  │                             │              │
 *  │  upgrade     │                             │              │
 *  │  slots       │                             │              │
 *  │  hint        │                             │              │
 *  └──────────────┴─────────────────────────────┴──────────────┘
 *     93px                 241px                    132px
 *
 * Top strip of LEFT panel = TWO nav buttons side by side:
 *   left half  = [< prev] — click to cycle backward
 *   right half = [next >] — click to cycle forward
 * RIGHT panel = compatible items only (same as original)
 */
public class UnifiedArmorUpgradeScreen extends Screen {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/wand_upgrade_gui.png");
    private static final int TEXTURE_W = 512, TEXTURE_H = 512;
    private static final int SLOT_UV_X = 485, SLOT_UV_Y = 5, SLOT_SIZE = 24;
    private static final int LIST_ITEM_H = 20;

    // Panel dimensions — identical to original screens so the background texture aligns
    private static final int BG_W = 473, BG_H = 210;
    private static final int LEFT_OFF  = 5;
    private static final int LEFT_W    = 93;
    private static final int CTR_OFF   = 99;
    private static final int CTR_W     = 241;
    private static final int RIGHT_OFF = 341;
    private static final int RIGHT_W   = 132;

    // Navigation strip height at the top of left/right panels
    private static final int NAV_STRIP_H = 20;

    // Circular navigation order
    private static final EquipmentSlot[] SLOT_ORDER = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private EquipmentSlot currentSlot;
    private int selectedUpgradeSlot = -1;

    private Component pendingTooltip = null;
    private int pendingTooltipX, pendingTooltipY;

    // ── Slide animation state ─────────────────────────────────────────────────
    private EquipmentSlot animFromSlot = null;
    private float animProgress = 1f;
    private int animDir = 0;
    private long animStartNano = 0L;
    private static final float ANIM_DURATION_MS = 180f;

    public UnifiedArmorUpgradeScreen(EquipmentSlot initialSlot) {
        super(Component.translatable("screen.koniava.armor_upgrade"));
        this.currentSlot = initialSlot;
    }

    // ── Layout helpers ────────────────────────────────────────────────────────

    private int startX() { return width  / 2 - BG_W / 2; }
    private int startY() { return height / 2 - BG_H / 2; }
    private int leftX()  { return startX() + LEFT_OFF; }
    private int ctrX()   { return startX() + CTR_OFF; }
    private int rightX() { return startX() + RIGHT_OFF; }

    // ── Navigation helpers ────────────────────────────────────────────────────

    private int slotIndex(EquipmentSlot slot) {
        for (int i = 0; i < SLOT_ORDER.length; i++) if (SLOT_ORDER[i] == slot) return i;
        return 0;
    }

    private EquipmentSlot prevSlot() {
        return SLOT_ORDER[(slotIndex(currentSlot) - 1 + SLOT_ORDER.length) % SLOT_ORDER.length];
    }

    private EquipmentSlot nextSlot() {
        return SLOT_ORDER[(slotIndex(currentSlot) + 1) % SLOT_ORDER.length];
    }

    // ── Item/upgrade helpers ──────────────────────────────────────────────────

    private ItemStack stackFor(EquipmentSlot slot) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null ? mc.player.getItemBySlot(slot) : ItemStack.EMPTY;
    }

    private boolean isManaEquip(ItemStack s) {
        return s.getItem() instanceof ManaArmorItem;
    }

    private EquipmentUpgradeData getUpgradeData(EquipmentSlot slot) {
        ItemStack s = stackFor(slot);
        if (s.getItem() instanceof ManaArmorItem) return ManaArmorItem.getData(s);
        return EquipmentUpgradeData.empty();
    }

    private int getMaxUpgradeSlots(EquipmentSlot slot) {
        ItemStack s = stackFor(slot);
        if (s.getItem() instanceof ManaArmorItem m) return m.getMaxUpgradeSlots();
        return 0;
    }

    private boolean isValidUpgrade(EquipmentSlot slot, ItemStack upgradeStack) {
        ItemStack s = stackFor(slot);
        if (s.getItem() instanceof ManaArmorItem m) return m.isValidUpgradeItem(upgradeStack);
        return false;
    }

    private String getBehaviorKey(EquipmentSlot slot, ItemStack upgradeStack) {
        ItemStack s = stackFor(slot);
        if (s.getItem() instanceof ManaArmorItem m) return m.getUpgradeBehaviorKey(upgradeStack);
        return upgradeStack.getDescriptionId();
    }

    private void sendInstall(EquipmentSlot slot, int upgradeSlot, int invSlot) {
        if (slot == EquipmentSlot.FEET) BootsUpgradeSwapPacket.sendInstall(upgradeSlot, invSlot);
        else ArmorUpgradeSwapPacket.sendInstall(slot, upgradeSlot, invSlot);
    }

    private void switchSlot(EquipmentSlot newSlot, int dir) {
        animFromSlot  = currentSlot;
        animDir       = dir;
        animProgress  = 0f;
        animStartNano = System.nanoTime();
        currentSlot   = newSlot;
        selectedUpgradeSlot = -1;
    }

    private void sendRemove(EquipmentSlot slot, int upgradeSlot) {
        if (slot == EquipmentSlot.FEET) BootsUpgradeSwapPacket.sendRemove(upgradeSlot);
        else ArmorUpgradeSwapPacket.sendRemove(slot, upgradeSlot);
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x55000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        if (animProgress < 1f) {
            float elapsed = (System.nanoTime() - animStartNano) / 1_000_000f;
            animProgress = Math.min(1f, elapsed / ANIM_DURATION_MS);
        }

        int sy = startY();
        g.blit(TEXTURE, startX(), sy, 0, 0, BG_W, BG_H, TEXTURE_W, TEXTURE_H);

        pendingTooltip = null;
        renderLeftPanel(g, leftX(), sy, mouseX, mouseY);
        renderPlayerPreview(g, mouseX, mouseY);
        renderRightPanel(g, rightX(), sy, mouseX, mouseY);

        if (pendingTooltip != null) {
            g.renderTooltip(font, pendingTooltip, pendingTooltipX, pendingTooltipY);
        }
    }

    // ── Left panel: three-part nav strip (prev | current | next) + upgrade slots ──
    // Proportions: 25% | 50% | 25% so the current piece reads larger

    private static final int NAV_SIDE_W = 23;  // 93 * 0.25
    private static final int NAV_CTR_W  = 47;  // 93 - 23 - 23

    private static final int BIG_ICON_SIZE = 32;
    private static final int BIG_ICON_TOP  = NAV_STRIP_H + 12;

    private void renderLeftPanel(GuiGraphics g, int x, int y, int mx, int my) {
        renderNavHalf(g,    x,                          y, NAV_SIDE_W, prevSlot(), true,  mx, my);
        renderNavCurrent(g, x + NAV_SIDE_W,             y, NAV_CTR_W,              mx, my);
        renderNavHalf(g,    x + NAV_SIDE_W + NAV_CTR_W, y, NAV_SIDE_W, nextSlot(), false, mx, my);

        int iconBaseX = x + (LEFT_W - BIG_ICON_SIZE) / 2;
        int iconBaseY = y + BIG_ICON_TOP;

        // ease-out curve
        float t = 1f - (1f - animProgress) * (1f - animProgress);
        int clipX1 = x, clipX2 = x + LEFT_W;
        int clipY1 = y + NAV_STRIP_H, clipY2 = iconBaseY + BIG_ICON_SIZE + 2;
        g.enableScissor(clipX1, clipY1, clipX2, clipY2);

        if (animProgress < 1f && animFromSlot != null) {
            int oldOffset = (int)(-animDir * t * LEFT_W);
            int newOffset = (int)(animDir * (1f - t) * LEFT_W);
            renderBigIconAt(g, iconBaseX + oldOffset, iconBaseY, stackFor(animFromSlot));
            renderBigIconAt(g, iconBaseX + newOffset, iconBaseY, stackFor(currentSlot));
        } else {
            ItemStack stack = stackFor(currentSlot);
            renderBigIconAt(g, iconBaseX, iconBaseY, stack);
            if (!stack.isEmpty()
                    && mx >= iconBaseX && mx < iconBaseX + BIG_ICON_SIZE
                    && my >= iconBaseY && my < iconBaseY + BIG_ICON_SIZE) {
                pendingTooltip = stack.getHoverName();
                pendingTooltipX = mx;
                pendingTooltipY = my;
            }
        }

        g.disableScissor();

        ItemStack stack = stackFor(currentSlot);
        if (!stack.isEmpty()) {
            int slotsY = iconBaseY + BIG_ICON_SIZE + 8;
            renderUpgradeSlots(g, x + 6, slotsY, mx, my);
        }

        var hintLines = font.split(Component.translatable("screen.koniava.wand_upgrade.hint"), LEFT_W - 6);
        int lineH = font.lineHeight + 1;
        int hintY = y + BG_H - 4 - hintLines.size() * lineH;
        for (var line : hintLines) {
            g.drawString(font, line, x + 3, hintY, 0x444444, false);
            hintY += lineH;
        }
    }

    // ── Center: 3D player preview ─────────────────────────────────────────────

    private void renderPlayerPreview(GuiGraphics g, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int x1 = ctrX(), y1 = startY(), x2 = ctrX() + CTR_W, y2 = startY() + BG_H;
        g.enableScissor(x1, y1, x2, y2);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, x1, y1, x2, y2, 40, 0f, mouseX, mouseY, mc.player);
        g.disableScissor();
    }

    // ── Right panel: compatible items (same as original) ─────────────────────

    private void renderRightPanel(GuiGraphics g, int x, int y, int mx, int my) {
        int contentY = y + 4;

        if (selectedUpgradeSlot == -1) {
            drawWrapped(g, Component.translatable("screen.koniava.wand_upgrade.select_slot"),
                    x + 5, contentY + 4, RIGHT_W - 10, 0x333333);
            return;
        }

        Component header = Component.translatable("screen.koniava.wand_upgrade.compatible_upgrades");
        int listY = drawWrapped(g, header, x + 5, contentY, RIGHT_W - 10, 0x222222) + 2;

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

    // ── Nav center cell (current piece, display only) ─────────────────────────

    private void renderNavCurrent(GuiGraphics g, int x, int y, int w, int mx, int my) {
    }

    private void renderBigIconAt(GuiGraphics g, int iconX, int iconY, ItemStack stack) {
        if (stack.isEmpty()) return;
        g.pose().pushPose();
        g.pose().translate(iconX, iconY, 0);
        g.pose().scale(2f, 2f, 1f);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

    // ── Nav half-button (prev or next, clickable) ─────────────────────────────

    private void renderNavHalf(GuiGraphics g, int x, int y, int w,
                                EquipmentSlot slot, boolean isLeft, int mx, int my) {
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + NAV_STRIP_H;

        ItemStack navStack = stackFor(slot);
        if (!navStack.isEmpty()) {
            int iconX = x + (w - 16) / 2;
            int iconY = y + (NAV_STRIP_H - 16) / 2 + 5;
            g.renderItem(navStack, iconX, iconY);
        }

        if (hovered && !navStack.isEmpty()) {
            pendingTooltip = navStack.getHoverName();
            pendingTooltipX = (int) mx;
            pendingTooltipY = (int) my;
        }
    }

    // ── Upgrade slots ──────────────────────────────────────────────────────────

    private void renderUpgradeSlots(GuiGraphics g, int baseX, int baseY, int mx, int my) {
        int slots = getMaxUpgradeSlots(currentSlot);
        if (slots == 0) return;
        EquipmentUpgradeData data = getUpgradeData(currentSlot);
        int[][] positions = slotPositions(slots, baseX, baseY);
        for (int i = 0; i < slots; i++) {
            ItemStack upg = data.getUpgrade(i);
            renderSlot(g, positions[i][0], positions[i][1], upg, selectedUpgradeSlot == i, mx, my,
                    Component.translatable("screen.koniava.wand_upgrade.upgrade_slot", i + 1));
        }
    }

    private void renderSlot(GuiGraphics g, int x, int y, ItemStack stack,
                             boolean selected, int mx, int my, Component label) {
        boolean hovered = mx >= x && mx < x + SLOT_SIZE && my >= y && my < y + SLOT_SIZE;
        g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x88000000);
        g.blit(TEXTURE, x, y, SLOT_UV_X, SLOT_UV_Y, SLOT_SIZE, SLOT_SIZE, TEXTURE_W, TEXTURE_H);
        if (selected)     g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x554488FF);
        else if (hovered) g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x22FFFFFF);
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

    // ── Compatible items ──────────────────────────────────────────────────────

    private List<ItemStack> getCompatibleItems() {
        Set<String> installed = new HashSet<>();
        EquipmentUpgradeData data = getUpgradeData(currentSlot);
        int maxSlots = getMaxUpgradeSlots(currentSlot);
        for (int i = 0; i < maxSlots; i++) {
            if (i == selectedUpgradeSlot) continue;
            ItemStack upg = data.getUpgrade(i);
            if (!upg.isEmpty()) installed.add(getBehaviorKey(currentSlot, upg));
        }
        List<ItemStack> result = new ArrayList<>();
        Inventory inv = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && isValidUpgrade(currentSlot, s)
                    && !installed.contains(getBehaviorKey(currentSlot, s))) {
                result.add(s);
            }
        }
        return result;
    }

    // ── Mouse click ───────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int sy = startY();

        // Nav strip: left 25% = prev, right 25% = next (center is display-only)
        if (my >= sy && my < sy + NAV_STRIP_H) {
            int lx = leftX();
            if (mx >= lx && mx < lx + NAV_SIDE_W) {
                switchSlot(prevSlot(), -1);
                return true;
            }
            if (mx >= lx + NAV_SIDE_W + NAV_CTR_W && mx < lx + LEFT_W) {
                switchSlot(nextSlot(), +1);
                return true;
            }
        }

        // Upgrade slot clicks
        int slots = getMaxUpgradeSlots(currentSlot);
        if (slots > 0) {
            int baseX = leftX() + 6;
            int baseY  = computeUpgradeSlotsY(sy);
            EquipmentUpgradeData data = getUpgradeData(currentSlot);
            int[][] positions = slotPositions(slots, baseX, baseY);
            for (int i = 0; i < slots; i++) {
                if (isInSlot(mx, my, positions[i][0], positions[i][1])) {
                    if (selectedUpgradeSlot == i) {
                        if (!data.getUpgrade(i).isEmpty()) sendRemove(currentSlot, i);
                        selectedUpgradeSlot = -1;
                    } else {
                        selectedUpgradeSlot = i;
                    }
                    return true;
                }
            }
        }

        // Compatible item clicks (right panel)
        if (selectedUpgradeSlot != -1) {
            int rx = rightX();
            Component header = Component.translatable("screen.koniava.wand_upgrade.compatible_upgrades");
            int listY = sy + 4 + font.split(header, RIGHT_W - 10).size() * (font.lineHeight + 1) + 2;
            for (ItemStack s : getCompatibleItems()) {
                if (listY + LIST_ITEM_H > sy + BG_H - 5) break;
                if (mx >= rx + 2 && mx < rx + RIGHT_W - 2 && my >= listY && my < listY + LIST_ITEM_H) {
                    int invSlot = findInventorySlot(s);
                    if (invSlot >= 0) {
                        sendInstall(currentSlot, selectedUpgradeSlot, invSlot);
                        selectedUpgradeSlot = -1;
                    }
                    return true;
                }
                listY += LIST_ITEM_H;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    /**
     * Recomputes upgrade-slots grid Y to match renderLeftPanel's dynamic text layout.
     */
    private int computeUpgradeSlotsY(int sy) {
        return sy + BIG_ICON_TOP + BIG_ICON_SIZE + 8;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private static int[][] slotPositions(int count, int baseX, int baseY) {
        int sp = SLOT_SIZE + 6;
        return switch (count) {
            case 3 -> new int[][]{{baseX, baseY}, {baseX + sp, baseY}, {baseX + sp * 2, baseY}};
            case 4 -> new int[][]{{baseX, baseY}, {baseX + sp, baseY},
                                   {baseX, baseY + sp}, {baseX + sp, baseY + sp}};
            default -> {
                int[][] pos = new int[count][2];
                for (int i = 0; i < count; i++)
                    pos[i] = new int[]{baseX + (i % 2) * sp, baseY + (i / 2) * sp};
                yield pos;
            }
        };
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
