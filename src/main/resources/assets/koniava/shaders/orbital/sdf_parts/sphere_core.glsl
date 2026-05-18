// ── Part：水晶球 SDF ──────────────────────────────────────────────────────────
float sdSphere(vec3 p, float localTime) {
    float scale   = computeScale(localTime);
    float r       = length(p);
    float theta_s = atan(p.z, p.x);
    float phi_s   = acos(clamp(p.y / (r + 0.001), -1.0, 1.0));
    float facet   = 0.22 * abs(sin(8.0 * theta_s)) * abs(sin(5.0 * phi_s));
    return abs(r + scale) - facet * smoothstep(1.0, 5.0, abs(scale));
}
