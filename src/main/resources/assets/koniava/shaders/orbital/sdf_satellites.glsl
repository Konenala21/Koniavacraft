// ── SDF：水晶球 + 8 顆衛星（無光柱）────────────────────────────────────────

float sDist(vec3 p, float localTime) {
    float scale = computeScale(localTime);

    // 水晶球主體
    float r       = length(p);
    float theta_s = atan(p.z, p.x);
    float phi_s   = acos(clamp(p.y / (r + 0.001), -1.0, 1.0));
    float facet   = 0.22 * abs(sin(8.0 * theta_s)) * abs(sin(5.0 * phi_s));
    float main_sphere = abs(r + scale) - facet * smoothstep(1.0, 5.0, abs(scale));

    float rotation = min(25.0 / (localTime - 25.0) + 1.5, 0.0);
    p.xz = rotate2(p.xz, rotation);

    // 8 顆衛星
    const float num  = 6.28318530718 / 8.0;
    float offset = -pow((localTime - 6.5) / 1.4, 2.0) + 60.0;
    float theta  = atan(p.z, p.x);
    theta = floor(theta / num);

    float c1 = num * (theta + 0.0);
    vec3 p1  = mat3(cos(c1), 0.0, -sin(c1), 0.0, 1.0, 0.0, sin(c1), 0.0, cos(c1)) * p;
    float c2 = num * (theta + 1.0);
    vec3 p2  = mat3(cos(c2), 0.0, -sin(c2), 0.0, 1.0, 0.0, sin(c2), 0.0, cos(c2)) * p;

    p1.x -= offset;
    p2.x -= offset;

    float outer_spheres = min(length(p1) + max(scale, -3.0),
                              length(p2) + max(scale, -3.0));

    return smooth_min(main_sphere, outer_spheres, 5.0);
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
