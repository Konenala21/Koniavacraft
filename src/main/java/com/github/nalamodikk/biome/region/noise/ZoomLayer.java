package com.github.nalamodikk.biome.region.noise;

/**
 * 2× zoom layer — NORMAL (mode-or-random) or FUZZY (always random).
 *
 * <p>Each child coordinate {@code (x, z)} samples the 2×2 grid of parent values
 * at {@code (x>>1, z>>1)}, {@code (x>>1+1, z>>1)}, {@code (x>>1, z>>1+1)},
 * {@code (x>>1+1, z>>1+1)} and blends them.
 *
 * <ul>
 *   <li><b>FUZZY</b>: picks uniformly at random — used for the first (coarsest) zoom
 *       to make patch boundaries more organic.</li>
 *   <li><b>NORMAL</b>: returns the majority (3-of-4) value when one exists, otherwise
 *       picks at random — used for subsequent zooms to preserve patch interior.</li>
 * </ul>
 */
public enum ZoomLayer implements AreaTransformer1 {
    FUZZY,
    NORMAL;

    private static int parentX(int x) { return x >> 1; }
    private static int parentZ(int z) { return z >> 1; }

    @Override
    public int apply(AreaContext ctx, Area parent, int x, int z) {
        // Sample 2×2 parent cells
        int px = parentX(x);
        int pz = parentZ(z);
        int v00 = parent.get(px,     pz);
        int v10 = parent.get(px + 1, pz);
        int v01 = parent.get(px,     pz + 1);
        int v11 = parent.get(px + 1, pz + 1);

        // ctx.initRandom(x, z) is called by Area.get() before this apply() invocation
        return this == FUZZY ? ctx.random(v00, v10, v01, v11)
                             : modeOrRandom(ctx, v00, v10, v01, v11);
    }

    /**
     * Return the value that appears 3 or 4 times in the 2×2 grid.
     * If no majority exists (all different, or two 2-way ties) pick randomly.
     */
    private static int modeOrRandom(AreaContext ctx, int a, int b, int c, int d) {
        // Check each possible 3-of-4 majority
        if (a == b && a == c) return a;
        if (a == b && a == d) return a;
        if (a == c && a == d) return a;
        if (b == c && b == d) return b;
        // No majority — pick uniformly
        return ctx.random(a, b, c, d);
    }
}
