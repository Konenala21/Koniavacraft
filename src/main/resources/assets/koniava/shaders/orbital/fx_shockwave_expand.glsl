// ── 膨脹階段衝擊波（T_START → T_END）────────────────────────────────────────
// 接收預計算的 dist，不依賴特定 SDF

float shockwave(float dist, float localTime) {
    float default_light = 10.0 / pow(dist, 2.0);
    float speed_factor  = clamp(1.0 - pow(localTime / T_EXPANSION, 2.0), -5.0, 5.0);
    float shock = 0.05 / abs(fract(2.0 * dist / T_EXPANSION - localTime * speed_factor) - 0.5) * 2.0;
    return default_light + shock * smoothstep(dist + 4.0, dist + 14.0,
                                              localTime * speed_factor * T_EXPANSION / 2.0);
}
