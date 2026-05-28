#version 150

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec2 uv = texCoord * 2.0 - 1.0;
    float dist = length(uv);
    // 用 smoothstep + max 取代 discard，邊緣自然淡出（避免 discard 在 MSAA / 部分 GPU 上產生瑕疵）
    float alpha = max(0.0, 1.0 - dist);
    alpha = alpha * alpha;
    fragColor = vec4(vertexColor.rgb, alpha * vertexColor.a);
}
