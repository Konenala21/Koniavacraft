// ── fx：淡出至黑 ──────────────────────────────────────────────────────────────
// 當 localTime > fadeStart 時開始將畫面混入黑色，power 控制淡出速度。
// 回傳 0.0（完全不淡）到 1.0（完全黑）的係數。
float fxFadeBlack(float localTime, float fadeStart, float power) {
    return pow(max(localTime - fadeStart, 0.0), power);
}
