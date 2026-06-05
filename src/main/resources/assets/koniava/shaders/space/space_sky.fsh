#version 430 core

uniform float iTime;
uniform vec2  iResolution;
uniform mat4  InvProjMat;
uniform mat4  InvViewMat;

in  vec2 texCoord;
out vec4 fragColor;

float hash31(vec3 p) {
    p  = fract(p * vec3(443.8975, 397.2973, 491.1871));
    p += dot(p.zxy, p.yxz + 19.19);
    return fract(p.x * p.y * p.z);
}

float starGrid(vec3 d, float sc, float thr, float sz) {
    vec3  g  = floor(d * sc);
    float h  = hash31(g);
    if (h < thr) return 0.0;
    vec3 off = vec3(hash31(g+vec3(1,0,0)), hash31(g+vec3(0,1,0)), hash31(g+vec3(0,0,1)));
    off = off * 0.6 + 0.2;
    float ca = dot(normalize(d), normalize(g + off));
    return smoothstep(1.0 - sz, 1.0, ca) * (h - thr) / (1.0 - thr);
}

void main() {
    vec2 ndc = texCoord * 2.0 - 1.0;
    vec4 vd  = InvProjMat * vec4(ndc, 1.0, 1.0);
    vd.xyz  /= vd.w;
    vec3 dir = normalize((InvViewMat * vec4(normalize(vd.xyz), 0.0)).xyz);

    // 兩層星星（省掉第三層 ~33% hash 開銷）
    float dim  = starGrid(dir,     80.0, 0.997, 0.0008);
    float big  = starGrid(dir.yzx, 45.0, 0.9988, 0.002);

    float bright = dim * 0.7 + big * 1.4;

    // 閃爍只加亮星（共用同一 hash，省一次 hash31 call）
    vec3  bigCell = floor(dir.yzx * 45.0);
    float twinkle = 0.8 + 0.2 * sin(iTime * 2.1 + hash31(bigCell) * 6.28);
    bright = bright - big * 1.4 + big * 1.4 * twinkle;

    // 星色（共用 bigCell hash）
    float cv  = hash31(bigCell + vec3(5.5));
    vec3 tint = mix(vec3(0.88, 0.93, 1.0), vec3(1.0, 0.95, 0.78), cv);

    // 極淡銀河
    float mw = exp(-pow(dir.y / 0.25, 2.0)) * 0.025;
    vec3 mwCol = vec3(0.12, 0.16, 0.28) * mw;

    fragColor = vec4(mwCol + tint * bright, 1.0);
}
