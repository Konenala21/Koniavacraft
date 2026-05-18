// ── Section E1：6 顆太陽誕生 ─────────────────────────────────────────────────
// 從中心向外擴散到軌道半徑，orbAge < T_ORB_ACCEL (2s)

void fxOrbsBirth(
    vec3 startP, vec3 endP, vec3 dir, float sceneD,
    float lt,
    inout vec3 addLight, inout vec3 repCol, inout float repW
) {
    float orbAge = lt - T_ORB_START;
    if (orbAge <= 0.0 || orbAge >= T_ORB_ACCEL) return;

    float orbFade = (1.0 - smoothstep(T_ORB_ACCEL + T_ORB_RISE + T_CONVERGE,
                                       T_ORB_ACCEL + T_ORB_RISE + T_CONVERGE + 0.8,
                                       orbAge)) * iAlpha;
    if (orbFade <= 0.001) return;

    float bestT   = sceneD;
    vec3  bestCol = vec3(0.0);

    for (int i = 0; i < 6; i++) {
        float stag  = float(i) * 0.08;
        float bp    = clamp((orbAge - stag) / (T_ORB_ACCEL * 0.92), 0.0, 1.0);
        float ringR = bp * bp * 6.0;
        float ang   = float(i) * (PI / 3.0);
        vec3  sPos  = vec3(cos(ang) * ringR, 0.4, sin(ang) * ringR);

        float t = raySphere(startP, dir, sPos, 0.45);
        if (t > 0.0 && t < bestT) {
            bestT   = t;
            bestCol = sunColor(i);
        }

        float d2 = dot(endP - sPos, endP - sPos) + 0.08;
        addLight += sunColor(i) * (0.005 / d2) * orbFade;
    }

    if (bestT < sceneD) {
        repCol = bestCol;
        repW   = max(repW, orbFade);
    }
}
