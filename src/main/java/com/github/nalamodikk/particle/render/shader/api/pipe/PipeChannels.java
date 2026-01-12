package com.github.nalamodikk.particle.render.shader.api.pipe;

import java.util.List;
import java.util.function.Supplier;

public interface PipeChannels {
    /**
     * ??????Channel
     */
    PipeChannels addChannel(Supplier<Integer> id);

    List<Supplier<Integer>> getChannels();

    Supplier<Integer> getChannel(int index);

    /**
     * ????岳??鞈?
     */
    int currentInputCount();

    /**
     * ????Channel ?秋撒???Context ??貔?潮???     */
    PipeChannels useOnContext(Runnable vertexDraw);
}
