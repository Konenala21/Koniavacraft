#version 150
#define STEPS    300
#define MIN_DIST 0.001
#define MAX_DIST 100.0

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform mat4  InvProjMat;
uniform mat4  InvViewMat;
uniform vec3  CameraPosition;
uniform vec3  BlockPosition;
uniform float iTime;

const vec3 blue   = vec3(0.3, 0.5, 1.0);
const vec3 bright = vec3(0.7, 0.9, 1.0);

in  vec2 texCoord;
out vec4 fragColor;

vec3 worldPos(vec3 point) {
    vec3 ndc = point * 2.0 - 1.0;
    vec4 hp  = InvProjMat * vec4(ndc, 1.0);
    vec3 vp  = hp.xyz / hp.w;
    return (InvViewMat * vec4(vp, 1.0)).xyz + CameraPosition;
}

float smooth_min(float a, float b, float k) {
    float diff = a - b;
    return 0.5 * (a + b - sqrt(diff * diff + k * k * k));
}

float sDist(vec3 p) {
    float mergeP = clamp(iTime / 3.0, 0.0, 1.0);
    float spread = (1.0 - mergeP * mergeP) * 3.5;
    float r      = 0.5 + mergeP * 0.8;
    const float PI = 3.14159265;

    float result = length(p) - (0.4 + mergeP * 1.2);
    result = smooth_min(result, length(p - vec3( spread, 0.0,    0.0   )) - r, 1.2);
    result = smooth_min(result, length(p - vec3(-spread, 0.0,    0.0   )) - r, 1.2);
    result = smooth_min(result, length(p - vec3( spread * 0.5,  0.0,  spread * 0.866)) - r, 1.2);
    result = smooth_min(result, length(p - vec3(-spread * 0.5,  0.0,  spread * 0.866)) - r, 1.2);
    result = smooth_min(result, length(p - vec3( spread * 0.5,  0.0, -spread * 0.866)) - r, 1.2);
    result = smooth_min(result, length(p - vec3(-spread * 0.5,  0.0, -spread * 0.866)) - r, 1.2);
    return result;
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
    float fade = 1.0 - smoothstep(5.5, 8.0, iTime);
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
    float fade      = 1.0 - smoothstep(5.5, 8.0, iTime);
    vec3  col       = mix(blue, bright, smoothstep(2.0, 8.0, hit.y));
    float threshold  = step(sDist(hitP), MIN_DIST * 2.0);
    threshold       *= step(distance(start_point, hitP), distance(start_point, end_point));
    threshold       *= fade;

    fragColor = vec4(mix(original * (1.0 + shockwave(end_point) * blue * 0.5), col, threshold), 1.0);
}
