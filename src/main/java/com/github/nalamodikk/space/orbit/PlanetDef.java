package com.github.nalamodikk.space.orbit;

import com.github.nalamodikk.client.renderer.dimension.PlanetRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * 定義一顆行星的軌道與視覺參數。
 * physicalRadius 是實際方塊半徑；視角大小由玩家距離動態計算。
 * 比例尺：1 AU = 500 方塊。
 */
public record PlanetDef(
    String id,
    ResourceKey<Level> dimension,
    float orbitalRadius,      // 離恆星距離（方塊，1 AU = 500）
    float orbitalPeriodDays,  // 公轉週期（遊戲天，1 天 = 24000 tick）
    float startAngleDeg,
    float physicalRadius,     // 星球物理半徑（方塊）
    PlanetRenderer.Type shaderType,
    Vector3f colorA,          // 主色（atmosphere=表面 / rocky=亮面）
    Vector3f colorB,          // 次色（atmosphere=大氣 / rocky=暗面）
    Vector3f heatColor,
    float    heatAmount,
    float    atmoDensity,
    float    atmoHeight
) {
    /** 計算行星在太空維度裡的實際方塊位置。 */
    public Vector3f worldPositionAt(long gameTick, Vector3f starWorldPos) {
        float progress = (gameTick / 24000.0f) / orbitalPeriodDays;
        float angle    = (float) Math.toRadians(startAngleDeg + progress * 360.0f);
        return new Vector3f(
            starWorldPos.x + (float) Math.cos(angle) * orbitalRadius,
            starWorldPos.y,
            starWorldPos.z + (float) Math.sin(angle) * orbitalRadius
        );
    }
}
