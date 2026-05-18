// ── 崩解階段衝擊波（T_END 之後）──────────────────────────────────────────────
// 接收預計算的 dist，不依賴特定 SDF

float shockwave(float dist, float localTime) {
    float fade_factor = clamp(5.0 / localTime - 0.25, 0.0, 1.0);
    return fade_factor * (10.0 / pow(dist, 2.0)
        + 20.0 / abs(dist - 50.0 * (localTime - 0.5)) - 0.3
        + 5.0  / abs(dist - 25.0 * (localTime - 0.5)) - 0.2
    ) + smoothstep(dist - 10.0, dist, 80.0 * (localTime - 2.5));
}
