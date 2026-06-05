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
    float    selfRotationDays,
    String   parentId,
    float    ringInner,    // 0 = 無環；否則為內緣半徑 / 星球半徑
    float    ringOuter,    // 外緣半徑 / 星球半徑
    float    ringTiltDeg   // 自轉軸傾斜度（土星 ≈ 26.7°）
) {
    public Vector3f worldPositionAt(long gameTick, Vector3f starWorldPos) {
        float progress = (gameTick / 24000.0f) / orbitalPeriodDays;
        float angle    = (float) Math.toRadians(startAngleDeg + progress * 360.0f);
        return new Vector3f(
            starWorldPos.x + (float) Math.cos(angle) * orbitalRadius,
            starWorldPos.y,
            starWorldPos.z + (float) Math.sin(angle) * orbitalRadius
        );
    }

    public float rotSpeedRadPerSec() {
        if (selfRotationDays == 0) return 0f;
        return (float)(2 * Math.PI / (selfRotationDays * 1200.0));
    }

    public boolean hasRings() { return ringInner > 0f; }
}
