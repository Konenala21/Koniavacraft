// ── Section B：水晶球 ─────────────────────────────────────────────────────────
// 膨脹再收縮的水晶刻面球體，附帶 8 根柱狀輝光與內部虛空填充

#define SPHERE_STEPS 90
#define SPHERE_HIT   0.08
#define SPHERE_MAXD  280.0

float _sdCrystalSphere(vec3 p, float R) {
    float r     = length(p);
    float shell = abs(r - R);
    float theta = atan(p.z, p.x);
    float phi   = acos(clamp(p.y / (r + 0.001), -1.0, 1.0));
    float facet = 0.28 * abs(sin(7.0 * theta)) * abs(sin(5.0 * phi));
    return shell - facet * smoothstep(1.0, 6.0, R);
}

float _marchSphere(vec3 ro, vec3 rd, float R, float maxDist) {
    float b    = dot(ro, rd);
    float disc = b*b - (dot(ro,ro) - (R+0.5)*(R+0.5));
    if (disc < 0.0) return SPHERE_MAXD;
    float sqD = sqrt(disc);
    float tN  = max(0.0, -b - sqD - 0.5);
    float tF  = min(maxDist, -b + sqD + 0.5);
    if (tN >= tF) return SPHERE_MAXD;
    float t = tN;
    for (int i = 0; i < SPHERE_STEPS; i++) {
        float d = _sdCrystalSphere(ro + rd * t, R);
        if (d < SPHERE_HIT) return t;
        if (t >= tF)        return SPHERE_MAXD;
        t += max(d * 0.55, 0.02);
    }
    return SPHERE_MAXD;
}

void fxCrystalSphere(
    vec3 startP, vec3 endP, vec3 dir, float sceneD,
    vec3 original, float lt,
    inout vec3 addLight, inout vec3 repCol, inout float repW
) {
    float burstP  = clamp((lt - T_CHARGE) / (T_BURST_END - T_CHARGE), 0.0, 1.0);
    float sphereR = sin(burstP * PI) * 21.0;
    if (sphereR <= 0.3) return;

    float t   = _marchSphere(startP, dir, sphereR, sceneD);
    bool  hit = (t < SPHERE_MAXD - 0.1);

    if (hit) {
        vec3  hp    = startP + dir * t;
        float r     = length(hp);
        float theta = atan(hp.z, hp.x);
        float phi   = acos(clamp(hp.y / (r + 0.001), -1.0, 1.0));
        float facet = abs(sin(7.0 * theta)) * abs(sin(5.0 * phi));
        float rim   = 1.0 - smoothstep(0.0, 0.30, _sdCrystalSphere(hp, sphereR));
        vec3  sCol  = mix(C_CORE, C_CRYSTAL, facet * 0.6);
        sCol        = mix(sCol, C_BRIGHT, rim * 0.55 + facet * facet * 0.35);
        float sFade = 1.0 - burstP * burstP;
        repCol = sCol * sFade;
        repW   = sFade * iAlpha;
    }

    float outerD = _sdCrystalSphere(endP, sphereR);
    if (outerD > 0.0)
        addLight += C_CORE * (0.022 / (outerD*outerD + 0.012)) * (1.0 - burstP * 0.7) * iAlpha;

    // 球體內部虛空填充
    if (!hit && length(endP) < sphereR * 0.88) {
        float vF = (1.0 - length(endP) / (sphereR * 0.88)) * (1.0 - burstP);
        repCol = mix(original * 0.30, C_VOID + C_CORE * 0.12, vF * 0.8);
        repW   = max(repW, vF * 0.60 * iAlpha);
    }

    // 8 根柱狀輝光（球存在期間）
    float pLife = smoothstep(T_CHARGE + 0.4, T_CHARGE + 2.0, lt)
                * (1.0 - smoothstep(T_BURST_END - 2.2, T_BURST_END, lt));
    if (pLife > 0.001) {
        for (int i = 0; i < 8; i++) {
            float ang = float(i) * PI * 0.25;
            float cx  = cos(ang) * 4.0, cz = sin(ang) * 4.0;
            float d2d = length(endP.xz - vec2(cx, cz));
            float vF  = step(0.0, endP.y) * smoothstep(26.0, 0.0, endP.y);
            vec3  pc  = mix(C_CORE, C_PILLAR, smoothstep(16.0, 26.0, endP.y));
            addLight += pc * (0.008 / (d2d*d2d + 0.008)) * vF * pLife * iAlpha;
        }
    }
}
