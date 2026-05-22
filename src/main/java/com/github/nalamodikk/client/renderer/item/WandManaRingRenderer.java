package com.github.nalamodikk.client.renderer.item;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.wand.WandRodItem;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class WandManaRingRenderer {

    private static final int COLOR_FILL  = 0xFF4488FF;
    private static final int COLOR_EMPTY = 0xFF334455;
    private static final int COLOR_RING  = 0xFFAABBCC;
    private static final int R_OUTER = 7;
    private static final int R_INNER = 5;
    private static final double ARC_HALF = 2 * Math.PI / 3; // 240-degree arc (120 each side from top)

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;

        var g = event.getGuiGraphics();
        int guiLeft = screen.getGuiLeft();
        int guiTop  = screen.getGuiTop();

        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (!(stack.getItem() instanceof WandRodItem)) continue;

            int current = stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
            int max     = stack.getOrDefault(ModDataComponents.MAX_MANA, 8000);
            float fill  = max > 0 ? (float) current / max : 0f;

            int cx = guiLeft + slot.x + 8;
            int cy = guiTop  + slot.y + 12;
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            drawRing(g, cx, cy, fill);
            g.pose().popPose();
        }
    }

    @SubscribeEvent
    public static void onHudRender(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int hotbarLeft = sw / 2 - 91;
        int hotbarTop  = sh - 22;

        var inv = mc.player.getInventory();
        var g   = event.getGuiGraphics();

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (!(stack.getItem() instanceof WandRodItem)) continue;

            int current = stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
            int max     = stack.getOrDefault(ModDataComponents.MAX_MANA, 8000);
            float fill  = max > 0 ? (float) current / max : 0f;

            int cx = hotbarLeft + 3 + i * 20 + 8;
            int cy = hotbarTop  + 3 + 12;
            drawRing(g, cx, cy, fill);
        }
    }

    private static void drawRing(net.minecraft.client.gui.GuiGraphics g, int cx, int cy, float fill) {
        for (int dx = -R_OUTER - 1; dx <= R_OUTER + 1; dx++) {
            for (int dy = -R_OUTER - 1; dy <= R_OUTER + 1; dy++) {
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > R_OUTER || dist < R_INNER) continue;

                // angle: 0 = top, clockwise positive
                double angle = Math.atan2(dx, -dy);
                if (Math.abs(angle) > ARC_HALF) continue;

                // 0 = left endpoint, 1 = right endpoint
                double normalized = (angle + ARC_HALF) / (2 * ARC_HALF);

                boolean isEdge = dist >= R_OUTER - 1.0 || dist <= R_INNER + 0.5;
                int color = isEdge ? COLOR_RING : (normalized <= fill ? COLOR_FILL : COLOR_EMPTY);
                g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
            }
        }
    }
}
