#version 150
uniform sampler2D DepthSampler;
uniform mat4  InvProjMat;
uniform mat4  InvViewMat;
uniform vec3  CameraPosition;
uniform vec3  AltarPos;
// Wave params: x=radius (blocks), y=alpha, z=thickness (blocks)
uniform vec3  Waves[3];

in  vec2 texCoord;
out vec4 fragColor;

// Reconstruct world position from UV + depth (matches common.glsl)
vec3 worldPos(vec3 point) {
    vec3 ndc     = point * 2.0 - 1.0;
    vec4 homPos  = InvProjMat * vec4(ndc, 1.0);
    vec3 viewPos = homPos.xyz / homPos.w;
    return (InvViewMat * vec4(viewPos, 1.0)).xyz + CameraPosition;
}

vec4 computeRing(vec3 wPos, vec3 wave) {
    float radius    = wave.x;
    float alpha     = wave.y;
    float thickness = wave.z;
    if (alpha <= 0.001 || radius <= 0.0) return vec4(0.0);

    // XZ distance from altar center
    float dist    = length(wPos.xz - AltarPos.xz);
    float ringDist = abs(dist - radius);
    float ring    = 1.0 - smoothstep(0.0, thickness, ringDist);

    // Fade away from ground plane (altar.y - 1.8)
    float groundY = AltarPos.y - 1.8;
    float yFade   = 1.0 - smoothstep(0.0, 1.8, abs(wPos.y - groundY));

    // Outer glow inner highlight
    vec3 col = mix(vec3(0.39, 0.71, 1.0), vec3(0.78, 0.92, 1.0), ring * ring);
    return vec4(col, ring * alpha * yFade);
}

void main() {
    float depth = texture(DepthSampler, texCoord).r;
    if (depth >= 0.9999) discard;

    vec3 wPos = worldPos(vec3(texCoord, depth));

    vec4 c = computeRing(wPos, Waves[0])
           + computeRing(wPos, Waves[1])
           + computeRing(wPos, Waves[2]);

    if (c.a <= 0.001) discard;
    fragColor = vec4(c.rgb, min(1.0, c.a));
}
