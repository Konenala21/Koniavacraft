// ── T6 Climax visual effects ──────────────────────────────────────────────────────────────
const vec3 C_PURPLE_T6 = vec3(0.52, 0.22, 1.00);

float sphereFacetT6(vec3 n) {
    float theta = atan(n.z, n.x);
    float phi   = acos(clamp(n.y, -1.0, 1.0));
    return 0.22 * abs(sin(8.0 * theta)) * abs(sin(5.0 * phi));
}

vec3 orbSurfaceColorT6(vec3 norm, float lt6) {
    vec3 base = mix(C_CORE, C_PURPLE_T6, abs(sin(0.314 * lt6)) * 0.55);
    return base + sphereFacetT6(norm) * C_BRIGHT * 0.45;
}
// iTime = elapsed seconds since T6 climax started (tick - T6_PHASE_OFFSET) / 20

// ── 1. Transition ground waves (0-2s, bridging T4-T5 tail) ──────────────────────────────
void fxGroundWavesT6(vec3 startP, vec3 dir, float sceneD, float lt6, inout vec3 addLight) {
    if (lt6 > 2.0 || abs(dir.y) <= 0.001) return;
    float tG = (RING_Y - startP.y) / dir.y;
    if (tG <= 0.05 || tG >= sceneD) return;
    vec2  hitXZ = (startP + dir * tG).xz;
    float hitR  = length(hitXZ);

    for (int w = 0; w < 2; w++) {
        float wAge = lt6 - float(w) * 1.0;
        if (wAge <= 0.0 || wAge > 1.0) continue;
        float p     = wAge / 1.0;
        float r     = p * 12.0;
        float alpha = sin(p * PI) * 0.80;
        float thick = 0.25 + (1.0 - p) * 0.35;
        float s     = smoothstep(thick, 0.0, abs(hitR - r)) * alpha;
        if (s < 0.003) continue;
        addLight += mix(C_CORE, C_WAVE, 0.6) * s;
    }
}

// ── 2. Ball suns emitting from core (2.5-24s) ───────────────────────────────────────────
vec3 orbPosT6(int i, float age) {
    float ox = 0.0; float oz = 0.0; float riseDur = 4.0; float peakH = 22.0;
    if      (i == 1) { ox =  2.8; oz =  1.5; riseDur = 3.0;  peakH = 16.0; }
    else if (i == 2) { ox = -2.0; oz =  2.8; riseDur = 3.5;  peakH = 20.0; }
    else if (i == 3) { ox =  1.5; oz = -2.8; riseDur = 4.5;  peakH = 14.0; }
    else if (i == 4) { ox = -2.8; oz = -1.5; riseDur = 3.25; peakH = 18.0; }
    float riseP = min(1.0, age / riseDur);
    return vec3(ox, 1.0 + riseP * riseP * peakH, oz);
}

void fxBallSunsT6(vec3 startP, vec3 endP, vec3 dir, float sceneD, float lt6, inout vec3 addLight) {
    if (lt6 < 2.5 || lt6 > 24.0) return;
    float age  = lt6 - 2.5;
    float fade = lt6 > 19.0 ? clamp(1.0 - (lt6 - 19.0) / 5.0, 0.0, 1.0) : 1.0;
    if (fade < 0.001) return;

    for (int i = 0; i < 5; i++) {
        vec3  oPos = orbPosT6(i, age);
        float br   = mix(1.0, 0.75, float(i) / 4.0) * fade;
        float orbR = 1.2;
        vec3  col  = mix(C_CORE, C_BRIGHT, float(i) / 4.0 * 0.5);

        // Orbital-style solid sphere
        float tHit = raySphere(startP, dir, oPos, orbR);
        if (tHit > 0.0 && tHit < sceneD) {
            vec3  hitP = startP + dir * tHit;
            vec3  norm = normalize(hitP - oPos);
            float ndv  = max(0.0, dot(norm, -dir));
            float lit  = ndv * ndv * 0.45 + (1.0 - ndv) * (1.0 - ndv) * 0.18 + 0.10;
            addLight += orbSurfaceColorT6(norm, lt6) * lit * br;
        }
        // Soft outer glow
        float tCA = dot(oPos - startP, dir);
        if (tCA > 0.01) {
            vec3  ca  = startP + dir * min(tCA, sceneD);
            float dCA = length(ca - oPos);
            float glow = exp(-(dCA * dCA) / (orbR * orbR * 3.0)) * br;
            addLight += col * glow * 0.35;
        }
    }
}

// ── 3. Sky plane waves (15-28s, camera is looking up at this point) ──────────────────────
const float T6_SKY_Y = 42.0;

void fxSkyWavesT6(vec3 startP, vec3 dir, float sceneD, float lt6, inout vec3 addLight) {
    if (lt6 < 15.0 || lt6 > 28.0 || dir.y <= 0.001) return;
    float tSky = (T6_SKY_Y - startP.y) / dir.y;
    if (tSky <= 0.05 || tSky >= sceneD) return;
    vec2  hitXZ = (startP + dir * tSky).xz;
    float hitR  = length(hitXZ);

    float skyAge = lt6 - 15.0;
    for (int w = 0; w < 4; w++) {
        float wAge = skyAge - float(w) * 2.25;
        if (wAge <= 0.0 || wAge > 2.75) continue;
        float p     = wAge / 2.75;
        float r     = 1.5 + p * 15.0;
        float alpha = sin(p * PI) * 0.55;
        float thick = 0.9 + (1.0 - p) * 1.1;
        float s     = smoothstep(thick, 0.0, abs(hitR - r)) * alpha;
        if (s < 0.003) continue;
        addLight += mix(C_CORE, C_BRIGHT, 0.5) * s;
    }
}

