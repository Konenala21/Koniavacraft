// ── Magic circle: world-space horizontal disc above the altar ─────────────────────────────
// Assembled after altar/common.glsl (provides DepthSampler, worldPos, BlockPosition, etc.)
// CircleWorldY    = height above BlockPosition.y (blocks)
// CircleWorldRadius = world-space radius (blocks)

uniform sampler2D MagicCircleTex;  // bound to GL_TEXTURE1
uniform float Progress;
uniform float Rotation;
uniform float Alpha;
uniform float CircleWorldY;
uniform float CircleWorldRadius;

void main() {
    float depth  = texture(DepthSampler, texCoord).r;
    vec3  startP = CameraPosition - BlockPosition;
    vec3  wPos   = worldPos(vec3(texCoord, depth));
    vec3  endP   = wPos - BlockPosition;
    float sceneD = max(length(endP), 0.01);
    vec3  dir    = endP / sceneD;

    // Only rays pointing upward reach the sky plane
    if (dir.y <= 0.001) discard;

    // Intersect ray with horizontal disc plane at CircleWorldY
    float tP = (CircleWorldY - startP.y) / dir.y;
    // Discard if behind camera or occluded by scene geometry (with 5% tolerance for depth precision)
    if (tP <= 0.05 || tP >= sceneD * 1.05) discard;

    vec2  hitXZ   = (startP + dir * tP).xz;
    float normDist = length(hitXZ) / CircleWorldRadius;

    // Crop to circle with soft edge
    float outerFade = 1.0 - smoothstep(0.90, 1.05, normDist);
    if (outerFade <= 0.001) discard;

    // Rotate UV for texture sampling
    vec2  uv = hitXZ / CircleWorldRadius;
    float c  = cos(Rotation);
    float s  = sin(Rotation);
    vec2  rotUV = vec2(c * uv.x - s * uv.y, s * uv.x + c * uv.y);

    // Map to texture UV [0, 1]
    vec2 texUV = clamp(rotUV * 0.5 + 0.5, 0.0, 1.0);
    vec4 tex   = texture(MagicCircleTex, texUV);

    // Reveal from center outward as Progress grows 0 → 1
    float soft   = 0.06;
    float reveal = 1.0 - smoothstep(Progress - soft, Progress + soft, normDist);

    fragColor = tex * vec4(1.0, 1.0, 1.0, Alpha * reveal * outerFade);
}
