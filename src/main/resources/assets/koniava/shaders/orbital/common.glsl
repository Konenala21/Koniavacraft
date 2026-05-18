#version 150

#define STEPS    800
#define MIN_DIST 0.001
#define MAX_DIST 2500.0

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform mat4  InvProjMat;
uniform mat4  InvViewMat;
uniform vec3  CameraPosition;
uniform vec3  BlockPosition;
uniform float iTime;

in  vec2 texCoord;
out vec4 fragColor;

const vec3 C_BLUE   = vec3(0.14, 0.48, 1.00);
const vec3 C_PURPLE = vec3(0.52, 0.22, 1.00);
const vec3 C_BRIGHT = vec3(0.60, 0.86, 1.00);

const float T_START     = 2.0;
const float T_EXPANSION = 16.0;
const float T_END       = T_START + T_EXPANSION;

// ── 共用數學工具 ─────────────────────────────────────────────────────────────

vec3 worldPos(vec3 point) {
    vec3 ndc     = point * 2.0 - 1.0;
    vec4 homPos  = InvProjMat * vec4(ndc, 1.0);
    vec3 viewPos = homPos.xyz / homPos.w;
    return (InvViewMat * vec4(viewPos, 1.0)).xyz + CameraPosition;
}

float smooth_min(float a, float b, float k) {
    float diff = a - b;
    return 0.5 * (a + b - sqrt(diff * diff + k * k * k));
}

vec2 rotate2(vec2 p, float r) {
    return mat2(cos(r), -sin(r), sin(r), cos(r)) * p;
}

float sdBox2(vec2 p, vec2 s) {
    p = abs(p) - s;
    return length(max(p, vec2(0.0))) + min(max(p.x, p.y), 0.0);
}

// scale 計算（主球半徑，隨 localTime 膨脹收縮）
float computeScale(float localTime) {
    return 1.2 * (T_EXPANSION * 25.0 / (localTime + 0.7) / 32.0
        - 32.0 * (1.0 - pow(clamp(2.0 * localTime / T_EXPANSION - 1.0, 0.0, 10.0), 2.0))) + 2.4;
}

// 衛星與光柱共用的軌道座標計算：回傳旋轉後的 p1, p2 與當前 scale
void computeOrbitalPoints(vec3 p, float localTime,
                           out vec3 p1, out vec3 p2, out float scale) {
    scale = computeScale(localTime);
    float rotation = min(25.0 / (localTime - 25.0) + 1.5, 0.0);
    p.xz = rotate2(p.xz, rotation);
    const float num = 6.28318530718 / 8.0;
    float offset = -pow((localTime - 6.5) / 1.4, 2.0) + 60.0;
    float theta  = atan(p.z, p.x);
    theta = floor(theta / num);
    float c1 = num * (theta + 0.0);
    p1 = mat3(cos(c1), 0.0, -sin(c1), 0.0, 1.0, 0.0, sin(c1), 0.0, cos(c1)) * p;
    float c2 = num * (theta + 1.0);
    p2 = mat3(cos(c2), 0.0, -sin(c2), 0.0, 1.0, 0.0, sin(c2), 0.0, cos(c2)) * p;
    p1.x -= offset;
    p2.x -= offset;
}
