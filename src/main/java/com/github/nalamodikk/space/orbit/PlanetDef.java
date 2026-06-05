package com.github.nalamodikk.space.orbit;

import com.github.nalamodikk.client.renderer.dimension.PlanetRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

public record PlanetDef(
    String id,
    ResourceKey<Level> dimension,
    float orbitalRadius,
    float orbitalPeriodDays,
    float startAngleDeg,
    float physicalRadius,
    PlanetRenderer.Type shaderType,
    Vector3f colorA,
    Vector3f colorB,
    Vector3f heatColor,
    float    heatAmount,
    float    atmoDensity,
    float    atmoHeight,
    float    selfRotationDays  // 自轉週期（遊戲天），負值=逆行
) {
    /** 行星在太空維度的實際方塊位置。 */
    public Vector3f worldPositionAt(long gameTick, Vector3f starWorldPos) {
        float progress = (gameTick / 24000.0f) / orbitalPeriodDays;
        float angle    = (float) Math.toRadians(startAngleDeg + progress * 360.0f);
        return new Vector3f(
            starWorldPos.x + (float) Math.cos(angle) * orbitalRadius,
            starWorldPos.y,
            starWorldPos.z + (float) Math.sin(angle) * orbitalRadius
        );
    }

    /** 自轉角速度（弧度/遊戲秒），供 shader 使用。 */
    public float rotSpeedRadPerSec() {
        if (selfRotationDays == 0) return 0f;
        // 1 遊戲天 = 24000 tick = 1200 遊戲秒
        return (float)(2 * Math.PI / (selfRotationDays * 1200.0));
    }
}
