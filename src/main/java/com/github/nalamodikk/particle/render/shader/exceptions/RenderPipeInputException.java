package com.github.nalamodikk.particle.render.shader.exceptions;

public class RenderPipeInputException extends RuntimeException {
    public RenderPipeInputException(int fbo, int channel) {
        super("Pipe Input Exception at FBO: " + fbo + ", Channel: " + channel);
    }
}
