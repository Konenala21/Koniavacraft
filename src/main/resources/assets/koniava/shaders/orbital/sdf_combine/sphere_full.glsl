// ── Combine：球 + 衛星 + 光柱（完整）────────────────────────────────────────
float sDist(vec3 p, float localTime) {
    return smooth_min(sdSphere(p, localTime),
           smooth_min(sdSatellites(p, localTime), sdBeams(p, localTime), 1.0),
           5.0);
}

vec2 raycast(vec3 point, vec3 dir, float localTime) {
    float traveled = 0.0; int close_steps = 0;
    for (int i = 0; i < STEPS; i++) {
        float safe = sDist(point, localTime);
        if (safe <= MIN_DIST || traveled >= MAX_DIST) break;
        traveled += safe; point += dir * safe;
        if (safe <= 0.01) close_steps++;
    }
    return vec2(traveled, float(close_steps));
}
