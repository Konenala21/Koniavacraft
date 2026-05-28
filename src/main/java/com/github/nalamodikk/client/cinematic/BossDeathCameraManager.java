package com.github.nalamodikk.client.cinematic;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.entity.PlayerCloneEntity;
import com.github.nalamodikk.common.entity.NaraPhantomEntity;
import com.github.nalamodikk.narasystem.nara.hud.NaraSoundHelper;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Boss 死亡演出的完整鏡頭接管：
 *  - TRACKING_BOSS：偵測到附近 boss DEATH_PHASE > 0 時啟動，相機環繞 boss
 *  - OUTRO_NARA：boss 消失後自動接 Nara 幻影視角 + 對白 + 配音
 *  - OUTRO_FADE：黑幕淡出後歸還控制
 * 透過 CameraMixin 的 getCameraPose() 接管相機；MouseHandlerT6Mixin 走 isActive() 鎖滑鼠
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class BossDeathCameraManager {

    public record CameraPose(double x, double y, double z, float yaw, float pitch) {}

    private enum State { NONE, TRACKING_BOSS, OUTRO_NARA, OUTRO_FADE }

    // 偵測範圍
    private static final double DETECT_RANGE = 80.0;
    private static final double DETECT_RANGE_SQ = DETECT_RANGE * DETECT_RANGE;

    // Outro 時序
    private static final int OUTRO_NARA_TICKS = 280;   // 14s Nara 對白（ZH 10s + EN 7.2s 都有 ~4s 緩衝）
    private static final int OUTRO_FADE_TICKS = 30;    // 1.5s 黑幕淡出
    private static final int NARA_VOICE_DELAY = 40;    // outro 開始 2s 後播配音（給玩家看清 Nara 出現再開口）

    // Cinematic 結束後的冷卻，防止管理員短時間內重複觸發同一支 boss 的演出
    private static final int POST_STOP_COOLDOWN = 100; // 5s 冷卻
    private static int postStopCooldown = 0;
    private static UUID lastCinematicBossId = null;

    private static State state = State.NONE;
    private static int stateTick = 0;
    private static PlayerCloneEntity trackedBoss = null;
    private static UUID bossSourceId = null;
    private static Vec3 bossLastPos = Vec3.ZERO;
    private static Vec3 bossDeathPos = Vec3.ZERO;
    private static NaraPhantomEntity trackedNara = null;
    private static CameraType savedCameraType = null;
    private static boolean voicePlayed = false;
    private static float fadeInAlpha = 0f;    // 從 boss 階段過渡 outro 時瞬間黑幕，淡入再亮起
    private static int fadeInTicks = 0;
    private static final int FADE_IN_TICKS_MAX = 15;
    // 累積式 orbit angle（取代 t × speed），避免階段切換時 angle 跳變導致相機瞬移
    private static float orbitAngle = 0f;

    public static boolean isActive() { return state != State.NONE; }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            if (state != State.NONE) hardStop();
            return;
        }

        if (postStopCooldown > 0) postStopCooldown--;
        switch (state) {
            case NONE -> {
                if (postStopCooldown > 0) return; // 冷卻期內不重新觸發
                PlayerCloneEntity boss = findNearestDyingBoss(player);
                if (boss != null) startTracking(boss);
            }
            case TRACKING_BOSS -> tickTracking(player);
            case OUTRO_NARA -> tickOutroNara(player);
            case OUTRO_FADE -> tickOutroFade();
        }
    }

    private static PlayerCloneEntity findNearestDyingBoss(LocalPlayer player) {
        AABB box = player.getBoundingBox().inflate(DETECT_RANGE);
        PlayerCloneEntity best = null;
        double bestSq = DETECT_RANGE_SQ;
        for (PlayerCloneEntity e : player.level().getEntitiesOfClass(PlayerCloneEntity.class, box)) {
            if (e.getDeathPhase() <= 0) continue;
            double dsq = e.distanceToSqr(player);
            if (dsq < bestSq) { bestSq = dsq; best = e; }
        }
        return best;
    }

    private static void startTracking(PlayerCloneEntity boss) {
        Minecraft mc = Minecraft.getInstance();
        state = State.TRACKING_BOSS;
        stateTick = 0;
        trackedBoss = boss;
        bossSourceId = boss.getSourceUUID().orElse(null);
        bossLastPos = boss.position();
        savedCameraType = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        voicePlayed = false;
        orbitAngle = 0f;
    }

    // Safety：boss 死亡演出最長 20s = 400t，加 100t buffer 避免 stateTick 失控卡死玩家
    private static final int TRACKING_MAX_TICKS = 500;

    private static void tickTracking(LocalPlayer player) {
        stateTick++;
        // Safety hatch：超時直接 fade 釋放控制（避免任何邊角狀況 boss 沒正常 remove 導致卡死）
        if (stateTick > TRACKING_MAX_TICKS) {
            bossDeathPos = bossLastPos;
            startFade();
            return;
        }
        if (trackedBoss != null && trackedBoss.isAlive() && !trackedBoss.isRemoved()) {
            bossLastPos = trackedBoss.position();
            int phase = trackedBoss.getDeathPhase();
            // Phase 5 boss 不再渲染（renderer return），繼續追蹤會讓玩家看到「boss 消失 + 等一下才切 Nara」
            // 兩次轉場感。直接在 phase 5 開始就轉 outro，讓 boss 消失 = 場景切換 = 一次轉場
            if (phase >= 5) {
                // 不 return，跳到下面的 outro 轉場邏輯
            } else {
                orbitAngle += (phase >= 4 ? 0.0026f : 0.0010f);
                return;
            }
        }
        // Boss 消失了 → 轉 Outro Nara 階段
        bossDeathPos = bossLastPos;
        NaraPhantomEntity nara = findNara(player);
        if (nara == null) {
            // 找不到 Nara 直接結束
            startFade();
            return;
        }
        trackedNara = nara;
        state = State.OUTRO_NARA;
        stateTick = 0;
        fadeInTicks = FADE_IN_TICKS_MAX;
    }

    private static NaraPhantomEntity findNara(LocalPlayer player) {
        AABB box = player.getBoundingBox().inflate(DETECT_RANGE);
        for (NaraPhantomEntity n : player.level().getEntitiesOfClass(NaraPhantomEntity.class, box)) {
            if (bossSourceId == null
                    || n.getSourceUUID().map(bossSourceId::equals).orElse(false)) {
                return n;
            }
        }
        return null;
    }

    private static void tickOutroNara(LocalPlayer player) {
        stateTick++;
        if (fadeInTicks > 0) fadeInTicks--;
        // 配音延遲後播放（給玩家先看清 Nara 出現）
        if (!voicePlayed && stateTick >= NARA_VOICE_DELAY) {
            NaraSoundHelper.play("void_mirror", "victory");
            voicePlayed = true;
        }
        // Nara 失蹤或 outro 時間到 → 進入淡出
        if (stateTick >= OUTRO_NARA_TICKS || trackedNara == null
                || !trackedNara.isAlive() || trackedNara.isRemoved()) {
            startFade();
        }
    }

    private static void startFade() {
        state = State.OUTRO_FADE;
        stateTick = 0;
    }

    private static void tickOutroFade() {
        stateTick++;
        if (stateTick >= OUTRO_FADE_TICKS) hardStop();
    }

    private static void hardStop() {
        Minecraft mc = Minecraft.getInstance();
        if (savedCameraType != null) {
            mc.options.setCameraType(savedCameraType);
            savedCameraType = null;
        }
        state = State.NONE;
        stateTick = 0;
        trackedBoss = null;
        trackedNara = null;
        bossSourceId = null;
        voicePlayed = false;
    }

    // ── 鏡頭位姿（CameraMixin 在 Camera.setup 末段呼叫）──────────────────────
    @Nullable
    public static CameraPose getCameraPose(float partialTick) {
        return switch (state) {
            case TRACKING_BOSS -> trackingPose(partialTick);
            case OUTRO_NARA -> naraPose(partialTick);
            case OUTRO_FADE -> naraPose(partialTick); // 淡出期間相機停在 Nara 處
            case NONE -> null;
        };
    }

    // Boss 死亡期間：相機在 boss 周圍緩慢繞圈 + 抬高俯視
    // orbit angle 用累積方式（每 tick += speed）避免階段切換時 angle 跳變
    private static CameraPose trackingPose(float partialTick) {
        if (trackedBoss == null) return null;
        Vec3 bossPos = bossLastPos;
        float t = stateTick + partialTick;
        double camRadius = 6.0;
        double camHeight = 3.2 + Math.sin(t * 0.04) * 0.4;
        int phase = trackedBoss.getDeathPhase();
        float currentSpeed = phase >= 4 ? 0.0026f : 0.0010f;
        float renderAngle = orbitAngle + currentSpeed * partialTick;
        double cx = bossPos.x + Math.cos(renderAngle) * camRadius;
        double cy = bossPos.y + camHeight;
        double cz = bossPos.z + Math.sin(renderAngle) * camRadius;

        // Phase 4（碎裂）加震動
        if (phase == 4) {
            double amp = 0.25;
            cx += (Math.random() - 0.5) * amp;
            cy += (Math.random() - 0.5) * amp;
            cz += (Math.random() - 0.5) * amp;
        }

        // 看向 boss 中心略偏上
        Vec3 look = bossPos.add(0, 1.2, 0);
        return lookAt(cx, cy, cz, look);
    }

    // Outro Nara 階段：相機在 Nara 前方 4 格、稍高，看向 Nara 上半身
    private static CameraPose naraPose(float partialTick) {
        if (trackedNara == null) return null;
        Vec3 naraPos = trackedNara.position();
        float t = stateTick + partialTick;

        // 起始位置：靠近 Nara 前方斜上方；隨 t 慢慢推近
        float p = Mth.clamp(t / OUTRO_NARA_TICKS, 0f, 1f);
        float smooth = p * p * (3f - 2f * p);
        // Nara yaw 為她當下面向；用她 yRot 來算前方
        float naraYaw = trackedNara.getYRot();
        double yawRad = Math.toRadians(naraYaw);
        // 前方向量（vanilla yaw=0 面對 +Z）
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);
        // 相機放 Nara 前方 4→3 格、高度 2.0，給玩家正面看
        double dist = Mth.lerp(smooth, 4.0, 3.0);
        double cx = naraPos.x + fwdX * dist;
        double cy = naraPos.y + 2.0;
        double cz = naraPos.z + fwdZ * dist;

        Vec3 look = naraPos.add(0, 1.5, 0);
        return lookAt(cx, cy, cz, look);
    }

    private static CameraPose lookAt(double cx, double cy, double cz, Vec3 look) {
        double dx = look.x - cx, dy = look.y - cy, dz = look.z - cz;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, horiz));
        return new CameraPose(cx, cy, cz, yaw, pitch);
    }

    // ── 黑幕 + 對話框 ────────────────────────────────────────────────────────
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (state == State.NONE) return;
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        // 過場到 outro 的瞬間有 0.75s 黑幕淡入再亮起
        if (state == State.OUTRO_NARA && fadeInTicks > 0) {
            float a = fadeInTicks / (float) FADE_IN_TICKS_MAX;
            int alpha = (int) (a * 255f) << 24;
            g.fill(0, 0, sw, sh, alpha);
        }
        // outro 結尾的黑幕淡出
        if (state == State.OUTRO_FADE) {
            float a = Mth.clamp(stateTick / (float) OUTRO_FADE_TICKS, 0f, 1f);
            int alpha = (int) (a * 255f) << 24;
            g.fill(0, 0, sw, sh, alpha);
        }
        // Nara 對話框（outro 配音播放期間顯示）
        if (state == State.OUTRO_NARA && stateTick >= NARA_VOICE_DELAY) {
            drawDialogue(mc, g, sw, sh);
        }
    }

    private static void drawDialogue(Minecraft mc, GuiGraphics g, int sw, int sh) {
        Component line = Component.translatable("nara.dialogue.void_mirror.victory");
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

    // 過場期間隱藏所有 vanilla HUD（保留 RenderGuiEvent.Post 給字幕框畫）
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (state != State.NONE) event.setCanceled(true);
    }

    // 抑制玩家移動 / 互動輸入
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (state == State.NONE) return;
        var in = event.getInput();
        in.leftImpulse = 0f;
        in.forwardImpulse = 0f;
        in.up = in.down = in.left = in.right = false;
        in.jumping = false;
        in.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (state != State.NONE) event.setCanceled(true);
    }
}
