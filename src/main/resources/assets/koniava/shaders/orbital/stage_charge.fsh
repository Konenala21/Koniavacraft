// ── Stage：充能地面環 ─────────────────────────────────────────────────────────
// 依賴 fx：fx_ground_twist, fx_ring_box, fx_fade_black

void main() {
    vec3  original    = texture(DiffuseSampler, texCoord).rgb;
    float localTime   = iTime;
    float depth       = texture(DepthSampler, texCoord).r;
    vec3  end_point   = worldPos(vec3(texCoord, depth)) - BlockPosition;

    fxGroundTwist(end_point, localTime);

    vec3 col = original + fxRingBox(end_point, localTime, localTime * 20.0) * C_BLUE;
    col = mix(col, vec3(0.0), fxFadeBlack(localTime, 3.0, 2.0));

    fragColor = vec4(col, 1.0);
}
