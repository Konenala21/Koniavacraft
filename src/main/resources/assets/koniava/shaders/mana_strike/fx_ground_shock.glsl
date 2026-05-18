// ── Section D：地面衝擊波環 ───────────────────────────────────────────────────
// 7 波地面擴散環，從 T_BURST_END - T_SHOCK_OFF 開始

void fxGroundShock(
    vec3 startP, vec3 endP, vec3 dir, float sceneD,
    float lt,
    inout vec3 addLight
) {
    float shockT = max(lt - (T_BURST_END - T_SHOCK_OFF), 0.0);
    if (shockT <= 0.0 || abs(dir.y) <= 0.001) return;

    float tG = -startP.y / dir.y;
    if (tG <= 0.05 || tG >= sceneD) return;

    float hitR = length((startP + dir * tG).xz);

    for (int w = 0; w < 7; w++) {
        float wf    = float(w);
        float wStart= wf * 0.75 + fract(wf * 0.6180339) * 0.45;
        float wt    = shockT - wStart;
        if (wt <= 0.0) continue;
        float speed = 7.0 + wf * 0.5;
        float maxR  = 4.5 + wf * 4.0;
        float waveR = wt * speed;
        if (waveR > maxR + 1.8) continue;
        float thick = 0.26 + wf * 0.042;
        float edgeF = smoothstep(maxR + thick, maxR * 0.28, waveR);
        float s     = smoothstep(thick, 0.0, abs(hitR - waveR)) * edgeF * iAlpha;
        if (s < 0.003) continue;
        float lead  = smoothstep(-thick * 0.6, thick * 0.32, hitR - waveR);
        addLight   += mix(C_CORE, C_WAVE, lead * 0.80) * s * (0.45 + wf * 0.055);
    }
}
