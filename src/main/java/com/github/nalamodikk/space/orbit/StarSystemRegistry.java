package com.github.nalamodikk.space.orbit;

import com.github.nalamodikk.client.renderer.dimension.PlanetRenderer;
import com.github.nalamodikk.dimension.ModDimensions;
import org.joml.Vector3f;

import java.util.List;

/**
 * 所有已知星系的登錄表。
 * 比例尺：1 AU = 500 方塊，行星物理半徑按現實比例縮放（地球 = 20 方塊）。
 * 太陽系中心在 (0, 64, 0)，玩家出生在地球軌道附近 (500, 64, 0)。
 * 比鄰星系在 (50000, 64, 0)，需要曲速才能抵達。
 */
public final class StarSystemRegistry {

    // ── 太陽系 ──────────────────────────────────────────────────────────────
    public static final StarSystem SOLAR_SYSTEM = new StarSystem(
        "solar_system",
        new Vector3f(0f, 64f, 0f),
        List.of(new StarDef("sol", new Vector3f(1.0f, 0.95f, 0.72f), 30f, 0f, 0f)),
        List.of(
            // 水星：灰褐岩石，熾熱，幾乎無大氣
            new PlanetDef("mercury", ModDimensions.SPACE,
                194f, 1.0f, 10f, 8f,
                PlanetRenderer.Type.ROCKY,
                new Vector3f(0.62f, 0.55f, 0.48f),
                new Vector3f(0.35f, 0.28f, 0.22f),
                new Vector3f(0.9f, 0.4f, 0.1f), 0.3f,
                0f, 0f),

            // 金星：超厚硫酸雲大氣
            new PlanetDef("venus", ModDimensions.SPACE,
                362f, 2.56f, 60f, 19f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.92f, 0.78f, 0.38f),
                new Vector3f(1.0f,  0.82f, 0.28f),
                new Vector3f(0f), 0f,
                3.5f, 0.28f),

            // 月球：灰色，繞地球軌道（近似放在地球軌道）
            new PlanetDef("moon", ModDimensions.SPACE,
                502f, 0.31f, 200f, 5f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.52f, 0.52f, 0.54f),
                new Vector3f(0.70f, 0.75f, 0.80f),
                new Vector3f(0f), 0f,
                0.3f, 0.008f),

            // 火星：紅色，薄二氧化碳大氣
            new PlanetDef("mars", ModDimensions.SPACE,
                762f, 3.91f, 145f, 11f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.72f, 0.32f, 0.18f),
                new Vector3f(0.85f, 0.52f, 0.28f),
                new Vector3f(0f), 0f,
                0.18f, 0.014f),

            // 木星：最大氣態巨星
            new PlanetDef("jupiter", ModDimensions.SPACE,
                2600f, 49.3f, 320f, 220f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.85f, 0.65f, 0.42f),
                new Vector3f(0.95f, 0.80f, 0.58f),
                new Vector3f(0f), 0f,
                1.8f, 0.10f),

            // 土星：淡黃，第二大氣態巨星
            new PlanetDef("saturn", ModDimensions.SPACE,
                4769f, 122.3f, 80f, 183f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.90f, 0.84f, 0.58f),
                new Vector3f(0.95f, 0.90f, 0.70f),
                new Vector3f(0f), 0f,
                1.2f, 0.09f),

            // 土衛六（泰坦）：甲烷濃霧橙色，繞土星
            new PlanetDef("titan", ModDimensions.SPACE,
                4810f, 122.1f, 85f, 11f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.80f, 0.58f, 0.22f),
                new Vector3f(0.95f, 0.62f, 0.15f),
                new Vector3f(0f), 0f,
                2.2f, 0.42f),

            // 天王星：冰質巨星，淡青色
            new PlanetDef("uranus", ModDimensions.SPACE,
                9595f, 346.9f, 240f, 80f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.55f, 0.82f, 0.88f),
                new Vector3f(0.65f, 0.90f, 0.95f),
                new Vector3f(0f), 0f,
                0.8f, 0.12f),

            // 海王星：深藍冰質巨星
            new PlanetDef("neptune", ModDimensions.SPACE,
                15035f, 680.7f, 170f, 77f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.18f, 0.32f, 0.82f),
                new Vector3f(0.25f, 0.45f, 0.95f),
                new Vector3f(0f), 0f,
                1.0f, 0.12f),

            // 冥王星：遠日冰岩，很小
            new PlanetDef("pluto", ModDimensions.SPACE,
                19740f, 1028f, 50f, 4f,
                PlanetRenderer.Type.ROCKY,
                new Vector3f(0.72f, 0.65f, 0.58f),
                new Vector3f(0.40f, 0.35f, 0.30f),
                new Vector3f(0f), 0f,
                0f, 0f)
        )
    );

    // ── 比鄰星系（Alpha Centauri）───────────────────────────────────────────
    // 需要曲速才能抵達（50,000 格外）
    public static final StarSystem ALPHA_CENTAURI = new StarSystem(
        "alpha_centauri",
        new Vector3f(50000f, 64f, 0f),
        List.of(
            new StarDef("alpha_cen_a", new Vector3f(1.0f, 0.95f, 0.75f), 28f, 30f, 80f),
            new StarDef("alpha_cen_b", new Vector3f(1.0f, 0.80f, 0.55f), 20f, 30f, 80f)
        ),
        List.of(
            // 比鄰星 b（潛在宜居行星，純岩石）
            new PlanetDef("proxima_b", ModDimensions.SPACE,
                38f, 0.31f, 0f, 18f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.35f, 0.55f, 0.42f),  // 暗綠藍（海洋？）
                new Vector3f(0.45f, 0.70f, 0.85f),
                new Vector3f(0f), 0f,
                0.9f, 0.10f)
        )
    );

    public static final List<StarSystem> ALL = List.of(SOLAR_SYSTEM, ALPHA_CENTAURI);

    private StarSystemRegistry() {}
}
