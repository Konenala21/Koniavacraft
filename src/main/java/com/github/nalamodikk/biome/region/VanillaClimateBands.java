package com.github.nalamodikk.biome.region;

import net.minecraft.world.level.biome.Climate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 原版 Climate 參數邊界值，對齊 OverworldBiomeBuilder 的實際 band 劃分。
 * 數值來源：原版 OverworldBiomeBuilder 原始碼 + TerraBlender ParameterUtils 研究。
 *
 * 用於 ParameterPointListBuilder 和 BiomeClimateConfigLoader 的 band 名稱解析。
 */
public final class VanillaClimateBands {

    private static final Logger LOGGER = LoggerFactory.getLogger(VanillaClimateBands.class);

    private VanillaClimateBands() {}

    // ==================== Temperature ====================

    public enum Temperature {
        ICY(Climate.Parameter.span(-1.0F, -0.45F)),
        COOL(Climate.Parameter.span(-0.45F, -0.15F)),
        NEUTRAL(Climate.Parameter.span(-0.15F, 0.2F)),
        WARM(Climate.Parameter.span(0.2F, 0.55F)),
        HOT(Climate.Parameter.span(0.55F, 1.0F));

        private final Climate.Parameter parameter;

        Temperature(Climate.Parameter parameter) {
            this.parameter = parameter;
        }

        public Climate.Parameter param() {
            return parameter;
        }

        /** 合併 min ~ max 兩個 band 為一個連續 span */
        public static Climate.Parameter span(Temperature min, Temperature max) {
            return Climate.Parameter.span(
                    Climate.unquantizeCoord(min.parameter.min()),
                    Climate.unquantizeCoord(max.parameter.max())
            );
        }
    }

    // ==================== Humidity ====================

    public enum Humidity {
        ARID(Climate.Parameter.span(-1.0F, -0.35F)),
        DRY(Climate.Parameter.span(-0.35F, -0.1F)),
        NEUTRAL(Climate.Parameter.span(-0.1F, 0.1F)),
        WET(Climate.Parameter.span(0.1F, 0.3F)),
        HUMID(Climate.Parameter.span(0.3F, 1.0F));

        private final Climate.Parameter parameter;

        Humidity(Climate.Parameter parameter) {
            this.parameter = parameter;
        }

        public Climate.Parameter param() {
            return parameter;
        }

        public static Climate.Parameter span(Humidity min, Humidity max) {
            return Climate.Parameter.span(
                    Climate.unquantizeCoord(min.parameter.min()),
                    Climate.unquantizeCoord(max.parameter.max())
            );
        }
    }

    // ==================== Continentalness ====================

    public enum Continentalness {
        MUSHROOM_FIELDS(Climate.Parameter.span(-1.2F, -1.05F)),
        DEEP_OCEAN(Climate.Parameter.span(-1.05F, -0.455F)),
        OCEAN(Climate.Parameter.span(-0.455F, -0.19F)),
        COAST(Climate.Parameter.span(-0.19F, -0.11F)),
        NEAR_INLAND(Climate.Parameter.span(-0.11F, 0.03F)),
        MID_INLAND(Climate.Parameter.span(0.03F, 0.3F)),
        FAR_INLAND(Climate.Parameter.span(0.3F, 1.0F));

        private final Climate.Parameter parameter;

        Continentalness(Climate.Parameter parameter) {
            this.parameter = parameter;
        }

        public Climate.Parameter param() {
            return parameter;
        }

        public static Climate.Parameter span(Continentalness min, Continentalness max) {
            return Climate.Parameter.span(
                    Climate.unquantizeCoord(min.parameter.min()),
                    Climate.unquantizeCoord(max.parameter.max())
            );
        }
    }

    // ==================== Erosion ====================

    public enum Erosion {
        EROSION_0(Climate.Parameter.span(-1.0F, -0.78F)),
        EROSION_1(Climate.Parameter.span(-0.78F, -0.375F)),
        EROSION_2(Climate.Parameter.span(-0.375F, -0.2225F)),
        EROSION_3(Climate.Parameter.span(-0.2225F, 0.05F)),
        EROSION_4(Climate.Parameter.span(0.05F, 0.45F)),
        EROSION_5(Climate.Parameter.span(0.45F, 0.55F)),
        EROSION_6(Climate.Parameter.span(0.55F, 1.0F));

        private final Climate.Parameter parameter;

        Erosion(Climate.Parameter parameter) {
            this.parameter = parameter;
        }

        public Climate.Parameter param() {
            return parameter;
        }

        public static Climate.Parameter span(Erosion min, Erosion max) {
            return Climate.Parameter.span(
                    Climate.unquantizeCoord(min.parameter.min()),
                    Climate.unquantizeCoord(max.parameter.max())
            );
        }
    }

    // ==================== Depth ====================

