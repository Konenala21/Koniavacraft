package com.github.nalamodikk.biome.region.noise;

import java.util.function.LongFunction;

/**
 * A layer transformer with one parent (internal node of the zoom chain).
 *
 * <p>Implementors provide {@link #apply} which reads from the parent {@link Area} and
 * uses the {@link AreaContext} for random decisions.
 */
public interface AreaTransformer1 {

    /**
     * Build an {@link AreaFactory} that wraps the given parent factory.
     *
     * @param seedModifier  unique long per layer, mixed with the world seed
     * @param ctxFactory    factory that creates an {@link AreaContext} for a given seed modifier
     * @param parentFactory upstream layer to sample from
     */
    default AreaFactory run(long seedModifier, LongFunction<AreaContext> ctxFactory, AreaFactory parentFactory) {
        return () -> {
            Area parent = parentFactory.make();
            AreaContext ctx = ctxFactory.apply(seedModifier);
            return ctx.createResult((context, x, z) -> this.apply(context, parent, x, z), parent);
        };
    }

    /** Compute the integer value at (x, z) by reading from the parent layer. */
    int apply(AreaContext ctx, Area parent, int x, int z);
}
