package com.github.nalamodikk.space.orbit;

import com.github.nalamodikk.client.renderer.dimension.PlanetRenderer;
import com.github.nalamodikk.dimension.ModDimensions;
import org.joml.Vector3f;

import java.util.List;

/**
 * 比例尺：1 AU = 1500 方塊，玩家出生 (1500,64,0)。
 * 軌道週期：1 遊戲天 = 1 現實地球日（地球 = 365 遊戲天 = 1 遊戲年）。
 * 行星大小：地球 = 120 格為基準，其他按真實比例。
 * 最後三個 float 欄位：ringInner, ringOuter, ringTiltDeg（0 = 無環）。
 */
public final class StarSystemRegistry {

    public static final StarSystem SOLAR_SYSTEM = new StarSystem(
        "solar_system",
        new Vector3f(0f, 64f, 0f),
        // 真實比例：太陽最大（1000）> 木星(834)；行星大小按現實相對地球(=76)
        // 軌道 ×2 騰出空間讓太陽夠大（水星軌道 1200 > 太陽 1000）
        List.of(new StarDef("sun", new Vector3f(1.0f, 0.95f, 0.72f), 1000f, 0f, 0f)),
        List.of(
            // 水星：0.383x 地球
            new PlanetDef("mercury", ModDimensions.SPACE,
                1200f, 88f, 10f, 29f,
                PlanetRenderer.Type.ROCKY,
                new Vector3f(0.62f, 0.55f, 0.48f), new Vector3f(0.35f, 0.28f, 0.22f),
                new Vector3f(0.9f, 0.4f, 0.1f), 0.3f, 0f, 0f, 58.6f, "", 0,0,0),

            // 金星：0.95x 地球，逆行
            new PlanetDef("venus", ModDimensions.SPACE,
                2200f, 225f, 60f, 72f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.92f, 0.78f, 0.38f), new Vector3f(1.0f, 0.82f, 0.28f),
                new Vector3f(0f), 0f, 3.5f, 0.18f, -243f, "", 0,0,0),

            // 地球：基準 76 格，365 天（= 1 遊戲年）
            new PlanetDef("earth", ModDimensions.SPACE,
                3000f, 365f, 90f, 76f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.18f, 0.42f, 0.68f), new Vector3f(0.35f, 0.62f, 1.0f),
                new Vector3f(0f), 0f, 1.4f, 0.08f, 1.0f, "", 0,0,0),

