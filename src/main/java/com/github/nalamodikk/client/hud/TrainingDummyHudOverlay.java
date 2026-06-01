package com.github.nalamodikk.client.hud;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.network.packet.client.turret.TrainingDummyStatsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 訓練假人戰鬥 HUD（螢幕中下）。
 *
 * 進行中：顯示計時器 + 累積傷害 + DPS + 命中數。計時由 client 自己從 session 開始時間跑，
 * 不依賴每個封包，所以兩次命中之間數字仍平順前進。逾時結束後切成結算表格（總傷害 / 用時 / DPS），
 * 定格 {@link #SUMMARY_MS} 毫秒再消失。傷害數值與畫面上的浮動傷害數字共用同一套（伺服器以最終傷害送出）。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public final class TrainingDummyHudOverlay {

    private static final long SUMMARY_MS = 5000;     // 結算定格時間
    private static final int PANEL_BG = 0xCC000000;
    private static final int COLOR_TITLE = 0xFFB6F0A0;
    private static final int COLOR_VALUE = 0xFFFFFFFF;
    private static final int COLOR_DPS = 0xFFFFD24A;

    private static final int COLOR_EFFECT = 0xFF8FD0FF;

    private static boolean active = false;      // 進行中
    private static boolean summary = false;     // 結算定格
    private static double totalDamage = 0;
    private static int hitCount = 0;
    private static int serverDurationTicks = 0;
    private static long clientStartMs = 0;
    private static long summaryUntilMs = 0;
    private static java.util.List<TrainingDummyStatsPacket.EffectLine> effects = java.util.List.of();

    private TrainingDummyHudOverlay() {}

    /** 收到伺服器 session 封包時更新狀態（已在 client 主執行緒）。 */
    public static void accept(TrainingDummyStatsPacket p) {
        totalDamage = p.totalDamage();
        hitCount = p.hitCount();
        serverDurationTicks = p.durationTicks();
        effects = p.effects();
        if (p.ended()) {
            active = false;
            summary = true;
            summaryUntilMs = System.currentTimeMillis() + SUMMARY_MS;
        } else {
            if (!active) clientStartMs = System.currentTimeMillis();
            active = true;
            summary = false;
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int baseY = sh * 3 / 5;

        if (active) {
            double elapsedSec = (System.currentTimeMillis() - clientStartMs) / 1000.0;
            double dps = elapsedSec > 0.1 ? totalDamage / elapsedSec : 0;
            java.util.List<Line> lines = new java.util.ArrayList<>();
            lines.add(new Line(Component.translatable("koniava.hud.training_dummy.time",
                    String.format("%.1f", elapsedSec)), COLOR_VALUE));
            lines.add(new Line(Component.translatable("koniava.hud.training_dummy.damage",
                    String.format("%.1f", totalDamage)), COLOR_VALUE));
            lines.add(new Line(Component.translatable("koniava.hud.training_dummy.dps",
                    String.format("%.1f", dps)), COLOR_DPS));
            lines.add(new Line(Component.translatable("koniava.hud.training_dummy.hits", hitCount), COLOR_VALUE));
            // 假人身上的效果(技能反應):破甲/流血/易傷/迷盲...
            for (TrainingDummyStatsPacket.EffectLine e : effects) {
                Component name = Component.translatable(e.descriptionId());
                Component line = e.amplifier() > 0
                        ? Component.translatable("koniava.hud.training_dummy.effect_lvl", name, e.amplifier() + 1)
                        : name;
                lines.add(new Line(line, COLOR_EFFECT));
            }
            drawPanel(g, font, sw, baseY,
                    Component.translatable("koniava.hud.training_dummy.title"),
                    lines.toArray(new Line[0]));
        } else if (summary) {
            if (System.currentTimeMillis() >= summaryUntilMs) { summary = false; return; }
            double durSec = serverDurationTicks / 20.0;
            double dps = durSec > 0.1 ? totalDamage / durSec : 0;
            drawPanel(g, font, sw, baseY,
                    Component.translatable("koniava.hud.training_dummy.summary_title"),
                    new Line[]{
                            new Line(Component.translatable("koniava.hud.training_dummy.damage",
                                    String.format("%.1f", totalDamage)), COLOR_VALUE),
                            new Line(Component.translatable("koniava.hud.training_dummy.time",
                                    String.format("%.1f", durSec)), COLOR_VALUE),
                            new Line(Component.translatable("koniava.hud.training_dummy.dps",
                                    String.format("%.1f", dps)), COLOR_DPS),
                    });
        }
    }

    private record Line(Component text, int color) {}

    private static void drawPanel(GuiGraphics g, Font font, int sw, int baseY, Component title, Line[] lines) {
        int lineH = font.lineHeight + 2;
        int titleW = font.width(title);
        int maxW = titleW;
        for (Line l : lines) maxW = Math.max(maxW, font.width(l.text()));
        int pad = 4;
        int panelW = maxW + pad * 2;
        int panelH = lineH * (lines.length + 1) + pad * 2;
        int panelX = (sw - panelW) / 2;

        g.fill(panelX, baseY - pad, panelX + panelW, baseY - pad + panelH, PANEL_BG);

        int y = baseY;
        g.drawString(font, title, (sw - titleW) / 2, y, COLOR_TITLE, false);
        y += lineH;
        for (Line l : lines) {
            int w = font.width(l.text());
            g.drawString(font, l.text(), (sw - w) / 2, y, l.color(), false);
            y += lineH;
        }
    }
}
