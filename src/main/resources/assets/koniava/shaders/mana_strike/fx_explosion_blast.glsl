// ── Section C1：核爆中央衝擊波閃光 ───────────────────────────────────────────
// 從中心向外膨脹的球形閃光，持續 T_EXPL_START → T_EXPL_END

void fxExplosionBlast(
    vec3 endP, float lt,
    inout vec3 addLight
) {
    float explAge = lt - T_EXPL_START;
    if (explAge <= 0.0 || lt >= T_EXPL_END) return;

    float explDur  = T_EXPL_END - T_EXPL_START;
    float explFade = (1.0 - explAge / explDur) * iAlpha;
    float flashP   = explAge / explDur;

    float blastR = explAge * 15.0;
    float blastD = abs(length(endP) - blastR);
    float blastG = 0.08 / (blastD * blastD + 2.5) * (1.0 - flashP * flashP) * iAlpha;
    addLight += mix(C_BRIGHT, vec3(1.0), 0.6) * blastG;
}
