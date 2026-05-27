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
            {0.0, 4.5, -22.0},  // A 開場：遠後方高處，俯瞰玩家從裂縫走出
            {0.0, 2.4, -9.0},   // B 與玩家走出同步推近到後方眼高
            {0.0, 2.6, -3.0},   // C 推進到玩家身邊
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
    // 各段時長（tick）：A 玩家走出, B 停(對話), C 推進, D 轉前方+boss 登場（總長 ~31s 更史詩）
    private static final int[] SEG_DURATIONS = {160, 90, 170, 200};
    private static final int TOTAL_TICKS = 160 + 90 + 170 + 200; // 620
    // 幻影爆炸時的相機震動視窗（對齊 boss INTRO_REVEAL_TICK = 570）
    private static final int SHAKE_START = 568;
    private static final int SHAKE_END = 595;
    // 收尾轉黑：最後 15t 淡入黑幕，結束後 15t 淡出
    private static final int FADE_IN_START = 620 - 15;
    private static final int FADE_OUT_LEN = 15;

    // 玩家從身後裂縫往前走出來的時間窗（開場娜拉視角這段）
    private static final int WALK_START = 20;
    private static final int WALK_END = 110;
    private static final double WALK_BACK = -3.2; // 起步點：登場點後方 3.2 格（沿玩家面向反方向）
    // 玩家走出後娜拉才開口
    private static final int DIALOGUE_START = 130;

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
        // 走出來：裂縫在玩家身後（-Z）噴殘餘能量粒子 + 手動推進走路動畫（過場中玩家實際不動）
        if (ticks >= WALK_START && ticks <= WALK_END && mc.player != null && mc.level != null) {
            double crackZ = mc.player.getZ() + WALK_BACK; // 裂縫在登場點後方
            for (int i = 0; i < 5; i++) {
                double a = mc.level.random.nextDouble() * Math.PI * 2;
                double r = 0.4 + mc.level.random.nextDouble() * 0.7;
                mc.level.addParticle(ParticleTypes.PORTAL,
                        mc.player.getX() + Math.cos(a) * r,
                        mc.player.getY() + 0.1,
                        crackZ + Math.sin(a) * r,
                        0.0, 0.05, 0.0);
            }
            mc.player.walkAnimation.update(1.0f, 1.0f); // 驅動四肢擺動
        }

        if (ticks == DIALOGUE_START) {
            NaraSoundHelper.play("void_mirror", "intro");
        }

        ticks++;
        if (ticks >= TOTAL_TICKS) {
            stop();
        }
    }

    // 玩家走出：渲染時把玩家模型從登場點後方平滑平移到登場點（真實位置固定，避免窒息/輸入鎖）
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!active || event.getEntity() != Minecraft.getInstance().player) return;
        double off = walkOffset(ticks + event.getPartialTick());
        event.getPoseStack().pushPose();
        event.getPoseStack().translate(0.0, 0.0, off);
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (!active || event.getEntity() != Minecraft.getInstance().player) return;
        event.getPoseStack().popPose();
    }

    // 走出位移：玩家從登場點後方 WALK_BACK 處沿 +Z 平滑走到登場點（0）
    private static double walkOffset(float t) {
        if (t < WALK_START) return WALK_BACK;            // 還在裂縫後方
        if (t >= WALK_END) return 0.0;                   // 已走到登場點
        float s = (t - WALK_START) / (WALK_END - WALK_START);
        s = s * s * (3f - 2f * s); // smoothstep
        return WALK_BACK * (1.0 - s);
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
