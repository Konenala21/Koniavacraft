// ── Stage: ritual failure explosion shockwave ─────────────────────────────────
// Includes: altar/common_explosion.glsl → altar/fx_shockwave.glsl

void main() {
    float depth  = texture(DepthSampler, texCoord).r;
    vec3  wPos   = worldPos(vec3(texCoord, depth));

    vec3  startP = CameraPosition - BlockPosition;
    vec3  endP   = wPos - BlockPosition;
    float sceneD = max(length(endP), 0.01);
    vec3  dir    = endP / sceneD;

    vec3 addLight = vec3(0.0);
    fxAltarShockwave(startP, dir, sceneD, addLight);

    float lum = max(addLight.r, max(addLight.g, addLight.b));
    if (lum < 0.002) discard;
    fragColor = vec4(addLight, 1.0);
}
