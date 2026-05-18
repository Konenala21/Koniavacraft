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

    private final Map<String, List<ModelElement>> groupElements = new HashMap<>();
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
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucentCull(TEXTURE));

        poseStack.pushPose();
        if (blockEntity.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            applyBlockRotation(poseStack, facing);
        }

        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "standalone_0", 0.0F, 0.0F, 0.0F, 0.0F);
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "standalone_4", 0.0F, 0.0F, 0.0F, 0.0F);
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "group", 0.0F, 0.0F, 0.0F, 0.0F);
        renderCrystalAnimation(poseStack, vertexConsumer, packedLight, packedOverlay, time, animationScale);
        poseStack.popPose();
    }

    private void renderCrystalAnimation(PoseStack poseStack, VertexConsumer vertexConsumer,
                                        int packedLight, int packedOverlay, float time, float animationScale) {
        float floatY = 0.0F;
        if (animationScale > 0.0F) {
            float floatAmplitude = 0.05F * animationScale;
            floatY = (float) Math.sin(time * 1.5F) * floatAmplitude;
        }
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bone", 0.0F, floatY, 0.0F, 0.0F);
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
        groupElements.putAll(BlockbenchModelRenderUtils.parseGroupedElements(modelData, true));
        LOGGER.debug("Solar collector model groups parsed.");
        groupElements.forEach((name, elements) -> LOGGER.debug("group={} elements={}", name, elements.size()));
    }

    private void renderGroup(PoseStack poseStack, VertexConsumer vertexConsumer,
                             int packedLight, int packedOverlay, String groupName,
                             float offsetX, float offsetY, float offsetZ, float rotationY) {
        BlockbenchModelRenderUtils.renderGroup(
                poseStack, vertexConsumer, packedLight, packedOverlay,
                groupElements, groupName, offsetX, offsetY, offsetZ, rotationY, this::getGroupOrigin
        );
    }

    private static final Vector3f ORIGIN_BONE    = new Vector3f(0.0F, 29.24749F / 16.0F, 0.29246F / 16.0F);
    private static final Vector3f ORIGIN_DEFAULT = new Vector3f(0.5F, 0.5F, 0.5F);

    private Vector3f getGroupOrigin(String groupName) {
        return "bone".equals(groupName) ? ORIGIN_BONE : ORIGIN_DEFAULT;
    }
}
