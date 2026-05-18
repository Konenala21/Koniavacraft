// ── Stage: T6 climax animation ────────────────────────────────────────────────────────────
// Assembled from:
//   altar/common.glsl     (uniforms, worldPos, helpers)
//   altar/fx_t6_climax.glsl (ground waves, ball suns, sky waves, multi-pillars)
//   this file             (main)
//
// iTime = elapsed seconds since T6 climax started = (tick - T6_PHASE_OFFSET) / 20

void main() {
    float depth  = texture(DepthSampler, texCoord).r;
    vec3  startP = CameraPosition - BlockPosition;
    vec3  wPos   = worldPos(vec3(texCoord, depth));
    vec3  endP   = wPos - BlockPosition;
    float sceneD = max(length(endP), 0.01);
    vec3  dir    = endP / sceneD;

    vec3 addLight = vec3(0.0);

    fxGroundWavesT6(startP, dir, sceneD, iTime, addLight);
    fxBallSunsT6(startP, endP, dir, sceneD, iTime, addLight);
    fxSkyWavesT6(startP, dir, sceneD, iTime, addLight);
    fxDrawingBeamsT6(startP, endP, dir, sceneD, iTime, addLight);
    fxPillarsT6(startP, endP, dir, sceneD, iTime, addLight);

    vec3  toned = addLight / (addLight + vec3(0.55)) * 1.35;
    float lum   = max(toned.r, max(toned.g, toned.b));
    if (lum < 0.002) discard;

    fragColor = vec4(toned, 1.0);
}
