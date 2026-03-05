package com.github.nalamodikk.biome.region.noise;

@FunctionalInterface
public interface PixelTransformer {
    int apply(AreaContext context, int x, int z);
}
