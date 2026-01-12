package com.github.nalamodikk.particle.render.shader.exceptions;

import net.minecraft.resources.ResourceLocation;

public class RenderPipeOutputNotSetException extends RuntimeException {
    public RenderPipeOutputNotSetException(ResourceLocation pipeID) {
        super("Pipe Output not set for pipe: " + pipeID);
    }
}
