// ========================================
// 文件位置: src/main/resources/assets/koniava/shaders/core/magic_circle.vsh
// ========================================
#version 150

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec2 screenPos;

void main() {
	gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
	vertexColor = Color;

	// 將世界坐標轉換為著色器坐標 (-1 到 1)
	screenPos = Position.xz / 2.2; // 2.2 是四邊形大小的一半
}
