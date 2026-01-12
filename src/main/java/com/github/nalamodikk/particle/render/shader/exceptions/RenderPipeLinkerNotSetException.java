package com.github.nalamodikk.particle.render.shader.exceptions;

import net.minecraft.resources.ResourceLocation;

public class RenderPipeLinkerNotSetException extends RuntimeException {
    public RenderPipeLinkerNotSetException(ResourceLocation pipeID) {
        super("Pipe Linker not set for pipe: " + pipeID);
    }
}
