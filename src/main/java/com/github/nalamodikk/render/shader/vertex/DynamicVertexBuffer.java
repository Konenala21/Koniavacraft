package com.github.nalamodikk.render.shader.vertex;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 動態頂點緩衝區
 * 封裝 OpenGL VBO (Vertex Buffer Object) 的操作
 * 支援動態更新頂點數據
 */
public class DynamicVertexBuffer {
    private int vao;
    private int vbo;
    private final int vertexCount;
    private final int vertexSize; // 每個頂點的字節數

    public DynamicVertexBuffer(int vertexCount, int vertexSize) {
        this.vertexCount = vertexCount;
        this.vertexSize = vertexSize;
        init();
    }

    private void init() {
        // 建立 VAO
        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        // 建立 VBO
        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        // 預分配空間
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) vertexCount * vertexSize, GL15.GL_DYNAMIC_DRAW);

        // 預設頂點屬性 (位置: 3 float)
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, vertexSize, 0);
        GL20.glEnableVertexAttribArray(0);

        GL30.glBindVertexArray(0);
    }

    /**
     * 更新緩衝區數據
     */
    public void upload(float[] data) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(data.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(data);
        buffer.flip();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, buffer);
    }

    public void bind() {
        GL30.glBindVertexArray(vao);
    }

    public void unbind() {
        GL30.glBindVertexArray(0);
    }

    public void draw() {
        bind();
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);
        unbind();
    }

    public void release() {
        GL15.glDeleteBuffers(vbo);
        GL30.glDeleteVertexArrays(vao);
    }
}
