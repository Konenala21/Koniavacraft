// ── Stage：崩解收尾 ───────────────────────────────────────────────────────────
// 依賴 fx：sDist, raycast, shockwave, fx_surface_color

void main() {
    vec3  original    = texture(DiffuseSampler, texCoord).rgb;
    float localTime   = iTime - T_END;
    float depth       = texture(DepthSampler, texCoord).r;
    vec3  start_point = worldPos(vec3(texCoord, 0.0)) - BlockPosition;
    vec3  end_point   = worldPos(vec3(texCoord, depth)) - BlockPosition;
    vec3  dir         = normalize(end_point - start_point);

    vec2 hit_result = raycast(start_point, dir, localTime);
    vec3 hit_point  = start_point + dir * hit_result.x;

    float surfaceP = smoothstep(5.0, 10.0, hit_result.y);
    vec3  col      = fxSurfaceColor(surfaceP, 0.0);

    float threshold = step(sDist(hit_point, localTime), MIN_DIST * 2.0);
    threshold *= step(distance(start_point, hit_point), distance(start_point, end_point));

    float sw = shockwave(sDist(end_point, localTime), localTime);

    fragColor = vec4(mix(original * sw * C_BLUE, col, threshold), 1.0);
}
