package com.github.nalamodikk.common.block.blockentity.mana_grinder;

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
import com.mojang.blaze3d.platform.NativeImage;
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

public class ManaGrinderRenderer implements BlockEntityRenderer<ManaGrinderBlockEntity>, IBlockEntityRendererExtension<ManaGrinderBlockEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation MODEL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "models/block/mana_grinder.json");
    private static final ResourceLocation TEXTURE_IDLE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/mana_grinder_texture.png");
    private static final ResourceLocation TEXTURE_ACTIVE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/mana_grinder_active.png");
    private static final ResourceLocation TEXTURE_ACTIVE_MC_META =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/mana_grinder_active.png.mcmeta");

    private static final String STATIC_MAIN_GROUP = "bb_main";
    private static final String CRYSTAL_MAIN = "bone";
    private static final String CRYSTAL_LEFT = "bone3";
    private static final String CRYSTAL_RIGHT = "bone4";
    private static final String CRUSHER_LEFT_CORE = "crusher_left_core";
    private static final String CRUSHER_RIGHT_CORE = "crusher_right_core";
    private static final String CRUSHER_LEFT_BLADES = "crusher_left_blades";
    private static final String CRUSHER_RIGHT_BLADES = "crusher_right_blades";
    private static final Vector3f DEFAULT_ORIGIN = new Vector3f(0.5F, 0.5F, 0.5F);
    private static final Vector3f CRYSTAL_MAIN_PIVOT = new Vector3f(8.3F / 16.0F, 29.24749F / 16.0F, 8.29246F / 16.0F);
    private static final Vector3f CRYSTAL_LEFT_PIVOT = new Vector3f(5.1F / 16.0F, 29.24749F / 16.0F, 8.29246F / 16.0F);
    private static final Vector3f CRYSTAL_RIGHT_PIVOT = new Vector3f(11.3F / 16.0F, 29.24749F / 16.0F, 8.29246F / 16.0F);

    private final Map<String, List<ModelElement>> groupElements = new HashMap<>();
    private final Map<String, Vector3f> customOrigins = new HashMap<>();
    private boolean modelLoaded = false;
    private int activeTextureFrameCount = 1;
    private int activeTextureFrameTime = 1;

    public ManaGrinderRenderer(BlockEntityRendererProvider.Context context) {
        loadAndParseModel();
        loadActiveTextureAnimationMeta();
    }

    @Override
    public void render(ManaGrinderBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        if (!modelLoaded || blockEntity.getLevel() == null) {
            return;
        }

        float animationScale = RenderAnimationLodUtils.getAnimationTimeScale(blockEntity.getBlockPos());
        float time = (blockEntity.getLevel().getGameTime() + partialTick) * 0.05F * animationScale;
        boolean isWorking = blockEntity.getBlockState().hasProperty(ManaGrinderBlock.WORKING)
                && blockEntity.getBlockState().getValue(ManaGrinderBlock.WORKING);

        poseStack.pushPose();
        Direction facing = blockEntity.getBlockState().getValue(ManaGrinderBlock.FACING);
        BlockbenchModelRenderUtils.applyHorizontalFacingRotation(poseStack, facing, 0.0F, 180.0F, 90.0F, -90.0F);

        ResourceLocation currentTexture = isWorking ? TEXTURE_ACTIVE : TEXTURE_IDLE;
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(currentTexture));
        BlockbenchModelRenderUtils.UvTransform previousUvTransform = null;
        if (isWorking && activeTextureFrameCount > 1) {
            int frameIndex = resolveActiveTextureFrame(blockEntity.getLevel().getGameTime(), partialTick);
            float frameScaleV = 1.0F / activeTextureFrameCount;
            float frameOffsetV = frameScaleV * frameIndex;
            previousUvTransform = BlockbenchModelRenderUtils.setUvTransform(1.0F, frameScaleV, 0.0F, frameOffsetV);
        }

        try {
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "standalone_0", 0.0F, 0.0F, 0.0F, 0.0F);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "standalone_1", 0.0F, 0.0F, 0.0F, 0.0F);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "standalone_2", 0.0F, 0.0F, 0.0F, 0.0F);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "standalone_3", 0.0F, 0.0F, 0.0F, 0.0F);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "standalone_4", 0.0F, 0.0F, 0.0F, 0.0F);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, STATIC_MAIN_GROUP, 0.0F, 0.0F, 0.0F, 0.0F);

            renderCrystalAnimation(poseStack, vertexConsumer, packedLight, packedOverlay, time, animationScale, isWorking);
            renderCrusherAnimation(poseStack, vertexConsumer, packedLight, packedOverlay, time, animationScale, isWorking);
        } finally {
            if (previousUvTransform != null) {
                BlockbenchModelRenderUtils.restoreUvTransform(previousUvTransform);
            }
        }

        poseStack.popPose();
    }

    private void renderCrystalAnimation(PoseStack poseStack, VertexConsumer vertexConsumer,
                                        int packedLight, int packedOverlay,
                                        float time, float animationScale, boolean isWorking) {
        if (animationScale <= 0.0F) {
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRYSTAL_MAIN, 0.0F, 0.0F, 0.0F, 0.0F);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRYSTAL_LEFT, 0.0F, 0.0F, 0.0F, 0.0F);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRYSTAL_RIGHT, 0.0F, 0.0F, 0.0F, 0.0F);
        } else if (isWorking) {
            float mainRotation = time * 10.0F;
            float leftRotation = -time * 10.8F;
            float rightRotation = time * 10.2F;
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRYSTAL_MAIN, 0.0F, 0.0F, 0.0F, mainRotation);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRYSTAL_LEFT, 0.0F, 0.0F, 0.0F, leftRotation);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRYSTAL_RIGHT, 0.0F, 0.0F, 0.0F, rightRotation);
        } else {
            float mainOffsetY = (float) Math.sin(time) * 0.125F;
            float leftOffsetY = (float) Math.sin(time + Math.PI * 2.0F / 3.0F) * 0.125F;
            float rightOffsetY = (float) Math.sin(time + Math.PI * 4.0F / 3.0F) * 0.125F;
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRYSTAL_MAIN, 0.0F, mainOffsetY, 0.0F, 0.0F);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRYSTAL_LEFT, 0.0F, leftOffsetY, 0.0F, 0.0F);
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, CRYSTAL_RIGHT, 0.0F, rightOffsetY, 0.0F, 0.0F);
        }
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

        renderGroupWithZRotation(poseStack, vertexConsumer, packedLight, packedOverlay, CRUSHER_LEFT_BLADES, leftRotation);
        renderGroupWithZRotation(poseStack, vertexConsumer, packedLight, packedOverlay, CRUSHER_RIGHT_BLADES, rightRotation);
    }

    private void renderGroup(PoseStack poseStack, VertexConsumer vertexConsumer,
                             int packedLight, int packedOverlay, String groupName,
                             float offsetX, float offsetY, float offsetZ, float rotationY) {
        BlockbenchModelRenderUtils.renderGroup(
                poseStack, vertexConsumer, packedLight, packedOverlay,
                groupElements, groupName, offsetX, offsetY, offsetZ, rotationY, this::getGroupOrigin
        );
    }

    private void renderGroupWithZRotation(PoseStack poseStack, VertexConsumer vertexConsumer,
                                          int packedLight, int packedOverlay,
                                          String groupName, float rotationZ) {
        if (Math.abs(rotationZ) < 1.0E-6F) {
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, groupName, 0.0F, 0.0F, 0.0F, 0.0F);
            return;
        }
        Vector3f origin = getGroupOrigin(groupName);
        poseStack.pushPose();
        poseStack.translate(origin.x(), origin.y(), origin.z());
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(rotationZ));
        poseStack.translate(-origin.x(), -origin.y(), -origin.z());
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, groupName, 0.0F, 0.0F, 0.0F, 0.0F);
        poseStack.popPose();
    }

    private Vector3f getGroupOrigin(String groupName) {
        return customOrigins.getOrDefault(groupName, DEFAULT_ORIGIN);
    }

    private void loadAndParseModel() {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(MODEL_LOCATION);
            if (resource.isEmpty()) {
                LOGGER.error("Missing mana grinder model resource: {}", MODEL_LOCATION);
                return;
            }

            try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                JsonObject modelData = JsonParser.parseReader(reader).getAsJsonObject();
                parseModelData(modelData);
                modelLoaded = true;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load mana grinder model.", e);
        }
    }

    private void parseModelData(JsonObject modelData) {
        groupElements.clear();
        customOrigins.clear();

        groupElements.putAll(BlockbenchModelRenderUtils.parseGroupedElements(modelData, true));
        // 與模型本體中心對齊，避免旋轉時產生公轉位移感
        customOrigins.put(CRYSTAL_MAIN, CRYSTAL_MAIN_PIVOT);
        customOrigins.put(CRYSTAL_LEFT, CRYSTAL_LEFT_PIVOT);
        customOrigins.put(CRYSTAL_RIGHT, CRYSTAL_RIGHT_PIVOT);

        List<ModelElement> parsedElements = BlockbenchModelRenderUtils.parseElements(modelData);
        JsonArray groups = modelData.getAsJsonArray("groups");
        registerWheelGroups(groups, parsedElements, "粉碎輪", CRUSHER_LEFT_CORE, CRUSHER_LEFT_BLADES);
        registerWheelGroups(groups, parsedElements, "粉碎輪2", CRUSHER_RIGHT_CORE, CRUSHER_RIGHT_BLADES);
    }

    private void registerWheelGroups(JsonArray groups, List<ModelElement> parsedElements,
                                     String targetGroupName, String coreGroupName, String bladesGroupName) {
        JsonObject targetGroup = findGroupByName(groups, targetGroupName);
        if (targetGroup == null) {
            LOGGER.warn("Mana grinder model group not found: {}", targetGroupName);
            return;
        }

        Vector3f wheelOrigin = readOrigin(targetGroup);
        Set<Integer> coreIndices = new LinkedHashSet<>();
        collectDirectElementIndices(targetGroup, coreIndices);
        if (coreIndices.isEmpty()) {
            LOGGER.warn("Model group {} has no direct pivot elements.", targetGroupName);
        } else {
            List<ModelElement> coreElements = collectElementsByIndices(parsedElements, coreIndices);
            if (!coreElements.isEmpty()) {
                groupElements.put(coreGroupName, coreElements);
                wheelOrigin = calculateElementsCenter(coreElements, wheelOrigin);
                customOrigins.put(coreGroupName, wheelOrigin);
            }
        }

        Set<Integer> bladeIndices = new LinkedHashSet<>();
        collectNestedElementIndices(targetGroup, bladeIndices);
        bladeIndices.removeAll(coreIndices);
        if (bladeIndices.isEmpty()) {
            LOGGER.warn("Model group {} has no renderable elements.", targetGroupName);
            return;
        }
        List<ModelElement> bladeElements = collectElementsByIndices(parsedElements, bladeIndices);
        if (bladeElements.isEmpty()) {
            return;
        }
        groupElements.put(bladesGroupName, bladeElements);
        customOrigins.put(bladesGroupName, wheelOrigin);
    }

    private List<ModelElement> collectElementsByIndices(List<ModelElement> parsedElements, Set<Integer> indices) {
        List<ModelElement> elements = new ArrayList<>();
        for (Integer index : indices) {
            if (index >= 0 && index < parsedElements.size()) {
                elements.add(parsedElements.get(index));
            }
        }
        return elements;
    }

    private Vector3f calculateElementsCenter(List<ModelElement> elements, Vector3f fallback) {
        if (elements.isEmpty()) {
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
            return DEFAULT_ORIGIN;
        }
        JsonArray origin = groupObject.getAsJsonArray("origin");
        return new Vector3f(
                origin.get(0).getAsFloat() / 16.0F,
                origin.get(1).getAsFloat() / 16.0F,
                origin.get(2).getAsFloat() / 16.0F
        );
    }

    private void loadActiveTextureAnimationMeta() {
        activeTextureFrameCount = 1;
        activeTextureFrameTime = 1;
        try {
            Optional<Resource> texture = Minecraft.getInstance().getResourceManager().getResource(TEXTURE_ACTIVE);
            if (texture.isPresent()) {
                try (NativeImage image = NativeImage.read(texture.get().open())) {
                    int width = image.getWidth();
                    int height = image.getHeight();
                    if (width > 0 && height >= width && height % width == 0) {
                        activeTextureFrameCount = Math.max(1, height / width);
                    }
                }
            }

            Optional<Resource> meta = Minecraft.getInstance().getResourceManager().getResource(TEXTURE_ACTIVE_MC_META);
            if (meta.isPresent()) {
                try (InputStreamReader reader = new InputStreamReader(meta.get().open(), StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (root.has("animation") && root.get("animation").isJsonObject()) {
                        JsonObject animation = root.getAsJsonObject("animation");
                        if (animation.has("frametime")) {
                            activeTextureFrameTime = Math.max(1, animation.get("frametime").getAsInt());
                        }
                        if (animation.has("frames") && animation.get("frames").isJsonArray()) {
                            int listedFrames = animation.getAsJsonArray("frames").size();
                            if (listedFrames > 0) {
                                activeTextureFrameCount = listedFrames;
                            }
                        }
                    }
                }
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to read mana_grinder_active animation metadata. Falling back to the static texture.", exception);
            activeTextureFrameCount = 1;
            activeTextureFrameTime = 1;
        }
    }

    private int resolveActiveTextureFrame(long gameTime, float partialTick) {
        if (activeTextureFrameCount <= 1) {
            return 0;
        }
        float ticksPerFrame = Math.max(1, activeTextureFrameTime);
        int frame = (int) Math.floor((gameTime + partialTick) / ticksPerFrame);
        return Math.floorMod(frame, activeTextureFrameCount);
    }

    @Override
    public boolean shouldRenderOffScreen(ManaGrinderBlockEntity blockEntity) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ManaGrinderBlockEntity blockEntity) {
        return BlockbenchModelRenderUtils.getTwoBlockTallBoundingBox(blockEntity.getBlockPos());
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
