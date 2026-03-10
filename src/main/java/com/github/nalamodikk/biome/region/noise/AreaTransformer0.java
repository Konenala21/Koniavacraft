package com.github.nalamodikk.biome.region.noise;

import java.util.function.LongFunction;

/**
 * A layer transformer with no parent (leaf of the zoom chain).
 *
 * <p>Implementors provide {@link #apply} which derives an integer value purely from
 * position and the random state in the supplied {@link AreaContext}.
 */
public interface AreaTransformer0 {

    /**
     * Build an {@link AreaFactory} for this transformer.
     *
     * @param seedModifier unique long per layer, mixed with the world seed
     * @param ctxFactory   factory that creates an {@link AreaContext} for a given seed modifier
     */
    default AreaFactory run(long seedModifier, LongFunction<AreaContext> ctxFactory) {
        return () -> {
            AreaContext ctx = ctxFactory.apply(seedModifier);
            return ctx.createResult(this::apply);
        };
    }

    /** Compute the integer value at (x, z) using the provided random context. */
    int apply(AreaContext ctx, int x, int z);
}
