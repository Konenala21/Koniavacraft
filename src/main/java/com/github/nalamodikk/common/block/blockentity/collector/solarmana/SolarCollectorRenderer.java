package com.github.nalamodikk.common.block.blockentity.collector.solarmana;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.utils.render.BlockbenchModelRenderUtils;
import com.github.nalamodikk.common.utils.render.BlockbenchModelRenderUtils.ModelElement;
import com.github.nalamodikk.client.utils.render.RenderAnimationLodUtils;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SolarCollectorRenderer implements BlockEntityRenderer<SolarManaCollectorBlockEntity>, IBlockEntityRendererExtension<SolarManaCollectorBlockEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation MODEL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "models/block/collector/solar_mana_collector.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/solar_mana_collector.png");
    private static final ResourceLocation TEXTURE_CRYSTAL =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/mana_crystal_3d.png");

    // 蕎麥麵 2026-06 重做的模型的群組名字（見 mana_solarcollector.bbmodel）
    private static final String BODY_GROUP = "機身";
    private static final String CRYSTAL_GROUP = "水晶";

    private final Map<String, List<ModelElement>> groupElements = new HashMap<>();
    private final Map<String, Vector3f> customOrigins = new HashMap<>();
    private boolean modelLoaded = false;

    public SolarCollectorRenderer(BlockEntityRendererProvider.Context context) {
        loadAndParseModel();
    }

    @Override
    public void render(SolarManaCollectorBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        if (!modelLoaded || blockEntity.getLevel() == null) {
            return;
        }

        float animationScale = RenderAnimationLodUtils.getAnimationTimeScale(blockEntity.getBlockPos());
        float time = (blockEntity.getLevel().getGameTime() + partialTick) * 0.05F * animationScale;

        poseStack.pushPose();
        if (blockEntity.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            applyBlockRotation(poseStack, facing);
        }

        // translucent 類型的 buffer 是排序共用的，先拿第二個會把第一個沖掉，所以要用完一個才拿下一個
        VertexConsumer bodyConsumer = bufferSource.getBuffer(RenderType.entityTranslucentCull(TEXTURE));
        renderGroup(poseStack, bodyConsumer, packedLight, packedOverlay, BODY_GROUP, 0.0F, 0.0F, 0.0F, 0.0F);

        VertexConsumer crystalConsumer = bufferSource.getBuffer(RenderType.entityTranslucentCull(TEXTURE_CRYSTAL));
        renderCrystalAnimation(poseStack, crystalConsumer, packedLight, packedOverlay, time, animationScale);
        poseStack.popPose();
    }

    private void renderCrystalAnimation(PoseStack poseStack, VertexConsumer vertexConsumer,
                                        int packedLight, int packedOverlay, float time, float animationScale) {
        float floatY = 0.0F;
        if (animationScale > 0.0F) {
            float floatAmplitude = 0.15F * animationScale;
            floatY = (float) Math.sin(time * 1.5F) * floatAmplitude;
        }
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRYSTAL_GROUP, 0.0F, floatY, 0.0F, 0.0F);
    }

    @Override
    public AABB getRenderBoundingBox(SolarManaCollectorBlockEntity blockEntity) {
        return BlockbenchModelRenderUtils.getTwoBlockTallBoundingBox(blockEntity.getBlockPos());
    }

    private void applyBlockRotation(PoseStack poseStack, Direction facing) {
        BlockbenchModelRenderUtils.applyHorizontalFacingRotation(poseStack, facing, 180.0F, 0.0F, 270.0F, 90.0F);
    }

    private void loadAndParseModel() {
        LOGGER.debug("Loading solar collector model.");
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(MODEL_LOCATION);
            if (resource.isEmpty()) {
                LOGGER.error("Missing solar collector model resource: {}", MODEL_LOCATION);
                return;
            }

            try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                JsonObject modelData = JsonParser.parseReader(reader).getAsJsonObject();
                parseModelData(modelData);
                modelLoaded = true;
                LOGGER.debug("Solar collector model loaded. groups={}", groupElements.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load solar collector model.", e);
        }
    }

    private void parseModelData(JsonObject modelData) {
        groupElements.clear();
        customOrigins.clear();
        groupElements.putAll(BlockbenchModelRenderUtils.parseGroupedElements(modelData, true));
        customOrigins.put(CRYSTAL_GROUP, calculateElementsCenter(groupElements.get(CRYSTAL_GROUP), ORIGIN_DEFAULT));
        LOGGER.debug("Solar collector model groups parsed.");
        groupElements.forEach((name, elements) -> LOGGER.debug("group={} elements={}", name, elements.size()));
    }

    private Vector3f calculateElementsCenter(List<ModelElement> elements, Vector3f fallback) {
        if (elements == null || elements.isEmpty()) {
            return fallback;
        }
        float sumX = 0.0F;
        float sumY = 0.0F;
        float sumZ = 0.0F;
        for (ModelElement element : elements) {
            sumX += (element.x1 + element.x2) * 0.5F;
            sumY += (element.y1 + element.y2) * 0.5F;
            sumZ += (element.z1 + element.z2) * 0.5F;
        }
        float count = elements.size();
        return new Vector3f(sumX / count, sumY / count, sumZ / count);
    }

    private void renderGroup(PoseStack poseStack, VertexConsumer vertexConsumer,
                             int packedLight, int packedOverlay, String groupName,
                             float offsetX, float offsetY, float offsetZ, float rotationY) {
        BlockbenchModelRenderUtils.renderGroup(
                poseStack, vertexConsumer, packedLight, packedOverlay,
                groupElements, groupName, offsetX, offsetY, offsetZ, rotationY, this::getGroupOrigin
        );
    }

    private static final Vector3f ORIGIN_DEFAULT = new Vector3f(0.5F, 0.5F, 0.5F);

    private Vector3f getGroupOrigin(String groupName) {
        return customOrigins.getOrDefault(groupName, ORIGIN_DEFAULT);
    }
}
