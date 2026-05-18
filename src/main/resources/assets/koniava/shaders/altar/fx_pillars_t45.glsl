// ── T4-T5 pillar phases ───────────────────────────────────────────────────────────────────
// Phase 0 (14-21s): early corner + center pillars appear as big suns rise
// Phase A (25-27s): 4 corner pillars + center core pillar shoot skyward (contraction)
// Phase B (27-29.5s): single massive pillar wrapping the entire ring structure

const float T45P_EARLY_START  = 14.0;
const float T45P_EARLY_END    = 21.0;
const float T45P_CORNER_START = 25.0;
const float T45P_CORNER_END   = 27.0;
const float T45P_SKY_START    = 27.0;
const float T45P_SKY_END      = 29.5;

void fxPillarsT45(vec3 startP, vec3 endP, vec3 dir, float sceneD, float lt, inout vec3 addLight) {

    // ── Phase 0: early pillars (14-21s) while big suns rise ──────────────────
    float p0 = smoothstep(T45P_EARLY_START, T45P_EARLY_START + 0.8, lt)
             * (1.0 - smoothstep(T45P_EARLY_END - 1.0, T45P_EARLY_END, lt));

    if (p0 > 0.001) {
        float denom = dot(dir.xz, dir.xz);
        for (int i = 0; i < 4; i++) {
            vec3  pp  = pillarPos45(i);
            vec2  off = endP.xz - pp.xz;
            float d2  = dot(off, off);
            float yF  = smoothstep(-2.0, 3.0, endP.y) * smoothstep(50.0, 30.0, endP.y);
            addLight += C_CORE   * (0.07 / (d2 + 0.28)) * yF * p0;
            addLight += C_BRIGHT * (0.022/ (d2 + 0.09)) * yF * p0;
            if (denom > 0.001) {
                vec2  dp  = startP.xz - pp.xz;
                float tCA = -dot(dp, dir.xz) / denom;
                if (tCA > 0.01 && tCA < sceneD) {
                    vec3  ca   = startP + dir * tCA;
                    vec2  caOff = ca.xz - pp.xz;
                    float dCA2  = dot(caOff, caOff);
                    float yCA   = smoothstep(-2.0, 3.0, ca.y) * smoothstep(50.0, 30.0, ca.y);
                    addLight += C_CORE   * (0.10 / (dCA2 + 0.28)) * yCA * p0;
                    addLight += C_BRIGHT * (0.032/ (dCA2 + 0.09)) * yCA * p0;
                }
            }
        }
        // Center core
        float dC2 = dot(endP.xz, endP.xz);
        float yFc = smoothstep(-2.0, 3.0, endP.y) * smoothstep(50.0, 30.0, endP.y);
        addLight += C_CORE   * (0.14 / (dC2 + 0.28)) * yFc * p0;
        addLight += C_BRIGHT * (0.045/ (dC2 + 0.09)) * yFc * p0;
        if (denom > 0.001) {
            float tCA = -dot(startP.xz, dir.xz) / denom;
            if (tCA > 0.01 && tCA < sceneD) {
                vec3  ca   = startP + dir * tCA;
                float dCA2 = dot(ca.xz, ca.xz);
                float yCA  = smoothstep(-2.0, 3.0, ca.y) * smoothstep(50.0, 30.0, ca.y);
                addLight += C_CORE   * (0.18 / (dCA2 + 0.28)) * yCA * p0;
                addLight += C_BRIGHT * (0.060/ (dCA2 + 0.09)) * yCA * p0;
            }
        }
    }

    // ── Phase A: corner + center pillars ─────────────────────────────────────
    float pA = smoothstep(T45P_CORNER_START, T45P_CORNER_START + 0.8, lt)
             * (1.0 - smoothstep(T45P_CORNER_END, T45P_CORNER_END + 0.6, lt));

    if (pA > 0.001) {
        // 4 corner pillars
        for (int i = 0; i < 4; i++) {
            vec3  pp  = pillarPos45(i);
            vec2  off = endP.xz - pp.xz;
            float d2  = dot(off, off);
            float yF  = smoothstep(-2.0, 3.0, endP.y) * smoothstep(65.0, 40.0, endP.y);
            addLight += C_CORE   * (0.10 / (d2 + 0.28)) * yF * pA;
            addLight += C_BRIGHT * (0.03 / (d2 + 0.09)) * yF * pA;

            // sky visibility — closest approach to pillar axis
            float denom = dot(dir.xz, dir.xz);
            if (denom > 0.001) {
                vec2  dp  = startP.xz - pp.xz;
                float tCA = -dot(dp, dir.xz) / denom;
                if (tCA > 0.01 && tCA < sceneD) {
                    vec3  ca   = startP + dir * tCA;
                    vec2  caOff = ca.xz - pp.xz;
                    float dCA2  = dot(caOff, caOff);
                    float yCA   = smoothstep(-2.0, 3.0, ca.y) * smoothstep(65.0, 40.0, ca.y);
                    addLight += C_CORE   * (0.14 / (dCA2 + 0.28)) * yCA * pA;
                    addLight += C_BRIGHT * (0.05 / (dCA2 + 0.09)) * yCA * pA;
                }
            }
        }

        // Center core pillar (brighter, tighter)
        float dC2 = dot(endP.xz, endP.xz);
        float yFc = smoothstep(-2.0, 3.0, endP.y) * smoothstep(65.0, 40.0, endP.y);
        addLight += C_CORE   * (0.22 / (dC2 + 0.28)) * yFc * pA;
        addLight += C_BRIGHT * (0.07 / (dC2 + 0.09)) * yFc * pA;
        addLight += vec3(1.0) * (0.015/ (dC2 + 0.010)) * yFc * pA;

        float denom = dot(dir.xz, dir.xz);
        if (denom > 0.001) {
            float tCA = -dot(startP.xz, dir.xz) / denom;
            if (tCA > 0.01 && tCA < sceneD) {
                vec3  ca   = startP + dir * tCA;
                float dCA2 = dot(ca.xz, ca.xz);
                float yCA  = smoothstep(-2.0, 3.0, ca.y) * smoothstep(65.0, 40.0, ca.y);
                addLight += C_CORE   * (0.30 / (dCA2 + 0.28)) * yCA * pA;
                addLight += C_BRIGHT * (0.10 / (dCA2 + 0.09)) * yCA * pA;
                addLight += vec3(1.0) * (0.022/(dCA2 + 0.010)) * yCA * pA;
            }
        }
    }

    // ── Phase B: massive skyward pillar covering entire ring ──────────────────
    float pB = smoothstep(T45P_SKY_START, T45P_SKY_START + 0.6, lt)
             * (1.0 - smoothstep(T45P_SKY_END - 0.6, T45P_SKY_END, lt));

    if (pB > 0.001) {
        float dAxis = length(endP.xz);
        float ht    = endP.y;
        float yF    = smoothstep(-2.0, 8.0, ht) * smoothstep(88.0, 60.0, ht);
        addLight += C_CORE   * (4.5  / (dAxis*dAxis + 65.0)) * yF * pB;
        addLight += C_BRIGHT * (1.20 / (dAxis*dAxis + 22.0)) * yF * pB;
        addLight += vec3(1.0) * (0.18 / (dAxis*dAxis + 3.5))  * yF * pB;

        float denom = dot(dir.xz, dir.xz);
        if (denom > 0.001) {
            float tCA = -dot(startP.xz, dir.xz) / denom;
            if (tCA > 0.01 && tCA < sceneD) {
                vec3  ca  = startP + dir * tCA;
                float dCA = length(ca.xz);
                float yCA = smoothstep(-2.0, 8.0, ca.y) * smoothstep(88.0, 60.0, ca.y);
                addLight += C_CORE   * (6.0  / (dCA*dCA + 65.0)) * yCA * pB;
                addLight += C_BRIGHT * (1.60 / (dCA*dCA + 22.0)) * yCA * pB;
                addLight += vec3(1.0) * (0.24 / (dCA*dCA + 3.5)) * yCA * pB;
            }
        }
    }
}
