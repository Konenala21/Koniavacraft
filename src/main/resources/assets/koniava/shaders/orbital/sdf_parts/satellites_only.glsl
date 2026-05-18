// ── Part：8 顆衛星 SDF ────────────────────────────────────────────────────────
// 依賴：computeOrbitalPoints（common.glsl）
float sdSatellites(vec3 p, float localTime) {
    vec3 p1, p2; float scale;
    computeOrbitalPoints(p, localTime, p1, p2, scale);
    return min(length(p1) + max(scale, -3.0),
               length(p2) + max(scale, -3.0));
}