// ── 4a. Thin orbital-style beams during magic circle drawing (22-35s = 440-700t) ─────────
// Fires 4 pulses: each fires center + 4 corners, radius ~0.25 (matching orbital's 0.2)
void fxDrawingBeamsT6(vec3 startP, vec3 endP, vec3 dir, float sceneD, float lt6, inout vec3 addLight) {
    if (lt6 < 22.0 || lt6 > 38.0) return;
    float denom = dot(dir.xz, dir.xz);

    for (int b = 0; b < 5; b++) {
        float fireT = 22.0 + float(b) * 3.2;
        float bAge  = lt6 - fireT;
        if (bAge < 0.0 || bAge > 3.0) continue;
        float alpha = sin(clamp(bAge / 3.0, 0.0, 1.0) * PI) * 0.85;
        if (alpha < 0.01) continue;

        // Center beam
        if (denom > 0.001) {
            float tCA = -dot(startP.xz, dir.xz) / denom;
            if (tCA > 0.01 && tCA < sceneD) {
                vec3  ca  = startP + dir * tCA;
                float dCA = length(ca.xz);
                float rim = smoothstep(0.45, 0.08, dCA);  // thin: ~0.25 block radius
                float yCA = smoothstep(-2.0, 0.5, ca.y) * smoothstep(72.0, 48.0, ca.y);
                addLight = max(addLight, (C_CORE * 0.4 + C_BRIGHT * rim) * yCA * alpha);
            }
        }

        // 4 corner beams
        for (int i = 0; i < 4; i++) {
            vec3  pp   = pillarPos45(i);
            if (denom > 0.001) {
                vec2  dp   = startP.xz - pp.xz;
                float tCA2 = -dot(dp, dir.xz) / denom;
                if (tCA2 > 0.01 && tCA2 < sceneD) {
                    vec3  ca2  = startP + dir * tCA2;
                    vec2  off  = ca2.xz - pp.xz;
                    float dCA2 = length(off);
                    float rim2 = smoothstep(0.38, 0.06, dCA2);  // slightly thinner
                    float yCA2 = smoothstep(-2.0, 0.5, ca2.y) * smoothstep(65.0, 42.0, ca2.y);
                    addLight = max(addLight, (C_CORE * 0.3 + C_BRIGHT * rim2 * 0.8) * yCA2 * alpha * 0.75);
                }
            }
        }
    }
}

// ── 4. Multiple pillars skyward (20-26.5s = 400-530t) ───────────────────────────────────
void fxPillarsT6(vec3 startP, vec3 endP, vec3 dir, float sceneD, float lt6, inout vec3 addLight) {
    if (lt6 < 20.0 || lt6 > 26.5) return;
    float pA = smoothstep(20.0, 20.75, lt6) * (1.0 - smoothstep(25.75, 26.5, lt6));
    if (pA < 0.001) return;
    float denom = dot(dir.xz, dir.xz);

    // Center core pillar (brightest, tallest)
    float dC2 = dot(endP.xz, endP.xz);
    float yFc = smoothstep(-2.0, 3.0, endP.y) * smoothstep(58.0, 35.0, endP.y);
    addLight += C_CORE   * (0.28 / (dC2 + 0.30)) * yFc * pA;
    addLight += C_BRIGHT * (0.10 / (dC2 + 0.10)) * yFc * pA;
    addLight += vec3(1.0) * (0.025/(dC2 + 0.012)) * yFc * pA;
    if (denom > 0.001) {
        float tCA = -dot(startP.xz, dir.xz) / denom;
        if (tCA > 0.01 && tCA < sceneD) {
            vec3  ca   = startP + dir * tCA;
            float dCA2 = dot(ca.xz, ca.xz);
            float yCA  = smoothstep(-2.0, 3.0, ca.y) * smoothstep(58.0, 35.0, ca.y);
            addLight += C_CORE   * (0.38 / (dCA2 + 0.30)) * yCA * pA;
            addLight += C_BRIGHT * (0.14 / (dCA2 + 0.10)) * yCA * pA;
            addLight += vec3(1.0) * (0.030/(dCA2 + 0.012)) * yCA * pA;
        }
    }

    // 4 corner pillars
    for (int i = 0; i < 4; i++) {
        vec3  pp  = pillarPos45(i);
        vec2  off = endP.xz - pp.xz;
        float d2  = dot(off, off);
        float yFi = smoothstep(-2.0, 3.0, endP.y) * smoothstep(48.0, 28.0, endP.y);
        addLight += C_CORE   * (0.11 / (d2 + 0.28)) * yFi * pA * 0.65;
        addLight += C_BRIGHT * (0.035/ (d2 + 0.09)) * yFi * pA * 0.65;
        if (denom > 0.001) {
            vec2  dp   = startP.xz - pp.xz;
            float tCA2 = -dot(dp, dir.xz) / denom;
            if (tCA2 > 0.01 && tCA2 < sceneD) {
                vec3  ca2   = startP + dir * tCA2;
                vec2  caOff = ca2.xz - pp.xz;
                float dCA22 = dot(caOff, caOff);
                float yCA2  = smoothstep(-2.0, 3.0, ca2.y) * smoothstep(48.0, 28.0, ca2.y);
                addLight += C_CORE   * (0.15 / (dCA22 + 0.28)) * yCA2 * pA * 0.65;
                addLight += C_BRIGHT * (0.055/ (dCA22 + 0.09)) * yCA2 * pA * 0.65;
            }
        }
    }
}
