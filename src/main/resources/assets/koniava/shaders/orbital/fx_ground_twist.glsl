// ── fx：地面旋轉扭曲 ──────────────────────────────────────────────────────────
// 將地面採樣點 XZ 旋轉，產生空間扭曲感。
// localTime 越大旋轉越快（4 次方曲線）。
void fxGroundTwist(inout vec3 endPoint, float localTime) {
    endPoint.xz = rotate2(endPoint.xz, pow(localTime / 2.0, 4.0));
}
