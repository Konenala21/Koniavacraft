// ── T4-T5 ground effects ─────────────────────────────────────────────────────────────────
// Section A: early expansion waves (0 → 6.5s) with optional XZ twist
// Section B: inward collapse wave (10 → 14s, visible as screen returns)
// Section C: contraction ground waves (25.5 → 29s)

const float T45G_EARLY_END      = 6.5;
const float T45G_COLLAPSE_START = 10.0;
const float T45G_COLLAPSE_END   = 14.0;
const float T45G_CSHOCK_START   = 25.5;
const float T45G_CSHOCK_END     = 29.0;

void fxGroundT45(vec3 startP, vec3 dir, float sceneD, float lt, inout vec3 addLight) {
    if (abs(dir.y) <= 0.001) return;

    float tG = (RING_Y - startP.y) / dir.y;
    if (tG <= 0.05 || tG >= sceneD) return;

    // ── A: early expansion waves (with a subtle XZ twist) ────────────────────
    if (lt < T45G_EARLY_END) {
        float twistA  = sin(clamp(lt / T45G_EARLY_END, 0.0, 1.0) * PI) * 0.45;
        vec2  hitXZ   = (startP + dir * tG).xz;
        if (abs(twistA) > 0.001) hitXZ = rotate2(hitXZ, twistA);
        float hitR    = length(hitXZ);

        for (int w = 0; w < 5; w++) {
            float wAge = lt - float(w) * 1.3;
            if (wAge <= 0.0 || wAge > 1.3) continue;
            float p      = wAge / 1.3;
            float radius = p * 18.0;
            float alpha  = sin(p * PI) * 0.85;
            float thick  = 0.20 + (1.0 - p) * 0.30;
            float s      = smoothstep(thick, 0.0, abs(hitR - radius)) * alpha;
            if (s < 0.003) continue;
            float lead   = smoothstep(-thick * 0.5, thick * 0.3, hitR - radius);
            addLight    += mix(C_CORE, C_WAVE, lead * 0.8) * s * 1.5;
            // soft inner echo
            float innerS = smoothstep(thick * 0.45, 0.0, abs(hitR - radius * 0.85)) * alpha * 0.65;
            addLight    += C_BRIGHT * innerS;
        }
        return;
    }

    float hitR_raw = length((startP + dir * tG).xz);

    // ── B: inward collapse wave (3 waves moving inward, 10-14s) ──────────────
    if (lt >= T45G_COLLAPSE_START && lt < T45G_COLLAPSE_END) {
        float cAge  = lt - T45G_COLLAPSE_START;
        float cDur  = T45G_COLLAPSE_END - T45G_COLLAPSE_START;
        float alpha = sin(clamp(cAge / cDur, 0.0, 1.0) * PI) * 0.65;

        for (int w = 0; w < 3; w++) {
            float startR = 20.0 - float(w) * 3.5;
            float wR     = startR - cAge * 4.5;
            if (wR < 0.5) continue;
            float thick  = 0.35;
            float s      = smoothstep(thick, 0.0, abs(hitR_raw - wR)) * alpha;
            if (s < 0.003) continue;
            addLight += mix(C_WAVE, C_CORE, 0.55) * s * 0.65;
        }
    }

    // ── C: contraction ground waves (5 waves, 25.5-29s) ─────────────────────
    if (lt >= T45G_CSHOCK_START && lt < T45G_CSHOCK_END) {
        float cAge = lt - T45G_CSHOCK_START;

        for (int w = 0; w < 5; w++) {
            float wAge = cAge - float(w) * 0.55;
            if (wAge <= 0.0) continue;
            float radius = wAge * 9.0;
            if (radius > 24.0) continue;
            float alpha  = max(0.0, 1.0 - radius / 24.0) * 0.75;
            float thick  = 0.28;
            float s      = smoothstep(thick, 0.0, abs(hitR_raw - radius)) * alpha;
            if (s < 0.003) continue;
            float lead   = smoothstep(-thick * 0.5, thick * 0.3, hitR_raw - radius);
            addLight    += mix(C_CORE, C_BRIGHT, lead * 0.7) * s * 0.95;
        }
    }
}
