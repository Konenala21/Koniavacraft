package com.github.nalamodikk.particle.render.shader.pipe.manager;

import com.github.nalamodikk.particle.render.shader.api.pipe.PipeLinker;
import com.github.nalamodikk.particle.render.shader.api.pipe.PipeLinkerNode;
import com.github.nalamodikk.particle.render.shader.api.pipe.ShaderPipe;
import com.github.nalamodikk.particle.render.shader.exceptions.RenderPipeInputException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphPipeLinker implements PipeLinker {
    private final Map<PipeLinkerNode, List<PipeLinkerNode>> pipeQueue = new HashMap<>();

    @Override
    public PipeLinker link(ShaderPipe inputPipe, int inputChannel, ShaderPipe outputPipe, int outputChannel) {
        return link(new PipeLinkerNode(inputPipe, inputChannel), new PipeLinkerNode(outputPipe, outputChannel));
    }

    @Override
    public PipeLinker link(PipeLinkerNode input, PipeLinkerNode output) {
        if (hasLinked(input)) {
            throw new RenderPipeInputException(input.pipe.fbo().fbo(), input.channel);
        }
        int channelSize = output.pipe.fbo().getOutputChannelCount();
        if (channelSize <= output.channel) {
            throw new ArrayIndexOutOfBoundsException("output doesn't have enough channels");
        }
        pipeQueue.computeIfAbsent(output, k -> new ArrayList<>()).add(input);
        return this;
    }

    @Override
    public Map<Integer, PipeLinkerNode> findAllChannel(ShaderPipe input) {
        Map<Integer, PipeLinkerNode> res = new HashMap<>();
        for (Map.Entry<PipeLinkerNode, List<PipeLinkerNode>> entry : pipeQueue.entrySet()) {
            for (PipeLinkerNode node : entry.getValue()) {
                if (node.pipe == input) {
                    res.put(node.channel, entry.getKey());
                }
            }
        }
        return res;
    }

    private boolean hasLinked(PipeLinkerNode target) {
        for (List<PipeLinkerNode> list : pipeQueue.values()) {
            if (list.contains(target)) return true;
        }
        return false;
    }
}