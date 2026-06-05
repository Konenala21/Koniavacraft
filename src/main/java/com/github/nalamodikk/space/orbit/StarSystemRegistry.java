package com.github.nalamodikk.space.orbit;

import com.github.nalamodikk.client.renderer.dimension.PlanetRenderer;
import com.github.nalamodikk.dimension.ModDimensions;
import org.joml.Vector3f;

import java.util.List;

/**
 * 比例尺：1 AU = 1500 方塊，太陽系中心 (0,64,0)，玩家出生 (1500,64,0)。
 * selfRotationDays：現實自轉週期（遊戲天換算），負值=逆行。
 */
public final class StarSystemRegistry {

    public static final StarSystem SOLAR_SYSTEM = new StarSystem(
        "solar_system",
        new Vector3f(0f, 64f, 0f),
        List.of(new StarDef("sun", new Vector3f(1.0f, 0.95f, 0.72f), 200f, 0f, 0f)),
        List.of(
            // 水星：自轉 58.6 天，幾乎無大氣
            new PlanetDef("mercury", ModDimensions.SPACE,
                600f, 1.0f, 10f, 45f,
                PlanetRenderer.Type.ROCKY,
                new Vector3f(0.62f, 0.55f, 0.48f), new Vector3f(0.35f, 0.28f, 0.22f),
                new Vector3f(0.9f, 0.4f, 0.1f), 0.3f, 0f, 0f, 58.6f),

            // 金星：逆行 243 天，超厚硫酸雲
            new PlanetDef("venus", ModDimensions.SPACE,
                1100f, 2.56f, 60f, 110f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.92f, 0.78f, 0.38f), new Vector3f(1.0f, 0.82f, 0.28f),
                new Vector3f(0f), 0f, 3.5f, 0.28f, -243f),

            // 地球：1 天自轉，強化藍色大氣
            new PlanetDef("earth", ModDimensions.SPACE,
                1500f, 4.15f, 0f, 120f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.18f, 0.42f, 0.68f), new Vector3f(0.35f, 0.62f, 1.0f),
                new Vector3f(0f), 0f, 1.2f, 0.15f, 1.0f),

            // 月球：潮汐鎖定 27.3 天（同步自轉）
            new PlanetDef("moon", ModDimensions.SPACE,
                1580f, 0.31f, 200f, 40f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.52f, 0.52f, 0.54f), new Vector3f(0.70f, 0.75f, 0.80f),
                new Vector3f(0f), 0f, 0.3f, 0.008f, 27.3f),

            // 火星：1.03 天，薄大氣
            new PlanetDef("mars", ModDimensions.SPACE,
                2300f, 3.91f, 145f, 65f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.72f, 0.32f, 0.18f), new Vector3f(0.85f, 0.52f, 0.28f),
                new Vector3f(0f), 0f, 0.18f, 0.014f, 1.03f),

            // 木星：0.41 天（快速自轉）
            new PlanetDef("jupiter", ModDimensions.SPACE,
                8000f, 49.3f, 320f, 1200f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.85f, 0.65f, 0.42f), new Vector3f(0.95f, 0.80f, 0.58f),
                new Vector3f(0f), 0f, 1.8f, 0.10f, 0.41f),

            // 土星：0.44 天（快速自轉）
            new PlanetDef("saturn", ModDimensions.SPACE,
                14000f, 122.3f, 80f, 1000f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.90f, 0.84f, 0.58f), new Vector3f(0.95f, 0.90f, 0.70f),
                new Vector3f(0f), 0f, 1.2f, 0.09f, 0.44f),

            // 土衛六：潮汐鎖定 15.9 天
            new PlanetDef("titan", ModDimensions.SPACE,
                14200f, 122.1f, 85f, 65f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.80f, 0.58f, 0.22f), new Vector3f(0.95f, 0.62f, 0.15f),
                new Vector3f(0f), 0f, 2.2f, 0.42f, 15.9f),

            // 天王星：逆行 0.72 天
            new PlanetDef("uranus", ModDimensions.SPACE,
                29000f, 346.9f, 240f, 400f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.55f, 0.82f, 0.88f), new Vector3f(0.65f, 0.90f, 0.95f),
                new Vector3f(0f), 0f, 0.8f, 0.12f, -0.72f),

            // 海王星：0.67 天
            new PlanetDef("neptune", ModDimensions.SPACE,
                45000f, 680.7f, 170f, 380f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.18f, 0.32f, 0.82f), new Vector3f(0.25f, 0.45f, 0.95f),
                new Vector3f(0f), 0f, 1.0f, 0.12f, 0.67f),

            // 冥王星：6.4 天
            new PlanetDef("pluto", ModDimensions.SPACE,
                60000f, 1028f, 50f, 12f,
                PlanetRenderer.Type.ROCKY,
                new Vector3f(0.72f, 0.65f, 0.58f), new Vector3f(0.40f, 0.35f, 0.30f),
                new Vector3f(0f), 0f, 0f, 0f, 6.4f)
        )
    );

    public static final StarSystem ALPHA_CENTAURI = new StarSystem(
        "alpha_centauri",
        new Vector3f(150000f, 64f, 0f),
        List.of(
            new StarDef("alpha_cen_a", new Vector3f(1.0f, 0.95f, 0.75f), 28f, 30f, 80f),
            new StarDef("alpha_cen_b", new Vector3f(1.0f, 0.80f, 0.55f), 20f, 30f, 80f)
        ),
        List.of(
            new PlanetDef("proxima_b", ModDimensions.SPACE,
                120f, 0.31f, 0f, 55f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.35f, 0.55f, 0.42f), new Vector3f(0.45f, 0.70f, 0.85f),
                new Vector3f(0f), 0f, 0.9f, 0.10f, 1.0f)
        )
    );

    public static final List<StarSystem> ALL = List.of(SOLAR_SYSTEM, ALPHA_CENTAURI);
    private StarSystemRegistry() {}
}
