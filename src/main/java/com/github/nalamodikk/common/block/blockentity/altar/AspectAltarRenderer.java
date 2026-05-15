package com.github.nalamodikk.common.block.blockentity.altar;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.utils.render.BlockbenchModelRenderUtils;
import com.github.nalamodikk.common.utils.render.BlockbenchModelRenderUtils.ModelElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
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
                modelLoaded = true;
                LOGGER.debug("Loaded {} elements for aspect altar", allElements.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load aspect altar model.", e);
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
        renderFormedCore(poseStack, bufferSource, packedLight, packedOverlay, time, active);
        renderRings(altar, poseStack, bufferSource, packedLight, packedOverlay, ringTime);

        if (active) {
            renderOrbitals(altar, poseStack, bufferSource, packedLight, packedOverlay, level, time);
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

            List<ModelElement> rotating  = new ArrayList<>(9);
            List<ModelElement> still = new ArrayList<>(18);
            for (ModelElement e : allElements) {
                (cfg.selector().test(e) ? rotating : still).add(e);
            }

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

        int count = (int) pedestalItems.stream().filter(s -> !s.isEmpty()).count();
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

    @Override
    public AABB getRenderBoundingBox(AspectAltarBlockEntity altar) {
        // T12 最大半徑 13，垂直方向也到 ±13；加 2 作為 margin
        double x = altar.getBlockPos().getX();
        double y = altar.getBlockPos().getY();
        double z = altar.getBlockPos().getZ();
        return new AABB(x - 15, y - 15, z - 15, x + 16, y + 16, z + 16);
    }
}
