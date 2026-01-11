package com.github.nalamodikk.particle.render.shader.api;

import com.github.nalamodikk.particle.render.shader.glsl.GlShader;
import org.joml.*;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL33.*;

public interface CooShaderProgram {
    int getProgram();
    GlShader getVertexShader();
    GlShader getFragmentShader();

    void init();
    void use();
    void reset();
    void release();

    default void useOnContext(Runnable drawMethod) {
        use();
        drawMethod.run();
        reset();
    }

    default void setInt(String key, int value) {
        int loc = getGlLocation(key);
        if (loc != -1) glUniform1i(loc, value);
    }

    default void setBoolean(String key, boolean value) {
        setInt(key, value ? 1 : 0);
    }

    default void setFloat(String key, float value) {
        int loc = getGlLocation(key);
        if (loc != -1) glUniform1f(loc, value);
    }

    default void setFloat2(String key, Vector2f value) {
        int loc = getGlLocation(key);
        if (loc != -1) glUniform2f(loc, value.x, value.y);
    }

    default void setFloat3(String key, Vector3f value) {
        int loc = getGlLocation(key);
        if (loc != -1) glUniform3f(loc, value.x, value.y, value.z);
    }

    default void setFloat4(String key, Vector4f value) {
        int loc = getGlLocation(key);
        if (loc != -1) glUniform4f(loc, value.x, value.y, value.z, value.w);
    }

    default void setMatrix4(String key, Matrix4f value) {
        int loc = getGlLocation(key);
        if (loc != -1) glUniformMatrix4fv(loc, false, value.get(BufferUtils.createFloatBuffer(16)));
    }

    // Helper method
    default int getGlLocation(String key) {
        return glGetUniformLocation(getProgram(), key);
    }
}
