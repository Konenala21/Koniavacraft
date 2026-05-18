// ── T4-T5 core sphere: expands from altar centre, covers entire ring structure ──────────────

const float T45S_START = 10.0;  // begin expanding as black screen lifts
const float T45S_FULL  = 14.5;  // fully expanded just as suns start rising
const float T45S_FADE  = 25.5;  // fade out when pillars take over
const float T45S_END   = 29.5;
const float T45S_MAX_R = 10.0;  // covers ring structure (T45_RING_R = 8)

void fxCoreT45(vec3 startP, vec3 endP, vec3 dir, float sceneD, float lt, inout vec3 addLight) {
    if (lt < T45S_START || lt > T45S_END) return;

    float expandP = smoothstep(T45S_START, T45S_FULL, lt);
    float fadeP   = 1.0 - smoothstep(T45S_FADE, T45S_END, lt);
    float alpha   = expandP * fadeP;
    if (alpha < 0.001) return;

    // ease-in: slow at start, fast at end
    float curR = expandP * expandP * T45S_MAX_R;
    vec3  ctr  = vec3(0.0, 1.5, 0.0);

    // rim glow via closest approach on ray
    float tCA = dot(ctr - startP, dir);
    if (tCA > 0.01) {
        vec3  ca   = startP + dir * min(tCA, sceneD);
        float dCA  = length(ca - ctr);
        float rimW = curR * 0.10 + 0.40;
        float rim  = exp(-abs(dCA - curR) / rimW);
        addLight += mix(C_CORE, C_BRIGHT, 0.55) * rim * alpha * 0.80;
    }

    // inner glow for geometry inside the sphere volume
    float surfD = length(endP - ctr);
    if (surfD < curR * 1.2) {
        float inner = max(0.0, 1.0 - surfD / (curR * 1.2)) * 0.18;
        addLight += C_CORE * inner * alpha;
    }
}

// ── T4-T5 sun orbs: 4 big suns rise from pillars, each splits into 4 small suns ──────────

// Phase timing (seconds since T4-T5 animation start)
const float T45_RISE_START  = 14.0;  // big suns start rising from pillar tops
const float T45_RISE_END    = 17.0;  // big suns reach peak (Y = 14 above altar top)
const float T45_SPLIT_START = 17.0;  // small suns begin separating from parent
const float T45_SPLIT_END   = 20.5;  // 4 small suns per group fully spread
const float T45_DESCEND_END = 24.5;  // small suns finish moving to ring positions
const float T45_ORIG_FADE   = 21.0;  // original big suns start fading
const float T45_ORIG_GONE   = 24.5;  // original big suns fully gone
const float T45_CONTR_START = 25.5;  // rapid spin + inward contraction
const float T45_CONTR_END   = 28.5;  // contraction complete

const float T45_SUN_PEAK_Y  = 14.0;  // height big suns reach (blocks above altar top)
const float T45_RING_R      = 8.0;   // ring formation radius
const float T45_RING_Y      = 2.0;   // ring formation height
const float T45_SMALL_R     = 1.0;   // small sun visual radius (~83% of initial big sun 1.2)
const vec3  C_PURPLE        = vec3(0.52, 0.22, 1.00);  // matches orbital/common.glsl

// Crystal faceting from orbital/sdf_parts/sphere_core.glsl
float sphereFacet(vec3 n) {
    float theta = atan(n.z, n.x);
    float phi   = acos(clamp(n.y, -1.0, 1.0));
    return 0.22 * abs(sin(8.0 * theta)) * abs(sin(5.0 * phi));
}

// Orbital-style surface: blue-purple time oscillation + facet highlights
vec3 sunSurfaceColor(vec3 norm, float lt) {
    vec3 base = mix(C_CORE, C_PURPLE, abs(sin(0.314 * lt)) * 0.55);
    return base + sphereFacet(norm) * C_BRIGHT * 0.45;
}

// ── Combined SDF for all 4 big suns (one pass covers all corners) ────────────
#define BS_STEPS   100
#define BS_MIN     0.005
#define BS_MAX     300.0

float sdBigSun4(vec3 p, float bigR, float riseP) {
    float d = BS_MAX;
    for (int i = 0; i < 4; i++) {
        vec3  pp   = pillarPos45(i);
        float bigY = pp.y + riseP * riseP * (T45_SUN_PEAK_Y - pp.y);
        vec3  lp   = p - vec3(pp.x, bigY, pp.z);
        float r    = length(lp);
        float theta = atan(lp.z, lp.x);
        float phi   = acos(clamp(lp.y / (r + 0.001), -1.0, 1.0));
        float facet = 0.22 * abs(sin(8.0 * theta)) * abs(sin(5.0 * phi));
        d = min(d, abs(r - bigR) - facet * 0.3);
    }
    return d;
}

void fxBigSunsRaymarched(vec3 startP, vec3 dir, float sceneD,
                         float lt, float bigR, float riseP, float origFade,
                         inout vec3 addLight) {
    if (origFade < 0.001) return;
    float traveled   = 0.0;
    int   closeSteps = 0;
    for (int i = 0; i < BS_STEPS; i++) {
        float d = sdBigSun4(startP + dir * traveled, bigR, riseP);
        if (d <= BS_MIN) closeSteps++;
        if (traveled >= min(sceneD, BS_MAX) || closeSteps >= 12) break;
        traveled += max(d, BS_MIN);
    }
    if (closeSteps >= 4 && traveled < sceneD) {
        // Use max() not += : matches orbital_test's "replace pixel" rather than accumulate
        float surfaceP = smoothstep(4.0, 10.0, float(closeSteps));
        vec3  base     = mix(C_CORE, C_PURPLE, abs(sin(0.314 * lt)) * 0.55);
        vec3  col      = (base + vec3(surfaceP) * C_BRIGHT * 0.5) * origFade;
        addLight = max(addLight, col);
    }
}

