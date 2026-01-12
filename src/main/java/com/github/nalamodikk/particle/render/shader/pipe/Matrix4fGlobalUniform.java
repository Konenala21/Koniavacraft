package com.github.nalamodikk.particle.render.shader.pipe;

import com.github.nalamodikk.particle.render.shader.api.CooShaderProgram;
import com.github.nalamodikk.particle.render.shader.api.pipe.GlobalUniform;
import org.joml.Matrix4f;

public class Matrix4fGlobalUniform extends GlobalUniform<Matrix4f> {
    public Matrix4fGlobalUniform(String key) {
        super(key);
        this.value = new Matrix4f(); // Identity by default
    }

    @Override
    public void upload(CooShaderProgram program) {
        program.setMatrix4(key, value);
    }
}
