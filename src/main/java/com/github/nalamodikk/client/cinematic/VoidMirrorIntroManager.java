package com.github.nalamodikk.client.cinematic;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.narasystem.nara.hud.NaraSoundHelper;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import javax.annotation.Nullable;

/**
 * 鏡中世界進場過場的相機控制（client）。
 * 自 tick 推進，keyframe 相機路徑相對玩家焦點，配合 CameraMixin 接管相機。
 * 目前是骨架：幾段示範運鏡，數值可用 IDEA hotswap 調，並用 /koniava voidmirror intro 重複觸發。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class VoidMirrorIntroManager {

    public record CameraPose(double x, double y, double z, float yaw, float pitch) {}

    // 焦點 = 玩家位置。前方登場點 = 焦點 +Z 20 格（幻影冒出處）。
    // 相機位置（相對焦點的世界偏移），keyframe
    private static final double[][] CAM_OFFSETS = {
            {0.0, 2.4, -15.0},  // A 娜拉視角：後方、眼高，看向玩家
            {0.0, 2.4, -15.0},  // B 停住（對話框時間）
            {0.0, 2.6, -3.0},   // C 推進到裂縫（玩家升出）
            {2.0, 4.0, 12.0},   // D 轉到玩家前方，看向登場點
            {2.0, 4.0, 12.0},   // E 停住（幻影登場）
    };
    // 視線目標（相對焦點偏移）：前三段看玩家，後兩段看前方 20 格登場點
    private static final double[][] LOOK_OFFSETS = {
            {0.0, 1.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 1.0, 20.0},
            {0.0, 1.0, 20.0},
    };
    // 各段時長（tick）：A→B 停, B→C 推進, C→D 轉前方, D→E 停（拉長讓節奏更有戲）
    private static final int[] SEG_DURATIONS = {100, 60, 100, 100};
    private static final int TOTAL_TICKS = 100 + 60 + 100 + 100;
    // 幻影爆炸時的相機震動視窗（對齊 boss INTRO_REVEAL_TICK ≈ 335）
    private static final int SHAKE_START = 333;
    private static final int SHAKE_END = 358;
    // 收尾轉黑：最後 15t 淡入黑幕，結束後 15t 淡出
    private static final int FADE_IN_START = 100 + 60 + 100 + 100 - 15;
    private static final int FADE_OUT_LEN = 15;

    // 玩家先從裂縫鑽出探頭（開場娜拉視角這段），之後娜拉才說話
    private static final int EMERGE_START = 15;
    private static final int EMERGE_END = 75;
    private static final double EMERGE_DEPTH = -2.2;
    // 玩家探出後娜拉才開口
    private static final int DIALOGUE_START = 70;

    private static boolean active = false;
    private static int ticks = 0;
    private static int fadeOutTicks = 0;
    private static Vec3 focus = Vec3.ZERO;
    private static CameraType savedCameraType = null;

    public static void start() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        focus = mc.player.position();
        ticks = 0;
        savedCameraType = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        active = true;
    }

    public static void stop() {
        active = false;
        fadeOutTicks = FADE_OUT_LEN;
        Minecraft mc = Minecraft.getInstance();
        if (savedCameraType != null) {
            mc.options.setCameraType(savedCameraType);
            savedCameraType = null;
        }
    }

    public static boolean isActive() {
        return active;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!active) {
            if (fadeOutTicks > 0) fadeOutTicks--;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // 鎖定第三人稱：吞掉切換視角(F5)按鍵，並把視角強制設回
        while (mc.options.keyTogglePerspective.consumeClick()) { /* swallow */ }
        if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        // 裂縫粒子：玩家升出期間在腳下的裂縫位置噴發
        if (ticks >= EMERGE_START - 10 && ticks <= EMERGE_END && mc.player != null && mc.level != null) {
            for (int i = 0; i < 4; i++) {
                double a = mc.level.random.nextDouble() * Math.PI * 2;
                double r = 0.4 + mc.level.random.nextDouble() * 0.6;
                mc.level.addParticle(ParticleTypes.PORTAL,
                        mc.player.getX() + Math.cos(a) * r,
                        mc.player.getY() + 0.1,
                        mc.player.getZ() + Math.sin(a) * r,
                        0.0, 0.05, 0.0);
            }
        }

        if (ticks == DIALOGUE_START) {
            NaraSoundHelper.play("void_mirror", "intro");
        }

        ticks++;
        if (ticks >= TOTAL_TICKS) {
            stop();
        }
    }

    // 玩家升出：渲染時把玩家模型從地下平滑升上來（真實位置仍在地表，避免窒息/卡牆）
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!active || event.getEntity() != Minecraft.getInstance().player) return;
        double off = emergeOffset(ticks + event.getPartialTick());
        event.getPoseStack().pushPose();
        event.getPoseStack().translate(0.0, off, 0.0);
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (!active || event.getEntity() != Minecraft.getInstance().player) return;
        event.getPoseStack().popPose();
    }

    private static double emergeOffset(float t) {
        if (t < EMERGE_START) return EMERGE_DEPTH;       // 還埋在地底
        if (t >= EMERGE_END) return 0.0;                 // 已站在地表
        float s = (t - EMERGE_START) / (EMERGE_END - EMERGE_START);
        s = s * s * (3f - 2f * s); // smoothstep
        return EMERGE_DEPTH * (1.0 - s);
    }

    // 過場期間隱藏所有 vanilla HUD 圖層（保留 RenderGuiEvent.Post 讓字幕框能畫）
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (active) event.setCanceled(true);
    }

    private static float currentFadeAlpha() {
        if (active) {
            if (ticks >= FADE_IN_START) {
                return Mth.clamp((ticks - FADE_IN_START) / (float) (TOTAL_TICKS - FADE_IN_START), 0f, 1f);
            }
            return 0f;
        }
        return fadeOutTicks > 0 ? fadeOutTicks / (float) FADE_OUT_LEN : 0f;
    }

    // 收尾黑幕 + 娜拉台詞字幕框（無立繪，底部）
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        float fade = currentFadeAlpha();
        boolean showDialogue = active && ticks >= DIALOGUE_START && ticks < FADE_IN_START;
        if (fade <= 0f && !showDialogue) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        if (fade > 0f) {
            int alpha = (int) (Mth.clamp(fade, 0f, 1f) * 255f) << 24;
            g.fill(0, 0, sw, sh, alpha);
        }
        if (!showDialogue) return;

        Component line = Component.translatable("nara.dialogue.void_mirror.intro");
        int boxW = Math.min(sw - 40, 360);
        int x = (sw - boxW) / 2;
        int nameH = mc.font.lineHeight + 3;
        var wrapped = mc.font.split(line, boxW - 16);
        int boxH = 8 + nameH + wrapped.size() * (mc.font.lineHeight + 1) + 4;
        int y = sh - boxH - 24;

        int bg = 0xCC0A0A0F;
        int teal = 0x8800FFCC;
        g.fill(x, y, x + boxW, y + boxH, bg);
        g.fill(x, y, x + boxW, y + 1, teal);
        g.fill(x, y + boxH - 1, x + boxW, y + boxH, teal);
        g.fill(x, y, x + 1, y + boxH, teal);
        g.fill(x + boxW - 1, y, x + boxW, y + boxH, teal);

        g.drawString(mc.font, Component.translatable("nara.hud.name"), x + 8, y + 6, 0xFF00FFCC, true);
        int ty = y + 6 + nameH;
        for (var l : wrapped) {
            g.drawString(mc.font, l, x + 8, ty, 0xFFFFFFFF, false);
            ty += mc.font.lineHeight + 1;
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!active) return;
        var in = event.getInput();
        in.leftImpulse = 0f;
        in.forwardImpulse = 0f;
        in.up = false;
        in.down = false;
        in.left = false;
        in.right = false;
        in.jumping = false;
        in.shiftKeyDown = false;
    }

    @Nullable
    public static CameraPose getCameraPose(float partialTick) {
        if (!active) return null;
        float t = Math.min(ticks + partialTick, TOTAL_TICKS);

        double[] camOff = sample(CAM_OFFSETS, t);
        double[] lookOff = sample(LOOK_OFFSETS, t);

        Vec3 cam = focus.add(camOff[0], camOff[1], camOff[2]);
        Vec3 look = focus.add(lookOff[0], lookOff[1], lookOff[2]);

        // 幻影爆炸時相機震動
        if (t >= SHAKE_START && t <= SHAKE_END) {
            float decay = 1.0f - (t - SHAKE_START) / (float) (SHAKE_END - SHAKE_START);
            double amp = 0.6 * decay;
            cam = cam.add((Math.random() - 0.5) * amp, (Math.random() - 0.5) * amp, (Math.random() - 0.5) * amp);
        }

        double dx = look.x - cam.x;
        double dy = look.y - cam.y;
        double dz = look.z - cam.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, horiz));

        return new CameraPose(cam.x, cam.y, cam.z, yaw, pitch);
    }

    private static double[] sample(double[][] kf, float t) {
        float acc = 0f;
        for (int i = 0; i < SEG_DURATIONS.length; i++) {
            int dur = SEG_DURATIONS[i];
            if (t <= acc + dur || i == SEG_DURATIONS.length - 1) {
                float local = dur <= 0 ? 1f : Mth.clamp((t - acc) / dur, 0f, 1f);
                float s = local * local * (3f - 2f * local); // smoothstep
                double[] a = kf[i];
                double[] b = kf[i + 1];
                return new double[]{
                        Mth.lerp(s, a[0], b[0]),
                        Mth.lerp(s, a[1], b[1]),
                        Mth.lerp(s, a[2], b[2]),
                };
            }
            acc += dur;
        }
        double[] last = kf[kf.length - 1];
        return new double[]{last[0], last[1], last[2]};
    }
}
