package com.github.nalamodikk.biome.region.noise;

/**
 * Holds per-layer LCG random state and creates {@link Area} instances.
 *
 * <p>Each layer in the zoom chain gets its own {@code AreaContext} built from the
 * world seed mixed with a per-layer seed modifier, matching the MC 1.12 style
 * Layer/ZoomLayer approach used by TerraBlender.
 *
 * <p>NOT thread-safe on its own; thread safety is provided by the {@link Area}'s
 * {@link java.util.concurrent.locks.StampedLock} which serialises all calls to
 * {@link #initRandom} / {@link #nextRandom} within a single write-lock scope.
 */
public class AreaContext {

    private final long worldSeed;
    private long seedModifier;
    private long currentSeed;
    private final int cacheSize;

    public AreaContext(int cacheSize, long worldSeed, long seedModifier) {
        this.cacheSize = cacheSize;
        this.worldSeed = worldSeed;
        mixSeed(worldSeed, seedModifier);
    }

    // ---------- seed setup ----------

    private static long lcgStep(long seed, long modifier) {
        return seed * (seed * 6364136223846793005L + 1442695040888963407L) + modifier;
    }

    /** Mix world seed with a layer-specific modifier and store result as the base seed. */
    public void mixSeed(long worldSeed, long seedModifier) {
        this.seedModifier = seedModifier;
        long s = worldSeed;
        s = lcgStep(s, seedModifier);
        s = lcgStep(s, seedModifier);
        s = lcgStep(s, seedModifier);
        this.currentSeed = s;
    }

    /**
     * Initialise the random state for a specific (x, z) position.
     * Must be called once before using {@link #nextRandom}.
     */
    public void initRandom(int x, int z) {
        long s = worldSeed;
        s = lcgStep(s, x);
        s = lcgStep(s, z);
        s = lcgStep(s, x);
        s = lcgStep(s, z);
        s ^= seedModifier;
        s = lcgStep(s, 0L);
        this.currentSeed = s;
    }

    // ---------- random operations ----------

    /** Return a value in [0, bound) and advance the LCG state. */
    public int nextRandom(int bound) {
        int r = (int) ((currentSeed >> 24) % bound);
        if (r < 0) r += bound;
        currentSeed = lcgStep(currentSeed, worldSeed);
        return r;
    }

    /** Pick one of two values with equal probability. */
    public int random(int a, int b) {
        return nextRandom(2) == 0 ? a : b;
    }

    /** Pick one of four values with equal probability. */
    public int random(int a, int b, int c, int d) {
        int r = nextRandom(4);
        if (r == 0) return a;
        if (r == 1) return b;
        if (r == 2) return c;
        return d;
    }

    // ---------- Area factory ----------

    /**
     * Create a leaf {@link Area} (no parent layer) backed by this context.
     *
     * @param transformer pixel computation; receives this context and (x, z)
     * @return a thread-safe, lazily-cached Area
     */
    public Area createResult(PixelTransformer transformer) {
        return new Area(cacheSize, this, transformer);
    }

    /**
     * Create a derived {@link Area} whose transformer may query a parent layer.
     * The parent is already captured in the transformer closure; the explicit
     * parameter is kept for API symmetry with the plan specification.
     *
     * @param transformer pixel computation; receives this context, parent Area, and (x, z)
     * @param parent      the parent layer (captured in transformer via lambda)
     * @return a thread-safe, lazily-cached Area
     */
    public Area createResult(PixelTransformer transformer, Area parent) {
        return new Area(cacheSize, this, transformer);
    }

    int getCacheSize() {
        return cacheSize;
    }
}
