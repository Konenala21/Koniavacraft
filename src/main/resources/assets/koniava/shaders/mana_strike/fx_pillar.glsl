// ── Section F：天空光柱 ───────────────────────────────────────────────────────
// 6 顆太陽收束後從中心向天際射出的光柱

void fxPillar(
    vec3 startP, vec3 endP, vec3 dir, float sceneD,
    float lt,
    inout vec3 addLight
) {
    float pillarStart = T_ORB_START + T_ORB_ACCEL + T_ORB_RISE + T_CONVERGE;
    float pillarAge   = lt - pillarStart;
    if (pillarAge <= 0.0) return;

    float pFade = smoothstep(0.0, 1.0, pillarAge)
                * (1.0 - smoothstep(T_TOTAL - 2.5, T_TOTAL, lt))
                * iAlpha;
    if (pFade <= 0.001) return;

    // 幾何輝光（照亮附近方塊）
    float dAxis = length(endP.xz);
    float ht    = endP.y;
    float yFade = smoothstep(-0.5, 2.5, ht) * smoothstep(85.0, 70.0, ht);
    addLight += C_CORE   * (0.22 / (dAxis*dAxis + 0.55)) * yFade * pFade;
    addLight += C_BRIGHT * (0.07 / (dAxis*dAxis + 0.07)) * yFade * pFade;
    addLight += vec3(1.0) * (0.010/ (dAxis*dAxis + 0.005)) * yFade * pFade;

    // 射線到 Y 軸最近點（對天空可見）— 有深度遮蔽
    float denom = dot(dir.xz, dir.xz);
    if (denom > 0.0001) {
        float tCA = -dot(startP.xz, dir.xz) / denom;
        if (tCA > 0.0 && tCA < sceneD) {
            vec3  ca   = startP + dir * tCA;
            float htCA = ca.y;
            float dCA  = length(ca.xz);
            float yCA  = smoothstep(-0.5, 2.5, htCA) * smoothstep(85.0, 70.0, htCA);
            addLight += C_CORE   * (0.28 / (dCA*dCA + 0.55)) * yCA * pFade;
            addLight += C_BRIGHT * (0.10 / (dCA*dCA + 0.07)) * yCA * pFade;
            addLight += vec3(1.0) * (0.018/ (dCA*dCA + 0.005)) * yCA * pFade;
        }
    }
}
