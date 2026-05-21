#version 150

uniform sampler2D DepthSampler;
uniform mat4  InvProjMat;
uniform mat4  InvViewMat;
uniform vec3  CameraPosition;
uniform vec3  BlockPosition;

uniform vec3  Waves[5];
uniform float PillarAlpha;
uniform float PillarRadius;
uniform float GroundTwistAngle;
uniform float iTime;

in  vec2 texCoord;
out vec4 fragColor;

const float PI     = 3.14159265;
const float RING_Y = -1.8;

// Red/orange explosion palette
const vec3 C_CORE   = vec3(1.00, 0.12, 0.04);
const vec3 C_BRIGHT = vec3(1.00, 0.55, 0.08);
const vec3 C_WAVE   = vec3(1.00, 0.90, 0.25);

vec2 rotate2(vec2 p, float r) {
    return mat2(cos(r), -sin(r), sin(r), cos(r)) * p;
}

float sdBox2(vec2 p, vec2 s) {
    p = abs(p) - s;
    return length(max(p, vec2(0.0))) + min(max(p.x, p.y), 0.0);
}

vec3 worldPos(vec3 uvd) {
    vec3 ndc = uvd * 2.0 - 1.0;
    vec4 hp  = InvProjMat * vec4(ndc, 1.0);
    vec3 vp  = hp.xyz / hp.w;
    return (InvViewMat * vec4(vp, 1.0)).xyz + CameraPosition;
}

float raySphere(vec3 ro, vec3 rd, vec3 center, float radius) {
    vec3  oc = ro - center;
    float b  = dot(oc, rd);
    float c  = dot(oc, oc) - radius * radius;
    float d  = b * b - c;
    if (d < 0.0) return -1.0;
    float t = -b - sqrt(d);
    return (t > 0.0) ? t : -1.0;
}

vec3 sunColorT45(int i) {
    return mix(vec3(0.30, 0.65, 1.00), vec3(0.85, 0.94, 1.00), float(i) / 3.0);
}

vec3 pillarPos45(int i) {
    float sx = (i < 2) ? -3.0 : 3.0;
    float sz = (i == 0 || i == 2) ? -3.0 : 3.0;
    return vec3(sx, -1.0, sz);
}
