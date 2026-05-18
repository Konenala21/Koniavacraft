#version 150
#define STEPS    300
#define MIN_DIST 0.001
#define MAX_DIST 500.0

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform mat4  InvProjMat;
uniform mat4  InvViewMat;
uniform vec3  CameraPosition;
uniform vec3  BlockPosition;
uniform float iTime;

const vec3 blue = vec3(0.1, 0.5, 1.0);

in  vec2 texCoord;
out vec4 fragColor;

vec3 worldPos(vec3 point) {
    vec3 ndc = point * 2.0 - 1.0;
    vec4 hp  = InvProjMat * vec4(ndc, 1.0);
    vec3 vp  = hp.xyz / hp.w;
    return (InvViewMat * vec4(vp, 1.0)).xyz + CameraPosition;
}

float sdCappedCylinder(vec3 p, float h, float r) {
    vec2 d = abs(vec2(length(p.xz), p.y - h * 0.5)) - vec2(r, h * 0.5);
    return min(max(d.x, d.y), 0.0) + length(max(d, vec2(0.0)));
}

float sDist(vec3 p) {
    float riseH = min(iTime * 8.0, 30.0);
    float outer = sdCappedCylinder(p, riseH, 1.2);
    float inner = sdCappedCylinder(p, riseH, 0.8);
    float shell = max(outer, -inner);
    float pulse = 0.1 * abs(sin(p.y * 1.5 - iTime * 6.0))
                * step(0.0, p.y) * step(p.y, riseH);
    return shell - pulse;
}

vec2 raycast(vec3 point, vec3 dir) {
    float traveled   = 0.0;
    int   close_steps = 0;
    for (int i = 0; i < STEPS; i++) {
        float safe = sDist(point);
        if (safe <= MIN_DIST || traveled >= MAX_DIST) break;
        traveled += max(safe * 0.5, 0.01);
        point    += dir * max(safe * 0.5, 0.01);
        if (safe <= 0.02) close_steps++;
    }
    return vec2(traveled, float(close_steps));
}

float shockwave(vec3 point) {
    float dist = abs(sDist(point)) + 0.1;
    float fade = 1.0 - smoothstep(7.0, 10.0, iTime);
    return 4.0 / (dist * dist) * fade;
}

void main() {
    vec3  original    = texture(DiffuseSampler, texCoord).rgb;
    float depth       = texture(DepthSampler, texCoord).r;
    vec3  start_point = worldPos(vec3(texCoord, 0.0)) - BlockPosition;
    vec3  end_point   = worldPos(vec3(texCoord, depth)) - BlockPosition;
    vec3  dir         = normalize(end_point - start_point);

    vec2  hit        = raycast(start_point, dir);
    vec3  hitP       = start_point + dir * hit.x;
    float fade       = 1.0 - smoothstep(7.0, 10.0, iTime);
    float heightCol  = clamp(hitP.y / max(min(iTime * 8.0, 30.0), 0.001), 0.0, 1.0);
    vec3  col        = mix(blue, vec3(0.6, 0.9, 1.0), heightCol);
    col              = mix(col, vec3(1.0), smoothstep(4.0, 8.0, hit.y) * 0.5);
    float threshold   = step(sDist(hitP), MIN_DIST * 2.0);
    threshold        *= step(distance(start_point, hitP), distance(start_point, end_point));
    threshold        *= fade;

    fragColor = vec4(mix(original * (1.0 + shockwave(end_point) * blue), col, threshold), 1.0);
}
