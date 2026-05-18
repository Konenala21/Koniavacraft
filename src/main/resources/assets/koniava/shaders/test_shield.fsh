#version 150
#define STEPS    200
#define MIN_DIST 0.001
#define MAX_DIST 200.0

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform mat4  InvProjMat;
uniform mat4  InvViewMat;
uniform vec3  CameraPosition;
uniform vec3  BlockPosition;
uniform float iTime;

const vec3 blue = vec3(0.4, 0.8, 1.0);

in  vec2 texCoord;
out vec4 fragColor;

vec3 worldPos(vec3 point) {
    vec3 ndc = point * 2.0 - 1.0;
    vec4 hp  = InvProjMat * vec4(ndc, 1.0);
    vec3 vp  = hp.xyz / hp.w;
    return (InvViewMat * vec4(vp, 1.0)).xyz + CameraPosition;
}

float sDist(vec3 p) {
    float expandP = clamp(iTime / 3.0, 0.0, 1.0);
    float r       = expandP * expandP * 8.0;
    float shell   = abs(length(p) - r) - 0.25;
    float pulse   = 0.08 * sin(length(p) * 3.0 - iTime * 4.0);
    return shell + pulse;
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
    return 6.0 / (dist * dist) * fade;
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
    vec3  col       = mix(blue * 0.5, blue, smoothstep(2.0, 8.0, hit.y));
    float threshold  = step(sDist(hitP), MIN_DIST * 2.0);
    threshold       *= step(distance(start_point, hitP), distance(start_point, end_point));
    threshold       *= fade;

    fragColor = vec4(mix(original * (1.0 + shockwave(end_point) * blue), col, threshold), 1.0);
}
