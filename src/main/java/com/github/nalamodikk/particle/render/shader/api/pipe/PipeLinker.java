package com.github.nalamodikk.particle.render.shader.api.pipe;

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

public interface PipeLinker {
    
    PipeLinker link(ShaderPipe inputPipe, int inputChannel, ShaderPipe outputPipe, int outputChannel);

    default PipeLinker link(Pair<ShaderPipe, Integer> input, Pair<ShaderPipe, Integer> output) {
        return link(input.getKey(), input.getValue(), output.getKey(), output.getValue());
    }
    
    PipeLinker link(PipeLinkerNode input, PipeLinkerNode output);

    Map<Integer, PipeLinkerNode> findAllChannel(ShaderPipe input);
}
