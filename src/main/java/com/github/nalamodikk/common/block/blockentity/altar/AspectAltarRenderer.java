package com.github.nalamodikk.common.block.blockentity.altar;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.renderer.altar.AltarUpgradeAnimManager;
import com.github.nalamodikk.common.utils.render.BlockbenchModelRenderUtils;
import com.github.nalamodikk.common.utils.render.BlockbenchModelRenderUtils.ModelElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.github.nalamodikk.client.screenAPI.MIRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class AspectAltarRenderer implements BlockEntityRenderer<AspectAltarBlockEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();


    // 整體閒置動畫（地球自轉風格）
    private static final float TILT_DEGREES  = 23.5f;
    private static final float SPIN_SPEED    = 1.2f;
    private static final float BOB_SPEED     = 0.05f;
    private static final float BOB_AMPLITUDE = 0.07f;

    // Rubik's cube 動畫節奏
    private static final float PHASE_TICKS   = 40f;
    private static final float TURN_FRACTION = 0.375f;

    // 共鳴環動畫 (T1–T12)
    // OBJ：YZ 平面，頂點幾何中心 (cx=0.0625, cy=0.0625, cz=-0.2125)，OBJ 中心線半徑 ~3.16 方塊
    // scale = 目標半徑 / 3.16f
    private static final float OBJ_RADIUS    = 3.16f;
    // 動態速度：時間扭曲振幅（越大速度變化越劇烈）與頻率（越大速度改變越頻繁）
    private static final float WARP_AMPLITUDE = 14f;
    private static final float WARP_FREQ      = 0.06f;
    private static final float RING_CX     = 0.0625f;
    private static final float RING_CY     = 0.0625f;
    private static final float RING_CZ     = -0.2125f;
    // 12 tier 渲染參數：{ scale, tiltAxisCode, tiltDeg, spinAxisCode, spinSpeed }
    // tiltAxisCode/spinAxisCode: 0=YP 1=ZP 2=XP
    // spinSpeed 負數 = 反向
    private record RingConfig(float scale, Axis tiltAxis, float tiltDeg, Axis spinAxis, float spinSpeed) {}
    // 所有環同速（0.60 / -0.60），t=0 全部在戴森球對齊位，每 15 秒回正一次
    private static final RingConfig[] RING_CONFIGS = {
        // T1–T3：半徑 7，內層，正轉
        new RingConfig(7f/OBJ_RADIUS,  Axis.ZP,  90f, Axis.YP,  0.60f),  // T1 水平 XZ
        new RingConfig(7f/OBJ_RADIUS,  Axis.YP,  90f, Axis.ZP,  0.60f),  // T2 垂直 XY
        new RingConfig(7f/OBJ_RADIUS,  Axis.YP,   0f, Axis.ZP,  0.60f),  // T3 垂直 YZ
        // T4–T6：半徑 9，外層，逆轉
        new RingConfig(9f/OBJ_RADIUS,  Axis.ZP,  90f, Axis.YP, -0.60f),  // T4 水平 XZ
        new RingConfig(9f/OBJ_RADIUS,  Axis.YP,  90f, Axis.ZP, -0.60f),  // T5 垂直 XY
        new RingConfig(9f/OBJ_RADIUS,  Axis.YP,   0f, Axis.ZP, -0.60f),  // T6 垂直 YZ
    };
    private static final ModelResourceLocation RING_MODEL_LOC = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "block/resonance_ring"),
            "standalone");

    // 元素位置分界線（Blockbench 座標 / 16 → 方塊單位 0~1）
    // Y 軸（水平層）
    private static final float Y_BOT_MID = 4.35f  / 16f;
    private static final float Y_MID_TOP = 9.05f  / 16f;
    // X 軸（左右縱列）
    private static final float X_L_MID   = 5.85f  / 16f;
    private static final float X_MID_R   = 10.2f  / 16f;
    // Z 軸（前後縱排）
    private static final float Z_F_MID   = 5.45f  / 16f;
    private static final float Z_MID_B   = 9.75f  / 16f;
    // 整個 cube 的幾何中心（Blockbench 座標量測值）
    private static final float CUBE_CY   = 6.7f  / 16f;  // Y 中心（X/Z 旋轉用）
    private static final float CUBE_CZ   = 7.6f  / 16f;  // Z 中心（Y/X 旋轉用，非 0.5）

    // 每個 phase 的設定：選哪些格子、繞哪個軸、樞紐點在哪
    // 樞紐點只有垂直於旋轉軸的分量才有意義：
    //   Y 旋轉 → XZ 樞紐 = (0.5, *, 0.5)
    //   X 旋轉 → YZ 樞紐 = (*, CUBE_CY, 0.5)
    //   Z 旋轉 → XY 樞紐 = (0.5, CUBE_CY, *)
    private record PhaseConfig(Predicate<ModelElement> selector, Axis axis,
                                float px, float py, float pz) {}

    private static final PhaseConfig[] PHASES = {
        // Y 旋轉樞紐：XZ 中心 = (0.5, CUBE_CZ)；X 旋轉樞紐：YZ 中心 = (CUBE_CY, CUBE_CZ)；Z 旋轉樞紐：XY 中心 = (0.5, CUBE_CY)
        new PhaseConfig(e -> cy(e) > Y_MID_TOP,                           Axis.YP, 0.5f, 0.5f,    CUBE_CZ),  // 頂層
        new PhaseConfig(e -> cx(e) > X_MID_R,                             Axis.XP, 0.5f, CUBE_CY, CUBE_CZ),  // 右列
        new PhaseConfig(e -> cz(e) < Z_F_MID,                             Axis.ZP, 0.5f, CUBE_CY, 0.5f),     // 前排
        new PhaseConfig(e -> cy(e) < Y_BOT_MID,                           Axis.YP, 0.5f, 0.5f,    CUBE_CZ),  // 底層
        new PhaseConfig(e -> cx(e) < X_L_MID,                             Axis.XP, 0.5f, CUBE_CY, CUBE_CZ),  // 左列
        new PhaseConfig(e -> cz(e) > Z_MID_B,                             Axis.ZP, 0.5f, CUBE_CY, 0.5f),     // 後排
        new PhaseConfig(e -> cy(e) >= Y_BOT_MID && cy(e) <= Y_MID_TOP,    Axis.YP, 0.5f, 0.5f,    CUBE_CZ),  // 中層 E
        new PhaseConfig(e -> cx(e) >= X_L_MID   && cx(e) <= X_MID_R,      Axis.XP, 0.5f, CUBE_CY, CUBE_CZ),  // 中列 M
        new PhaseConfig(e -> cz(e) >= Z_F_MID   && cz(e) <= Z_MID_B,      Axis.ZP, 0.5f, CUBE_CY, 0.5f),     // 中排 S
    };

    private static float cx(ModelElement e) { return (e.x1 + e.x2) * 0.5f; }
    private static float cy(ModelElement e) { return (e.y1 + e.y2) * 0.5f; }
    private static float cz(ModelElement e) { return (e.z1 + e.z2) * 0.5f; }

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/aspect_altar_texture.png");
    private static final ResourceLocation MODEL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "models/block/aspect_altar.json");

    private final List<ModelElement> allElements = new ArrayList<>();
    private boolean modelLoaded = false;
    private List<BakedQuad> cachedRingQuads = null;

    private List<ModelElement>[] phaseRotating = null;
    private List<ModelElement>[] phaseStill    = null;

    public AspectAltarRenderer(BlockEntityRendererProvider.Context ctx) {
        loadAndParseModel();
    }

    private void loadAndParseModel() {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(MODEL_LOCATION);
            if (resource.isEmpty()) {
                LOGGER.error("Missing aspect altar model: {}", MODEL_LOCATION);
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                JsonObject modelData = JsonParser.parseReader(reader).getAsJsonObject();
                allElements.clear();
                allElements.addAll(BlockbenchModelRenderUtils.parseElements(modelData));
                buildPhasePartitions();
                modelLoaded = true;
                LOGGER.debug("Loaded {} elements for aspect altar", allElements.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load aspect altar model.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void buildPhasePartitions() {
        phaseRotating = new List[PHASES.length];
        phaseStill    = new List[PHASES.length];
        for (int i = 0; i < PHASES.length; i++) {
            Predicate<ModelElement> sel = PHASES[i].selector();
            List<ModelElement> rot   = new ArrayList<>();
            List<ModelElement> still = new ArrayList<>();
            for (ModelElement e : allElements) {
                (sel.test(e) ? rot : still).add(e);
            }
            phaseRotating[i] = List.copyOf(rot);
            phaseStill[i]    = List.copyOf(still);
        }
    }

    @Override
    public void render(AspectAltarBlockEntity altar, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = altar.getLevel();
        if (level == null || !altar.isFormed() || !modelLoaded) return;

        float time = level.getGameTime() + partialTick;
        float ringTime = (level.getGameTime() - altar.getRingPhaseStart()) + partialTick;
        boolean active = altar.isActive();
        int tier = altar.getUpgradeTier();

        renderFormedCore(poseStack, bufferSource, packedLight, packedOverlay, time, active);
        renderRings(altar, poseStack, bufferSource, packedLight, packedOverlay, ringTime);
        renderSolarCore(altar, poseStack, bufferSource, time);
        renderSealSystem(altar, poseStack, bufferSource, packedLight, packedOverlay);

        if (active) {
            renderOrbitals(altar, poseStack, bufferSource, packedLight, packedOverlay, level, time);
        }

        int compTick = altar.getCompletionAnimTick();
        if (compTick > 0) {
            renderCompletionShow(altar, poseStack, bufferSource, compTick, altar.getCompletionDuration(), time, tier);
        }

        AltarUpgradeAnimManager.AnimState upgradeState = AltarUpgradeAnimManager.getState(altar.getBlockPos());
        if (upgradeState != null && !upgradeState.isDone()) {
            renderUpgradeAnimation(upgradeState, poseStack, bufferSource, partialTick);
        }
    }

    // ── 升級動畫 ──────────────────────────────────────────────────────────────────

    private void renderUpgradeAnimation(AltarUpgradeAnimManager.AnimState state,
                                         PoseStack ps, MultiBufferSource mbs, float partialTick) {
        float tick = state.tick() + partialTick;
        if (state.tier() <= 3) {
            renderT1T3Upgrade(ps, mbs, tick);
        } else if (state.tier() <= 5) {
            renderT4T5Upgrade(ps, mbs, tick);
        } else if (state.tier() == 6) {
            if (tick < AltarUpgradeAnimManager.T6_PHASE_OFFSET) {
                renderT4T5Upgrade(ps, mbs, tick);                          // T4-T5 前奏
            } else {
                renderT6Upgrade(ps, mbs, tick - AltarUpgradeAnimManager.T6_PHASE_OFFSET); // T6 高潮
            }
        }
    }

    private void renderT1T3Upgrade(PoseStack ps, MultiBufferSource mbs, float tick) {
        VertexConsumer vc = mbs.getBuffer(MIRenderTypes.solarGlow());

        // 3 段地面環形衝擊波，各 26 ticks，間隔 26t 依序起始
        for (int w = 0; w < 3; w++) {
            float wAge = tick - w * 26f;
            if (wAge <= 0f || wAge > 26f) continue;
            float progress  = wAge / 26f;
            float radius    = progress * 14f;
            float alpha     = (float) Math.sin(progress * Math.PI);
            float thickness = 0.20f + (1f - progress) * 0.30f;
            int   iAlpha    = (int)(alpha * 190);

            ps.pushPose();
            ps.translate(0.5, -1.8, 0.5);
            renderEnergyRing(ps, vc, radius,         thickness,         100, 180, 255, iAlpha);
            renderEnergyRing(ps, vc, radius * 0.85f, thickness * 0.45f, 160, 220, 255, (int)(alpha * 110));
            ps.popPose();
        }

        // 光柱（79-109t）突然出現，短暫停留，淡出
        if (tick > 79f && tick < 109f) {
            float pAge  = tick - 79f;
            float pProg = pAge / 30f;
            float pAlpha;
            if (pProg < 0.15f)      pAlpha = pProg / 0.15f;
            else if (pProg < 0.65f) pAlpha = 1.0f;
            else                     pAlpha = 1.0f - (pProg - 0.65f) / 0.35f;
            renderBluePillar(ps, mbs, 18f, pAlpha);
        }
    }

    private void renderT6Upgrade(PoseStack ps, MultiBufferSource mbs, float tick) {
        VertexConsumer vc = mbs.getBuffer(MIRenderTypes.solarGlow());

        // 過渡地面波（0-40t，銜接 T4-5 結尾）
        for (int w = 0; w < 2; w++) {
            float wAge = tick - w * 20f;
            if (wAge <= 0f || wAge > 20f) continue;
            float progress = wAge / 20f;
            float alpha = (float) Math.sin(progress * Math.PI);
            ps.pushPose();
            ps.translate(0.5, -1.8, 0.5);
            renderEnergyRing(ps, vc, progress * 12f, 0.25f + (1f - progress) * 0.35f, 150, 200, 255, (int)(alpha * 160));
            ps.popPose();
        }

        // 光柱衝天（400-520t，黑幕清除後才可見）
        if (tick > 400f && tick < 520f) {
            float age  = tick - 400f;
            float prog = age / 120f;
            float alph = prog < 0.15f ? prog / 0.15f : (prog < 0.7f ? 1f : 1f - (prog - 0.7f) / 0.3f);
            renderBluePillar(ps, mbs, 35f, alph * 0.9f);
        }
    }

    private void renderT4T5Upgrade(PoseStack ps, MultiBufferSource mbs, float tick) {
        VertexConsumer vc = mbs.getBuffer(MIRenderTypes.solarGlow());

        // Phase A: 5 段地面環形衝擊波（0-130t），各 26t，依序錯開
        for (int w = 0; w < 5; w++) {
            float wAge = tick - w * 26f;
            if (wAge <= 0f || wAge > 26f) continue;
            float progress  = wAge / 26f;
            float radius    = progress * 18f;
            float alpha     = (float) Math.sin(progress * Math.PI);
            float thickness = 0.25f + (1f - progress) * 0.40f;
            int   iAlpha    = (int)(alpha * 200);

            ps.pushPose();
            ps.translate(0.5, -1.8, 0.5);
            renderEnergyRing(ps, vc, radius,         thickness,         100, 180, 255, iAlpha);
            renderEnergyRing(ps, vc, radius * 0.85f, thickness * 0.45f, 160, 220, 255, (int)(alpha * 120));
            renderEnergyRing(ps, vc, radius * 0.70f, thickness * 0.20f, 200, 240, 255, (int)(alpha * 70));
            ps.popPose();
        }

        // Phase D: 最終大光柱（500-580t），音效觸發前出現
        if (tick > 500f && tick < 580f) {
            float pAge  = tick - 500f;
            float pProg = pAge / 80f;
            float pAlpha;
            if (pProg < 0.12f)      pAlpha = pProg / 0.12f;
            else if (pProg < 0.70f) pAlpha = 1.0f;
            else                     pAlpha = 1.0f - (pProg - 0.70f) / 0.30f;
            renderBluePillar(ps, mbs, 30f, pAlpha);
        }
    }

    private void renderFormedCore(PoseStack poseStack, MultiBufferSource bufferSource,
                                   int packedLight, int packedOverlay, float time, boolean ritualActive) {
        float bob   = (float) Math.sin(time * BOB_SPEED) * BOB_AMPLITUDE;
        float spinY = (time * SPIN_SPEED) % 360f;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5 + bob, 0.5);
        poseStack.mulPose(new Quaternionf().rotationZ((float) Math.toRadians(TILT_DEGREES)));
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(spinY)));
        poseStack.translate(-0.5, -0.5, -0.5);

        if (ritualActive) {
            // 儀式進行中：魔術方塊分層旋轉
            int phase       = (int)(time / PHASE_TICKS) % PHASES.length;
            float phaseTime = time % PHASE_TICKS;
            float turnEnd   = PHASE_TICKS * TURN_FRACTION;
            float angle     = phaseTime < turnEnd
                    ? 90f * (float)(Math.pow(phaseTime / turnEnd, 2) * (3 - 2 * phaseTime / turnEnd))
                    : 90f;
            PhaseConfig cfg = PHASES[phase];

            List<ModelElement> rotating = phaseRotating[phase];
            List<ModelElement> still    = phaseStill[phase];

            BlockbenchModelRenderUtils.renderElementList(poseStack, consumer, packedLight, packedOverlay, still);

            poseStack.pushPose();
            poseStack.translate(cfg.px(), cfg.py(), cfg.pz());
            poseStack.mulPose(cfg.axis().rotationDegrees(angle));
            poseStack.translate(-cfg.px(), -cfg.py(), -cfg.pz());
            BlockbenchModelRenderUtils.renderElementList(poseStack, consumer, packedLight, packedOverlay, rotating);
            poseStack.popPose();
        } else {
            // 閒置：整體靜止，只靠外層自轉
            BlockbenchModelRenderUtils.renderElementList(poseStack, consumer, packedLight, packedOverlay, allElements);
        }

        poseStack.popPose();
    }

    private void renderRings(AspectAltarBlockEntity altar, PoseStack poseStack,
                              MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                              float time) {
        int tier = altar.getUpgradeTier();
        if (tier == 0) return;
        if (AltarUpgradeAnimManager.isAnimating(altar.getBlockPos())) return;

        if (cachedRingQuads == null) {
            BakedModel ringModel = Minecraft.getInstance().getModelManager().getModel(RING_MODEL_LOC);
            if (ringModel == null) return;
            RandomSource rand = RandomSource.create(42L);
            cachedRingQuads = new ArrayList<>(ringModel.getQuads(null, null, rand, ModelData.EMPTY, null));
            for (Direction dir : Direction.values()) {
                cachedRingQuads.addAll(ringModel.getQuads(null, dir, rand, ModelData.EMPTY, null));
            }
            if (cachedRingQuads.isEmpty()) { cachedRingQuads = null; return; }
        }
        List<BakedQuad> quads = cachedRingQuads;

        var consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));

        for (int t = 0; t < tier && t < RING_CONFIGS.length; t++) {
            RingConfig cfg = RING_CONFIGS[t];
            renderOneRing(poseStack, consumer, packedLight, packedOverlay, quads, cfg, time);
        }
    }

    private void renderOneRing(PoseStack poseStack, VertexConsumer consumer,
                                int packedLight, int packedOverlay, List<BakedQuad> quads,
                                RingConfig cfg, float time) {
        // 時間扭曲：低頻 sin 讓速度忽快忽慢，全圈旋轉不擺盪
        float warp  = WARP_AMPLITUDE * (float) Math.sin(Math.toRadians(time * WARP_FREQ));
        float spinAngle = (time + warp) * cfg.spinSpeed();
        poseStack.pushPose();
        // 頂點套用順序（1→5）：1.偏心補正 2.自轉 3.傾斜 4.縮放 5.移到方塊中心
        poseStack.translate(0.5, 0.5, 0.5);                                                            // 5
        poseStack.scale(cfg.scale(), cfg.scale(), cfg.scale());                                         // 4
        if (cfg.tiltDeg() != 0f) poseStack.mulPose(cfg.tiltAxis().rotationDegrees(cfg.tiltDeg()));     // 3
        poseStack.mulPose(cfg.spinAxis().rotationDegrees(spinAngle));                                   // 2
        poseStack.translate(-RING_CX, -RING_CY, -RING_CZ);                                             // 1

        PoseStack.Pose pose = poseStack.last();
        for (BakedQuad quad : quads) {
            consumer.putBulkData(pose, quad, 1f, 1f, 1f, 1f, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private void renderOrbitals(AspectAltarBlockEntity altar, PoseStack poseStack,
                                 MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                 Level level, float time) {
        List<ItemStack> pedestalItems = altar.getPedestalItems();
        float progress = altar.getProgress();

        float radius     = 1.4f - progress * 1.2f;
        float orbitSpeed = (1f + progress * 4f) * 0.04f;
        float baseY      = 0.9f + progress * 0.3f;

        int count = 0;
        for (ItemStack s : pedestalItems) if (!s.isEmpty()) count++;
        if (count == 0) return;

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        int idx = 0;
        for (ItemStack item : pedestalItems) {
            if (item.isEmpty()) continue;

            float phase = (float) (idx * Math.PI * 2.0 / count);
            float angle = time * orbitSpeed + phase;
            float x = (float) Math.sin(angle) * radius;
            float z = (float) Math.cos(angle) * radius;
            float y = baseY + (float) Math.sin(time * 0.08f + phase) * 0.05f;

            poseStack.pushPose();
            poseStack.translate(0.5f + x, y, 0.5f + z);
            poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(time * 3f)));
            poseStack.scale(0.28f, 0.28f, 0.28f);

            BakedModel model = itemRenderer.getModel(item, level, null, 0);
            itemRenderer.render(item, ItemDisplayContext.GROUND, false, poseStack,
                    bufferSource, packedLight, packedOverlay, model);
            poseStack.popPose();
            idx++;
        }
    }

    // ── 太陽核心 ─────────────────────────────────────────────────────────────
    private static final float SOLAR_HEIGHT    = 2.8f;
    private static final float SOLAR_FADE_NEAR = 5f;    // 5 格內全隱
    private static final float SOLAR_FADE_FAR  = 12f;   // 12 格外全顯

    // ── 封印鍊子 ─────────────────────────────────────────────────────────────────
    // 光柱：儀式啟動時從各角落柱頂射向核心
    // 符文：成形就顯示，4個角落各一張貼圖
    private static final float   CHAIN_RADIUS = 0.04f;
    private static final float[] CHAIN_END    = {0.5f, 0.3f, 0.5f};
    private static final float[][] CHAIN_STARTS = {
        {-2.5f, 0f, -2.5f},  // NW
        { 3.5f, 0f, -2.5f},  // NE
        {-2.5f, 0f,  3.5f},  // SW
        { 3.5f, 0f,  3.5f},  // SE
    };
    // 符文：各角落柱頂方塊最小 X/Z，浮空距離
    private static final int[][] SEAL_PX_PZ = {
        {-3, -3}, { 3, -3}, {-3,  3}, { 3,  3}
    };
    private static final float SEAL_FLOAT = 0.25f;
    private static final ResourceLocation[] SEAL_TEXTURES = {
        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/entity/altar/pillar_seal1.png"),
        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/entity/altar/pillar_seal2.png"),
        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/entity/altar/pillar_seal3.png"),
        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/entity/altar/pillar_seal4.png"),
    };

    // 預算圓形頂點（只計算一次，避免每幀 sin/cos）
    private static final int   CIRCLE_SEGS = 32;
    private static final float[] CIRCLE_COS = new float[CIRCLE_SEGS];
    private static final float[] CIRCLE_SIN = new float[CIRCLE_SEGS];
    static {
        for (int i = 0; i < CIRCLE_SEGS; i++) {
            double a = i * 2 * Math.PI / CIRCLE_SEGS;
            CIRCLE_COS[i] = (float) Math.cos(a);
            CIRCLE_SIN[i] = (float) Math.sin(a);
        }
    }

    private void renderSolarCore(AspectAltarBlockEntity altar, PoseStack ps,
                                  MultiBufferSource mbs, float time) {
        if (altar.getUpgradeTier() < 3) return;   // T3 以上才觸發
        if (altar.isActive()) return;              // 工作中完全不渲染太陽核心

        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;

        // 水平距離淡入淡出（5 格內全隱，12 格外全顯，中間平滑過渡）
        double cx = altar.getBlockPos().getX() + 0.5;
        double cz = altar.getBlockPos().getZ() + 0.5;
        double dx = player.getX() - cx, dz = player.getZ() - cz;
        float dist = (float) Math.sqrt(dx*dx + dz*dz);
        if (dist < SOLAR_FADE_NEAR) return;
        float fade = Math.min(1f, (dist - SOLAR_FADE_NEAR) / (SOLAR_FADE_FAR - SOLAR_FADE_NEAR));

        float bob   = (float) Math.sin(time * BOB_SPEED * 0.6f) * BOB_AMPLITUDE * 2f;
        float pulse = 1f + 0.12f * (float) Math.sin(time * 0.10f);

        VertexConsumer vc = mbs.getBuffer(MIRenderTypes.solarGlow());

        // === 輝光（Billboard，用 Camera.rotation() 取正確朝向）===
        ps.pushPose();
        ps.translate(0.5, SOLAR_HEIGHT + bob, 0.5);
        ps.mulPose(mc.gameRenderer.getMainCamera().rotation());

        // alpha 乘上 fade 係數，5-12 格之間平滑出現
        renderGlowCircle(ps, vc, 7.0f * pulse,  40,  12, 130, (int)( 80 * fade));
        renderGlowCircle(ps, vc, 5.0f * pulse,  65, 130, 255, (int)(100 * fade));
        renderGlowCircle(ps, vc, 3.3f * pulse, 140, 210, 255, (int)(120 * fade));
        renderGlowCircle(ps, vc, 1.8f * pulse, 255, 205,  70, (int)(150 * fade));
        renderGlowCircle(ps, vc, 0.8f * pulse, 255, 248, 195, (int)(200 * fade));
        renderGlowCircle(ps, vc, 0.28f,        255, 255, 255, (int)(220 * fade));

        ps.popPose();
    }

    // 同心環堆疊出平滑圓形輝光，使用預算靜態 cos/sin，無每幀三角函數開銷
    private void renderGlowCircle(PoseStack ps, VertexConsumer vc,
                                   float radius, int r, int g, int b, int maxAlpha) {
        if (maxAlpha <= 0) return;
        Matrix4f mat = ps.last().pose();
        int rings = 6;
        for (int ring = 0; ring < rings; ring++) {
            float t0 = (float) ring       / rings;
            float t1 = (float)(ring + 1f) / rings;
            float r0 = radius * t0;
            float r1 = radius * t1;
            int a0 = (int)(maxAlpha * (1f - t0) * (1f - t0));
            int a1 = (int)(maxAlpha * (1f - t1) * (1f - t1));
            for (int i = 0; i < CIRCLE_SEGS; i++) {
                int j = (i + 1) % CIRCLE_SEGS;
                float c1 = CIRCLE_COS[i], s1 = CIRCLE_SIN[i];
                float c2 = CIRCLE_COS[j], s2 = CIRCLE_SIN[j];
                vc.addVertex(mat, r0*c1, r0*s1, 0).setColor(r, g, b, a0);
                vc.addVertex(mat, r1*c1, r1*s1, 0).setColor(r, g, b, a1);
                vc.addVertex(mat, r1*c2, r1*s2, 0).setColor(r, g, b, a1);
                vc.addVertex(mat, r0*c2, r0*s2, 0).setColor(r, g, b, a0);
            }
        }
    }

    private void renderEnergyRing(PoseStack ps, VertexConsumer vc,
                                    float radius, float thickness,
                                    int r, int g, int b, int a) {
        Matrix4f mat = ps.last().pose();
        int segments = 24;
        float outerR = radius + thickness;
        float innerR = radius - thickness;
        for (int i = 0; i < segments; i++) {
            float a1 = (float)(i     * 2 * Math.PI / segments);
            float a2 = (float)((i+1) * 2 * Math.PI / segments);
            float cos1 = (float)Math.cos(a1), sin1 = (float)Math.sin(a1);
            float cos2 = (float)Math.cos(a2), sin2 = (float)Math.sin(a2);
            vc.addVertex(mat, innerR*cos1, 0, innerR*sin1).setColor(r, g, b, a);
            vc.addVertex(mat, outerR*cos1, 0, outerR*sin1).setColor(r, g, b, a);
            vc.addVertex(mat, outerR*cos2, 0, outerR*sin2).setColor(r, g, b, a);
            vc.addVertex(mat, innerR*cos2, 0, innerR*sin2).setColor(r, g, b, a);
        }
    }

    // ── 儀式完成演出 ─────────────────────────────────────────────────────────────

    private void renderCompletionShow(AspectAltarBlockEntity altar, PoseStack ps,
                                       MultiBufferSource mbs, int animTick, int duration,
                                       float time, int tier) {
        // t: 1.0 → 0.0（從剛完成到演出結束）
        float t = animTick / (float) duration;
        float progress = 1.0f - t;

        // 輝光強度曲線：前 15% 快速升起，中段維持，後 30% 淡出
        float glowAlpha;
        if (progress < 0.15f)       glowAlpha = progress / 0.15f;
        else if (progress < 0.70f)  glowAlpha = 1.0f;
        else                         glowAlpha = 1.0f - (progress - 0.70f) / 0.30f;

        // T1+：爆發輝光圓（billboard，camera 朝向）
        {
            Minecraft mc = Minecraft.getInstance();
            VertexConsumer vc = mbs.getBuffer(MIRenderTypes.solarGlow());
            ps.pushPose();
            ps.translate(0.5, 0.8, 0.5);
            ps.mulPose(mc.gameRenderer.getMainCamera().rotation());
            float burstSize = (float) Math.sin(progress * Math.PI) * (1.8f + tier * 0.4f);
            int base = (int) (glowAlpha * 110);
            renderGlowCircle(ps, vc, burstSize * 1.6f, 80,  160, 255, base / 4);
            renderGlowCircle(ps, vc, burstSize * 1.0f, 130, 200, 255, base / 3);
            renderGlowCircle(ps, vc, burstSize * 0.5f, 200, 230, 255, base / 2);
            renderGlowCircle(ps, vc, burstSize * 0.18f, 240, 248, 255, base);
            ps.popPose();
        }

        // T3+：藍色光柱衝天
        if (tier >= 3 && glowAlpha > 0.01f) {
            float maxHeight = 20.0f + tier * 4.0f;
            float growFrac = Math.min(1.0f, progress / 0.25f);
            float pillarHeight = maxHeight * growFrac;
            if (pillarHeight > 0.5f) {
                renderBluePillar(ps, mbs, pillarHeight, glowAlpha);
            }
        }

        // T4+：多波空間波紋環（BER版，暫時停用，由shader負責）
        /*
        if (tier >= 4) {
            VertexConsumer vc = mbs.getBuffer(MIRenderTypes.solarGlow());
            int waveCount = Math.min(tier - 2, 4);
            float maxRadius = 6.0f + tier * 1.5f;
            for (int w = 0; w < waveCount; w++) {
                float delay = w * 0.10f;
                float waveP = Math.max(0, progress - delay) / (1.0f - delay);
                if (waveP <= 0) continue;
                float radius = waveP * maxRadius;
                float waveAlpha = (1.0f - waveP) * glowAlpha;
                if (waveAlpha <= 0.01f) continue;
                float thickness = 0.12f + (1.0f - waveP) * 0.25f;
                ps.pushPose();
                ps.translate(0.5, 0.3 + w * 0.15f, 0.5);
                renderEnergyRing(ps, vc, radius, thickness, 100, 180, 255, (int)(waveAlpha * 200));
                renderEnergyRing(ps, vc, radius * 0.85f, thickness * 0.6f, 160, 220, 255, (int)(waveAlpha * 120));
                ps.popPose();
            }
        }
        */
    }

    private void renderBluePillar(PoseStack ps, MultiBufferSource mbs, float height, float alphaFactor) {
        VertexConsumer vc = mbs.getBuffer(MIRenderTypes.solarGlow());
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        Matrix4f mat = ps.last().pose();
        // 由外到內：外暈 → 中暈 → 芯 → 白芯
        renderPillarCylinder(mat, vc, 1.1f, height, 70,  140, 255, (int)(40  * alphaFactor));
        renderPillarCylinder(mat, vc, 0.55f, height, 110, 185, 255, (int)(90  * alphaFactor));
        renderPillarCylinder(mat, vc, 0.22f, height, 170, 220, 255, (int)(160 * alphaFactor));
        renderPillarCylinder(mat, vc, 0.08f, height, 235, 248, 255, (int)(220 * alphaFactor));
        ps.popPose();
    }

    private void renderPillarCylinder(Matrix4f mat, VertexConsumer vc,
                                       float radius, float height,
                                       int r, int g, int b, int a) {
        if (a <= 0) return;
        float fadeH = Math.min(2.0f, height * 0.1f);
        float midBot = fadeH;
        float midTop = height - fadeH;
        if (midTop <= midBot) midTop = midBot + 0.1f;

        for (int i = 0; i < CIRCLE_SEGS; i++) {
            int j = (i + 1) % CIRCLE_SEGS;
            float c1 = CIRCLE_COS[i] * radius, s1 = CIRCLE_SIN[i] * radius;
            float c2 = CIRCLE_COS[j] * radius, s2 = CIRCLE_SIN[j] * radius;
            // 底部淡入
            vc.addVertex(mat, c1, 0f,    s1).setColor(r, g, b, 0);
            vc.addVertex(mat, c2, 0f,    s2).setColor(r, g, b, 0);
            vc.addVertex(mat, c2, midBot, s2).setColor(r, g, b, a);
            vc.addVertex(mat, c1, midBot, s1).setColor(r, g, b, a);
            // 中段全亮
            vc.addVertex(mat, c1, midBot, s1).setColor(r, g, b, a);
            vc.addVertex(mat, c2, midBot, s2).setColor(r, g, b, a);
            vc.addVertex(mat, c2, midTop, s2).setColor(r, g, b, a);
            vc.addVertex(mat, c1, midTop, s1).setColor(r, g, b, a);
            // 頂部淡出
            vc.addVertex(mat, c1, midTop,  s1).setColor(r, g, b, a);
            vc.addVertex(mat, c2, midTop,  s2).setColor(r, g, b, a);
            vc.addVertex(mat, c2, height,  s2).setColor(r, g, b, 0);
            vc.addVertex(mat, c1, height,  s1).setColor(r, g, b, 0);
        }
    }

    // ── 封印系統（鍊子 + 符文） ───────────────────────────────────────────────────

    private void renderSealSystem(AspectAltarBlockEntity altar, PoseStack ps,
                                   MultiBufferSource mbs, int packedLight, int packedOverlay) {
        // 符文：成形就顯示，先全部畫完（避免與光柱的 render type 交錯）
        for (int i = 0; i < 4; i++) {
            renderPillarSeal(ps, mbs, i, SEAL_PX_PZ[i][0], SEAL_PX_PZ[i][1], packedLight, packedOverlay);
        }

        // 光柱：只有儀式進行中才顯示
        if (!altar.isActive()) return;
        VertexConsumer vc = mbs.getBuffer(MIRenderTypes.sealChain());
        for (int i = 0; i < 4; i++) {
            renderChainTube(ps, vc, CHAIN_STARTS[i], CHAIN_END);
        }
    }

    // 細管鍊子：4面 prism，從 start 到 end
    private void renderChainTube(PoseStack ps, VertexConsumer vc, float[] start, float[] end) {
        float dx = end[0]-start[0], dy = end[1]-start[1], dz = end[2]-start[2];
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 0.01f) return;
        float nx = dx/len, ny = dy/len, nz = dz/len;

        // 兩個垂直於鍊子方向的正交向量
        float[] tmp = (Math.abs(ny) < 0.9f) ? new float[]{0,1,0} : new float[]{1,0,0};
        float rx = ny*tmp[2]-nz*tmp[1], ry = nz*tmp[0]-nx*tmp[2], rz = nx*tmp[1]-ny*tmp[0];
        float rlen = (float) Math.sqrt(rx*rx + ry*ry + rz*rz);
        rx/=rlen; ry/=rlen; rz/=rlen;
        float ux = ry*nz-rz*ny, uy = rz*nx-rx*nz, uz = rx*ny-ry*nx;

        float r = CHAIN_RADIUS;
        float[][] sv = {
            {start[0]+rx*r+ux*r, start[1]+ry*r+uy*r, start[2]+rz*r+uz*r},
            {start[0]-rx*r+ux*r, start[1]-ry*r+uy*r, start[2]-rz*r+uz*r},
            {start[0]-rx*r-ux*r, start[1]-ry*r-uy*r, start[2]-rz*r-uz*r},
            {start[0]+rx*r-ux*r, start[1]+ry*r-uy*r, start[2]+rz*r-uz*r},
        };
        float[][] ev = {
            {end[0]+rx*r+ux*r, end[1]+ry*r+uy*r, end[2]+rz*r+uz*r},
            {end[0]-rx*r+ux*r, end[1]-ry*r+uy*r, end[2]-rz*r+uz*r},
            {end[0]-rx*r-ux*r, end[1]-ry*r-uy*r, end[2]-rz*r-uz*r},
            {end[0]+rx*r-ux*r, end[1]+ry*r-uy*r, end[2]+rz*r-uz*r},
        };

        Matrix4f mat = ps.last().pose();
        int cr=75, cg=55, cb=140, ca=230;  // 深紫色鍊子
        for (int i = 0; i < 4; i++) {
            int j = (i+1) % 4;
            vc.addVertex(mat, sv[i][0], sv[i][1], sv[i][2]).setColor(cr,cg,cb,ca);
            vc.addVertex(mat, sv[j][0], sv[j][1], sv[j][2]).setColor(cr,cg,cb,ca);
            vc.addVertex(mat, ev[j][0], ev[j][1], ev[j][2]).setColor(cr,cg,cb,ca);
            vc.addVertex(mat, ev[i][0], ev[i][1], ev[i][2]).setColor(cr,cg,cb,ca);
        }
    }


    // 符文：浮空 SEAL_FLOAT 格，4個面用同一張貼圖
    private void renderPillarSeal(PoseStack ps, MultiBufferSource mbs,
                                   int chainIdx, int px, int pz, int packedLight, int packedOverlay) {
        float x0 = px, x1 = px + 1f;
        float y0 = -1f, y1 = 0f;
        float z0 = pz, z1 = pz + 1f;
        float f = SEAL_FLOAT;
        Matrix4f mat = ps.last().pose();
        VertexConsumer vc = mbs.getBuffer(RenderType.entityCutoutNoCull(SEAL_TEXTURES[chainIdx]));

        sealQuad(vc, mat, x1,y1,z0-f, x0,y1,z0-f, x0,y0,z0-f, x1,y0,z0-f, 0,0,-1, packedLight, packedOverlay);
        sealQuad(vc, mat, x0,y1,z1+f, x1,y1,z1+f, x1,y0,z1+f, x0,y0,z1+f, 0,0,1, packedLight, packedOverlay);
        sealQuad(vc, mat, x0-f,y1,z1, x0-f,y1,z0, x0-f,y0,z0, x0-f,y0,z1, -1,0,0, packedLight, packedOverlay);
        sealQuad(vc, mat, x1+f,y1,z0, x1+f,y1,z1, x1+f,y0,z1, x1+f,y0,z0, 1,0,0, packedLight, packedOverlay);
    }

    private void sealQuad(VertexConsumer vc, Matrix4f mat,
                           float x0, float y0, float z0, float x1, float y1, float z1,
                           float x2, float y2, float z2, float x3, float y3, float z3,
                           float nx, float ny, float nz, int packedLight, int packedOverlay) {
        vc.addVertex(mat,x0,y0,z0).setColor(255,255,255,255).setUv(0f,0f).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx,ny,nz);
        vc.addVertex(mat,x1,y1,z1).setColor(255,255,255,255).setUv(1f,0f).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx,ny,nz);
        vc.addVertex(mat,x2,y2,z2).setColor(255,255,255,255).setUv(1f,1f).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx,ny,nz);
        vc.addVertex(mat,x3,y3,z3).setColor(255,255,255,255).setUv(0f,1f).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx,ny,nz);
    }

    @Override
    public AABB getRenderBoundingBox(AspectAltarBlockEntity altar) {
        // T12 最大半徑 13，垂直方向也到 ±13；加 2 作為 margin
        double x = altar.getBlockPos().getX();
        double y = altar.getBlockPos().getY();
        double z = altar.getBlockPos().getZ();
        return new AABB(x - 15, y - 15, z - 15, x + 16, y + 16, z + 16);
    }
}
