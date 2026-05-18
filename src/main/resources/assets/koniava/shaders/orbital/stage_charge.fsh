// ── Stage 0：充能（地面旋轉能量環）─────────────────────────────────────────

void main() {
    vec3  original    = texture(DiffuseSampler, texCoord).rgb;
    float localTime   = iTime;

    float depth       = texture(DepthSampler, texCoord).r;
    vec3  start_point = worldPos(vec3(texCoord, 0.0)) - BlockPosition;
    vec3  end_point   = worldPos(vec3(texCoord, depth)) - BlockPosition;

    end_point.xz = rotate2(end_point.xz, pow(localTime / 2.0, 4.0));
    float dist = max(
        max(length(end_point.xz) - 24.0, -(length(end_point.xz) - 12.0 * (localTime - 1.7))),
        -min(sdBox2(end_point.xz, vec2(0.0, 24.0)), sdBox2(end_point.xz, vec2(24.0, 0.0)))
    );
    vec3 col = original + 0.2 / pow(dist, 2.0) * C_BLUE * step(length(end_point), localTime * 20.0);
    col = mix(col, vec3(0.0), pow(max(localTime - 3.0, 0.0), 2.0));
    fragColor = vec4(col, 1.0);
}
