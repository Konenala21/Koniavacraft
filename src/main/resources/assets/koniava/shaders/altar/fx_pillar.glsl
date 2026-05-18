// ── Altar light pillar ────────────────────────────────────────────────────────
// Adapted from mana_strike/fx_pillar.glsl.
// Pillar intensity is driven by PillarAlpha + PillarRadius uniforms.

void fxAltarPillar(vec3 startP, vec3 endP, vec3 dir, float sceneD, inout vec3 addLight) {
    if (PillarAlpha <= 0.001) return;
    float r2 = PillarRadius * PillarRadius;

    // ── Geometry glow (illuminates nearby scene geometry) ────────────────────
    float dAxis = length(endP.xz);
    float ht    = endP.y;
    float yFade = smoothstep(-1.0, 3.0, ht) * smoothstep(65.0, 45.0, ht);
    addLight += C_CORE   * (0.20 / (dAxis*dAxis + r2 * 6.0)) * yFade * PillarAlpha;
    addLight += C_BRIGHT * (0.07 / (dAxis*dAxis + r2))        * yFade * PillarAlpha;
    addLight += vec3(1.0) * (0.02 / (dAxis*dAxis + r2 * 0.2)) * yFade * PillarAlpha;

    // ── Ray-to-axis closest approach (sky visibility) ────────────────────────
    float denom = dot(dir.xz, dir.xz);
    if (denom > 0.0001) {
        float tCA = -dot(startP.xz, dir.xz) / denom;
        if (tCA > 0.01 && tCA < sceneD) {
            vec3  ca   = startP + dir * tCA;
            float htCA = ca.y;
            float dCA  = length(ca.xz);
            float yCA  = smoothstep(-1.0, 3.0, htCA) * smoothstep(65.0, 45.0, htCA);
            addLight += C_CORE   * (0.26 / (dCA*dCA + r2 * 6.0)) * yCA * PillarAlpha;
            addLight += C_BRIGHT * (0.10 / (dCA*dCA + r2))        * yCA * PillarAlpha;
            addLight += vec3(1.0) * (0.016/ (dCA*dCA + 0.006))    * yCA * PillarAlpha;
        }
    }
}
