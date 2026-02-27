package com.github.nalamodikk.common.utils.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class BlockbenchModelRenderUtils {
    private BlockbenchModelRenderUtils() {
    }

    public static Map<String, List<ModelElement>> parseGroupedElements(JsonObject modelData, boolean allowStandaloneElements) {
        JsonArray elements = modelData.getAsJsonArray("elements");
        JsonArray groups = modelData.getAsJsonArray("groups");

        Map<Integer, String> elementToGroup = new HashMap<>();
        if (groups != null) {
            for (int i = 0; i < groups.size(); i++) {
                JsonElement groupElement = groups.get(i);
                if (groupElement.isJsonObject()) {
                    JsonObject groupObject = groupElement.getAsJsonObject();
                    String groupName = groupObject.get("name").getAsString();
                    if (groupObject.has("children")) {
                        JsonArray children = groupObject.getAsJsonArray("children");
                        for (int j = 0; j < children.size(); j++) {
                            JsonElement childElement = children.get(j);
                            if (childElement.isJsonPrimitive()) {
                                int elementIndex = childElement.getAsInt();
                                elementToGroup.put(elementIndex, groupName);
                            }
                        }
                    }
                } else if (allowStandaloneElements && groupElement.isJsonPrimitive()) {
                    int elementIndex = groupElement.getAsInt();
                    elementToGroup.put(elementIndex, "standalone_" + elementIndex);
                }
            }
        }

        Map<String, List<ModelElement>> groupedElements = new HashMap<>();
        for (int i = 0; i < elements.size(); i++) {
            JsonObject element = elements.get(i).getAsJsonObject();
            String groupName = elementToGroup.getOrDefault(i,
                    element.has("name") ? element.get("name").getAsString() : "unknown_" + i);
            ModelElement modelElement = parseElement(element);
            groupedElements.computeIfAbsent(groupName, unused -> new ArrayList<>()).add(modelElement);
        }

        return groupedElements;
    }

    public static List<ModelElement> parseElements(JsonObject modelData) {
        JsonArray elements = modelData.getAsJsonArray("elements");
        List<ModelElement> parsedElements = new ArrayList<>(elements.size());
        for (int i = 0; i < elements.size(); i++) {
            JsonObject element = elements.get(i).getAsJsonObject();
            parsedElements.add(parseElement(element));
        }
        return parsedElements;
    }

    public static void renderGroup(PoseStack poseStack, VertexConsumer vertexConsumer,
                                   int packedLight, int packedOverlay,
                                   Map<String, List<ModelElement>> groupedElements,
                                   String groupName, float offsetX, float offsetY, float offsetZ, float rotationY,
                                   Function<String, Vector3f> groupOriginResolver) {
        List<ModelElement> elements = groupedElements.get(groupName);
        if (elements == null || elements.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(offsetX, offsetY, offsetZ);

        if (rotationY != 0.0F) {
            Vector3f groupOrigin = groupOriginResolver != null ? groupOriginResolver.apply(groupName) : new Vector3f(0.5F, 0.5F, 0.5F);
            if (groupOrigin == null) {
                groupOrigin = new Vector3f(0.5F, 0.5F, 0.5F);
            }
            poseStack.translate(groupOrigin.x(), groupOrigin.y(), groupOrigin.z());
            poseStack.mulPose(com.mojang.math.Axis.YP.rotation(rotationY));
            poseStack.translate(-groupOrigin.x(), -groupOrigin.y(), -groupOrigin.z());
        }

        for (ModelElement element : elements) {
            renderElement(poseStack, vertexConsumer, packedLight, packedOverlay, element);
        }
        poseStack.popPose();
    }

    public static void applyHorizontalFacingRotation(PoseStack poseStack, Direction facing,
                                                     float northDegrees, float southDegrees,
                                                     float westDegrees, float eastDegrees) {
        poseStack.translate(0.5, 0.0, 0.5);
        switch (facing) {
            case NORTH:
                if (northDegrees != 0.0F) {
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(northDegrees));
                }
                break;
            case SOUTH:
                if (southDegrees != 0.0F) {
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(southDegrees));
                }
                break;
            case WEST:
                if (westDegrees != 0.0F) {
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(westDegrees));
                }
                break;
            case EAST:
                if (eastDegrees != 0.0F) {
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(eastDegrees));
                }
                break;
            default:
                break;
        }
        poseStack.translate(-0.5, 0.0, -0.5);
    }

    public static AABB getTwoBlockTallBoundingBox(BlockPos blockPos) {
        int x = blockPos.getX();
        int y = blockPos.getY();
        int z = blockPos.getZ();
        return new AABB(x, y, z, x + 1, y + 2, z + 1);
    }

    private static ModelElement parseElement(JsonObject element) {
        JsonArray from = element.getAsJsonArray("from");
        JsonArray to = element.getAsJsonArray("to");

        float x1 = from.get(0).getAsFloat() / 16.0F;
        float y1 = from.get(1).getAsFloat() / 16.0F;
        float z1 = from.get(2).getAsFloat() / 16.0F;
        float x2 = to.get(0).getAsFloat() / 16.0F;
        float y2 = to.get(1).getAsFloat() / 16.0F;
        float z2 = to.get(2).getAsFloat() / 16.0F;

        float rotationAngle = 0.0F;
        String rotationAxis = "y";
        float[] rotationOrigin = new float[]{0.0F, 0.0F, 0.0F};
        if (element.has("rotation")) {
            JsonObject rotation = element.getAsJsonObject("rotation");
            rotationAngle = rotation.get("angle").getAsFloat();
            rotationAxis = rotation.get("axis").getAsString();

            JsonArray origin = rotation.getAsJsonArray("origin");
            rotationOrigin[0] = origin.get(0).getAsFloat() / 16.0F;
            rotationOrigin[1] = origin.get(1).getAsFloat() / 16.0F;
            rotationOrigin[2] = origin.get(2).getAsFloat() / 16.0F;
        }

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

    private static void renderElement(PoseStack poseStack, VertexConsumer vertexConsumer,
                                      int packedLight, int packedOverlay, ModelElement element) {
        poseStack.pushPose();
        if (element.rotationAngle != 0.0F) {
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
                default:
                    break;
            }
            poseStack.translate(-element.rotationOrigin[0], -element.rotationOrigin[1], -element.rotationOrigin[2]);
        }

        renderCube(poseStack, vertexConsumer, packedLight, packedOverlay, element);
        poseStack.popPose();
    }

    private static void renderCube(PoseStack poseStack, VertexConsumer vertexConsumer,
                                   int packedLight, int packedOverlay, ModelElement element) {
        Matrix4f matrix = poseStack.last().pose();

        float x1 = element.x1;
        float y1 = element.y1;
        float z1 = element.z1;
        float x2 = element.x2;
        float y2 = element.y2;
        float z2 = element.z2;

        FaceUV topUV = element.faceUVs.getOrDefault("up", FaceUV.DEFAULT);
        addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1,
                0.0F, 1.0F, 0.0F, topUV);

        FaceUV bottomUV = element.faceUVs.getOrDefault("down", FaceUV.DEFAULT);
        addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2,
                0.0F, -1.0F, 0.0F, bottomUV);

        FaceUV northUV = element.faceUVs.getOrDefault("north", FaceUV.DEFAULT);
        addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1,
                0.0F, 0.0F, -1.0F, northUV);

        FaceUV southUV = element.faceUVs.getOrDefault("south", FaceUV.DEFAULT);
        addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2,
                0.0F, 0.0F, 1.0F, southUV);

        FaceUV westUV = element.faceUVs.getOrDefault("west", FaceUV.DEFAULT);
        addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1,
                -1.0F, 0.0F, 0.0F, westUV);

        FaceUV eastUV = element.faceUVs.getOrDefault("east", FaceUV.DEFAULT);
        addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2,
                1.0F, 0.0F, 0.0F, eastUV);
    }

    private static void addQuadWithUV(VertexConsumer vertexConsumer, Matrix4f matrix, int packedLight, int packedOverlay,
                                      float x1, float y1, float z1, float x2, float y2, float z2,
                                      float x3, float y3, float z3, float x4, float y4, float z4,
                                      float nx, float ny, float nz, FaceUV faceUV) {
        Vector3f pos1 = matrix.transformPosition(x1, y1, z1, new Vector3f());
        Vector3f pos2 = matrix.transformPosition(x2, y2, z2, new Vector3f());
        Vector3f pos3 = matrix.transformPosition(x3, y3, z3, new Vector3f());
        Vector3f pos4 = matrix.transformPosition(x4, y4, z4, new Vector3f());

        float[][] uvCoords = getRotatedUVCoords(faceUV);
        int color = -1;
        vertexConsumer.addVertex(pos1.x(), pos1.y(), pos1.z(), color, uvCoords[0][0], uvCoords[0][1], packedOverlay, packedLight, nx, ny, nz);
        vertexConsumer.addVertex(pos2.x(), pos2.y(), pos2.z(), color, uvCoords[1][0], uvCoords[1][1], packedOverlay, packedLight, nx, ny, nz);
        vertexConsumer.addVertex(pos3.x(), pos3.y(), pos3.z(), color, uvCoords[2][0], uvCoords[2][1], packedOverlay, packedLight, nx, ny, nz);
        vertexConsumer.addVertex(pos4.x(), pos4.y(), pos4.z(), color, uvCoords[3][0], uvCoords[3][1], packedOverlay, packedLight, nx, ny, nz);
    }

    private static float[][] getRotatedUVCoords(FaceUV faceUV) {
        float[][] baseCoords = new float[][]{
                {faceUV.u1, faceUV.v2},
                {faceUV.u1, faceUV.v1},
                {faceUV.u2, faceUV.v1},
                {faceUV.u2, faceUV.v2}
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

    public static class ModelElement {
        public final float x1;
        public final float y1;
        public final float z1;
        public final float x2;
        public final float y2;
        public final float z2;
        public final float rotationAngle;
        public final String rotationAxis;
        public final float[] rotationOrigin;
        public final Map<String, FaceUV> faceUVs;

        public ModelElement(float x1, float y1, float z1, float x2, float y2, float z2,
                            float rotationAngle, String rotationAxis, float[] rotationOrigin,
                            Map<String, FaceUV> faceUVs) {
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.x2 = x2;
            this.y2 = y2;
            this.z2 = z2;
            this.rotationAngle = rotationAngle;
            this.rotationAxis = rotationAxis;
            this.rotationOrigin = rotationOrigin;
            this.faceUVs = faceUVs;
        }
    }

    public static class FaceUV {
        public static final FaceUV DEFAULT = new FaceUV(0.0F, 0.0F, 1.0F, 1.0F, 0);

        public final float u1;
        public final float v1;
        public final float u2;
        public final float v2;
        public final int rotation;

        public FaceUV(float u1, float v1, float u2, float v2, int rotation) {
            this.u1 = u1;
            this.v1 = v1;
            this.u2 = u2;
            this.v2 = v2;
            this.rotation = rotation;
        }
    }
}
