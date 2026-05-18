// ── SDF：崩解環（T_END 之後）────────────────────────────────────────────────

float sDist(vec3 p, float localTime) {
    return length(p.xz) + 8.0 / localTime - 24.0;
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
    float dist         = sDist(endPoint, localTime);
    float fade_factor  = clamp(5.0 / localTime - 0.25, 0.0, 1.0);
    return fade_factor * (10.0 / pow(dist, 2.0)
        + 20.0 / abs(dist - 50.0 * (localTime - 0.5)) - 0.3
        + 5.0  / abs(dist - 25.0 * (localTime - 0.5)) - 0.2
    ) + smoothstep(dist - 10.0, dist, 80.0 * (localTime - 2.5));
}
