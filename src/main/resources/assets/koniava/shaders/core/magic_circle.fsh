#version 150

// 數學常數
#define PI 3.14159265359
#define TWO_PI 6.28318530718

// 輸入變數 (從 Vertex Shader 傳入)
in vec4 vertexColor;
in vec2 screenPos;

// 輸出變數
out vec4 fragColor;

// Uniform 變數 (從 Java 傳入)
uniform float GameTime;
uniform float CircleRadius;
uniform int RingCount;
uniform float RotationSpeed;
uniform float Complexity;
uniform float Brightness;
uniform vec3 InnerColor;
uniform vec3 OuterColor;

// 平滑步驟函數
float smootherstep(float edge0, float edge1, float x) {
    float t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
    return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
}

// 創建圓環
float createRing(vec2 pos, float radius, float thickness) {
    float dist = length(pos);
    return 1.0 - smoothstep(radius - thickness * 0.5, radius + thickness * 0.5, dist);
}

// 創建多個同心圓環
float createConcentricRings(vec2 pos, float time) {
    float result = 0.0;

    for (int i = 0; i < RingCount; i++) {
        float fi = float(i);
        float ringRadius = 0.2 + fi * 0.25;
        float thickness = 0.03 + sin(time * 2.0 + fi) * 0.01;

        // 每個環獨立旋轉
        float rotation = time * RotationSpeed * (1.0 + fi * 0.3);
        float cosR = cos(rotation);
        float sinR = sin(rotation);
        vec2 rotatedPos = vec2(
        pos.x * cosR - pos.y * sinR,
        pos.x * sinR + pos.y * cosR
        );

        float ring = createRing(rotatedPos, ringRadius, thickness);

        // 添加脈動效果
        float pulse = 0.8 + 0.2 * sin(time * 3.0 + fi * 0.5);
        result += ring * pulse;
    }

    return clamp(result, 0.0, 1.0);
}

// 創建符文圖案
float createRunePattern(vec2 pos, float time) {
    float dist = length(pos);
    float angle = atan(pos.y, pos.x);

    float runeIntensity = 0.0;

    // 主要符文
    float mainRunes = sin(angle * Complexity + time * RotationSpeed * 2.0);
    mainRunes = pow(abs(mainRunes), 3.0);

    // 只在特定距離顯示符文
    float runeRadius = smootherstep(0.8, 1.2, dist) - smootherstep(1.2, 1.6, dist);
    runeIntensity += mainRunes * runeRadius;

    // 內圈小符文
    float innerRunes = sin(angle * (Complexity * 0.5) - time * RotationSpeed);
    float innerRadius = smootherstep(0.3, 0.5, dist) - smootherstep(0.5, 0.7, dist);
    runeIntensity += pow(abs(innerRunes), 2.0) * innerRadius * 0.6;

    return clamp(runeIntensity, 0.0, 1.0);
}

// 創建中心發光
float createCenterGlow(vec2 pos, float time) {
    float dist = length(pos);
    float glow = 1.0 / (1.0 + dist * 8.0);

    // 脈動效果
    float pulse = 0.7 + 0.3 * sin(time * 4.0);
    return glow * pulse;
}

// 創建能量波紋
float createEnergyRipples(vec2 pos, float time) {
    float dist = length(pos);

    // 從中心向外擴散的波紋
    float ripple1 = sin(dist * 20.0 - time * 8.0);
    float ripple2 = sin(dist * 15.0 - time * 6.0 + PI * 0.5);
    float ripple3 = sin(dist * 25.0 - time * 10.0 + PI);

    float ripples = (ripple1 + ripple2 + ripple3) / 3.0;

    // 只在特定區域顯示波紋
    float rippleArea = smootherstep(0.1, 0.3, dist) - smootherstep(1.4, 1.8, dist);

    return abs(ripples) * rippleArea * 0.3;
}

// 主函數
void main() {
    vec2 pos = screenPos;
    float time = GameTime * 0.05;

    // 基礎衰減
    float edgeFade = 1.0 - smoothstep(0.8, 1.2, length(pos));

    // 組合各種效果
    float rings = createConcentricRings(pos, time);
    float runes = createRunePattern(pos, time);
    float centerGlow = createCenterGlow(pos, time);
    float ripples = createEnergyRipples(pos, time);

    // 計算最終強度
    float totalIntensity = rings * 0.6 + runes * 0.8 + centerGlow * 0.4 + ripples;
    totalIntensity *= edgeFade * Brightness;

    // 顏色漸變
    float colorMix = clamp(length(pos) * 0.8, 0.0, 1.0);
    vec3 finalColor = mix(InnerColor, OuterColor, colorMix);

    // 添加光暈效果
    finalColor += vec3(0.2, 0.4, 0.8) * centerGlow * 0.3;

    // 最終輸出
    fragColor = vec4(finalColor * totalIntensity, totalIntensity * 0.8);
}