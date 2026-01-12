package com.github.nalamodikk.particle.render.shader.api.pipe.handler;

import com.github.nalamodikk.particle.render.shader.api.CooShaderProgram;

/**
 * ????????? * ??踐???賣??方???蹌?Shader Uniform ?鞊?
 */
@FunctionalInterface
public interface ShaderProgramUploader {
    /**
     * ??蹌?Shader ?鞊?
     * @param current ?????謓剝頛魂???Shader Program
     */
    void uploadShaderData(CooShaderProgram current);
}
