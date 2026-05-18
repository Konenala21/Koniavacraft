#version 150
uniform sampler2D MagicCircleTex;
uniform vec2  ScreenSize;
uniform float Progress;
uniform float Rotation;
uniform float Alpha;
uniform float CircleSize;
in vec2 vUV;
out vec4 fragColor;

void main() {
    // Aspect-correct UV centered on screen
    vec2 uv = vUV - 0.5;
    uv.x *= ScreenSize.x / ScreenSize.y;

    // Normalized dist: 0 = center, 1 = edge of circle
    float dist = length(uv) / CircleSize;

    // Soft outer crop
    float outerFade = 1.0 - smoothstep(0.90, 1.05, dist);
    if (outerFade <= 0.001) discard;

    // Rotate UV for texture sampling
    float c = cos(Rotation), s = sin(Rotation);
    vec2 rotUV = vec2(c * uv.x - s * uv.y, s * uv.x + c * uv.y);

    // Map to texture UV 0..1 (circle fills texture square)
    vec2 texUV = clamp(rotUV / CircleSize * 0.5 + 0.5, 0.0, 1.0);
    vec4 tex = texture(MagicCircleTex, texUV);

    // Reveal from center outward as Progress grows 0->1
    float soft = 0.06;
    float reveal = 1.0 - smoothstep(Progress - soft, Progress + soft, dist);

    fragColor = tex * vec4(1.0, 1.0, 1.0, Alpha * reveal * outerFade);
}
