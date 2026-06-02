package com.github.nalamodikk.client.model.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parsed Bedrock animation file (format_version 1.8.0).
 * Supports rotation, position, scale channels with linear and catmullrom interpolation.
 */
public final class BedrockAnimData {

    public record Keyframe(float time, float[] value, float[] pre, boolean catmullrom) {}

    public record BoneAnim(List<Keyframe> rotation, List<Keyframe> position, List<Keyframe> scale) {}

    public record AnimEntry(float length, boolean loop, Map<String, BoneAnim> bones) {}

    public final Map<String, AnimEntry> animations;

    private BedrockAnimData(Map<String, AnimEntry> animations) {
        this.animations = Collections.unmodifiableMap(animations);
    }

    public static BedrockAnimData parse(ResourceLocation animId) {
        String path = "assets/" + animId.getNamespace() + "/animations/" + animId.getPath();
        InputStream stream = BedrockAnimData.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) throw new IllegalStateException("Animation file not found: " + path);
        try (stream) {
            return parseStream(stream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse animation: " + animId, e);
        }
    }

    private static BedrockAnimData parseStream(InputStream stream) {
        JsonObject root = JsonParser.parseReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();

        Map<String, AnimEntry> anims = new LinkedHashMap<>();
        JsonObject animsObj = root.getAsJsonObject("animations");

        for (Map.Entry<String, JsonElement> animEntry : animsObj.entrySet()) {
            JsonObject anim = animEntry.getValue().getAsJsonObject();
            float length = anim.has("animation_length") ? anim.get("animation_length").getAsFloat() : 0f;
            boolean loop = anim.has("loop") && anim.get("loop").getAsBoolean();

            Map<String, BoneAnim> boneMap = new LinkedHashMap<>();
            if (anim.has("bones")) {
                for (Map.Entry<String, JsonElement> boneEntry : anim.getAsJsonObject("bones").entrySet()) {
                    JsonObject bone = boneEntry.getValue().getAsJsonObject();
                    List<Keyframe> rot   = parseChannel(bone.get("rotation"));
                    List<Keyframe> pos   = parseChannel(bone.get("position"));
                    List<Keyframe> scale = parseChannel(bone.get("scale"));
                    boneMap.put(boneEntry.getKey(), new BoneAnim(rot, pos, scale));
                }
            }
            anims.put(animEntry.getKey(), new AnimEntry(length, loop, boneMap));
        }
        return new BedrockAnimData(anims);
    }

    // ── Channel parsing ────────────────────────────────────────────────────────

    private static List<Keyframe> parseChannel(JsonElement el) {
        if (el == null) return List.of();

        // Constant scalar or vector (no timestamps)
        if (el.isJsonPrimitive()) {
            float s = el.getAsFloat();
            return List.of(new Keyframe(0f, new float[]{s, s, s}, null, false));
        }
        if (el.isJsonArray()) {
            return List.of(new Keyframe(0f, vec3(el), null, false));
        }

        // Keyed: JsonObject whose keys are timestamps
        if (el.isJsonObject()) {
            List<Keyframe> frames = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
                try {
                    float t = Float.parseFloat(entry.getKey());
                    frames.add(parseKeyframe(t, entry.getValue()));
                } catch (NumberFormatException ignored) {}
            }
            frames.sort(Comparator.comparingDouble(Keyframe::time));
            return frames;
        }
        return List.of();
    }

    private static Keyframe parseKeyframe(float time, JsonElement val) {
        if (val.isJsonArray()) {
            return new Keyframe(time, vec3(val), null, false);
        }
        if (val.isJsonPrimitive()) {
            float s = val.getAsFloat();
            return new Keyframe(time, new float[]{s, s, s}, null, false);
        }
        if (val.isJsonObject()) {
            JsonObject o = val.getAsJsonObject();
            float[] post = o.has("post") ? vec3orScalar(o.get("post")) : new float[3];
            float[] pre  = o.has("pre")  ? vec3orScalar(o.get("pre"))  : null;
            boolean cr   = o.has("lerp_mode") && "catmullrom".equals(o.get("lerp_mode").getAsString());
            return new Keyframe(time, post, pre, cr);
        }
        return new Keyframe(time, new float[3], null, false);
    }

    // ── Interpolation ──────────────────────────────────────────────────────────

    /** Returns interpolated [x, y, z] at the given time. */
    public static float[] interpolate(List<Keyframe> frames, float t) {
        if (frames.isEmpty()) return new float[3];
        if (frames.size() == 1) return frames.get(0).value().clone();

        // Clamp to range
        if (t <= frames.get(0).time()) return frames.get(0).value().clone();
        if (t >= frames.get(frames.size() - 1).time()) return frames.get(frames.size() - 1).value().clone();

        // Find surrounding keyframes
        int i1 = frames.size() - 2;
        for (int i = 0; i < frames.size() - 1; i++) {
            if (t < frames.get(i + 1).time()) { i1 = i; break; }
        }
        int i2 = i1 + 1;

        Keyframe k1 = frames.get(i1);
        Keyframe k2 = frames.get(i2);
        float span = k2.time() - k1.time();
        float alpha = span == 0 ? 0 : (t - k1.time()) / span;

        if (k1.catmullrom()) {
            // Catmull-Rom: need P0..P3
            float[] p0 = i1 > 0 ? frames.get(i1 - 1).value() : k1.value();
            float[] p1 = k1.value();
            float[] p2 = k2.value();
            float[] p3 = i2 < frames.size() - 1 ? frames.get(i2 + 1).value() : k2.value();
            return catmullrom(p0, p1, p2, p3, alpha);
        }
        return lerp(k1.value(), k2.value(), alpha);
    }

    private static float[] lerp(float[] a, float[] b, float t) {
        return new float[]{
            a[0] + (b[0] - a[0]) * t,
            a[1] + (b[1] - a[1]) * t,
            a[2] + (b[2] - a[2]) * t
        };
    }

    private static float[] catmullrom(float[] p0, float[] p1, float[] p2, float[] p3, float t) {
        float t2 = t * t, t3 = t2 * t;
        float[] result = new float[3];
        for (int i = 0; i < 3; i++) {
            result[i] = 0.5f * (
                (2 * p1[i]) +
                (-p0[i] + p2[i]) * t +
                (2*p0[i] - 5*p1[i] + 4*p2[i] - p3[i]) * t2 +
                (-p0[i] + 3*p1[i] - 3*p2[i] + p3[i]) * t3
            );
        }
        return result;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static float[] vec3(JsonElement el) {
        if (el.isJsonArray()) {
            var a = el.getAsJsonArray();
            return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
        }
        float s = el.getAsFloat();
        return new float[]{s, s, s};
    }

    private static float[] vec3orScalar(JsonElement el) {
        if (el.isJsonPrimitive()) { float s = el.getAsFloat(); return new float[]{s, s, s}; }
        return vec3(el);
    }
}
