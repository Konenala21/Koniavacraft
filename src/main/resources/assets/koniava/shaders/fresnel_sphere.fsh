#version 150

uniform sampler2D DiffuseSampler;
uniform vec3 CameraPosition;
uniform vec3 BlockPosition;
uniform mat4 InvProjMat;
uniform mat4 InvViewMat;
uniform float iTime;

out vec4 fragColor;

vec3 screenToWorld(vec2 ndc) {
    vec4 view  = InvProjMat * vec4(ndc, -1.0, 1.0);
    view /= view.w;
    return (InvViewMat * view).xyz + CameraPosition;
}

vec2 sphereHit(vec3 ro, vec3 rd, vec3 center, float r) {
    vec3  oc = ro - center;
    float b  = dot(oc, rd);
    float c  = dot(oc, oc) - r * r;
    float h  = b * b - c;
    if (h < 0.0) return vec2(-1.0);
    h = sqrt(h);
    return vec2(-b - h, -b + h);
}

void main() {
    ivec2 sz  = textureSize(DiffuseSampler, 0);
    vec2  uv  = gl_FragCoord.xy / vec2(sz);
    vec2  ndc = uv * 2.0 - 1.0;

    vec3 ro = CameraPosition;
    vec3 rd = normalize(screenToWorld(ndc) - ro);

    float radius = 1.8 + 0.4 * sin(iTime * 1.5);
    vec2  t      = sphereHit(ro, rd, BlockPosition, radius);
    if (t.x < 0.0) { fragColor = vec4(0.0); return; }

    vec3  hit     = ro + rd * t.x;
    vec3  normal  = normalize(hit - BlockPosition);
    float fresnel = pow(1.0 - max(dot(normal, -rd), 0.0), 4.0);

    float pulse = 0.6 + 0.4 * sin(iTime * 2.5);
    // Colour cycles blue → violet → cyan
    float hue   = sin(iTime * 0.4) * 0.5 + 0.5;
    vec3  col   = mix(vec3(0.1, 0.4, 1.0), vec3(0.9, 0.1, 1.0), hue);

    fragColor = vec4(col * fresnel * pulse, fresnel * 0.95);
}
