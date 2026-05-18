// ── fx：十字環形輝光 ──────────────────────────────────────────────────────────
// 地面上以十字框遮罩的環形發光，產生充能圓環外觀。
// expandFront：光圈往外擴張的前緣半徑（通常 localTime * 20.0）。
// 回傳加法輝光貢獻（乘上顏色後疊加進 col）。
float fxRingBox(vec3 endPoint, float localTime, float expandFront) {
    float dist = max(
        max(length(endPoint.xz) - 24.0,
            -(length(endPoint.xz) - 12.0 * (localTime - 1.7))),
        -min(sdBox2(endPoint.xz, vec2(0.0, 24.0)),
             sdBox2(endPoint.xz, vec2(24.0, 0.0)))
    );
    return 0.2 / pow(dist, 2.0) * step(length(endPoint), expandFront);
}
