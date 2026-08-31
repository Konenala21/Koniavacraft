package com.github.nalamodikk.common.block.blockentity.mana_grinder;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.utils.render.BlockbenchModelRenderUtils;
import com.github.nalamodikk.common.utils.render.BlockbenchModelRenderUtils.ModelElement;
import com.github.nalamodikk.client.utils.render.RenderAnimationLodUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private static final ResourceLocation TEXTURE_CRYSTAL =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/mana_crystal_3d.png");

    // 蕎麥麵 2026-06 重做的粉碎機模型的群組名字（見 mana_pulverizer.bbmodel）
    private static final String BODY_GROUP = "機身";
    private static final String WHEEL_GROUP = "滾輪";
    private static final String CRYSTAL_MAIN = "水晶中";
    private static final String CRYSTAL_LEFT = "水晶左";
    private static final String CRYSTAL_RIGHT = "水晶右";
    private static final Vector3f DEFAULT_ORIGIN = new Vector3f(0.5F, 0.5F, 0.5F);

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

        // mana_grinder_active.png 是蕎麥麵重做模型「之前」的舊資產，UV 版面跟新模型完全對不上（構圖是
        // 另一組小圖示動畫，不是新機身貼圖的縮小版），套上去會顯示錯亂內容。蕎麥麵補一份照新版面畫的
        // 運作動畫貼圖之前，機身貼圖固定用閒置版；滾輪轉動、水晶行為這些運作中的動畫邏輯完全不受影響。
        // 要接回運作貼圖動畫時：currentBodyTexture 改回 isWorking ? TEXTURE_ACTIVE : TEXTURE_IDLE，
        // 並把下面這段 frame 動畫的 UvTransform 邏輯放回來（邏輯本身沒問題，只是先沒有能用的貼圖）。
        ResourceLocation currentBodyTexture = TEXTURE_IDLE;
        BlockbenchModelRenderUtils.UvTransform previousUvTransform = null;

        try {
            // 兩張貼圖分開拿 buffer；用完一個才拿下一個，避免共用排序 buffer 時把前一個沖掉導致 crash
            VertexConsumer bodyConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(currentBodyTexture));
            renderGroup(poseStack, bodyConsumer, packedLight, packedOverlay, BODY_GROUP, 0.0F, 0.0F, 0.0F, 0.0F);
            renderWheelAnimation(poseStack, bodyConsumer, packedLight, packedOverlay, time, animationScale, isWorking);

            VertexConsumer crystalConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE_CRYSTAL));
            renderCrystalAnimation(poseStack, crystalConsumer, packedLight, packedOverlay, time, animationScale, isWorking);
        } finally {
            if (previousUvTransform != null) {
                BlockbenchModelRenderUtils.restoreUvTransform(previousUvTransform);
            }
        }

        poseStack.popPose();
    }

    private void renderWheelAnimation(PoseStack poseStack, VertexConsumer vertexConsumer,
                                      int packedLight, int packedOverlay,
                                      float time, float animationScale, boolean isWorking) {
        // 滾輪一起同步轉，繞水平的 X 軸滾動，表面看起來是上下捲動（不是繞 Z 軸像鐘面那樣轉圈），不工作時停止
        float rotation = (animationScale > 0.0F && isWorking) ? time * -15.0F : 0.0F;
        renderGroupWithXRotation(poseStack, vertexConsumer, packedLight, packedOverlay, WHEEL_GROUP, rotation);
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

    private void renderGroup(PoseStack poseStack, VertexConsumer vertexConsumer,
                             int packedLight, int packedOverlay, String groupName,
                             float offsetX, float offsetY, float offsetZ, float rotationY) {
        BlockbenchModelRenderUtils.renderGroup(
                poseStack, vertexConsumer, packedLight, packedOverlay,
                groupElements, groupName, offsetX, offsetY, offsetZ, rotationY, this::getGroupOrigin
        );
    }

    private void renderGroupWithXRotation(PoseStack poseStack, VertexConsumer vertexConsumer,
                                          int packedLight, int packedOverlay,
                                          String groupName, float rotationX) {
        if (Math.abs(rotationX) < 1.0E-6F) {
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, groupName, 0.0F, 0.0F, 0.0F, 0.0F);
            return;
        }
        Vector3f origin = getGroupOrigin(groupName);
        poseStack.pushPose();
        poseStack.translate(origin.x(), origin.y(), origin.z());
        poseStack.mulPose(com.mojang.math.Axis.XP.rotation(rotationX));
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

        // 群組沒有自己標定精確的旋轉軸心時，退回用群組本身方塊的幾何中心當軸心
        customOrigins.put(WHEEL_GROUP, calculateElementsCenter(groupElements.get(WHEEL_GROUP), DEFAULT_ORIGIN));
        customOrigins.put(CRYSTAL_MAIN, calculateElementsCenter(groupElements.get(CRYSTAL_MAIN), DEFAULT_ORIGIN));
        customOrigins.put(CRYSTAL_LEFT, calculateElementsCenter(groupElements.get(CRYSTAL_LEFT), DEFAULT_ORIGIN));
        customOrigins.put(CRYSTAL_RIGHT, calculateElementsCenter(groupElements.get(CRYSTAL_RIGHT), DEFAULT_ORIGIN));
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
    public AABB getRenderBoundingBox(ManaGrinderBlockEntity blockEntity) {
        return BlockbenchModelRenderUtils.getTwoBlockTallBoundingBox(blockEntity.getBlockPos());
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
