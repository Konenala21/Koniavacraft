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

    // 共鳴環動畫
    // OBJ 模型在 YZ 平面，中心在 (0,0,0)，半徑 ~3 方塊單位，不需要額外縮放
    // T1：Z 軸傾斜 90° → 變水平（XZ 平面），繞 Y 軸自轉
    // T2：Y 軸傾斜 90° → 變 XY 垂直面，繞 Z 軸自轉
    // T3：不傾斜（本來就是 YZ 平面），繞 X 軸自轉
    private static final float RING_SPIN_1 = 0.5f;
    private static final float RING_SPIN_2 = 0.7f;
    private static final float RING_SPIN_3 = 0.6f;
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
        boolean active = altar.isActive();
        renderFormedCore(poseStack, bufferSource, packedLight, packedOverlay, time, active);
        renderRings(altar, poseStack, bufferSource, packedLight, packedOverlay, time);

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

        // Lazy-load ring BakedModel（bake 完成後才能取得）
        BakedModel ringModel = Minecraft.getInstance().getModelManager().getModel(RING_MODEL_LOC);
        if (ringModel == null) return;

        RandomSource rand = RandomSource.create(42L);
        List<BakedQuad> quads = new ArrayList<>(ringModel.getQuads(null, null, rand, ModelData.EMPTY, null));
        for (Direction dir : Direction.values()) {
            quads.addAll(ringModel.getQuads(null, dir, rand, ModelData.EMPTY, null));
        }
        if (quads.isEmpty()) return;

        var consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));

        // T1：Z+90° 傾斜讓 YZ 環躺平（→XZ 水平），繞 Y 自轉
        if (tier >= 1) {
            renderOneRing(poseStack, consumer, packedLight, packedOverlay, quads,
                    Axis.ZP, 90f, Axis.YP, time * RING_SPIN_1);
        }
        // T2：Y+90° 傾斜讓環轉到 XY 平面（垂直），繞 Z 自轉
        if (tier >= 2) {
            renderOneRing(poseStack, consumer, packedLight, packedOverlay, quads,
                    Axis.YP, 90f, Axis.ZP, time * RING_SPIN_2);
        }
        // T3：不傾斜（原本就是 YZ 平面，垂直），繞 X 自轉
        if (tier >= 3) {
            renderOneRing(poseStack, consumer, packedLight, packedOverlay, quads,
                    Axis.YP, 0f, Axis.XP, time * RING_SPIN_3);
        }
    }

    private void renderOneRing(PoseStack poseStack, VertexConsumer consumer,
                                int packedLight, int packedOverlay, List<BakedQuad> quads,
                                Axis tiltAxis, float tiltDegrees, Axis spinAxis, float spinAngle) {
        poseStack.pushPose();
        // OBJ 環心在 (0,0,0)，translate 到祭壇方塊中心
        poseStack.translate(0.5, 0.5, 0.5);
        if (tiltDegrees != 0f) poseStack.mulPose(tiltAxis.rotationDegrees(tiltDegrees));
        poseStack.mulPose(spinAxis.rotationDegrees(spinAngle % 360f));
        // 不需要 scale，不需要 translate-back（環已對齊原點）

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
        return new AABB(
                altar.getBlockPos().getX() - 1.5,
                altar.getBlockPos().getY() - 0.2,
                altar.getBlockPos().getZ() - 1.5,
                altar.getBlockPos().getX() + 2.5,
                altar.getBlockPos().getY() + 2.5,
                altar.getBlockPos().getZ() + 2.5
        );
    }
}
