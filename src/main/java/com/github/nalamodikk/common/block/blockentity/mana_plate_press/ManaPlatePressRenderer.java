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
    private static final float MAX_PRESS_OFFSET = 0.25f;

    private final Map<String, List<ModelElement>> groupElements = new HashMap<>();
    private final Map<BlockPos, Float> animatedOffsets = new HashMap<>();
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

        float targetOffset = 0f;
        if (isWorking) {
            int progress = blockEntity.getPressingProgress();
            int maxProgress = blockEntity.getMaxPressingTime();
            if (maxProgress > 0) {
                targetOffset = -((float) progress / maxProgress) * MAX_PRESS_OFFSET;
            }
        }

        BlockPos pos = blockEntity.getBlockPos();
        float current = animatedOffsets.getOrDefault(pos, 0f);
        boolean returning = targetOffset > current;
        float lerpFactor = returning ? 0.18f : 0.35f;
        current += (targetOffset - current) * lerpFactor;
        animatedOffsets.put(pos, current);

        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, PRESS_CORE_GROUP, 0f, current, 0f);

        poseStack.popPose();
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
