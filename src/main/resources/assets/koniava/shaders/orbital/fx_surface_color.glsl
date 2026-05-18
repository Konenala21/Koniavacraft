// ── fx：球體表面顏色 ──────────────────────────────────────────────────────────
// 根據 raymarching 的 close_steps（表面刻面深度）與 localTime 計算球體顏色。
// surfaceP：smoothstep(5, 10, close_steps)，越接近 1 表示刻面越深。
// 回傳 RGB 顏色（直接用於 col）。
vec3 fxSurfaceColor(float surfaceP, float localTime) {
    return mix(C_BLUE, C_PURPLE, abs(sin(3.14159265 * localTime / T_EXPANSION)) * 0.6)
         + vec3(surfaceP) * C_BRIGHT * 0.5;
}

// 爆炸收尾時的衝擊波顏色（藍紫 → 白）
vec3 fxShockwaveColor(float localTime) {
    return mix(mix(C_BLUE, C_PURPLE, 0.4), vec3(1.0), clamp(localTime / T_END - 1.0, 0.0, 1.0));
}
