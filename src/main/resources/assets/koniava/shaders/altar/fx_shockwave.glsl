// ── Altar ground shockwave rings ─────────────────────────────────────────────
// Adapted from mana_strike/fx_ground_shock.glsl.
// Wave data is driven by Waves[5] uniforms instead of timing constants.

void fxAltarShockwave(vec3 startP, vec3 dir, float sceneD, inout vec3 addLight) {
    if (abs(dir.y) <= 0.001) return;

    // Intersect ray with the ring plane (RING_Y relative to block top)
    float tG = (RING_Y - startP.y) / dir.y;
    if (tG <= 0.05 || tG >= sceneD) return;

    float hitR = length((startP + dir * tG).xz);

    for (int w = 0; w < 5; w++) {
        float radius    = Waves[w].x;
        float alpha     = Waves[w].y;
        float thickness = Waves[w].z;
        if (alpha <= 0.001 || radius <= 0.0) continue;

        float s = smoothstep(thickness, 0.0, abs(hitR - radius)) * alpha;
        if (s < 0.003) continue;

        // Leading edge highlight (brighter at ring front)
        float lead = smoothstep(-thickness * 0.5, thickness * 0.3, hitR - radius);
        addLight += mix(C_CORE, C_WAVE, lead * 0.8) * s;

        // Inner echo ring (softer, 85% radius)
        float innerR  = radius * 0.85;
        float innerS  = smoothstep(thickness * 0.45, 0.0, abs(hitR - innerR)) * alpha * 0.55;
        addLight += C_BRIGHT * innerS;
    }
}
