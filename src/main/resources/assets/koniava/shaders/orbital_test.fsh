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

const vec3 blue   = vec3(0.14, 0.48, 1.00);
const vec3 purple = vec3(0.52, 0.22, 1.00);
const vec3 bright = vec3(0.60, 0.86, 1.00);

const float startTime     = 2.0;
const float expansionTime = 16.0;
const float endTime       = startTime + expansionTime;
float localTime = 0.0;

in  vec2 texCoord;
out vec4 fragColor;

float smooth_min(float a, float b, float k) {
    float diff = a - b;
    return 0.5 * (a + b - sqrt(diff * diff + k * k * k));
}

vec2 rotate(vec2 p, float r) {
    return mat2(cos(r), -sin(r), sin(r), cos(r)) * p;
}

float sDist(vec3 p) {
    if (iTime < endTime) {
        float scale = 1.2 * (expansionTime * 25.0 / (localTime + 0.7) / 32.0
                    - 32.0 * (1.0 - pow(clamp(2.0 * localTime / expansionTime - 1.0, 0.0, 10.0), 2.0))) + 2.4;

        // 水晶刻面主球（取代原版光滑球）
        float r       = length(p);
        float theta_s = atan(p.z, p.x);
        float phi_s   = acos(clamp(p.y / (r + 0.001), -1.0, 1.0));
        float facet   = 0.22 * abs(sin(8.0 * theta_s)) * abs(sin(5.0 * phi_s));
        float main_sphere = abs(r + scale) - facet * smoothstep(1.0, 5.0, abs(scale));

        float rotation = min(25.0 / (localTime - 25.0) + 1.5, 0.0);
        p.xz = rotate(p.xz, rotation);

        // 8 顆衛星（原版 6 顆）
        const float num = 6.28318530718 / 8.0;
        float offset = -pow((localTime - 6.5) / 1.4, 2.0) + 60.0;
        float theta  = atan(p.z, p.x);
        theta = floor(theta / num);

        float c1 = num * (theta + 0.0);
        vec3 p1 = mat3(cos(c1), 0.0, -sin(c1),  0.0, 1.0, 0.0,  sin(c1), 0.0, cos(c1)) * p;
        float c2 = num * (theta + 1.0);
        vec3 p2 = mat3(cos(c2), 0.0, -sin(c2),  0.0, 1.0, 0.0,  sin(c2), 0.0, cos(c2)) * p;

        p1.x -= offset;
        p2.x -= offset;

        float outer_spheres = min(length(p1) + max(scale, -3.0),
                                  length(p2) + max(scale, -3.0));

        float base   = clamp(pow(localTime - 8.0, 3.0) * 50.0, 0.0, 5000.0);
        float height = min(pow(localTime - 5.5, 3.0) * 50.0, 5000.0);
        float outer_beams = min(
            length(vec3(p1.x, clamp(p1.y, base, height) - p1.y, p1.z)) - 0.2,
            length(vec3(p2.x, clamp(p2.y, base, height) - p2.y, p2.z)) - 0.2
        );

        return smooth_min(main_sphere, smooth_min(outer_spheres, outer_beams, 1.0), 5.0);
    }

    return length(p.xz) + 8.0 / localTime - 24.0;
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

vec3 worldPos(vec3 point) {
    vec3 ndc    = point * 2.0 - 1.0;
    vec4 homPos = InvProjMat * vec4(ndc, 1.0);
    vec3 viewPos = homPos.xyz / homPos.w;
    return (InvViewMat * vec4(viewPos, 1.0)).xyz + CameraPosition;
}

float shockwave(vec3 point) {
    float dist         = sDist(point);
    float default_light = 10.0 / pow(dist, 2.0);

    if (iTime < endTime) {
        float speed_factor = clamp(1.0 - pow(localTime / expansionTime, 2.0), -5.0, 5.0);
        float shock = 0.05 / abs(fract(2.0 * dist / expansionTime - localTime * speed_factor) - 0.5) * 2.0;
        return default_light + shock * smoothstep(dist + 4.0, dist + 14.0, localTime * speed_factor * expansionTime / 2.0);
    }

    float fade_factor = clamp(5.0 / localTime - 0.25, 0.0, 1.0);
    return fade_factor * (default_light
        + 20.0 / abs(dist - 50.0 * (localTime - 0.5)) - 0.3
        + 5.0  / abs(dist - 25.0 * (localTime - 0.5)) - 0.2
    ) + smoothstep(dist - 10.0, dist, 80.0 * (localTime - 2.5));
}

float sdBox(vec2 p, vec2 s) {
    p = abs(p) - s;
    return length(max(p, vec2(0.0))) + min(max(p.x, p.y), 0.0);
}

void main() {
    vec3  original  = texture(DiffuseSampler, texCoord).rgb;
    localTime = iTime - startTime * step(startTime, iTime) - expansionTime * step(endTime, iTime);

    float depth       = texture(DepthSampler, texCoord).r;
    vec3  start_point = worldPos(vec3(texCoord, 0.0)) - BlockPosition;
    vec3  end_point   = worldPos(vec3(texCoord, depth)) - BlockPosition;
    vec3  dir         = normalize(end_point - start_point);

    // Phase 0: charge-up
    if (iTime < startTime) {
        end_point.xz = rotate(end_point.xz, pow(localTime / 2.0, 4.0));
        float dist = max(
            max(length(end_point.xz) - 24.0, -(length(end_point.xz) - 12.0 * (localTime - 1.7))),
            -min(sdBox(end_point.xz, vec2(0.0, 24.0)), sdBox(end_point.xz, vec2(24.0, 0.0)))
        );
        vec3 col = original + 0.2 / pow(dist, 2.0) * blue * step(length(end_point), localTime * 20.0);
        col = mix(col, vec3(0.0), pow(max(localTime - 3.0, 0.0), 2.0));
        fragColor = vec4(col, 1.0);
        return;
    }

    vec2 hit_result = raycast(start_point, dir);
    vec3 hit_point  = start_point + dir * hit_result.x;

    // 深藍→紫漸層，表面越凹凸越偏紫
    float surfaceP = smoothstep(5.0, 10.0, hit_result.y);
    vec3 col = mix(blue, purple, abs(sin(3.14159265 * localTime / expansionTime)) * 0.6)
             + vec3(surfaceP) * bright * 0.5;

    float threshold = step(sDist(hit_point), MIN_DIST * 2.0);
    threshold *= step(distance(start_point, hit_point), distance(start_point, end_point));
    threshold *= 1.0 - pow(clamp(iTime / endTime - 1.0, 0.0, 1.0), 2.0);

    // 爆炸時從藍紫轉白
    vec3 shockwave_color = mix(mix(blue, purple, 0.4), vec3(1.0), clamp(iTime / endTime - 1.0, 0.0, 1.0));

    fragColor = vec4(mix(original * shockwave(end_point) * shockwave_color, col, threshold), 1.0);
}
