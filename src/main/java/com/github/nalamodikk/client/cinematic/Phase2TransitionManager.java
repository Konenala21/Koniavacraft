package com.github.nalamodikk.client.cinematic;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screen.cinematic.CinematicSkipHelper;
import com.github.nalamodikk.common.entity.PlayerCloneEntity;
import com.github.nalamodikk.common.network.packet.server.Phase2SkipPacket;
import com.github.nalamodikk.register.client.ModKeyMappings;
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
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * 二階段方塊機甲變身過場（client）。
 * 鎖相機環繞 boss 旋轉，仿進場演出但更短（4 秒），支援按 R 跳過。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class Phase2TransitionManager {

    public record CameraPose(double x, double y, double z, float yaw, float pitch) {}

    private static final int TOTAL_TICKS = 220;         // 11 秒，配合 server 端 PHASE2_TRANSITION_LEN
    private static final int FADE_IN_START = 205;
    private static final int FADE_OUT_LEN = 15;

    // 鏡頭 keyframe（local offset，相對 boss 的「前後左右」，會依 boss yaw 旋轉到世界座標）
    // KF0/1 = 後方遠處（看方塊從天空飛來組裝），KF2/3 = 前方遠處（看完成的機甲面向玩家）
    // local 軸：+Z = boss 前方、-Z = boss 後方、+X = boss 右側、+Y = 高度
    private static final double[][] CAM_OFFSETS = {
            {0, 6, -20},   // KF0 後方遠處
            {0, 6, -20},   // KF1（hold）
            {0, 6,  20},   // KF2 前方遠處
            {0, 6,  20},   // KF3（hold）
    };
    private static final double[][] LOOK_OFFSETS = {
            {0, 5, 0},   // 看 boss 胸口高度
            {0, 5, 0},
            {0, 5, 0},
            {0, 5, 0},
    };
    // hold 80t → 轉場 40t → hold 100t（合計 220）
    private static final int[] SEG_DURATIONS = {80, 40, 100};

    private static boolean active = false;
    private static int ticks = 0;
    private static int fadeOutTicks = 0;
    private static int bossEntityId = -1;
    private static CameraType savedCameraType = null;

    public static void start(int bossId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        bossEntityId = bossId;
        ticks = 0;
        savedCameraType = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        active = true;
    }

    public static void stop() {
        active = false;
        fadeOutTicks = FADE_OUT_LEN;
        bossEntityId = -1;
        Minecraft mc = Minecraft.getInstance();
        if (savedCameraType != null) {
            mc.options.setCameraType(savedCameraType);
            savedCameraType = null;
        }
    }

    public static boolean isActive() {
        return active;
    }

    @Nullable
    private static PlayerCloneEntity boss() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || bossEntityId < 0) return null;
        return mc.level.getEntity(bossEntityId) instanceof PlayerCloneEntity pc ? pc : null;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!active) {
            if (fadeOutTicks > 0) fadeOutTicks--;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (ModKeyMappings.NARA_SKIP.consumeClick()) {
            CinematicSkipHelper.requestSkip(() -> {
                PacketDistributor.sendToServer(Phase2SkipPacket.INSTANCE);
                if (ticks < FADE_IN_START) ticks = FADE_IN_START;
            });
        }
        while (mc.options.keyTogglePerspective.consumeClick()) { /* swallow */ }
        if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        // boss 周圍粒子環（強化「方塊聚集」感）
        PlayerCloneEntity b = boss();
        if (b != null && mc.level != null) {
            for (int i = 0; i < 4; i++) {
                double a = mc.level.random.nextDouble() * Math.PI * 2;
                double r = 2.0 + mc.level.random.nextDouble() * 3.0;
                double y = mc.level.random.nextDouble() * 6.0;
                mc.level.addParticle(ParticleTypes.LARGE_SMOKE,
                        b.getX() + Math.cos(a) * r,
                        b.getY() + y,
                        b.getZ() + Math.sin(a) * r,
                        0.0, 0.04, 0.0);
            }
        }

        ticks++;
        if (ticks >= TOTAL_TICKS) {
            stop();
        }
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (active) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (active) event.setCanceled(true);
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

    private static float currentFadeAlpha() {
        if (active) {
            if (ticks >= FADE_IN_START) {
                return Mth.clamp((ticks - FADE_IN_START) / (float) (TOTAL_TICKS - FADE_IN_START), 0f, 1f);
            }
            return 0f;
        }
        return fadeOutTicks > 0 ? fadeOutTicks / (float) FADE_OUT_LEN : 0f;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        if (active) {
            Component hint = Component.translatable("message.koniava.void_mirror.skip_hint");
            g.drawString(mc.font, hint, (sw - mc.font.width(hint)) / 2, sh - 12, 0xFFCCCCCC, true);
        }

        float fade = currentFadeAlpha();
        if (fade <= 0f) return;
        int alpha = (int) (Mth.clamp(fade, 0f, 1f) * 255f) << 24;
        g.fill(0, 0, sw, sh, alpha);
    }

    @Nullable
    public static CameraPose getCameraPose(float partialTick) {
        if (!active) return null;
        PlayerCloneEntity b = boss();
        if (b == null) return null;
        float t = Math.min(ticks + partialTick, TOTAL_TICKS);
        double[] camOff = sample(CAM_OFFSETS, t);
        double[] lookOff = sample(LOOK_OFFSETS, t);
        // local → world：依 boss yaw 旋轉 X/Z 偏移，讓「前後」永遠是 boss 的朝向
        double bx = b.getX(), by = b.getY(), bz = b.getZ();
        double yawRad = Math.toRadians(b.getYRot());
        double sinY = Math.sin(yawRad), cosY = Math.cos(yawRad);
        double cx = bx + camOff[0] * cosY - camOff[2] * sinY;
        double cy = by + camOff[1];
        double cz = bz + camOff[0] * sinY + camOff[2] * cosY;
        double lx = bx + lookOff[0] * cosY - lookOff[2] * sinY;
        double ly = by + lookOff[1];
        double lz = bz + lookOff[0] * sinY + lookOff[2] * cosY;

        // 變身瞬間（前 15t）震動
        if (t < 15) {
            float decay = 1.0f - t / 15f;
            double amp = 0.4 * decay;
            cx += (Math.random() - 0.5) * amp;
            cy += (Math.random() - 0.5) * amp;
            cz += (Math.random() - 0.5) * amp;
        }

        double dx = lx - cx;
        double dy = ly - cy;
        double dz = lz - cz;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, horiz));
        return new CameraPose(cx, cy, cz, yaw, pitch);
    }

    private static double[] sample(double[][] kf, float t) {
        float acc = 0f;
        for (int i = 0; i < SEG_DURATIONS.length; i++) {
            int dur = SEG_DURATIONS[i];
            if (t <= acc + dur || i == SEG_DURATIONS.length - 1) {
                float local = dur <= 0 ? 1f : Mth.clamp((t - acc) / dur, 0f, 1f);
                float s = local * local * (3f - 2f * local); // smoothstep
                double[] a = kf[i];
                double[] c = kf[i + 1];
                return new double[]{
                        Mth.lerp(s, a[0], c[0]),
                        Mth.lerp(s, a[1], c[1]),
                        Mth.lerp(s, a[2], c[2]),
                };
            }
            acc += dur;
        }
        double[] last = kf[kf.length - 1];
        return new double[]{last[0], last[1], last[2]};
    }
}