    public enum Depth {
        SURFACE(Climate.Parameter.point(0.0F)),
        UNDERGROUND(Climate.Parameter.span(0.2F, 0.9F)),
        FLOOR(Climate.Parameter.point(1.0F));

        private final Climate.Parameter parameter;

        Depth(Climate.Parameter parameter) {
            this.parameter = parameter;
        }

        public Climate.Parameter param() {
            return parameter;
        }
    }

    // ==================== Weirdness ====================
    // 13 個 slice，精確對齊 OverworldBiomeBuilder 常數

    public enum Weirdness {
        VALLEY(Climate.Parameter.span(-0.05F, 0.05F)),
        LOW_SLICE_NORMAL_DESCENDING(Climate.Parameter.span(-1.0F, -0.93333334F)),
        LOW_SLICE_WEIRD_DESCENDING(Climate.Parameter.span(-0.93333334F, -0.7666667F)),
        MID_SLICE_NORMAL_ASCENDING(Climate.Parameter.span(-0.7666667F, -0.56666666F)),
        MID_SLICE_WEIRD_ASCENDING(Climate.Parameter.span(-0.56666666F, -0.4F)),
        HIGH_SLICE_NORMAL_ASCENDING(Climate.Parameter.span(-0.4F, -0.26666668F)),
        HIGH_SLICE_WEIRD_ASCENDING(Climate.Parameter.span(-0.26666668F, -0.05F)),
        HIGH_SLICE_NORMAL_DESCENDING(Climate.Parameter.span(0.05F, 0.26666668F)),
        HIGH_SLICE_WEIRD_DESCENDING(Climate.Parameter.span(0.26666668F, 0.4F)),
        MID_SLICE_NORMAL_DESCENDING(Climate.Parameter.span(0.4F, 0.56666666F)),
        MID_SLICE_WEIRD_DESCENDING(Climate.Parameter.span(0.56666666F, 0.7666667F)),
        LOW_SLICE_NORMAL_ASCENDING(Climate.Parameter.span(0.7666667F, 0.93333334F)),
        LOW_SLICE_WEIRD_ASCENDING(Climate.Parameter.span(0.93333334F, 1.0F));

        private final Climate.Parameter parameter;

        Weirdness(Climate.Parameter parameter) {
            this.parameter = parameter;
        }

        public Climate.Parameter param() {
            return parameter;
        }

        public static Climate.Parameter span(Weirdness min, Weirdness max) {
            return Climate.Parameter.span(
                    Climate.unquantizeCoord(min.parameter.min()),
                    Climate.unquantizeCoord(max.parameter.max())
            );
        }
    }

    // ==================== JSON 解析工具方法 ====================

    /**
     * 按 JSON field 名稱和 band 名稱解析為 Climate.Parameter。
     *
     * @param field    "temperature", "humidity", "continentalness", "erosion", "depth", "weirdness"
     * @param bandName enum 常數名稱（如 "WARM", "EROSION_3", "SURFACE"）
     * @return 對應的 Climate.Parameter，解析失敗時回傳 null
     */
    public static Climate.Parameter parseParameter(String field, String bandName) {
        try {
            return switch (field) {
                case "temperature" -> Temperature.valueOf(bandName).param();
                case "humidity" -> Humidity.valueOf(bandName).param();
                case "continentalness" -> Continentalness.valueOf(bandName).param();
                case "erosion" -> Erosion.valueOf(bandName).param();
                case "depth" -> Depth.valueOf(bandName).param();
                case "weirdness" -> Weirdness.valueOf(bandName).param();
                default -> {
                    LOGGER.warn("Unknown climate field '{}' for band parsing", field);
                    yield null;
                }
            };
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown band name '{}' for field '{}'", bandName, field);
            return null;
        }
    }

    /**
     * 按 JSON field 名稱和兩個 band 名稱解析為跨越兩 band 的 Climate.Parameter span。
     *
     * @param field    "temperature", "humidity", 等
     * @param bandMin  最小 band 名稱（含）
     * @param bandMax  最大 band 名稱（含）
     * @return 對應的 Climate.Parameter span，解析失敗時回傳 null
     */
    public static Climate.Parameter parseSpan(String field, String bandMin, String bandMax) {
        Climate.Parameter min = parseParameter(field, bandMin);
        Climate.Parameter max = parseParameter(field, bandMax);
        if (min == null || max == null) {
            return null;
        }
        float fMin = Climate.unquantizeCoord(min.min());
        float fMax = Climate.unquantizeCoord(max.max());
        if (fMin > fMax) {
            LOGGER.warn("band_min '{}' is greater than band_max '{}' for field '{}', swapping", bandMin, bandMax, field);
            float tmp = fMin; fMin = fMax; fMax = tmp;
        }
        return Climate.Parameter.span(fMin, fMax);
    }
}
