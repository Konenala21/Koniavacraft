package com.github.nalamodikk.particle.render.shader.api.pipe;

import java.util.Objects;

public class PipeLinkerNode {
    public final ShaderPipe pipe;
    public final int channel;

    public PipeLinkerNode(ShaderPipe pipe, int channel) {
        this.pipe = pipe;
        this.channel = channel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PipeLinkerNode that = (PipeLinkerNode) o;
        return channel == that.channel && Objects.equals(pipe, that.pipe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pipe, channel);
    }
}
