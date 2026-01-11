package com.github.nalamodikk.particle.render.shader.vertex;

import com.github.nalamodikk.particle.render.shader.data.CooVertexFormat;
import com.github.nalamodikk.particle.render.shader.data.VertexData;
import org.lwjgl.opengl.GL33;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL33.*;

public class DynamicVertexBuffer {
    private int vao = 0;
    private int vbo = 0;
    private int lastVAO = 0;
    private CooVertexFormat currentVertexFormat = CooVertexFormat.POINT_FORMAT;
    private int drawMode = GL_TRIANGLES;
    private final List<VertexData> vertices = new ArrayList<>();
    private boolean dirty = false;
    private int uploadedProgram = 0;

    public void init() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        use();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        reset();
    }

    public void use() {
        lastVAO = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        glBindVertexArray(vao);
    }

    public void reset() {
        glBindVertexArray(lastVAO);
    }

    public void setVertices(List<VertexData> vertices, CooVertexFormat format) {
        this.vertices.clear();
        this.vertices.addAll(vertices);
        this.currentVertexFormat = format;
        this.dirty = true;
    }

    public void uploadVertexes() {
        int programNow = glGetInteger(GL_CURRENT_PROGRAM);
        boolean updateProgram = programNow != uploadedProgram;
        
        if (dirty || updateProgram) {
            int currentVAO = glGetInteger(GL_VERTEX_ARRAY_BINDING);
            boolean needReset = false;
            if (currentVAO != vao) {
                use();
                needReset = true;
            }
            
            upload();
            
            if (needReset) {
                reset();
            }
            dirty = false;
        }
        
        if (updateProgram) {
            uploadedProgram = programNow;
        }
    }

    private void upload() {
        float[] data = vertexToData();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_DYNAMIC_DRAW);
        
        int stride = currentVertexFormat.getElementsPerVertex() * Float.BYTES;
        
        // Position (Location 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        
        if (currentVertexFormat == CooVertexFormat.POINT_COLOR_TEXTURE_UV_FORMAT) {
            // Color (Location 1)
            glVertexAttribPointer(1, 4, GL_FLOAT, false, stride, 3L * Float.BYTES);
            glEnableVertexAttribArray(1);
            // UV (Location 2)
            glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 7L * Float.BYTES);
            glEnableVertexAttribArray(2);
        } else if (currentVertexFormat == CooVertexFormat.POINT_COLOR_FORMAT) {
            glVertexAttribPointer(1, 4, GL_FLOAT, false, stride, 3L * Float.BYTES);
            glEnableVertexAttribArray(1);
        } else if (currentVertexFormat == CooVertexFormat.POINT_TEXTURE_UV_FORMAT) {
            glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3L * Float.BYTES);
            glEnableVertexAttribArray(1);
        }
    }

    public void draw() {
        if (vertices.isEmpty()) return;
        use();
        uploadVertexes();
        glDrawArrays(drawMode, 0, vertices.size());
        reset();
    }

    public void release() {
        if (vao != 0) glDeleteVertexArrays(vao);
        if (vbo != 0) glDeleteBuffers(vbo);
    }

    private float[] vertexToData() {
        int count = vertices.size();
        if (count <= 0) return new float[0];
        
        int elements = currentVertexFormat.getElementsPerVertex();
        float[] res = new float[count * elements];
        
        for (int i = 0; i < count; i++) {
            VertexData vertex = vertices.get(i);
            int base = i * elements;
            
            res[base] = vertex.pos.x;
            res[base + 1] = vertex.pos.y;
            res[base + 2] = vertex.pos.z;
            
            switch (currentVertexFormat) {
                case POINT_COLOR_FORMAT:
                    res[base + 3] = vertex.color.x;
                    res[base + 4] = vertex.color.y;
                    res[base + 5] = vertex.color.z;
                    res[base + 6] = vertex.color.w;
                    break;
                case POINT_TEXTURE_UV_FORMAT:
                    res[base + 3] = vertex.uv.x;
                    res[base + 4] = vertex.uv.y;
                    break;
                case POINT_COLOR_TEXTURE_UV_FORMAT:
                    res[base + 3] = vertex.color.x;
                    res[base + 4] = vertex.color.y;
                    res[base + 5] = vertex.color.z;
                    res[base + 6] = vertex.color.w;
                    res[base + 7] = vertex.uv.x;
                    res[base + 8] = vertex.uv.y;
                    break;
                default:
                    break;
            }
        }
        return res;
    }
    
    public void setDrawMode(int mode) {
        this.drawMode = mode;
    }
}
