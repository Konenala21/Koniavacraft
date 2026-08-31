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
    private static final Vector3f DEFAULT_GROUP_ORIGIN = new Vector3f(0.5F, 0.5F, 0.5F);
    private static final UvTransform IDENTITY_UV_TRANSFORM = new UvTransform(1.0F, 1.0F, 0.0F, 0.0F);
    private static final ThreadLocal<Vector3f[]> TEMP_POSITIONS = ThreadLocal.withInitial(() ->
            new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()});
    private static final ThreadLocal<UvTransform> UV_TRANSFORM = ThreadLocal.withInitial(() -> IDENTITY_UV_TRANSFORM);

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

    public static void renderElementList(PoseStack poseStack, VertexConsumer vertexConsumer,
                                         int packedLight, int packedOverlay,
                                         List<ModelElement> elements) {
        for (ModelElement element : elements) {
            renderElement(poseStack, vertexConsumer, packedLight, packedOverlay, element);
        }
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
            Vector3f groupOrigin = DEFAULT_GROUP_ORIGIN;
            if (groupOriginResolver != null) {
                Vector3f resolved = groupOriginResolver.apply(groupName);
                if (resolved != null) {
                    groupOrigin = resolved;
                }
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

    public static UvTransform setUvTransform(float scaleU, float scaleV, float offsetU, float offsetV) {
        UvTransform previous = UV_TRANSFORM.get();
        UV_TRANSFORM.set(new UvTransform(scaleU, scaleV, offsetU, offsetV));
        return previous;
    }

    public static void restoreUvTransform(UvTransform previousTransform) {
        UV_TRANSFORM.set(previousTransform == null ? IDENTITY_UV_TRANSFORM : previousTransform);
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

        Map<String, FaceUV> faceUVs = new HashMap<>(6);
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

    /**
     * 頂點走位跟 UV 分配都照 vanilla {@code EnumFaceDirection} / {@code BlockFaceUV}
     * (net.minecraft.client.renderer.block.model，MCP 反編譯核對過)的實際規則，
     * 不是憑感覺排的，6 面走位+UV公式都跟 vanilla 逐點對過。
     */
    private static void renderCube(PoseStack poseStack, VertexConsumer vertexConsumer,
                                   int packedLight, int packedOverlay, ModelElement element) {
        Matrix4f matrix = poseStack.last().pose();
        org.joml.Matrix3f nm = poseStack.last().normal();

        float x1 = element.x1, y1 = element.y1, z1 = element.z1;
        float x2 = element.x2, y2 = element.y2, z2 = element.z2;

        FaceUV up = element.faceUVs.get("up");
        if (up != null) {
            Vector3f n = nm.transform(new Vector3f(0f, 1f, 0f));
            addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                    x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, n.x(), n.y(), n.z(), up);
        }

        FaceUV down = element.faceUVs.get("down");
        if (down != null) {
            Vector3f n = nm.transform(new Vector3f(0f, -1f, 0f));
            addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                    x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2, n.x(), n.y(), n.z(), down);
        }

        FaceUV north = element.faceUVs.get("north");
        if (north != null) {
            Vector3f n = nm.transform(new Vector3f(0f, 0f, -1f));
            addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                    x2, y2, z1, x2, y1, z1, x1, y1, z1, x1, y2, z1, n.x(), n.y(), n.z(), north);
        }

        FaceUV south = element.faceUVs.get("south");
        if (south != null) {
            Vector3f n = nm.transform(new Vector3f(0f, 0f, 1f));
            addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                    x1, y2, z2, x1, y1, z2, x2, y1, z2, x2, y2, z2, n.x(), n.y(), n.z(), south);
        }

        FaceUV west = element.faceUVs.get("west");
        if (west != null) {
            Vector3f n = nm.transform(new Vector3f(-1f, 0f, 0f));
            addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                    x1, y2, z1, x1, y1, z1, x1, y1, z2, x1, y2, z2, n.x(), n.y(), n.z(), west);
        }

        FaceUV east = element.faceUVs.get("east");
        if (east != null) {
            Vector3f n = nm.transform(new Vector3f(1f, 0f, 0f));
            addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                    x2, y2, z2, x2, y1, z2, x2, y1, z1, x2, y2, z1, n.x(), n.y(), n.z(), east);
        }
    }

    private static void addQuadWithUV(VertexConsumer vertexConsumer, Matrix4f matrix, int packedLight, int packedOverlay,
                                      float x1, float y1, float z1, float x2, float y2, float z2,
                                      float x3, float y3, float z3, float x4, float y4, float z4,
                                      float nx, float ny, float nz, FaceUV faceUV) {
        Vector3f[] tempPositions = TEMP_POSITIONS.get();
        Vector3f pos1 = matrix.transformPosition(x1, y1, z1, tempPositions[0]);
        Vector3f pos2 = matrix.transformPosition(x2, y2, z2, tempPositions[1]);
        Vector3f pos3 = matrix.transformPosition(x3, y3, z3, tempPositions[2]);
        Vector3f pos4 = matrix.transformPosition(x4, y4, z4, tempPositions[3]);

        int rotationSteps = ((faceUV.rotation / 90) % 4 + 4) % 4;
        int color = -1;
        addVertexWithRotatedUv(vertexConsumer, pos1, color, packedOverlay, packedLight, nx, ny, nz, faceUV, 0, rotationSteps);
        addVertexWithRotatedUv(vertexConsumer, pos2, color, packedOverlay, packedLight, nx, ny, nz, faceUV, 1, rotationSteps);
        addVertexWithRotatedUv(vertexConsumer, pos3, color, packedOverlay, packedLight, nx, ny, nz, faceUV, 2, rotationSteps);
        addVertexWithRotatedUv(vertexConsumer, pos4, color, packedOverlay, packedLight, nx, ny, nz, faceUV, 3, rotationSteps);
    }

    private static void addVertexWithRotatedUv(VertexConsumer vertexConsumer, Vector3f position,
                                               int color, int packedOverlay, int packedLight,
                                               float nx, float ny, float nz, FaceUV faceUV,
                                               int targetIndex, int rotationSteps) {
        int sourceIndex = (targetIndex - rotationSteps + 4) & 3;
        // vanilla BlockFaceUV: index 0->(u1,v1), 1->(u1,v2), 2->(u2,v2), 3->(u2,v1)
        float u = (sourceIndex == 0 || sourceIndex == 1) ? faceUV.u1 : faceUV.u2;
        float v = (sourceIndex == 1 || sourceIndex == 2) ? faceUV.v2 : faceUV.v1;
        UvTransform transform = UV_TRANSFORM.get();
        u = u * transform.scaleU + transform.offsetU;
        v = v * transform.scaleV + transform.offsetV;
        vertexConsumer.addVertex(position.x(), position.y(), position.z(), color, u, v, packedOverlay, packedLight, nx, ny, nz);
    }

    public static final class UvTransform {
        public final float scaleU;
        public final float scaleV;
        public final float offsetU;
        public final float offsetV;

        public UvTransform(float scaleU, float scaleV, float offsetU, float offsetV) {
            this.scaleU = scaleU;
            this.scaleV = scaleV;
            this.offsetU = offsetU;
            this.offsetV = offsetV;
        }
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
