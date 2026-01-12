package com.github.nalamodikk.particle.render.shader.api.pipe;

import com.github.nalamodikk.particle.render.shader.api.CooShaderProgram;

public abstract class GlobalUniform<T> {
    public final String key;
    public T value;

    public GlobalUniform(String key) {
        this.key = key;
    }

    public abstract void upload(CooShaderProgram program);
}
