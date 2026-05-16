package com.github.nalamodikk.client.renderer.deployer;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.*;
import java.util.*;

/** Loads and caches Bedrock geometry + animation JSON files. */
public final class BedrockLoader {

    private BedrockLoader() {}

    // ── Geometry ──────────────────────────────────────────────────────────────

    public static BedrockGeoData loadModel(ResourceManager rm, ResourceLocation loc) {
        try (Reader r = reader(rm, loc)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            JsonObject geo  = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            JsonObject desc = geo.getAsJsonObject("description");

            float tw = desc.get("texture_width").getAsFloat();
            float th = desc.get("texture_height").getAsFloat();

            List<BedrockGeoData.Bone> bones = new ArrayList<>();
            for (JsonElement bel : geo.getAsJsonArray("bones")) {
                JsonObject bo = bel.getAsJsonObject();
                String name   = bo.get("name").getAsString();
                String parent = bo.has("parent") ? bo.get("parent").getAsString() : null;
                float[] pivot = arr3(bo, "pivot");

                List<BedrockGeoData.Cube> cubes = new ArrayList<>();
                if (bo.has("cubes")) {
                    for (JsonElement cel : bo.getAsJsonArray("cubes")) {
                        JsonObject co = cel.getAsJsonObject();
                        float[] origin   = arr3(co, "origin");
                        float[] size     = arr3(co, "size");
                        float[] cpivot   = co.has("pivot")    ? arr3(co, "pivot")    : origin.clone();
                        float[] crot     = co.has("rotation") ? arr3(co, "rotation") : new float[3];
                        JsonObject uvObj = co.has("uv") ? co.getAsJsonObject("uv") : null;
                        cubes.add(new BedrockGeoData.Cube(origin, size, cpivot, crot,
                            face(uvObj, "north"), face(uvObj, "south"),
                            face(uvObj, "east"),  face(uvObj, "west"),
                            face(uvObj, "up"),    face(uvObj, "down")));
                    }
                }
                bones.add(new BedrockGeoData.Bone(name, parent, pivot, cubes));
            }
            return new BedrockGeoData(tw, th, bones);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Bedrock model: " + loc, e);
        }
    }

    // ── Animation ─────────────────────────────────────────────────────────────

    public static Map<String, BedrockAnimData> loadAnimations(ResourceManager rm, ResourceLocation loc) {
        try (Reader r = reader(rm, loc)) {
            JsonObject root  = JsonParser.parseReader(r).getAsJsonObject();
            JsonObject anims = root.getAsJsonObject("animations");
            Map<String, BedrockAnimData> result = new LinkedHashMap<>();

            for (Map.Entry<String, JsonElement> ae : anims.entrySet()) {
                JsonObject ao = ae.getValue().getAsJsonObject();
                float   len  = ao.has("animation_length") ? ao.get("animation_length").getAsFloat() : 0f;
                boolean loop = ao.has("loop") && ao.get("loop").getAsBoolean();

                Map<String, BedrockAnimData.BoneAnim> boneAnims = new LinkedHashMap<>();
                if (ao.has("bones")) {
                    for (Map.Entry<String, JsonElement> be : ao.getAsJsonObject("bones").entrySet()) {
                        JsonObject ba = be.getValue().getAsJsonObject();
                        boneAnims.put(be.getKey(), new BedrockAnimData.BoneAnim(
                            channel(ba, "position"), constChannel(ba, "position"),
                            channel(ba, "rotation"), constChannel(ba, "rotation")));
                    }
                }
                result.put(ae.getKey(), new BedrockAnimData(ae.getKey(), len, loop, boneAnims));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load animations: " + loc, e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Reader reader(ResourceManager rm, ResourceLocation loc) throws IOException {
        return new InputStreamReader(rm.getResourceOrThrow(loc).open());
    }

    private static float[] arr3(JsonObject o, String key) {
        JsonArray a = o.getAsJsonArray(key);
        return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }

    private static BedrockGeoData.FaceUV face(JsonObject uvObj, String face) {
        if (uvObj == null || !uvObj.has(face)) return null;
        JsonObject f  = uvObj.getAsJsonObject(face);
        JsonArray  uv = f.getAsJsonArray("uv");
        JsonArray  sz = f.getAsJsonArray("uv_size");
        return new BedrockGeoData.FaceUV(
            uv.get(0).getAsFloat(), uv.get(1).getAsFloat(),
            sz.get(0).getAsFloat(), sz.get(1).getAsFloat());
    }

    /** Returns keyframe map if the channel is an object (keyframed). */
    private static TreeMap<Float, float[]> channel(JsonObject ba, String ch) {
        if (!ba.has(ch) || ba.get(ch).isJsonArray()) return null;
        TreeMap<Float, float[]> map = new TreeMap<>();
        for (Map.Entry<String, JsonElement> e : ba.getAsJsonObject(ch).entrySet()) {
            if (e.getValue().isJsonArray()) {
                JsonArray a = e.getValue().getAsJsonArray();
                map.put(Float.parseFloat(e.getKey()),
                    new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()});
            }
            // skip complex keyframe objects (interpolation, etc.) — treat as linear
        }
        return map.isEmpty() ? null : map;
    }

    /** Returns constant value if the channel is a plain array. */
    private static float[] constChannel(JsonObject ba, String ch) {
        if (!ba.has(ch) || !ba.get(ch).isJsonArray()) return null;
        JsonArray a = ba.getAsJsonArray(ch);
        return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }
}
