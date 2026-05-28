#version 150

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

// 紫色撕裂縫 shader：lens 形狀 + 邊緣鋸齒 + 中央 zigzag = 「撕裂」視覺特徵
// 單一 quad 由 fragment 算 alpha → 無 strip seam，無漏渲染黑點
void main() {
    // UV (0,0)~(1,1) → (-1,-1)~(1,1)
    vec2 uv = texCoord * 2.0 - 1.0;

    // Lens 形狀：中央 60% 是全寬大洞、上下各 20% 快速收尖
    // 大洞範圍佔整體高度大半 → 視覺上像「空間真的破開了個口」
    float ay = abs(uv.y);
    float lensW = 1.0 - smoothstep(0.6, 1.0, ay);
    lensW = pow(lensW, 0.4); // 中段更接近 1.0 平緩，邊緣收得更快

    // 邊緣鋸齒：sin(y * 15) 高頻擾動
    float jag = 0.85 + 0.15 * sin(uv.y * 15.0);
    float lensWidth = lensW * jag;

    // 中央 zigzag 位移
    float center = 0.08 * sin(uv.y * 7.0);

    // 距離 lens 中線（依高度變寬度）
    float dist = abs(uv.x - center) / max(lensWidth, 0.001);

    if (dist > 1.0) discard;

    // 二次衰減 alpha
    float alpha = 1.0 - dist;
    alpha = alpha * alpha;

    fragColor = vec4(vertexColor.rgb, alpha * vertexColor.a);
}
