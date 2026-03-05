package com.github.nalamodikk.biome.region;

import net.minecraft.world.level.biome.Climate;

/**
 * Immutable climate definition for biome injection entries.
 */
public record BiomeClimateDefinition(
        Climate.Parameter temperature,
        Climate.Parameter humidity,
        Climate.Parameter continentalness,
        Climate.Parameter erosion,
        Climate.Parameter depth,
        Climate.Parameter weirdness,
        long offset
) {
    public static Builder builder() {
        return new Builder();
    }

    public Climate.ParameterPoint toPoint(long offsetJitter) {
        return new Climate.ParameterPoint(
                temperature,
                humidity,
                continentalness,
                erosion,
                depth,
                weirdness,
                offset + offsetJitter
        );
    }

    public static class Builder {
        private Climate.Parameter temperature = Climate.Parameter.span(-2.0F, 2.0F);
        private Climate.Parameter humidity = Climate.Parameter.span(-2.0F, 2.0F);
        private Climate.Parameter continentalness = Climate.Parameter.span(-2.0F, 2.0F);
        private Climate.Parameter erosion = Climate.Parameter.span(-2.0F, 2.0F);
        private Climate.Parameter depth = Climate.Parameter.span(-2.0F, 2.0F);
        private Climate.Parameter weirdness = Climate.Parameter.span(-2.0F, 2.0F);
        private long offset = 0L;

        public Builder temperature(float min, float max) {
            this.temperature = Climate.Parameter.span(Math.min(min, max), Math.max(min, max));
            return this;
        }

        public Builder humidity(float min, float max) {
            this.humidity = Climate.Parameter.span(Math.min(min, max), Math.max(min, max));
            return this;
        }

        public Builder continentalness(float min, float max) {
            this.continentalness = Climate.Parameter.span(Math.min(min, max), Math.max(min, max));
            return this;
        }

        public Builder erosion(float min, float max) {
            this.erosion = Climate.Parameter.span(Math.min(min, max), Math.max(min, max));
            return this;
        }

        public Builder depth(float min, float max) {
            this.depth = Climate.Parameter.span(Math.min(min, max), Math.max(min, max));
            return this;
        }

        public Builder weirdness(float min, float max) {
            this.weirdness = Climate.Parameter.span(Math.min(min, max), Math.max(min, max));
            return this;
        }

        public Builder offset(float offset) {
            this.offset = (long) (offset * 1000L);
            return this;
        }

        public Builder offset(long offset) {
            this.offset = offset;
            return this;
        }

        public BiomeClimateDefinition build() {
            return new BiomeClimateDefinition(
                    temperature,
                    humidity,
                    continentalness,
                    erosion,
                    depth,
                    weirdness,
                    offset
            );
        }
    }
}
