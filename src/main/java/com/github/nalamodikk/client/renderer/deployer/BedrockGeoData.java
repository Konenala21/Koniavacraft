package com.github.nalamodikk.client.renderer.deployer;

import java.util.*;

/** Parsed Bedrock 1.12.0 geometry — immutable after construction. */
public final class BedrockGeoData {

    public final float texW, texH;
    /** All bones in original file order; children field populated after construction. */
    public final List<Bone> bones;
    /** Only root bones (no parent). Roots are iterated first for rendering. */
    public final List<Bone> roots;

    public BedrockGeoData(float texW, float texH, List<Bone> bones) {
        this.texW = texW;
        this.texH = texH;
        this.bones = bones;

        Map<String, Bone> map = new LinkedHashMap<>();
        for (Bone b : bones) map.put(b.name, b);

        List<Bone> rootList = new ArrayList<>();
        for (Bone b : bones) {
            if (b.parent != null && map.containsKey(b.parent)) {
                map.get(b.parent).children.add(b);
            } else {
                rootList.add(b);
            }
        }
        this.roots = Collections.unmodifiableList(rootList);
    }

    // ── Bone ─────────────────────────────────────────────────────────────────

    public static final class Bone {
        public final String name;
        public final String parent;      // null = root
        public final float[] pivot;      // pixel units [x,y,z]
        public final List<Cube> cubes;
        public final List<Bone> children = new ArrayList<>();

        public Bone(String name, String parent, float[] pivot, List<Cube> cubes) {
            this.name   = name;
            this.parent = parent;
            this.pivot  = pivot;
            this.cubes  = cubes;
        }
    }

    // ── Cube ─────────────────────────────────────────────────────────────────

    public static final class Cube {
        public final float[] origin;    // pixel [x,y,z] min corner
        public final float[] size;      // pixel [w,h,d]
        public final float[] pivot;     // rotation pivot, pixel [x,y,z]
        public final float[] rotation;  // Euler XYZ degrees; [0,0,0] if absent
        public final FaceUV north, south, east, west, up, down;

        public Cube(float[] origin, float[] size, float[] pivot, float[] rotation,
                    FaceUV north, FaceUV south, FaceUV east, FaceUV west,
                    FaceUV up, FaceUV down) {
            this.origin   = origin;
            this.size     = size;
            this.pivot    = pivot;
            this.rotation = rotation;
            this.north    = north;
            this.south    = south;
            this.east     = east;
            this.west     = west;
            this.up       = up;
            this.down     = down;
        }
    }

    // ── FaceUV ────────────────────────────────────────────────────────────────

    public static final class FaceUV {
        /** Pixel coordinates (may be negative for flipped UV). */
        public final float u, v, du, dv;

        public FaceUV(float u, float v, float du, float dv) {
            this.u = u; this.v = v; this.du = du; this.dv = dv;
        }

        public float u0(float texW) { return u / texW; }
        public float v0(float texH) { return v / texH; }
        public float u1(float texW) { return (u + du) / texW; }
        public float v1(float texH) { return (v + dv) / texH; }
    }
}
