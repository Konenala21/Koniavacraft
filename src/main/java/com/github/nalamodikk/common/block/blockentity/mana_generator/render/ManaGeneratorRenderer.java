package com.github.nalamodikk.common.block.blockentity.mana_generator.render;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlock;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlockEntity;
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

public class ManaGeneratorRenderer implements BlockEntityRenderer<ManaGeneratorBlockEntity>, IBlockEntityRendererExtension<ManaGeneratorBlockEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation TEXTURE_BODY =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/mana_generator_texture.png");
    private static final ResourceLocation TEXTURE_CRYSTAL =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/mana_crystal_3d.png");
    private static final ResourceLocation MODEL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "models/block/generator/mana_generator.json");

    // 蕎麥麵 2026-08 重做的發電機模型群組名字（見 mana_generator.bbmodel）
    private static final String BODY_GROUP = "power-main";
    private static final String CRYSTAL_MAIN = "cryst-main";
    private static final String CRYSTAL_SUB = "cryst";
    private static final Vector3f DEFAULT_ORIGIN = new Vector3f(0.5F, 0.5F, 0.5F);

    private final Map<String, List<ModelElement>> groupElements = new HashMap<>();
    private final Map<String, Vector3f> customOrigins = new HashMap<>();
    private boolean modelLoaded = false;

    public ManaGeneratorRenderer(BlockEntityRendererProvider.Context context) {
        loadAndParseModel();
    }

    private void loadAndParseModel() {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(MODEL_LOCATION);
            if (resource.isEmpty()) {
                LOGGER.error("Missing mana generator model resource: {}", MODEL_LOCATION);
                return;
            }

            try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                JsonObject modelData = JsonParser.parseReader(reader).getAsJsonObject();
                parseModelData(modelData);
                modelLoaded = true;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load mana generator model.", e);
        }
    }

    private void parseModelData(JsonObject modelData) {
        groupElements.clear();
        customOrigins.clear();
        groupElements.putAll(BlockbenchModelRenderUtils.parseGroupedElements(modelData, false));

        customOrigins.put(CRYSTAL_MAIN, calculateElementsCenter(groupElements.get(CRYSTAL_MAIN), DEFAULT_ORIGIN));
        customOrigins.put(CRYSTAL_SUB, calculateElementsCenter(groupElements.get(CRYSTAL_SUB), DEFAULT_ORIGIN));
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

    @Override
    public void render(ManaGeneratorBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        if (!modelLoaded || blockEntity.getLevel() == null) {
            return;
        }

        float animationScale = RenderAnimationLodUtils.getAnimationTimeScale(blockEntity.getBlockPos());
        float time = (blockEntity.getLevel().getGameTime() + partialTick) * 0.05F * animationScale;
        boolean isWorking = blockEntity.isWorking();

        poseStack.pushPose();
        Direction facing = blockEntity.getBlockState().getValue(ManaGeneratorBlock.FACING);
        applyBlockRotation(poseStack, facing);

        // 兩張貼圖分開拿 buffer；用完一個才拿下一個，避免共用排序 buffer 時把前一個沖掉導致 crash
        VertexConsumer bodyConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE_BODY));
        renderGroup(poseStack, bodyConsumer, packedLight, packedOverlay, BODY_GROUP, 0.0F, 0.0F, 0.0F, 0.0F);

        VertexConsumer crystalConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE_CRYSTAL));
        renderCrystalAnimation(poseStack, crystalConsumer, packedLight, packedOverlay, time, animationScale, isWorking);

        poseStack.popPose();
    }

    private void renderCrystalAnimation(PoseStack poseStack, VertexConsumer vertexConsumer,
                                        int packedLight, int packedOverlay,
                                        float time, float animationScale, boolean isWorking) {
        if (animationScale <= 0.0F) {
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRYSTAL_MAIN, 0.0F, 0.0F, 0.0F, 0.0F);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRYSTAL_SUB, 0.0F, 0.0F, 0.0F, 0.0F);
        } else if (isWorking) {
            float mainRotation = time * 10.0F;
            float subRotation = -time * 10.8F;
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRYSTAL_MAIN, 0.0F, 0.0F, 0.0F, mainRotation);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRYSTAL_SUB, 0.0F, 0.0F, 0.0F, subRotation);
        } else {
            float mainOffsetY = (float) Math.sin(time) * 0.125F;
            float subOffsetY = (float) Math.sin(time + Math.PI * 2.0F / 3.0F) * 0.125F;
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRYSTAL_MAIN, 0.0F, mainOffsetY, 0.0F, 0.0F);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRYSTAL_SUB, 0.0F, subOffsetY, 0.0F, 0.0F);
        }
    }

    private void applyBlockRotation(PoseStack poseStack, Direction facing) {
        BlockbenchModelRenderUtils.applyHorizontalFacingRotation(poseStack, facing, 0.0F, 180.0F, 90.0F, -90.0F);
    }

    private void renderGroup(PoseStack poseStack, VertexConsumer vertexConsumer,
                             int packedLight, int packedOverlay, String groupName,
                             float offsetX, float offsetY, float offsetZ, float rotationY) {
        BlockbenchModelRenderUtils.renderGroup(
                poseStack, vertexConsumer, packedLight, packedOverlay,
                groupElements, groupName, offsetX, offsetY, offsetZ, rotationY, this::getGroupOrigin
        );
    }

    private Vector3f getGroupOrigin(String groupName) {
        return customOrigins.getOrDefault(groupName, DEFAULT_ORIGIN);
    }

    @Override
    public AABB getRenderBoundingBox(ManaGeneratorBlockEntity blockEntity) {
        return BlockbenchModelRenderUtils.getTwoBlockTallBoundingBox(blockEntity.getBlockPos());
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
