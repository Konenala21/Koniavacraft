package com.github.nalamodikk.common.block.blockentity.mana_generator.render;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlock;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlockEntity;
import com.github.nalamodikk.common.utils.render.BlockbenchModelRenderUtils;
import com.github.nalamodikk.common.utils.render.BlockbenchModelRenderUtils.ModelElement;
import com.github.nalamodikk.common.utils.render.RenderAnimationLodUtils;
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

    private static final ResourceLocation TEXTURE_IDLE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/mana_generator_texture.png");
    private static final ResourceLocation TEXTURE_ACTIVE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/mana_generator_active.png");
    private static final ResourceLocation MODEL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "models/block/generator/mana_generator.json");

    private final Map<String, List<ModelElement>> groupElements = new HashMap<>();
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
        groupElements.putAll(BlockbenchModelRenderUtils.parseGroupedElements(modelData, false));
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

        ResourceLocation currentTexture = isWorking ? TEXTURE_ACTIVE : TEXTURE_IDLE;
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entitySolid(currentTexture));

        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bb_main", 0.0F, 0.0F, 0.0F, 0.0F);
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bone2", 0.0F, 0.0F, 0.0F, 0.0F);

        if (animationScale <= 0.0F) {
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bone", 0.0F, 0.0F, 0.0F, 0.0F);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bone3", 0.0F, 0.0F, 0.0F, 0.0F);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bone4", 0.0F, 0.0F, 0.0F, 0.0F);
        } else if (isWorking) {
            float centralRot = time * 10.0F;
            float leftRot = -time * 10.8F;
            float rightRot = time * 10.2F;
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bone", 0.0F, 0.0F, 0.0F, centralRot);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bone3", 0.0F, 0.0F, 0.0F, leftRot);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bone4", 0.0F, 0.0F, 0.0F, rightRot);
        } else {
            float centralFloat = (float) Math.sin(time) * 0.125F;
            float leftFloat = (float) Math.sin(time + Math.PI * 2.0F / 3.0F) * 0.125F;
            float rightFloat = (float) Math.sin(time + Math.PI * 4.0F / 3.0F) * 0.125F;
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bone", 0.0F, centralFloat, 0.0F, 0.0F);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bone3", 0.0F, leftFloat, 0.0F, 0.0F);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bone4", 0.0F, rightFloat, 0.0F, 0.0F);
        }

        poseStack.popPose();
    }

    private void applyBlockRotation(PoseStack poseStack, Direction facing) {
        BlockbenchModelRenderUtils.applyHorizontalFacingRotation(poseStack, facing, 0.0F, 180.0F, 90.0F, -90.0F);
    }

    private void renderGroup(PoseStack poseStack, VertexConsumer vertexConsumer,
                             int packedLight, int packedOverlay, String groupName,
                             float offsetX, float offsetY, float offsetZ, float rotationY) {
        BlockbenchModelRenderUtils.renderGroup(
                poseStack, vertexConsumer, packedLight, packedOverlay,
                groupElements, groupName, offsetX, offsetY, offsetZ, rotationY, this::getBlockbenchGroupOrigin
        );
    }

    private Vector3f getBlockbenchGroupOrigin(String groupName) {
        return switch (groupName) {
            case "bone" -> new Vector3f(8.0F / 16.0F, 29.24749F / 16.0F, 8.29246F / 16.0F);
            case "bone3" -> new Vector3f(3.0F / 16.0F, 29.24749F / 16.0F, 8.29246F / 16.0F);
            case "bone4" -> new Vector3f(13.0F / 16.0F, 29.24749F / 16.0F, 8.29246F / 16.0F);
            default -> new Vector3f(0.0F, 0.0F, 0.0F);
        };
    }

    @Override
    public boolean shouldRenderOffScreen(ManaGeneratorBlockEntity blockEntity) {
        return true;
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
