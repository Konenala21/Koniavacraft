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
