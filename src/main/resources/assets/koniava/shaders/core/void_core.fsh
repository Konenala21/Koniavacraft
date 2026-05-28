#version 150

in vec2 texCoord;
in vec4 vertexColor;

uniform float GameTime;

out vec4 fragColor;

// 黑核：和 void_rift 同形狀的鋸齒裂縫，alpha blend 把後面挖空 = 看穿到虛空
// 注意：noise seed 必須跟 void_rift.fsh 一致才能對齊

float hash(float n) { return fract(sin(n) * 43758.5453); }

float noise1D(float x) {
    float i = floor(x);
    float f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(hash(i), hash(i + 1.0), f);
}

void main() {
    vec2 uv = texCoord * 2.0 - 1.0;
    float y = uv.y;
    float ay = abs(y);

    // 雙段 taper（必須跟 void_rift.fsh 一致）
    float bodyTaper = 1.0 - smoothstep(0.55, 0.80, ay);
    float tailTaper = (1.0 - smoothstep(0.80, 1.0, ay)) * 0.18;
    float taper = max(bodyTaper, tailTaper);
    if (taper <= 0.0) discard;

    // 時間 phase 必須跟 void_rift.fsh 完全一致，才能黑核跟紫光同步晃動
    float t = GameTime * 600.0;

    float center = noise1D(y * 4.5 + 2.1 + t * 0.4) * 0.55 - 0.275;
    center += noise1D(y * 18.0 + t * 0.8) * 0.10 - 0.05;

    float baseW = 0.08;
    float bulgeL = noise1D(y * 6.0 + 11.0 + t * 0.5) * 0.32;
    float bulgeR = noise1D(y * 6.0 + 27.0 + t * 0.6) * 0.32;
    float leftW  = (baseW + bulgeL + noise1D(y * 20.0 + 5.0  + t * 1.2) * 0.10) * taper;
    float rightW = (baseW + bulgeR + noise1D(y * 20.0 + 33.0 + t * 1.3) * 0.10) * taper;

    float xRel = uv.x - center;
    float dist;
    if (xRel > 0.0) {
        if (xRel > rightW) discard;
        dist = xRel / rightW;
    } else {
        if (-xRel > leftW) discard;
        dist = -xRel / leftW;
    }

    // 黑核：中段全黑，外緣稍微淡出避免和外圍紫光形成硬接縫
    float alpha = 1.0 - smoothstep(0.70, 1.0, dist);
    fragColor = vec4(vertexColor.rgb, alpha * vertexColor.a);
}
