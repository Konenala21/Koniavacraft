package com.github.nalamodikk.common.block.blockentity.collector.solarmana;

import com.github.nalamodikk.KoniavacraftMod;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.util.*;

public class SolarCollectorRenderer implements BlockEntityRenderer<SolarManaCollectorBlockEntity>, IBlockEntityRendererExtension<SolarManaCollectorBlockEntity> {

    private static final Logger LOGGER = LogUtils.getLogger();

    // 📖 模型文件路徑
    private static final ResourceLocation MODEL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "models/block/collector/solar_mana_collector.json");

    // 📦 解析後的模型數據
    private final Map<String, List<ModelElement>> groupElements = new HashMap<>();
    private boolean modelLoaded = false;

    public SolarCollectorRenderer(BlockEntityRendererProvider.Context context) {
        loadAndParseModel();
    }

    @Override
    public void render(SolarManaCollectorBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        if (!modelLoaded || blockEntity.getLevel() == null) return;

        // 🎯 計算動畫時間
        float time = (blockEntity.getLevel().getGameTime() + partialTick) * 0.05F;

        // 🎨 使用材質
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/solar_mana_collector.png");
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucentCull(texture));

        poseStack.pushPose();

        // 🧭 根據方塊朝向旋轉
        if (blockEntity.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            applyBlockRotation(poseStack, facing);
        }

        // 🏗️ 渲染各個部分
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "standalone_0", 0, 0, 0, 0); // 支柱
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "standalone_4", 0, 0, 0, 0); // 連接線
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "group", 0, 0, 0, 0);        // 基座和面板

        // 🔮 動畫晶體群組
        renderCrystalAnimation(poseStack, vertexConsumer, packedLight, packedOverlay, time);

        poseStack.popPose();
    }

    /**
     * 🔮 渲染晶體動畫 - 只有上下浮動
     */
    private void renderCrystalAnimation(PoseStack poseStack, VertexConsumer vertexConsumer,
                                        int packedLight, int packedOverlay, float time) {

        // 只有柔和的上下浮動，沒有旋轉
        float floatY = (float) Math.sin(time * 1.5) * 0.05F;

        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bone", 0, floatY, 0, 0);
    }

    @Override
    public AABB getRenderBoundingBox(SolarManaCollectorBlockEntity blockEntity) {
        int x = blockEntity.getBlockPos().getX();
        int y = blockEntity.getBlockPos().getY();
        int z = blockEntity.getBlockPos().getZ();
        return new AABB(x, y, z, x + 1, y + 2, z + 1);
    }

    /**
     * 🧭 根據方塊的 FACING 狀態應用旋轉
     */
    private void applyBlockRotation(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5, 0.0, 0.5);

        switch (facing) {
            case NORTH:
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));

                break;
            case SOUTH:
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(0.0F));
                break;
            case WEST:
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
                break;
            case EAST:
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
                break;
        }

        poseStack.translate(-0.5, 0.0, -0.5);
    }

    /**
     * 📖 載入並解析 JSON 模型
     */
    private void loadAndParseModel() {
        LOGGER.info("🔍 開始載入太陽能收集器模型");

        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(MODEL_LOCATION);
            if (resource.isPresent()) {
                try (InputStreamReader reader = new InputStreamReader(resource.get().open())) {
                    JsonObject modelData = JsonParser.parseReader(reader).getAsJsonObject();
                    parseModelData(modelData);
                    modelLoaded = true;
                    LOGGER.info("✅ 模型載入成功，共 {} 個群組", groupElements.size());
                }
            } else {
                LOGGER.error("❌ 找不到模型檔案: {}", MODEL_LOCATION);
            }
        } catch (Exception e) {
            LOGGER.error("❌ 載入模型失敗", e);
        }
    }

    /**
     * 🔧 解析 JSON 模型數據
     */
    private void parseModelData(JsonObject modelData) {
        JsonArray elements = modelData.getAsJsonArray("elements");
        JsonArray groups = modelData.getAsJsonArray("groups");

        // 🎯 建立元素到群組的映射
        Map<Integer, String> elementToGroup = new HashMap<>();

        // 解析群組結構
        if (groups != null) {
            for (int i = 0; i < groups.size(); i++) {
                JsonElement groupElement = groups.get(i);

                if (groupElement.isJsonObject()) {
                    // 真正的群組對象
                    JsonObject group = groupElement.getAsJsonObject();
                    String groupName = group.get("name").getAsString();

                    if (group.has("children")) {
                        JsonArray children = group.getAsJsonArray("children");
                        for (int j = 0; j < children.size(); j++) {
                            int elementIndex = children.get(j).getAsInt();
                            elementToGroup.put(elementIndex, groupName);
                        }
                    }
                    LOGGER.debug("📦 群組 '{}' 包含 {} 個元素", groupName,
                            group.has("children") ? group.getAsJsonArray("children").size() : 0);

                } else if (groupElement.isJsonPrimitive()) {
                    // 獨立元素
                    int elementIndex = groupElement.getAsInt();
                    elementToGroup.put(elementIndex, "standalone_" + elementIndex);
                    LOGGER.debug("🧱 獨立元素: {}", elementIndex);
                }
            }
        }

        // 🧱 解析所有元素
        for (int i = 0; i < elements.size(); i++) {
            JsonObject element = elements.get(i).getAsJsonObject();

            // 決定群組名稱
            String groupName = elementToGroup.getOrDefault(i,
                    element.has("name") ? element.get("name").getAsString() : "unknown_" + i);

            ModelElement modelElement = parseElement(element);
            groupElements.computeIfAbsent(groupName, k -> new ArrayList<>()).add(modelElement);
        }

        LOGGER.info("📊 模型解析完成:");
        groupElements.forEach((name, elements1) ->
                LOGGER.info("  - {}: {} 個元素", name, elements1.size()));
    }

    /**
     * 🧱 解析單個元素
     */
    private ModelElement parseElement(JsonObject element) {
        // 解析位置和尺寸
        JsonArray from = element.getAsJsonArray("from");
        JsonArray to = element.getAsJsonArray("to");

        float x1 = from.get(0).getAsFloat() / 16.0F;
        float y1 = from.get(1).getAsFloat() / 16.0F;
        float z1 = from.get(2).getAsFloat() / 16.0F;
        float x2 = to.get(0).getAsFloat() / 16.0F;
        float y2 = to.get(1).getAsFloat() / 16.0F;
        float z2 = to.get(2).getAsFloat() / 16.0F;

        // 解析旋轉
        float rotationAngle = 0;
        String rotationAxis = "y";
        float[] rotationOrigin = {0, 0, 0};

        if (element.has("rotation")) {
            JsonObject rotation = element.getAsJsonObject("rotation");
            rotationAngle = rotation.get("angle").getAsFloat();
            rotationAxis = rotation.get("axis").getAsString();

            JsonArray origin = rotation.getAsJsonArray("origin");
            rotationOrigin[0] = origin.get(0).getAsFloat() / 16.0F;
            rotationOrigin[1] = origin.get(1).getAsFloat() / 16.0F;
            rotationOrigin[2] = origin.get(2).getAsFloat() / 16.0F;
        }

        // 解析 UV 映射
        Map<String, FaceUV> faceUVs = new HashMap<>();
        if (element.has("faces")) {
            JsonObject faces = element.getAsJsonObject("faces");
            for (String faceName : faces.keySet()) {
                JsonObject face = faces.getAsJsonObject(faceName);
                if (face.has("uv")) {
                    JsonArray uv = face.getAsJsonArray("uv");
                    float u1 = uv.get(0).getAsFloat() / 16.0F;
                    float v1 = uv.get(1).getAsFloat() / 16.0F;
                    float u2 = uv.get(2).getAsFloat() / 16.0F;
                    float v2 = uv.get(3).getAsFloat() / 16.0F;

                    int rotation = face.has("rotation") ? face.get("rotation").getAsInt() : 0;
                    faceUVs.put(faceName, new FaceUV(u1, v1, u2, v2, rotation));
                }
            }
        }

        return new ModelElement(x1, y1, z1, x2, y2, z2, rotationAngle, rotationAxis, rotationOrigin, faceUVs);
    }

    /**
     * 🎨 渲染特定群組
     */
    private void renderGroup(PoseStack poseStack, VertexConsumer vertexConsumer,
                             int packedLight, int packedOverlay, String groupName,
                             float offsetX, float offsetY, float offsetZ, float rotationY) {

        List<ModelElement> elements = groupElements.get(groupName);
        if (elements == null || elements.isEmpty()) return;

        poseStack.pushPose();

        // 應用動畫變換
        poseStack.translate(offsetX, offsetY, offsetZ);

        // 旋轉動畫
        if (rotationY != 0) {
            Vector3f groupOrigin = getGroupOrigin(groupName);

            poseStack.translate(groupOrigin.x(), groupOrigin.y(), groupOrigin.z());
            poseStack.mulPose(com.mojang.math.Axis.YP.rotation(rotationY));
            poseStack.translate(-groupOrigin.x(), -groupOrigin.y(), -groupOrigin.z());
        }

        // 渲染所有元素
        for (ModelElement element : elements) {
            renderElement(poseStack, vertexConsumer, packedLight, packedOverlay, element);
        }

        poseStack.popPose();
    }

    /**
     * 🎯 獲取群組旋轉原點
     */
    private Vector3f getGroupOrigin(String groupName) {
        return switch (groupName) {
            case "bone" -> new Vector3f(0.0F / 16.0F, 29.24749F / 16.0F, 0.29246F / 16.0F);
            default -> new Vector3f(0.5F, 0.5F, 0.5F);
        };
    }

    /**
     * 🧱 渲染單個元素
     */
    private void renderElement(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, ModelElement element) {

        poseStack.pushPose();

        // 處理元素旋轉
        if (element.rotationAngle != 0) {
            poseStack.translate(element.rotationOrigin[0], element.rotationOrigin[1], element.rotationOrigin[2]);

            switch (element.rotationAxis) {
                case "x":
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(element.rotationAngle));
                    break;
                case "y":
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(element.rotationAngle));
                    break;
                case "z":
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(element.rotationAngle));
                    break;
            }

            poseStack.translate(-element.rotationOrigin[0], -element.rotationOrigin[1], -element.rotationOrigin[2]);
        }

        renderCube(poseStack, vertexConsumer, packedLight, packedOverlay, element);

        poseStack.popPose();
    }

    /**
     * 🧊 渲染立方體
     */
    private void renderCube(PoseStack poseStack, VertexConsumer vertexConsumer,
                            int packedLight, int packedOverlay, ModelElement element) {

        Matrix4f matrix = poseStack.last().pose();
        float x1 = element.x1, y1 = element.y1, z1 = element.z1;
        float x2 = element.x2, y2 = element.y2, z2 = element.z2;

        // 渲染6個面
        FaceUV topUV = element.faceUVs.getOrDefault("up", new FaceUV(0, 0, 1, 1, 0));
        addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1,
                0, 1, 0, topUV);

        FaceUV bottomUV = element.faceUVs.getOrDefault("down", new FaceUV(0, 0, 1, 1, 0));
        addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2,
                0, -1, 0, bottomUV);

        FaceUV northUV = element.faceUVs.getOrDefault("north", new FaceUV(0, 0, 1, 1, 0));
        addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1,
                0, 0, -1, northUV);

        FaceUV southUV = element.faceUVs.getOrDefault("south", new FaceUV(0, 0, 1, 1, 0));
        addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2,
                0, 0, 1, southUV);

        FaceUV westUV = element.faceUVs.getOrDefault("west", new FaceUV(0, 0, 1, 1, 0));
        addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1,
                -1, 0, 0, westUV);

        FaceUV eastUV = element.faceUVs.getOrDefault("east", new FaceUV(0, 0, 1, 1, 0));
        addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2,
                1, 0, 0, eastUV);
    }

    /**
     * 📐 添加四邊形
     */
    private void addQuadWithUV(VertexConsumer vertexConsumer, Matrix4f matrix, int packedLight, int packedOverlay,
                               float x1, float y1, float z1, float x2, float y2, float z2,
                               float x3, float y3, float z3, float x4, float y4, float z4,
                               float nx, float ny, float nz, FaceUV faceUV) {

        Vector3f pos1 = matrix.transformPosition(x1, y1, z1, new Vector3f());
        Vector3f pos2 = matrix.transformPosition(x2, y2, z2, new Vector3f());
        Vector3f pos3 = matrix.transformPosition(x3, y3, z3, new Vector3f());
        Vector3f pos4 = matrix.transformPosition(x4, y4, z4, new Vector3f());

        int color = -1;
        float[][] uvCoords = getRotatedUVCoords(faceUV);

        vertexConsumer.addVertex(pos1.x(), pos1.y(), pos1.z(), color, uvCoords[0][0], uvCoords[0][1], packedOverlay, packedLight, nx, ny, nz);
        vertexConsumer.addVertex(pos2.x(), pos2.y(), pos2.z(), color, uvCoords[1][0], uvCoords[1][1], packedOverlay, packedLight, nx, ny, nz);
        vertexConsumer.addVertex(pos3.x(), pos3.y(), pos3.z(), color, uvCoords[2][0], uvCoords[2][1], packedOverlay, packedLight, nx, ny, nz);
        vertexConsumer.addVertex(pos4.x(), pos4.y(), pos4.z(), color, uvCoords[3][0], uvCoords[3][1], packedOverlay, packedLight, nx, ny, nz);
    }

    /**
     * 🔄 根據旋轉獲取 UV 坐標
     */
    private float[][] getRotatedUVCoords(FaceUV faceUV) {
        float u1 = faceUV.u1, v1 = faceUV.v1, u2 = faceUV.u2, v2 = faceUV.v2;

        float[][] baseCoords = {
                {u1, v2}, {u1, v1}, {u2, v1}, {u2, v2}
        };

        if (faceUV.rotation == 0) {
            return baseCoords;
        }

        float[][] rotatedCoords = new float[4][2];
        for (int i = 0; i < 4; i++) {
            int sourceIndex = (i - faceUV.rotation / 90 + 4) % 4;
            rotatedCoords[i] = baseCoords[sourceIndex];
        }
        return rotatedCoords;
    }

    // ======================================
    // 📦 資料類別
    // ======================================

    private static class ModelElement {
        final float x1, y1, z1, x2, y2, z2;
        final float rotationAngle;
        final String rotationAxis;
        final float[] rotationOrigin;
        final Map<String, FaceUV> faceUVs;

        ModelElement(float x1, float y1, float z1, float x2, float y2, float z2,
                     float rotationAngle, String rotationAxis, float[] rotationOrigin,
                     Map<String, FaceUV> faceUVs) {
            this.x1 = x1; this.y1 = y1; this.z1 = z1;
            this.x2 = x2; this.y2 = y2; this.z2 = z2;
            this.rotationAngle = rotationAngle;
            this.rotationAxis = rotationAxis;
            this.rotationOrigin = rotationOrigin;
            this.faceUVs = faceUVs;
        }
    }

    private static class FaceUV {
        final float u1, v1, u2, v2;
        final int rotation;

        FaceUV(float u1, float v1, float u2, float v2, int rotation) {
            this.u1 = u1; this.v1 = v1;
            this.u2 = u2; this.v2 = v2;
            this.rotation = rotation;
        }
    }
}
