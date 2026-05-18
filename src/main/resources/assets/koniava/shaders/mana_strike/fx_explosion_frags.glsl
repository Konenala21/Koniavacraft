// ── Section C2：24 顆碎片飛出 ────────────────────────────────────────────────
// 24 顆實心球從中心向外四散，帶有輝光暈

void fxExplosionFrags(
    vec3 startP, vec3 endP, vec3 dir, float sceneD,
    float lt,
    inout vec3 addLight, inout vec3 repCol, inout float repW
) {
    float explAge = lt - T_EXPL_START;
    if (explAge <= 0.0 || lt >= T_EXPL_END) return;

    float explDur  = T_EXPL_END - T_EXPL_START;
    float explFade = (1.0 - explAge / explDur) * iAlpha;

    float bestFragT   = sceneD;
    vec3  bestFragCol = vec3(0.0);

    for (int i = 0; i < 24; i++) {
        float fi      = float(i);
        float phi_i   = acos(1.0 - 2.0 * (fi + 0.5) / 24.0);
        float theta_i = fi * 2.399963;
        vec3  od = normalize(vec3(sin(phi_i)*cos(theta_i),
                                  cos(phi_i) * 0.4 + 0.15,
                                  sin(phi_i)*sin(theta_i)));
        float speed = 9.0 + fract(fi * 0.618) * 7.0;
        vec3  op    = od * explAge * speed;

        float t = raySphere(startP, dir, op, 0.40);
        if (t > 0.0 && t < bestFragT) {
            bestFragT   = t;
            bestFragCol = mix(C_CORE, C_BRIGHT, fi / 23.0);
        }

        float d2 = dot(endP - op, endP - op) + 0.08;
        addLight += mix(C_CORE, C_BRIGHT, fi / 23.0) * (0.006 / d2) * explFade;
    }

    if (bestFragT < sceneD) {
        repCol = bestFragCol;
        repW   = max(repW, explFade * 0.9);
    }
}
