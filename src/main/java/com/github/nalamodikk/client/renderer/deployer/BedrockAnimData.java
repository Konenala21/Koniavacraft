package com.github.nalamodikk.client.renderer.deployer;

import java.util.*;

/** Parsed Bedrock 1.8.0 animation — immutable after construction. */
public final class BedrockAnimData {

    public final String name;
    public final float  length; // seconds
    public final boolean loop;
    /** boneName → per-bone channel data */
    public final Map<String, BoneAnim> bones;

    public BedrockAnimData(String name, float length, boolean loop,
                           Map<String, BoneAnim> bones) {
        this.name   = name;
        this.length = length;
        this.loop   = loop;
        this.bones  = bones;
    }

    // ── BoneAnim ──────────────────────────────────────────────────────────────

    public static final class BoneAnim {
        // Exactly one of (frames / constant) is non-null per channel; both null = no data.
        final TreeMap<Float, float[]> posFrames;
        final float[]                 posConst;
        final TreeMap<Float, float[]> rotFrames;
        final float[]                 rotConst;

        static final float[] ZERO3 = {0f, 0f, 0f};

        BoneAnim(TreeMap<Float, float[]> posFrames, float[] posConst,
                 TreeMap<Float, float[]> rotFrames, float[] rotConst) {
            this.posFrames = posFrames;
            this.posConst  = posConst;
            this.rotFrames = rotFrames;
            this.rotConst  = rotConst;
        }

        public float[] position(float time) { return eval(posFrames, posConst, time); }
        public float[] rotation(float time) { return eval(rotFrames, rotConst, time); }

        private static float[] eval(TreeMap<Float, float[]> frames, float[] constant, float t) {
            if (constant != null)                       return constant;
            if (frames == null || frames.isEmpty())     return ZERO3;
            Map.Entry<Float, float[]> lo = frames.floorEntry(t);
            Map.Entry<Float, float[]> hi = frames.ceilingEntry(t);
            if (lo == null) return hi.getValue();
            if (hi == null) return lo.getValue();
            if (lo.getKey().equals(hi.getKey())) return lo.getValue();
            float fac = (t - lo.getKey()) / (hi.getKey() - lo.getKey());
            float[] a = lo.getValue(), b = hi.getValue();
            return new float[]{
                a[0] + fac * (b[0] - a[0]),
                a[1] + fac * (b[1] - a[1]),
                a[2] + fac * (b[2] - a[2])
            };
        }
    }
}
