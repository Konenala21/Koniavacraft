// ── Section A：地面充能環 ─────────────────────────────────────────────────────
// 地面上由外往內收縮的能量圓環，持續到 T_CHARGE (2s)

void fxChargeRings(
    vec3 startP, vec3 dir, float sceneD,
    float lt,
    inout vec3 addLight
) {
    if (lt >= T_CHARGE || abs(dir.y) <= 0.001) return;
    float cp = lt / T_CHARGE;
    float tG = -startP.y / dir.y;
    if (tG <= 0.05 || tG >= sceneD) return;
    float hr   = length((startP + dir * tG).xz);
    float ring = 0.06 / (abs(mod(hr * 0.45 - (T_CHARGE - lt) * 2.2, 2.2) - 1.1) + 0.05);
    ring *= smoothstep(28.0, 1.5, hr) * cp * iAlpha;
    addLight += C_CORE * ring;
}