void fxOrbsT45(
    vec3 startP, vec3 endP, vec3 dir, float sceneD,
    float lt,
    inout vec3 addLight
) {
    // ── Big suns (4 total, one per corner pillar) ─────────────────────────────
    float riseP    = smoothstep(T45_RISE_START, T45_RISE_END, lt);
    float origFade = 1.0 - smoothstep(T45_ORIG_FADE, T45_ORIG_GONE, lt);

    if (riseP > 0.001 && origFade > 0.001) {
        float expandP = smoothstep(T45_RISE_START, T45_ORIG_GONE, lt);
        float bigR    = mix(1.2, 1.8, expandP);

        // ── Raymarched orbital spheres (combined SDF, 100 steps) ─────────────
        fxBigSunsRaymarched(startP, dir, sceneD, lt, bigR, riseP, origFade, addLight);

        // ── Per-sun outer glow halo (closest approach, fast) ─────────────────
        for (int i = 0; i < 4; i++) {
            vec3  pp   = pillarPos45(i);
            float bigY = pp.y + riseP * riseP * (T45_SUN_PEAK_Y - pp.y);
            vec3  bPos = vec3(pp.x, bigY, pp.z);
            float tCA  = dot(bPos - startP, dir);
            if (tCA > 0.01) {
                vec3  ca  = startP + dir * min(tCA, sceneD);
                float dCA = length(ca - bPos);
                float glow = exp(-(dCA * dCA) / (bigR * bigR * 3.0)) * origFade;
                addLight += sunColorT45(i) * glow * 0.12;
            }
        }
    }

    // ── Small suns (16 total = 4 groups × 4) ─────────────────────────────────
    float smallAge = lt - T45_SPLIT_START;
    if (smallAge <= 0.0) return;

    float smallFade = 1.0 - smoothstep(T45_CONTR_END - 0.5, T45_CONTR_END, lt);
    if (smallFade <= 0.001) return;

    float spreadP = smoothstep(0.0, T45_SPLIT_END - T45_SPLIT_START, smallAge);
    float descP   = smoothstep(T45_SPLIT_END, T45_DESCEND_END, lt);
    float contrP  = smoothstep(T45_CONTR_START, T45_CONTR_END, lt);

    for (int i = 0; i < 4; i++) {
        vec3 pp        = pillarPos45(i);
        vec3 parentPos = vec3(pp.x, T45_SUN_PEAK_Y, pp.z); // parent at peak

        for (int j = 0; j < 4; j++) {
            int   idx      = i * 4 + j;

            // Birth: small sun orbits locally around parent, spreading outward
            float localAng  = float(j) * PI * 0.5 + float(i) * PI * 0.25 + lt * 2.0;
            float localR    = spreadP * 1.5;
            vec3  birthPos  = parentPos + vec3(cos(localAng) * localR, 0.0, sin(localAng) * localR);

            // Ring target: 16 evenly spaced at RING_R, RING_Y
            float ringAng   = float(idx) * PI * 2.0 / 16.0;
            vec3  ringPos   = vec3(cos(ringAng) * T45_RING_R, T45_RING_Y, sin(ringAng) * T45_RING_R);

            // Contraction: spin inward from ring position
            float spinRate  = 3.0 + contrP * 14.0;
            float spinAng   = ringAng + (lt - T45_CONTR_START) * spinRate;
            float contrR    = T45_RING_R * (1.0 - contrP * contrP);
            float contrY    = mix(T45_RING_Y, 0.0, contrP * contrP);
            vec3  contrPos  = vec3(cos(spinAng) * contrR, contrY, sin(spinAng) * contrR);

            // Interpolate through phases
            vec3 sPos;
            if (contrP > 0.0) {
                sPos = contrPos;
            } else {
                sPos = mix(birthPos, ringPos, descP);
            }

            // Brighter and whiter as contracting
            vec3 sCol = mix(sunColorT45(j), vec3(0.90, 0.96, 1.0), contrP * 0.65);

            // Small sun surface: max() not += to prevent 16-sun stacking
            float tHitS = raySphere(startP, dir, sPos, T45_SMALL_R);
            if (tHitS > 0.0 && tHitS < sceneD) {
                vec3  hitS  = startP + dir * tHitS;
                vec3  normS = normalize(hitS - sPos);
                float ndvS  = max(0.0, dot(normS, -dir));
                float litS  = ndvS * ndvS * 0.40 + (1.0 - ndvS) * (1.0 - ndvS) * 0.16 + 0.10;
                vec3 surfCol = mix(sunSurfaceColor(normS, lt), vec3(0.88, 0.94, 1.0), contrP * 0.6);
                addLight = max(addLight, surfCol * litS * smallFade);
            }
            // Glow: additive but very tight
            float tCAS = dot(sPos - startP, dir);
            if (tCAS > 0.01) {
                vec3  caS  = startP + dir * min(tCAS, sceneD);
                float dCAS = length(caS - sPos);
                float glowS = exp(-(dCAS*dCAS) / (T45_SMALL_R*T45_SMALL_R*2.0)) * smallFade;
                addLight += sCol * glowS * 0.05;
            }
        }
    }
}
