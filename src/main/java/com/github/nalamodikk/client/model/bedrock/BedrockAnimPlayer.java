package com.github.nalamodikk.client.model.bedrock;

import net.minecraft.client.model.geom.ModelPart;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies Bedrock animation data to a live ModelPart tree.
 * Call apply() each frame from setupAnim().
 */
public final class BedrockAnimPlayer {

    private final Map<String, ModelPart> boneCache = new HashMap<>();

    public BedrockAnimPlayer(ModelPart root) {
        collectBones(root, boneCache);
    }

    /**
     * Applies a single named animation at the given time in seconds.
     * Resets all bones in the cache to their rest pose before applying.
     */
    public void apply(String animName, float timeSec, BedrockAnimData data) {
        BedrockAnimData.AnimEntry anim = data.animations.get(animName);
        if (anim == null) return;

        float t = anim.loop() && anim.length() > 0
                ? timeSec % anim.length()
                : Math.min(timeSec, anim.length());

        // Reset all bones to rest pose first so previous frames don't bleed
        // resetPose() restores position/rotation but NOT visible, so reset that manually
        for (ModelPart part : boneCache.values()) {
            part.resetPose();
            part.visible = true;
        }

        for (Map.Entry<String, BedrockAnimData.BoneAnim> entry : anim.bones().entrySet()) {
            ModelPart part = boneCache.get(entry.getKey());
            if (part == null) continue;
            BedrockAnimData.BoneAnim bone = entry.getValue();

            applyRotation(part, bone.rotation(), t);
            applyPosition(part, bone.position(), t);
            applyScale(part, bone.scale(), t);
        }
    }

    // ── Channel application ────────────────────────────────────────────────────

    private static void applyRotation(ModelPart part, List<BedrockAnimData.Keyframe> frames, float t) {
        if (frames.isEmpty()) return;
        float[] v = BedrockAnimData.interpolate(frames, t);
        // Bedrock rotation is in degrees, additive on top of rest pose
        part.xRot += rad(v[0]);
        part.yRot += rad(v[1]);
        part.zRot += rad(v[2]);
    }

    private static void applyPosition(ModelPart part, List<BedrockAnimData.Keyframe> frames, float t) {
        if (frames.isEmpty()) return;
        float[] v = BedrockAnimData.interpolate(frames, t);
        // Bedrock Y increases up; MC ModelPart Y increases down → invert Y
        part.x += v[0];
        part.y -= v[1];
        part.z += v[2];
    }

    private static void applyScale(ModelPart part, List<BedrockAnimData.Keyframe> frames, float t) {
        if (frames.isEmpty()) return;
        float[] v = BedrockAnimData.interpolate(frames, t);
        float maxS = Math.max(Math.max(Math.abs(v[0]), Math.abs(v[1])), Math.abs(v[2]));
        part.visible = maxS > 0.01f;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static final Field CHILDREN_FIELD;
    static {
        try {
            CHILDREN_FIELD = ModelPart.class.getDeclaredField("children");
            CHILDREN_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ModelPart> getChildren(ModelPart part) {
        try {
            return (Map<String, ModelPart>) CHILDREN_FIELD.get(part);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void collectBones(ModelPart part, Map<String, ModelPart> map) {
        for (Map.Entry<String, ModelPart> e : getChildren(part).entrySet()) {
            map.put(e.getKey(), e.getValue());
            collectBones(e.getValue(), map);
        }
    }

    private static float rad(float deg) {
        return (float) Math.toRadians(deg);
    }
}
