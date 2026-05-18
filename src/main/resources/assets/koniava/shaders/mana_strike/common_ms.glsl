#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform mat4  InvProjMat;
uniform mat4  InvViewMat;
uniform vec3  CameraPosition;
uniform vec3  BlockPosition;
uniform float iTime;
uniform float iAlpha;

in  vec2 texCoord;
out vec4 fragColor;

// ── Timing ────────────────────────────────────────────────────────────────────
const float T_CHARGE     = 2.0;
const float T_BURST_END  = 10.0;
const float T_EXPL_START = 7.5;
const float T_EXPL_END   = 10.5;
const float T_SHOCK_OFF  = 1.2;
const float T_ORB_START  = 14.0;
const float T_ORB_ACCEL  = 2.0;
const float T_ORB_RISE   = 1.5;
const float T_CONVERGE   = 0.6;
const float T_TOTAL      = 24.0;
const float PI           = 3.14159265;

// ── Colors ────────────────────────────────────────────────────────────────────
const vec3 C_VOID    = vec3(0.02, 0.04, 0.20);
const vec3 C_CORE    = vec3(0.14, 0.48, 1.00);
const vec3 C_BRIGHT  = vec3(0.60, 0.86, 1.00);
const vec3 C_CRYSTAL = vec3(0.52, 0.22, 1.00);
const vec3 C_PILLAR  = vec3(0.75, 0.90, 1.00);
const vec3 C_WAVE    = vec3(0.88, 0.95, 1.00);

// ── Shared helpers ────────────────────────────────────────────────────────────

vec3 worldPos(vec3 uvd) {
    vec3 ndc = uvd * 2.0 - 1.0;
    vec4 hp  = InvProjMat * vec4(ndc, 1.0);
    vec3 vp  = hp.xyz / hp.w;
    return (InvViewMat * vec4(vp, 1.0)).xyz + CameraPosition;
}

// 解析射線球體相交，回傳距離；miss → -1
float raySphere(vec3 ro, vec3 rd, vec3 center, float radius) {
    vec3  oc   = ro - center;
    float b    = dot(oc, rd);
    float c    = dot(oc, oc) - radius * radius;
    float disc = b * b - c;
    if (disc < 0.0) return -1.0;
    float t = -b - sqrt(disc);
    return (t > 0.0) ? t : -1.0;
}

// 6 顆太陽各自的顏色（藍→亮藍漸層）
vec3 sunColor(int i) {
    float f = float(i) / 5.0;
    return mix(vec3(0.08, 0.30, 1.00), vec3(0.55, 0.90, 1.00), f);
}
