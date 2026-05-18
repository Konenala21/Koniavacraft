// ── SDF：水晶球（無衛星，無光柱）────────────────────────────────────────────

float sDist(vec3 p, float localTime) {
    float scale   = computeScale(localTime);
    float r       = length(p);
    float theta_s = atan(p.z, p.x);
    float phi_s   = acos(clamp(p.y / (r + 0.001), -1.0, 1.0));
    float facet   = 0.22 * abs(sin(8.0 * theta_s)) * abs(sin(5.0 * phi_s));
    return abs(r + scale) - facet * smoothstep(1.0, 5.0, abs(scale));
}

vec2 raycast(vec3 point, vec3 dir, float localTime) {
    float traveled    = 0.0;
    int   close_steps = 0;
    for (int i = 0; i < STEPS; i++) {
        float safe = sDist(point, localTime);
        if (safe <= MIN_DIST || traveled >= MAX_DIST) break;
        traveled += safe;
        point    += dir * safe;
        if (safe <= 0.01) close_steps++;
    }
    return vec2(traveled, float(close_steps));
}

float shockwave(vec3 endPoint, float localTime) {
    float dist          = sDist(endPoint, localTime);
    float default_light = 10.0 / pow(dist, 2.0);
    float speed_factor  = clamp(1.0 - pow(localTime / T_EXPANSION, 2.0), -5.0, 5.0);
    float shock = 0.05 / abs(fract(2.0 * dist / T_EXPANSION - localTime * speed_factor) - 0.5) * 2.0;
    return default_light + shock * smoothstep(dist + 4.0, dist + 14.0, localTime * speed_factor * T_EXPANSION / 2.0);
}
