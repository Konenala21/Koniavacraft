#version 150
#define STEPS    300
#define MIN_DIST 0.001
#define MAX_DIST 200.0

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform mat4  InvProjMat;
uniform mat4  InvViewMat;
uniform vec3  CameraPosition;
uniform vec3  BlockPosition;
uniform float iTime;

const vec3 blue = vec3(0.2, 0.6, 1.0);

in  vec2 texCoord;
out vec4 fragColor;

vec3 worldPos(vec3 point) {
    vec3 ndc = point * 2.0 - 1.0;
    vec4 hp  = InvProjMat * vec4(ndc, 1.0);
    vec3 vp  = hp.xyz / hp.w;
    return (InvViewMat * vec4(vp, 1.0)).xyz + CameraPosition;
}

vec2 rot2(vec2 p, float a) {
    return mat2(cos(a), -sin(a), sin(a), cos(a)) * p;
}

float sdTorus(vec3 p, float R, float r) {
    return length(vec2(length(p.xz) - R, p.y)) - r;
}

float sDist(vec3 p) {
    float appear = clamp(iTime / 1.0, 0.0, 1.0);
    vec2 r1 = rot2(p.xz,  iTime * 1.2);
    vec2 r2 = rot2(p.xz, -iTime * 0.8);
    vec2 r3 = rot2(p.xz,  iTime * 2.0);
    float t1 = sdTorus(vec3(r1.x, p.y,       r1.y), 4.0 * appear, 0.18);
    float t2 = sdTorus(vec3(r2.x, p.y + 1.5, r2.y), 3.0 * appear, 0.13);
    float t3 = sdTorus(vec3(r3.x, p.y - 1.5, r3.y), 5.5 * appear, 0.13);
    return min(min(t1, t2), t3);
}

vec2 raycast(vec3 point, vec3 dir) {
    float traveled   = 0.0;
    int   close_steps = 0;
    for (int i = 0; i < STEPS; i++) {
        float safe = sDist(point);
        if (safe <= MIN_DIST || traveled >= MAX_DIST) break;
        traveled += safe;
        point    += dir * safe;
        if (safe <= 0.01) close_steps++;
    }
    return vec2(traveled, float(close_steps));
}

float shockwave(vec3 point) {
    float dist = abs(sDist(point)) + 0.1;
    float fade = 1.0 - smoothstep(6.0, 8.0, iTime);
    return 5.0 / (dist * dist) * fade;
}

void main() {
    vec3  original    = texture(DiffuseSampler, texCoord).rgb;
    float depth       = texture(DepthSampler, texCoord).r;
    vec3  start_point = worldPos(vec3(texCoord, 0.0)) - BlockPosition;
    vec3  end_point   = worldPos(vec3(texCoord, depth)) - BlockPosition;
    vec3  dir         = normalize(end_point - start_point);

    vec2  hit       = raycast(start_point, dir);
    vec3  hitP      = start_point + dir * hit.x;
    float fade      = 1.0 - smoothstep(6.0, 8.0, iTime);
    vec3  col       = mix(blue, vec3(0.8, 0.95, 1.0), smoothstep(2.0, 8.0, hit.y));
    float threshold  = step(sDist(hitP), MIN_DIST * 2.0);
    threshold       *= step(distance(start_point, hitP), distance(start_point, end_point));
    threshold       *= fade;

    fragColor = vec4(mix(original * (1.0 + shockwave(end_point) * blue), col, threshold), 1.0);
}
