package com.github.nalamodikk.common.block.blockentity.mana_plate_press;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.utils.render.BlockbenchModelRenderUtils;
import com.github.nalamodikk.common.utils.render.BlockbenchModelRenderUtils.ModelElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import org.joml.Vector3f;
import org.slf4j.Logger;

import net.minecraft.core.BlockPos;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ManaPlatePressRenderer implements BlockEntityRenderer<ManaPlatePressBlockEntity>,
        IBlockEntityRendererExtension<ManaPlatePressBlockEntity> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation MODEL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "models/block/mana_plate_press.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/mana_plate_press_texture.png");

    private static final String PRESS_CORE_GROUP = "壓板核心";
    private static final String BODY_GROUP = "主體";
    private static final float MAX_PRESS_OFFSET = 0.6875f; // 壓頭下壓行程（格），0.6875 = 11px：剛好貼底座不穿模
    private static final float FALLBACK_CYCLE_TICKS = 80f;  // 拿不到配方時間時的循環長度

    private final Map<String, List<ModelElement>> groupElements = new HashMap<>();
    private final Map<BlockPos, Float> animatedOffsets = new HashMap<>();
    private final Map<BlockPos, Long> pressStartTimes = new HashMap<>();
    private boolean modelLoaded = false;

    public ManaPlatePressRenderer(BlockEntityRendererProvider.Context context) {
        loadAndParseModel();
    }

    @Override
    public void render(ManaPlatePressBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        if (!modelLoaded || blockEntity.getLevel() == null) return;

        boolean isWorking = blockEntity.getBlockState().hasProperty(ManaPlatePressBlock.WORKING)
                && blockEntity.getBlockState().getValue(ManaPlatePressBlock.WORKING);

        poseStack.pushPose();
        Direction facing = blockEntity.getBlockState().getValue(ManaPlatePressBlock.FACING);
        BlockbenchModelRenderUtils.applyHorizontalFacingRotation(poseStack, facing, 0.0f, 180.0f, 90.0f, -90.0f);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucentCull(TEXTURE));

        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, BODY_GROUP, 0f, 0f, 0f);

        // 動畫由 WORKING blockstate 驅動（這個有同步到 client；progress 沒同步，靠它會永遠 0）。
        // 加工中：用遊戲時間做本地壓印循環（下壓→回升反覆）。停止：平滑回到原位。
        BlockPos pos = blockEntity.getBlockPos();
        float current;
        if (isWorking) {
            // 循環長度對齊配方壓製時間：一次壓印 = 一次合成
            float cycle = blockEntity.getMaxPressingTime() > 0 ? blockEntity.getMaxPressingTime() : FALLBACK_CYCLE_TICKS;
            // phase 鎖在 working 開始的時間點：phase 從 0 起算，壓頭從原位平滑下壓，不會一開工就瞬移
            long now = blockEntity.getLevel().getGameTime();
            long start = pressStartTimes.computeIfAbsent(pos, p -> now);
            float elapsed = (now - start) + partialTick;
            float phase = (elapsed % cycle) / cycle;                         // 0..1
            current = -pressCurve(phase) * MAX_PRESS_OFFSET;
        } else {
            pressStartTimes.remove(pos);
            current = animatedOffsets.getOrDefault(pos, 0f);
            current += (0f - current) * 0.35f;                               // 收工平滑回原位
        }
        animatedOffsets.put(pos, current);

        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, PRESS_CORE_GROUP, 0f, current, 0f);

        poseStack.popPose();
    }

    // 壓印曲線：下壓（前段）→ 壓到底停住（中段）→ 回升（後段），回傳 0..1（1=壓到底）。
    // 比純 sine 更像真的壓住：在底部有停留，看得出「壓到底」這個動作。
    private static float pressCurve(float phase) {
        final float DOWN_END = 0.30f;   // 下壓段
        final float HOLD_END = 0.62f;   // 停底段
        if (phase < DOWN_END) {
            float t = phase / DOWN_END;
            return t * t * (3f - 2f * t);            // smoothstep 0→1
        } else if (phase < HOLD_END) {
            return 1f;                                // 壓到底停住
        } else {
            float t = (phase - HOLD_END) / (1f - HOLD_END);
            return 1f - t * t * (3f - 2f * t);        // smoothstep 1→0
        }
    }

    private void renderGroup(PoseStack poseStack, VertexConsumer vertexConsumer,
                             int packedLight, int packedOverlay,
                             String groupName, float offsetX, float offsetY, float offsetZ) {
        BlockbenchModelRenderUtils.renderGroup(
                poseStack, vertexConsumer, packedLight, packedOverlay,
                groupElements, groupName, offsetX, offsetY, offsetZ, 0f,
                name -> new Vector3f(0.5f, 0.5f, 0.5f)
        );
    }

    private void loadAndParseModel() {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(MODEL_LOCATION);
            if (resource.isEmpty()) {
                LOGGER.error("Missing mana_plate_press model: {}", MODEL_LOCATION);
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                JsonObject modelData = JsonParser.parseReader(reader).getAsJsonObject();
                groupElements.putAll(BlockbenchModelRenderUtils.parseGroupedElements(modelData, true));
                modelLoaded = true;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load mana_plate_press model.", e);
        }
    }

    @Override
    public AABB getRenderBoundingBox(ManaPlatePressBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos());
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
