// ── Stage: T4-T5 full upgrade animation ───────────────────────────────────────────────────
// Assembled from:
//   altar/common.glsl      (uniforms, worldPos, raySphere, pillar/sun helpers)
//   altar/fx_orbs_t45.glsl  (4 big suns + 16 small suns)
//   altar/fx_ground_t45.glsl (early waves, collapse wave, contraction waves)
//   altar/fx_pillars_t45.glsl (corner pillars + skyward pillar)
//   this file              (main)
//
// iTime = elapsed seconds since T4-T5 animation started (tick / 20)

void main() {
    float depth = texture(DepthSampler, texCoord).r;

    vec3  startP = CameraPosition - BlockPosition;
    vec3  wPos   = worldPos(vec3(texCoord, depth));
    vec3  endP   = wPos - BlockPosition;
    float sceneD = max(length(endP), 0.01);
    vec3  dir    = endP / sceneD;

    vec3 addLight = vec3(0.0);

    fxCoreT45(startP, endP, dir, sceneD, iTime, addLight);
    fxOrbsT45(startP, endP, dir, sceneD, iTime, addLight);
    fxGroundT45(startP, dir, sceneD, iTime, addLight);
    fxPillarsT45(startP, endP, dir, sceneD, iTime, addLight);

    // HDR tonemap: compress bright additive light, cap at 1.5× original
    vec3 toned = addLight / (addLight + vec3(0.55)) * 1.35;

    float lum = max(toned.r, max(toned.g, toned.b));
    if (lum < 0.002) discard;

    fragColor = vec4(toned, 1.0);
}
