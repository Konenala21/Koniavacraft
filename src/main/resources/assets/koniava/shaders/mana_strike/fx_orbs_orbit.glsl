// ── Section E2：6 顆太陽高速旋轉 + 上升 + 收束 ───────────────────────────────
// orbAge >= T_ORB_ACCEL：加速自轉、升高至 15 格、最後向中心收束

void fxOrbsOrbit(
    vec3 startP, vec3 endP, vec3 dir, float sceneD,
    float lt,
    inout vec3 addLight, inout vec3 repCol, inout float repW
) {
    float orbAge = lt - T_ORB_START;
    if (orbAge < T_ORB_ACCEL) return;

    float orbFade = (1.0 - smoothstep(T_ORB_ACCEL + T_ORB_RISE + T_CONVERGE,
                                       T_ORB_ACCEL + T_ORB_RISE + T_CONVERGE + 0.8,
                                       orbAge)) * iAlpha;
    if (orbFade <= 0.001) return;

    float fastAge   = orbAge - T_ORB_ACCEL;
    float baseAngle = 5.0 * fastAge;

    float riseP  = clamp(fastAge / T_ORB_RISE, 0.0, 1.0);
    float height = mix(0.4, 15.0, riseP);

    float convAge = max(fastAge - T_ORB_RISE, 0.0);
    float convP   = clamp(convAge / T_CONVERGE, 0.0, 1.0);
    float orbR    = mix(6.0, 0.0, convP * convP);
    float convFade = 1.0 - convP * convP;

    float bestT   = sceneD;
    vec3  bestCol = vec3(0.0);

    for (int i = 0; i < 6; i++) {
        float ang  = baseAngle + float(i) * (PI / 3.0);
        vec3  sPos = vec3(cos(ang) * orbR, height, sin(ang) * orbR);

        float t = raySphere(startP, dir, sPos, 0.45);
        if (t > 0.0 && t < bestT) {
            bestT   = t;
            bestCol = sunColor(i);
        }

        // 深度遮蔽：orb 在幾何體後方時不加輝光
        float tOrb  = raySphere(startP, dir, sPos, 6.0);
        float occl  = (tOrb < 0.0 || tOrb < sceneD) ? 1.0 : 0.0;
        float d2    = dot(endP - sPos, endP - sPos) + 0.08;
        addLight   += sunColor(i) * (0.005 / d2) * orbFade * convFade * occl;
    }

    if (bestT < sceneD) {
        repCol = bestCol;
        repW   = max(repW, orbFade * convFade);
    }
}
