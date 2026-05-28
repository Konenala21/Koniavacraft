#version 150

in vec2 texCoord;
in vec4 vertexColor;

uniform float GameTime;

out vec4 fragColor;

// 鋸齒裂縫 shader：不對稱、有分叉感，像玻璃裂痕不是橢圓
// 用 hash noise 製造左右不對稱的寬度 + 中線歪斜 + 上下不規則收尖

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

    // 雙段 taper：主體（中央寬）+ 細裂痕延伸（上下細細的尾巴）
    float bodyTaper = 1.0 - smoothstep(0.55, 0.80, ay);          // 主體：中央滿、0.55→0.80 收掉
    float tailTaper = (1.0 - smoothstep(0.80, 1.0, ay)) * 0.18;  // 尾巴：細細一條（18% 寬度），延伸到邊緣
    float taper = max(bodyTaper, tailTaper);
    if (taper <= 0.0) discard;

    // GameTime [0,1) 一天循環，乘大讓裂縫晃動有感
    float t = GameTime * 600.0;

    // 中線歪斜（zigzag + 時間漂移 → 整條裂縫慢慢扭動）
    float center = noise1D(y * 4.5 + 2.1 + t * 0.4) * 0.55 - 0.275;
    center += noise1D(y * 18.0 + t * 0.8) * 0.10 - 0.05;

    // 左右各自的寬度（時間 phase 不同 → 兩邊獨立呼吸）
    float baseW = 0.08;
    float bulgeL = noise1D(y * 6.0 + 11.0 + t * 0.5) * 0.32;
    float bulgeR = noise1D(y * 6.0 + 27.0 + t * 0.6) * 0.32;
    float detailL = noise1D(y * 20.0 + 5.0  + t * 1.2) * 0.10;
    float detailR = noise1D(y * 20.0 + 33.0 + t * 1.3) * 0.10;
    float leftW  = (baseW + bulgeL + detailL) * taper;
    float rightW = (baseW + bulgeR + detailR) * taper;

    float xRel = uv.x - center;
    float dist;
    if (xRel > 0.0) {
        if (xRel > rightW) discard;
        dist = xRel / rightW;
    } else {
        if (-xRel > leftW) discard;
        dist = -xRel / leftW;
    }

    // 邊緣有點軟（不全硬切，但比 lens 銳很多）
    float alpha = 1.0 - smoothstep(0.55, 1.0, dist);
    fragColor = vec4(vertexColor.rgb, alpha * vertexColor.a);
}
