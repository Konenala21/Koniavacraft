package com.github.nalamodikk.biome.region;

import net.minecraft.world.level.biome.Climate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 以笛卡爾積方式建構 Climate.ParameterPoint 列表。
 * 移植自 TerraBlender ParameterPointListBuilder 設計，不引入函式庫依賴。
 *
 * <p>使用方式：
 * <pre>
 * List&lt;Climate.ParameterPoint&gt; points = ParameterPointListBuilder.create()
 *     .temperature(VanillaClimateBands.Temperature.NEUTRAL)
 *     .temperature(VanillaClimateBands.Temperature.WARM)        // 兩個 alternative
 *     .humidity(VanillaClimateBands.Humidity.DRY, VanillaClimateBands.Humidity.WET) // span
 *     .continentalness(VanillaClimateBands.Continentalness.NEAR_INLAND)
 *     .erosion(VanillaClimateBands.Erosion.EROSION_3)
 *     .depth(VanillaClimateBands.Depth.SURFACE)
 *     .weirdness(VanillaClimateBands.Weirdness.VALLEY)
 *     .build();
 * // 2(temp) × 1(hum) × 1(cont) × 1(erosion) × 1(depth) × 1(weird) = 2 個 ParameterPoint
 * </pre>
 *
 * <p>每個維度未設定時預設使用全範圍 span(-2, 2)，與 BiomeClimateDefinition.Builder 一致。
 * 超過 64 個組合時輸出 WARN 日誌提示。
 */
