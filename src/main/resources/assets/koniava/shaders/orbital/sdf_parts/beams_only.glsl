// ── Part：垂直光柱 SDF ────────────────────────────────────────────────────────
// 依賴：computeOrbitalPoints（common.glsl）
float sdBeams(vec3 p, float localTime) {
    vec3 p1, p2; float scale;
    computeOrbitalPoints(p, localTime, p1, p2, scale);
    float base   = clamp(pow(localTime - 8.0, 3.0) * 50.0, 0.0, 5000.0);
    float height = min(pow(localTime - 5.5, 3.0) * 50.0, 5000.0);
    return min(
        length(vec3(p1.x, clamp(p1.y, base, height) - p1.y, p1.z)) - 0.2,
        length(vec3(p2.x, clamp(p2.y, base, height) - p2.y, p2.z)) - 0.2
    );
}
