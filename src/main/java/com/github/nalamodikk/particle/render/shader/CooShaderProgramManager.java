package com.github.nalamodikk.particle.render.shader;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.particle.render.shader.api.CooShaderProgram;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局 Shader 程式管理器
 */
public class CooShaderProgramManager {
    private static final CooShaderProgramManager INSTANCE = new CooShaderProgramManager();
    private final Map<ResourceLocation, CooShaderProgram> programs = new HashMap<>();
    
    private Matrix4f projectionMatrix = new Matrix4f();
    private Matrix4f modelViewMatrix = new Matrix4f();

    private CooShaderProgramManager() {}

    public static CooShaderProgramManager getInstance() {
        return INSTANCE;
    }

    public void register(ResourceLocation id, CooShaderProgram program) {
        programs.put(id, program);
        program.init();
        KoniavacraftMod.LOGGER.info("Registered Shader Program: {}", id);
    }

    public CooShaderProgram getProgram(ResourceLocation id) {
        return programs.get(id);
    }

    public void updateMatrices(Matrix4f projection, Matrix4f modelView) {
        this.projectionMatrix.set(projection);
        this.modelViewMatrix.set(modelView);
    }

    /**
     * 應用矩陣到指定的 Shader
     */
    public void applyMatrices(CooShaderProgram program) {
        program.setMatrix4("ProjMat", projectionMatrix);
        program.setMatrix4("ModelViewMat", modelViewMatrix);
    }

    public void releaseAll() {
        programs.values().forEach(CooShaderProgram::release);
        programs.clear();
    }
}
