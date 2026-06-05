package com.github.nalamodikk.space.orbit;

import org.joml.Vector3f;

/**
 * 單顆恆星的定義。
 * 雙星系統：兩個 StarDef 各有 orbitRadius > 0，繞共同質心互轉。
 * 單星系統：一個 StarDef，orbitRadius = 0。
 */
public record StarDef(
    String id,
    Vector3f color,
    float radius,
    float orbitRadius,     // 離系統中心的距離（0 = 靜止在中心）
    float orbitPeriodDays  // 繞質心的週期（雙星互繞）
) {
    /** 在 gameTick 時這顆恆星的實際世界位置。 */
    public Vector3f worldPositionAt(long gameTick, Vector3f systemWorldPos) {
        if (orbitRadius < 0.01f) return new Vector3f(systemWorldPos);
        float progress = (gameTick / 24000.0f) / orbitPeriodDays;
        float angle    = (float) Math.toRadians(progress * 360.0f);
        return new Vector3f(
            systemWorldPos.x + (float) Math.cos(angle) * orbitRadius,
            systemWorldPos.y,
            systemWorldPos.z + (float) Math.sin(angle) * orbitRadius
        );
    }
}
