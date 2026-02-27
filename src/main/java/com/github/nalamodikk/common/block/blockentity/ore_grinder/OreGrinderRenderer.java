package com.github.nalamodikk.common.block.blockentity.ore_grinder;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.utils.render.BlockbenchModelRenderUtils;
import com.github.nalamodikk.common.utils.render.BlockbenchModelRenderUtils.ModelElement;
import com.github.nalamodikk.common.utils.render.RenderAnimationLodUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class OreGrinderRenderer implements BlockEntityRenderer<OreGrinderBlockEntity>, IBlockEntityRendererExtension<OreGrinderBlockEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation MODEL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "models/block/mana_grinder.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/mana_grinder_texture.png");

    private static final String STATIC_MAIN_GROUP = "bb_main";
    private static final String CRYSTAL_MAIN = "bone";
    private static final String CRYSTAL_LEFT = "bone3";
    private static final String CRYSTAL_RIGHT = "bone4";
    private static final String CRUSHER_LEFT_CORE = "crusher_left_core";
    private static final String CRUSHER_RIGHT_CORE = "crusher_right_core";
    private static final String CRUSHER_LEFT_BLADES = "crusher_left_blades";
    private static final String CRUSHER_RIGHT_BLADES = "crusher_right_blades";

    private final Map<String, List<ModelElement>> groupElements = new HashMap<>();
    private final Map<String, Vector3f> customOrigins = new HashMap<>();
    private boolean modelLoaded = false;

    public OreGrinderRenderer(BlockEntityRendererProvider.Context context) {
        loadAndParseModel();
    }

    @Override
    public void render(OreGrinderBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        if (!modelLoaded || blockEntity.getLevel() == null) {
            return;
        }

        float animationScale = RenderAnimationLodUtils.getAnimationTimeScale(blockEntity.getBlockPos());
        float time = (blockEntity.getLevel().getGameTime() + partialTick) * 0.05F * animationScale;
        boolean isWorking = blockEntity.getProgress() > 0;

        poseStack.pushPose();
        Direction facing = blockEntity.getBlockState().getValue(OreGrinderBlock.FACING);
        BlockbenchModelRenderUtils.applyHorizontalFacingRotation(poseStack, facing, 0.0F, 180.0F, 90.0F, -90.0F);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entitySolid(TEXTURE));

        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "standalone_0", 0.0F, 0.0F, 0.0F, 0.0F);
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "standalone_1", 0.0F, 0.0F, 0.0F, 0.0F);
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "standalone_2", 0.0F, 0.0F, 0.0F, 0.0F);
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "standalone_3", 0.0F, 0.0F, 0.0F, 0.0F);
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "standalone_4", 0.0F, 0.0F, 0.0F, 0.0F);
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, STATIC_MAIN_GROUP, 0.0F, 0.0F, 0.0F, 0.0F);

        renderCrystalAnimation(poseStack, vertexConsumer, packedLight, packedOverlay, time, animationScale, isWorking);
        renderCrusherAnimation(poseStack, vertexConsumer, packedLight, packedOverlay, time, animationScale, isWorking);

        poseStack.popPose();
    }

    private void renderCrystalAnimation(PoseStack poseStack, VertexConsumer vertexConsumer,
                                        int packedLight, int packedOverlay,
                                        float time, float animationScale, boolean isWorking) {
        float mainOffsetY = 0.0F;
        float leftOffsetY = 0.0F;
        float rightOffsetY = 0.0F;

        if (animationScale > 0.0F && !isWorking) {
            float amplitude = 0.09F * animationScale;
            mainOffsetY = (float) Math.sin(time) * amplitude;
            leftOffsetY = (float) Math.sin(time + Math.PI * 2.0F / 3.0F) * amplitude;
            rightOffsetY = (float) Math.sin(time + Math.PI * 4.0F / 3.0F) * amplitude;
        }

        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRYSTAL_MAIN, 0.0F, mainOffsetY, 0.0F, 0.0F);
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRYSTAL_LEFT, 0.0F, leftOffsetY, 0.0F, 0.0F);
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRYSTAL_RIGHT, 0.0F, rightOffsetY, 0.0F, 0.0F);
    }

    private void renderCrusherAnimation(PoseStack poseStack, VertexConsumer vertexConsumer,
                                        int packedLight, int packedOverlay,
                                        float time, float animationScale, boolean isWorking) {
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRUSHER_LEFT_CORE, 0.0F, 0.0F, 0.0F, 0.0F);
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRUSHER_RIGHT_CORE, 0.0F, 0.0F, 0.0F, 0.0F);

        float leftRotation = 0.0F;
        float rightRotation = 0.0F;

        if (animationScale > 0.0F && isWorking) {
            float angularFactor = 15.0F;
            leftRotation = time * angularFactor;
            rightRotation = -time * angularFactor;
        }

        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRUSHER_LEFT_BLADES, 0.0F, 0.0F, 0.0F, leftRotation);
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRUSHER_RIGHT_BLADES, 0.0F, 0.0F, 0.0F, rightRotation);
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
        return customOrigins.getOrDefault(groupName, new Vector3f(0.5F, 0.5F, 0.5F));
    }

    private void loadAndParseModel() {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(MODEL_LOCATION);
            if (resource.isEmpty()) {
                LOGGER.error("❌ 找不到模型檔案: {}", MODEL_LOCATION);
                return;
            }

            try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                JsonObject modelData = JsonParser.parseReader(reader).getAsJsonObject();
                parseModelData(modelData);
                modelLoaded = true;
            }
        } catch (Exception e) {
            LOGGER.error("❌ 載入粉碎機模型失敗", e);
        }
    }

    private void parseModelData(JsonObject modelData) {
        groupElements.clear();
        customOrigins.clear();

        groupElements.putAll(BlockbenchModelRenderUtils.parseGroupedElements(modelData, true));
        customOrigins.put(CRYSTAL_MAIN, new Vector3f(8.0F / 16.0F, 29.24749F / 16.0F, 8.29246F / 16.0F));
        customOrigins.put(CRYSTAL_LEFT, new Vector3f(3.0F / 16.0F, 29.24749F / 16.0F, 8.29246F / 16.0F));
        customOrigins.put(CRYSTAL_RIGHT, new Vector3f(13.0F / 16.0F, 29.24749F / 16.0F, 8.29246F / 16.0F));

        List<ModelElement> parsedElements = BlockbenchModelRenderUtils.parseElements(modelData);
        JsonArray groups = modelData.getAsJsonArray("groups");
        registerWheelGroups(groups, parsedElements, "粉碎輪", CRUSHER_LEFT_CORE, CRUSHER_LEFT_BLADES);
        registerWheelGroups(groups, parsedElements, "粉碎輪2", CRUSHER_RIGHT_CORE, CRUSHER_RIGHT_BLADES);
    }

    private void registerWheelGroups(JsonArray groups, List<ModelElement> parsedElements,
                                     String targetGroupName, String coreGroupName, String bladesGroupName) {
        JsonObject targetGroup = findGroupByName(groups, targetGroupName);
        if (targetGroup == null) {
            LOGGER.warn("⚠️ mana_grinder 模型找不到群組: {}", targetGroupName);
            return;
        }

        Set<Integer> coreIndices = new LinkedHashSet<>();
        collectDirectElementIndices(targetGroup, coreIndices);
        if (coreIndices.isEmpty()) {
            LOGGER.warn("⚠️ 群組 {} 找不到軸心元素（直接 children index）", targetGroupName);
        } else {
            registerGroupByIndices(parsedElements, coreIndices, targetGroup, coreGroupName);
        }

        Set<Integer> bladeIndices = new LinkedHashSet<>();
        collectNestedElementIndices(targetGroup, bladeIndices);
        if (bladeIndices.isEmpty()) {
            LOGGER.warn("⚠️ 群組 {} 沒有可渲染元素", targetGroupName);
            return;
        }
        registerGroupByIndices(parsedElements, bladeIndices, targetGroup, bladesGroupName);
    }

    private void registerGroupByIndices(List<ModelElement> parsedElements, Set<Integer> indices,
                                        JsonObject sourceGroup, String outputGroupName) {
        List<ModelElement> elements = new ArrayList<>();
        for (Integer index : indices) {
            if (index >= 0 && index < parsedElements.size()) {
                elements.add(parsedElements.get(index));
            }
        }
        if (elements.isEmpty()) {
            return;
        }
        groupElements.put(outputGroupName, elements);
        customOrigins.put(outputGroupName, readOrigin(sourceGroup));
    }

    private JsonObject findGroupByName(JsonArray groups, String targetName) {
        for (int i = 0; i < groups.size(); i++) {
            JsonElement element = groups.get(i);
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject groupObject = element.getAsJsonObject();
            if (groupObject.has("name") && targetName.equals(groupObject.get("name").getAsString())) {
                return groupObject;
            }
            if (groupObject.has("children")) {
                JsonArray children = groupObject.getAsJsonArray("children");
                JsonObject nestedResult = findGroupByName(children, targetName);
                if (nestedResult != null) {
                    return nestedResult;
                }
            }
        }
        return null;
    }

    private void collectDirectElementIndices(JsonObject groupObject, Set<Integer> result) {
        if (!groupObject.has("children")) {
            return;
        }
        JsonArray children = groupObject.getAsJsonArray("children");
        for (int i = 0; i < children.size(); i++) {
            JsonElement child = children.get(i);
            if (child.isJsonPrimitive()) {
                result.add(child.getAsInt());
            }
        }
    }

    private void collectNestedElementIndices(JsonObject groupObject, Set<Integer> result) {
        if (!groupObject.has("children")) {
            return;
        }
        JsonArray children = groupObject.getAsJsonArray("children");
        for (int i = 0; i < children.size(); i++) {
            JsonElement child = children.get(i);
            if (child.isJsonObject()) {
                collectAllElementIndices(child.getAsJsonObject(), result);
            }
        }
    }

    private void collectAllElementIndices(JsonObject groupObject, Set<Integer> result) {
        if (!groupObject.has("children")) {
            return;
        }
        JsonArray children = groupObject.getAsJsonArray("children");
        for (int i = 0; i < children.size(); i++) {
            JsonElement child = children.get(i);
            if (child.isJsonPrimitive()) {
                result.add(child.getAsInt());
            } else if (child.isJsonObject()) {
                collectAllElementIndices(child.getAsJsonObject(), result);
            }
        }
    }

    private Vector3f readOrigin(JsonObject groupObject) {
        if (!groupObject.has("origin")) {
            return new Vector3f(0.5F, 0.5F, 0.5F);
        }
        JsonArray origin = groupObject.getAsJsonArray("origin");
        return new Vector3f(
                origin.get(0).getAsFloat() / 16.0F,
                origin.get(1).getAsFloat() / 16.0F,
                origin.get(2).getAsFloat() / 16.0F
        );
    }

    @Override
    public boolean shouldRenderOffScreen(OreGrinderBlockEntity blockEntity) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(OreGrinderBlockEntity blockEntity) {
        return BlockbenchModelRenderUtils.getTwoBlockTallBoundingBox(blockEntity.getBlockPos());
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
