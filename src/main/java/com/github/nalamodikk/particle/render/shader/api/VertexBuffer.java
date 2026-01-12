package com.github.nalamodikk.particle.render.shader.api;

import com.github.nalamodikk.particle.render.shader.data.CooVertexFormat;
import com.github.nalamodikk.particle.render.shader.data.VertexData;
import java.util.List;

/**
 * VAO/VBO ?鞈芣??鈭
 */
public interface VertexBuffer {
    /**
     * ??蹌?蹇??鞊?
     */
    void uploadVertexes();

    void setVertexes(List<VertexData> vertexes, CooVertexFormat format);

    void draw();

    /**
     * ?豲???vao ??vbo
     */
    void init();

    /**
     * ?秋撒??輯撒??vao ??vbo
     */
    void use();

    /**
     * ????????????     */
    void reset();

    /**
     * ?????
     */
    void release();
}
