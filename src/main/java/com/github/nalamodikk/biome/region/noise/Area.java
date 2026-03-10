package com.github.nalamodikk.biome.region.noise;

import java.util.concurrent.locks.StampedLock;

/**
 * Thread-safe, position-indexed integer cache for a single layer in the zoom chain.
 *
 * <p>Uses an open-addressing hash table of fixed capacity (power-of-2).  On a cache
 * miss the slot is recomputed under the write lock; eviction is implicit (the slot is
 * simply overwritten).  Concurrent readers use optimistic read / read-lock fallback,
 * writers use the exclusive write lock.
 *
 * <p>The {@link AreaContext} associated with this Area is only accessed under the write
 * lock, so its mutable LCG state is safe despite not being thread-safe itself.
 */
public final class Area {

    private final int size;
    private final int mask;

    private final long[] keys;
    private final int[] values;
    private final boolean[] occupied;

    private final StampedLock lock = new StampedLock();

    private final AreaContext context;
    private final PixelTransformer transformer;

    Area(int cacheSize, AreaContext context, PixelTransformer transformer) {
        // Round up to next power of two
        int capacity = Integer.highestOneBit(Math.max(cacheSize - 1, 1)) << 1;
        if (capacity < 2) capacity = 2;
        this.size = capacity;
        this.mask = capacity - 1;
        this.keys = new long[capacity];
        this.values = new int[capacity];
        this.occupied = new boolean[capacity];
        this.context = context;
        this.transformer = transformer;
    }

    /**
     * Return the region index at the given (x, z) position.
     * Results are cached; misses are computed under exclusive lock.
     */
    public int get(int x, int z) {
        long key = packKey(x, z);
        int slot = slot(key);

        // Optimistic read — avoids lock acquisition on hot paths
        long stamp = lock.tryOptimisticRead();
        boolean hit = occupied[slot] && keys[slot] == key;
        int cachedVal = hit ? values[slot] : 0;
        if (lock.validate(stamp) && hit) return cachedVal;

        // Read lock — handles concurrent reads without misses
        stamp = lock.readLock();
        try {
            if (occupied[slot] && keys[slot] == key) return values[slot];
        } finally {
            lock.unlockRead(stamp);
        }

        // Write lock — compute and cache
        stamp = lock.writeLock();
        try {
            if (occupied[slot] && keys[slot] == key) return values[slot];
            context.initRandom(x, z);
            int result = transformer.apply(context, x, z);
            keys[slot] = key;
            values[slot] = result;
            occupied[slot] = true;
            return result;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    private static long packKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private int slot(long key) {
        return (int) ((key ^ (key >>> 32)) & mask);
    }
}
