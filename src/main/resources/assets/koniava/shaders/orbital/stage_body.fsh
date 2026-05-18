// ── Stage Body：主體 raymarched（搭配任何 sdf_*.glsl）────────────────────
// 依賴：sDist(vec3, float), raycast(vec3, vec3, float), shockwave(vec3, float)

void main() {
    vec3  original    = texture(DiffuseSampler, texCoord).rgb;
    float localTime   = iTime - T_START;

    float depth       = texture(DepthSampler, texCoord).r;
    vec3  start_point = worldPos(vec3(texCoord, 0.0)) - BlockPosition;
    vec3  end_point   = worldPos(vec3(texCoord, depth)) - BlockPosition;
    vec3  dir         = normalize(end_point - start_point);

    vec2 hit_result = raycast(start_point, dir, localTime);
    vec3 hit_point  = start_point + dir * hit_result.x;

    // 深藍→紫漸層，表面刻面越深越偏紫
    float surfaceP = smoothstep(5.0, 10.0, hit_result.y);
    vec3 col = mix(C_BLUE, C_PURPLE, abs(sin(3.14159265 * localTime / T_EXPANSION)) * 0.6)
             + vec3(surfaceP) * C_BRIGHT * 0.5;

    float threshold = step(sDist(hit_point, localTime), MIN_DIST * 2.0);
    threshold *= step(distance(start_point, hit_point), distance(start_point, end_point));
    threshold *= 1.0 - pow(clamp(iTime / T_END - 1.0, 0.0, 1.0), 2.0);

    // 爆炸時從藍紫轉白
    vec3 shockwave_color = mix(mix(C_BLUE, C_PURPLE, 0.4), vec3(1.0), clamp(iTime / T_END - 1.0, 0.0, 1.0));

    fragColor = vec4(mix(original * shockwave(end_point, localTime) * shockwave_color, col, threshold), 1.0);
}