public final class ParameterPointListBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterPointListBuilder.class);
    private static final int MAX_POINTS_WARN = 64;

    private final List<Climate.Parameter> temperatures = new ArrayList<>();
    private final List<Climate.Parameter> humidities = new ArrayList<>();
    private final List<Climate.Parameter> continentalnesses = new ArrayList<>();
    private final List<Climate.Parameter> erosions = new ArrayList<>();
    private final List<Climate.Parameter> depths = new ArrayList<>();
    private final List<Climate.Parameter> weirdnesses = new ArrayList<>();
    private long offset = 0L;

    private ParameterPointListBuilder() {}

    public static ParameterPointListBuilder create() {
        return new ParameterPointListBuilder();
    }

    // ==================== Temperature ====================

    public ParameterPointListBuilder temperature(VanillaClimateBands.Temperature t) {
        temperatures.add(t.param());
        return this;
    }

    /** 合併 min ~ max 兩個 band 為單一 span */
    public ParameterPointListBuilder temperature(VanillaClimateBands.Temperature min, VanillaClimateBands.Temperature max) {
        temperatures.add(VanillaClimateBands.Temperature.span(min, max));
        return this;
    }

    public ParameterPointListBuilder temperature(Climate.Parameter p) {
        temperatures.add(p);
        return this;
    }

    // ==================== Humidity ====================

    public ParameterPointListBuilder humidity(VanillaClimateBands.Humidity h) {
        humidities.add(h.param());
        return this;
    }

    public ParameterPointListBuilder humidity(VanillaClimateBands.Humidity min, VanillaClimateBands.Humidity max) {
        humidities.add(VanillaClimateBands.Humidity.span(min, max));
        return this;
    }

    public ParameterPointListBuilder humidity(Climate.Parameter p) {
        humidities.add(p);
        return this;
    }

    // ==================== Continentalness ====================

    public ParameterPointListBuilder continentalness(VanillaClimateBands.Continentalness c) {
        continentalnesses.add(c.param());
        return this;
    }

    public ParameterPointListBuilder continentalness(VanillaClimateBands.Continentalness min, VanillaClimateBands.Continentalness max) {
        continentalnesses.add(VanillaClimateBands.Continentalness.span(min, max));
        return this;
    }

    public ParameterPointListBuilder continentalness(Climate.Parameter p) {
        continentalnesses.add(p);
        return this;
    }

    // ==================== Erosion ====================

    public ParameterPointListBuilder erosion(VanillaClimateBands.Erosion e) {
        erosions.add(e.param());
        return this;
    }

    public ParameterPointListBuilder erosion(VanillaClimateBands.Erosion min, VanillaClimateBands.Erosion max) {
        erosions.add(VanillaClimateBands.Erosion.span(min, max));
        return this;
    }

    public ParameterPointListBuilder erosion(Climate.Parameter p) {
        erosions.add(p);
        return this;
    }

    // ==================== Depth ====================

    public ParameterPointListBuilder depth(VanillaClimateBands.Depth d) {
        depths.add(d.param());
        return this;
    }

    public ParameterPointListBuilder depth(Climate.Parameter p) {
        depths.add(p);
        return this;
    }

    // ==================== Weirdness ====================

    public ParameterPointListBuilder weirdness(VanillaClimateBands.Weirdness w) {
        weirdnesses.add(w.param());
        return this;
    }

    public ParameterPointListBuilder weirdness(VanillaClimateBands.Weirdness min, VanillaClimateBands.Weirdness max) {
        weirdnesses.add(VanillaClimateBands.Weirdness.span(min, max));
        return this;
    }

    public ParameterPointListBuilder weirdness(Climate.Parameter p) {
        weirdnesses.add(p);
        return this;
    }

    // ==================== Offset ====================

    public ParameterPointListBuilder offset(long offset) {
        this.offset = offset;
        return this;
    }

    public ParameterPointListBuilder offset(float offset) {
        this.offset = (long) (offset * 10000L);
        return this;
    }

    // ==================== Build ====================

    /**
     * 建構所有組合的 ParameterPoint 列表（笛卡爾積）。
     * 未設定的維度使用全範圍 span(-2, 2)。
     */
    public List<Climate.ParameterPoint> build() {
        List<Climate.Parameter> temps  = temperatures.isEmpty()       ? List.of(Climate.Parameter.span(-2.0F, 2.0F)) : temperatures;
        List<Climate.Parameter> hums   = humidities.isEmpty()         ? List.of(Climate.Parameter.span(-2.0F, 2.0F)) : humidities;
        List<Climate.Parameter> conts  = continentalnesses.isEmpty()  ? List.of(Climate.Parameter.span(-2.0F, 2.0F)) : continentalnesses;
        List<Climate.Parameter> eros   = erosions.isEmpty()           ? List.of(Climate.Parameter.span(-2.0F, 2.0F)) : erosions;
        List<Climate.Parameter> deps   = depths.isEmpty()             ? List.of(Climate.Parameter.span(-2.0F, 2.0F)) : depths;
        List<Climate.Parameter> weirds = weirdnesses.isEmpty()        ? List.of(Climate.Parameter.span(-2.0F, 2.0F)) : weirdnesses;

        int total = temps.size() * hums.size() * conts.size() * eros.size() * deps.size() * weirds.size();
        if (total > MAX_POINTS_WARN) {
            LOGGER.warn("ParameterPointListBuilder: generating {} climate points (>{})! Consider reducing alternatives.",
                    total, MAX_POINTS_WARN);
        }

        List<Climate.ParameterPoint> result = new ArrayList<>(total);
        for (Climate.Parameter t : temps) {
            for (Climate.Parameter h : hums) {
                for (Climate.Parameter c : conts) {
                    for (Climate.Parameter e : eros) {
                        for (Climate.Parameter d : deps) {
                            for (Climate.Parameter w : weirds) {
                                result.add(new Climate.ParameterPoint(t, h, c, e, d, w, offset));
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * 建構並轉換為 BiomeClimateDefinition 列表，供 BiomeInjectionEntry 直接使用。
     */
    public List<BiomeClimateDefinition> buildAsDefinitions() {
        return build().stream()
                .map(pp -> new BiomeClimateDefinition(
                        pp.temperature(), pp.humidity(), pp.continentalness(),
                        pp.erosion(), pp.depth(), pp.weirdness(), offset))
                .collect(Collectors.toList());
    }
}