            // 月球：0.273x 地球，繞地球（降落 → MOON 維度）
            new PlanetDef("moon", ModDimensions.MOON,
                200f, 27.3f, 200f, 21f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.52f, 0.52f, 0.54f), new Vector3f(0.70f, 0.75f, 0.80f),
                new Vector3f(0f), 0f, 0.3f, 0.008f, 27.3f, "earth", 0,0,0),

            // 火星：0.532x 地球
            new PlanetDef("mars", ModDimensions.SPACE,
                4600f, 687f, 145f, 40f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.72f, 0.32f, 0.18f), new Vector3f(0.85f, 0.52f, 0.28f),
                new Vector3f(0f), 0f, 0.18f, 0.014f, 1.03f, "", 0,0,0),

            // 木星：10.97x 地球（行星中最大，仍 < 太陽 1000）
            new PlanetDef("jupiter", ModDimensions.SPACE,
                16000f, 4333f, 320f, 834f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.85f, 0.65f, 0.42f), new Vector3f(0.95f, 0.80f, 0.58f),
                new Vector3f(0f), 0f, 1.8f, 0.10f, 0.41f, "", 0,0,0),

            // 土星：9.14x 地球，土星環（內 1.3x~外 2.4x，傾斜 26.7°）
            new PlanetDef("saturn", ModDimensions.SPACE,
                28000f, 10759f, 80f, 695f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.90f, 0.84f, 0.58f), new Vector3f(0.95f, 0.90f, 0.70f),
                new Vector3f(0f), 0f, 1.2f, 0.09f, 0.44f, "", 1.3f, 2.4f, 26.7f),

            // 土衛六：0.404x 地球，繞土星（軌道 2000 在環外 1668）
            new PlanetDef("titan", ModDimensions.SPACE,
                2000f, 15.9f, 85f, 31f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.80f, 0.58f, 0.22f), new Vector3f(0.95f, 0.62f, 0.15f),
                new Vector3f(0f), 0f, 2.2f, 0.42f, 15.9f, "saturn", 0,0,0),

            // 天王星：3.98x 地球，逆行，97.8° 傾斜（環近乎垂直）
            new PlanetDef("uranus", ModDimensions.SPACE,
                58000f, 30589f, 240f, 302f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.55f, 0.82f, 0.88f), new Vector3f(0.65f, 0.90f, 0.95f),
                new Vector3f(0f), 0f, 0.8f, 0.12f, -0.72f, "", 1.50f, 2.02f, 97.8f),

            // 海王星：3.86x 地球，29.6° 傾斜（亞當斯環最亮）
            new PlanetDef("neptune", ModDimensions.SPACE,
                90000f, 59800f, 170f, 293f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.18f, 0.32f, 0.82f), new Vector3f(0.25f, 0.45f, 0.95f),
                new Vector3f(0f), 0f, 1.0f, 0.12f, 0.67f, "", 1.70f, 2.56f, 29.6f),

            // 冥王星：0.186x 地球（在柯伊伯帶內，現實也是）
            new PlanetDef("pluto", ModDimensions.SPACE,
                120000f, 90560f, 50f, 14f,
                PlanetRenderer.Type.ROCKY,
                new Vector3f(0.72f, 0.65f, 0.58f), new Vector3f(0.40f, 0.35f, 0.30f),
                new Vector3f(0f), 0f, 0f, 0f, 6.4f, "", 0,0,0)
        ),
        // 帶：小行星帶（火星 4600-木星 16000 間）、柯伊伯帶（海王星 90000 外）
        List.of(
            new BeltDef("asteroid_belt",
                6400f, 10800f, 360f, 0.93f,
                new Vector3f(0.45f, 0.40f, 0.34f)),   // 灰褐金屬岩
            new BeltDef("kuiper_belt",
                100000f, 136000f, 1200f, 0.95f,
                new Vector3f(0.55f, 0.62f, 0.70f))    // 冰白藍
        )
    );

    public static final StarSystem ALPHA_CENTAURI = new StarSystem(
        "alpha_centauri",
        new Vector3f(400000f, 64f, 0f),   // 移遠避免跟柯伊伯帶(136000)重疊
        List.of(
            new StarDef("alpha_cen_a", new Vector3f(1.0f, 0.95f, 0.75f), 140f, 150f, 80f),
            new StarDef("alpha_cen_b", new Vector3f(1.0f, 0.80f, 0.55f), 100f, 150f, 80f)
        ),
        List.of(
            new PlanetDef("proxima_b", ModDimensions.SPACE,
                120f, 365f, 0f, 55f,
                PlanetRenderer.Type.ATMOSPHERE,
                new Vector3f(0.35f, 0.55f, 0.42f), new Vector3f(0.45f, 0.70f, 0.85f),
                new Vector3f(0f), 0f, 0.9f, 0.10f, 1.0f, "", 0,0,0)
        )
    );

    // 內建系統：JSON 沒載到任何星系時的安全 fallback
    public static final List<StarSystem> BUILTIN = List.of(SOLAR_SYSTEM, ALPHA_CENTAURI);

    // JSON datapack 載入的星系（StarSystemLoader 填）；空 = 用 BUILTIN
    private static volatile List<StarSystem> loaded = List.of();

    public static void setLoaded(List<StarSystem> systems) {
        loaded = systems == null ? List.of() : List.copyOf(systems);
    }

    /** 目前生效的星系：有 JSON 載到就用 JSON，否則用內建。 */
    public static List<StarSystem> getActive() {
        return loaded.isEmpty() ? BUILTIN : loaded;
    }

    private StarSystemRegistry() {}
}
