package com.github.nalamodikk.client.model.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Converts a Bedrock .geo.json (format_version 1.12.0) to a MC LayerDefinition.
 * Supports: bone hierarchy, per-cube rotation (as child ModelParts), mirror, inflate.
 * Does NOT support: per-face UV objects, non-box geometry.
 */
public final class BedrockGeoParser {

    private BedrockGeoParser() {}

    public static LayerDefinition parse(ResourceLocation geoId) {
        String path = "assets/" + geoId.getNamespace() + "/geo/" + geoId.getPath();
        InputStream stream = BedrockGeoParser.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) throw new IllegalStateException("Bedrock geo not found: " + path);
        try (stream) {
            return parseStream(stream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse bedrock geo: " + geoId, e);
        }
    }

    private static LayerDefinition parseStream(InputStream stream) {
        JsonObject root = JsonParser.parseReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject geo = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject desc = geo.getAsJsonObject("description");

        int texW = desc.get("texture_width").getAsInt();
        int texH = desc.get("texture_height").getAsInt();

        Map<String, JsonObject> boneMap = new LinkedHashMap<>();
        for (JsonElement el : geo.getAsJsonArray("bones")) {
            JsonObject bone = el.getAsJsonObject();
            boneMap.put(bone.get("name").getAsString(), bone);
        }

        MeshDefinition mesh = new MeshDefinition();
        addBones(mesh.getRoot(), boneMap, new HashMap<>(), null);
        return LayerDefinition.create(mesh, texW, texH);
    }

    private static void addBones(PartDefinition root, Map<String, JsonObject> boneMap,
                                  Map<String, PartDefinition> parts, String parentName) {
        for (Map.Entry<String, JsonObject> entry : boneMap.entrySet()) {
            String name = entry.getKey();
            JsonObject bone = entry.getValue();
            String boneParent = bone.has("parent") ? bone.get("parent").getAsString() : null;
            if (!Objects.equals(boneParent, parentName)) continue;

            float[] pivot = vec3(bone, "pivot");
            float[] rot   = vec3(bone, "rotation");

            PartPose pose;
            if (boneParent == null) {
                // Root bone: convert Bedrock ground-origin Y to MC top-down Y (24-unit offset)
                pose = PartPose.offsetAndRotation(pivot[0], 24f - pivot[1], pivot[2],
                        rad(rot[0]), rad(rot[1]), rad(rot[2]));
            } else {
                float[] pp = vec3(boneMap.get(boneParent), "pivot");
                pose = PartPose.offsetAndRotation(
                        pivot[0] - pp[0], -(pivot[1] - pp[1]), pivot[2] - pp[2],
                        rad(rot[0]), rad(rot[1]), rad(rot[2]));
            }

            // Normal cubes (no per-cube rotation)
            CubeListBuilder builder = CubeListBuilder.create();
            if (bone.has("cubes")) {
                for (JsonElement el : bone.getAsJsonArray("cubes")) {
                    JsonObject cube = el.getAsJsonObject();
                    if (!cube.has("rotation")) {
                        appendCube(builder, cube, pivot);
                    }
                }
            }

            PartDefinition partParent = boneParent == null ? root : parts.get(boneParent);
            PartDefinition part = partParent.addOrReplaceChild(name, builder, pose);
            parts.put(name, part);

            // Cubes with per-cube rotation become child ModelParts
            if (bone.has("cubes")) {
                int ri = 0;
                for (JsonElement el : bone.getAsJsonArray("cubes")) {
                    JsonObject cube = el.getAsJsonObject();
                    if (!cube.has("rotation")) continue;

                    float[] cp  = cube.has("pivot") ? vec3(cube, "pivot") : pivot;
                    float[] cr  = vec3(cube, "rotation");
                    float[] ori = vec3(cube, "origin");
                    float[] sz  = vec3(cube, "size");
                    int[]   uv  = uvOf(cube);
                    boolean mir = cube.has("mirror") && cube.get("mirror").getAsBoolean();
                    float inf    = cube.has("inflate") ? cube.get("inflate").getAsFloat() : 0f;

                    PartPose subPose = PartPose.offsetAndRotation(
                            cp[0] - pivot[0], -(cp[1] - pivot[1]), cp[2] - pivot[2],
                            rad(cr[0]), rad(cr[1]), rad(cr[2]));

                    CubeListBuilder sub = CubeListBuilder.create().texOffs(uv[0], uv[1]);
                    if (mir) sub.mirror();
                    sub.addBox(ori[0] - cp[0], -(ori[1] - cp[1]) - sz[1], ori[2] - cp[2],
                            sz[0], sz[1], sz[2], new CubeDeformation(inf));
                    if (mir) sub.mirror();

                    part.addOrReplaceChild(name + "_r" + ri++, sub, subPose);
                }
            }

            // Recurse to add child bones
            addBones(root, boneMap, parts, name);
        }
    }

    private static void appendCube(CubeListBuilder b, JsonObject cube, float[] bp) {
        float[] ori = vec3(cube, "origin");
        float[] sz  = vec3(cube, "size");
        int[]   uv  = uvOf(cube);
        boolean mir = cube.has("mirror") && cube.get("mirror").getAsBoolean();
        float inf    = cube.has("inflate") ? cube.get("inflate").getAsFloat() : 0f;

        b.texOffs(uv[0], uv[1]);
        if (mir) b.mirror();
        b.addBox(ori[0] - bp[0], -(ori[1] - bp[1]) - sz[1], ori[2] - bp[2],
                sz[0], sz[1], sz[2], new CubeDeformation(inf));
        if (mir) b.mirror(); // reset mirror state for next cube
    }

    private static float[] vec3(JsonObject obj, String key) {
        if (!obj.has(key)) return new float[3];
        JsonArray a = obj.getAsJsonArray(key);
        return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }

    private static int[] uvOf(JsonObject cube) {
        if (!cube.has("uv")) return new int[2];
        JsonElement el = cube.get("uv");
        if (el.isJsonArray()) {
            JsonArray a = el.getAsJsonArray();
            return new int[]{a.get(0).getAsInt(), a.get(1).getAsInt()};
        }
        return new int[2];
    }

    private static float rad(float deg) {
        return (float) Math.toRadians(deg);
    }
}
