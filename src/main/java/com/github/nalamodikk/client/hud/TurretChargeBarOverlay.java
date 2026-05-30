package com.github.nalamodikk.client.hud;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.weapon.FloatingTurretItem;
import com.github.nalamodikk.common.item.weapon.turret.TurretUpgradeBehavior;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.List;

/**
 * 浮游砲雙持蓄力表（螢幕中下）。
 *
 * 蓄力中指針隨進度從左往右移，鬆手位置決定彈種：左段弱蓄力彈、中段控制彈（依裝的控制插件數等分，
 * 每格一個效果，靠顏色 + 位置選）、右段強蓄力彈。閾值與分格邏輯跟 server 端 FloatingTurretItem 共用
 * （CONTROL_BAND_MIN/MAX + installedControls），顯示才會跟實際判定一致。純靠看條，無額外提示。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public final class TurretChargeBarOverlay {

    private static final int BAR_W = 182;
    private static final int BAR_H = 7;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || mc.screen != null) return;

        // 只在雙持浮游砲且正在蓄力時顯示（控制彈是雙持蓄力專屬操作）
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (!(main.getItem() instanceof FloatingTurretItem) || !(off.getItem() instanceof FloatingTurretItem)) return;
        if (!player.isUsingItem() || !(player.getUseItem().getItem() instanceof FloatingTurretItem)) return;

        int ticksHeld = FloatingTurretItem.MAX_CHARGE_TICKS - player.getUseItemRemainingTicks();
        if (ticksHeld <= 0) return;
        float ratio = Math.min(1.0f, (float) ticksHeld / FloatingTurretItem.MAX_CHARGE_TICKS);

        GuiGraphics g = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int barX = (sw - BAR_W) / 2;
        int barY = sh * 2 / 3; // 中下

        List<TurretUpgradeBehavior> controls = FloatingTurretItem.installedControls(main, off);

        float min = FloatingTurretItem.CONTROL_BAND_MIN;
        float max = FloatingTurretItem.CONTROL_BAND_MAX;
        int xMin = barX + (int) (min * BAR_W);
        int xMax = barX + (int) (max * BAR_W);

        // 背景框
        g.fill(barX - 1, barY - 1, barX + BAR_W + 1, barY + BAR_H + 1, 0xCC000000);

        // 左段：弱蓄力彈（灰）
        g.fill(barX, barY, xMin, barY + BAR_H, 0xFF555555);
        // 右段：強蓄力彈（琥珀）
        g.fill(xMax, barY, barX + BAR_W, barY + BAR_H, 0xFFF39C12);
        // 中段：控制區
        if (controls.isEmpty()) {
            // 沒裝控制插件：中段不可用，畫暗色（鬆手在這也只會發蓄力彈）
            g.fill(xMin, barY, xMax, barY + BAR_H, 0xFF2A2A2A);
        } else {
            int n = controls.size();
            for (int i = 0; i < n; i++) {
                int cx0 = xMin + (xMax - xMin) * i / n;
                int cx1 = xMin + (xMax - xMin) * (i + 1) / n;
                g.fill(cx0, barY, cx1, barY + BAR_H, 0xFF000000 | controlColor(controls.get(i)));
                if (i > 0) g.fill(cx0, barY, cx0 + 1, barY + BAR_H, 0xFF000000); // 格線
            }
        }

        // 指針：隨蓄力進度移動，鬆手時所在段決定彈種
        int px = barX + (int) (ratio * BAR_W);
        g.fill(px - 1, barY - 2, px + 1, barY + BAR_H + 2, 0xFFFFFFFF);
    }

    // 控制效果代表色（緩速/定身/漂浮），玩家靠顏色 + 位置記憶選效果
    private static int controlColor(TurretUpgradeBehavior b) {
        return switch (b) {
            case SLOW -> 0x5599FF;     // 緩速：藍
            case ROOT -> 0xB0506E;     // 定身：紫紅（RootMobEffect 色系）
            case LEVITATE -> 0xCCEEFF; // 漂浮：青白
            default -> 0xAAAAAA;
        };
    }
}
