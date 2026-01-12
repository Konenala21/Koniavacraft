package com.github.nalamodikk.particle.render.shader.pipe.manager;

import com.github.nalamodikk.particle.render.shader.api.pipe.PipeChannels;
import com.github.nalamodikk.particle.render.shader.texture.SimpleTextures;
import org.lwjgl.opengl.GL33;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FramePipeChannels implements PipeChannels {
    private final List<Supplier<Integer>> channels = new ArrayList<>();
    private int lastTextureID = 0;
    private int lastActiveChannel = 0;
    private final SimpleTextures.VariablePair<Integer, Boolean>[] prevTextures;

    @SuppressWarnings("unchecked")
    public FramePipeChannels() {
        this.prevTextures = new SimpleTextures.VariablePair[32];
        for (int i = 0; i < 32; i++) {
            this.prevTextures[i] = new SimpleTextures.VariablePair<>(0, false);
        }
    }

    @Override
    public PipeChannels addChannel(Supplier<Integer> id) {
        if (channels.size() + 1 > 32) {
            throw new IllegalArgumentException("Only 32 channels supported");
        }
        channels.add(id);
        return this;
    }

    @Override
    public List<Supplier<Integer>> getChannels() {
        return new ArrayList<>(channels);
    }

    @Override
    public Supplier<Integer> getChannel(int index) {
        return channels.get(index);
    }

    @Override
    public int currentInputCount() {
        return channels.size();
    }

    @Override
    public PipeChannels useOnContext(Runnable vertexDraw) {
        use();
        vertexDraw.run();
        reset();
        return this;
    }

    public void use() {
        lastActiveChannel = GL33.glGetInteger(GL33.GL_ACTIVE_TEXTURE);
        lastTextureID = GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
        
        for (int i = 0; i < channels.size(); i++) {
            int zero = GL33.GL_TEXTURE0;
            int channel = zero + i;
            GL33.glActiveTexture(channel);
            int prev = GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
            
            prevTextures[i].first = prev;
            prevTextures[i].second = true;
            
            GL33.glBindTexture(GL33.GL_TEXTURE_2D, channels.get(i).get());
        }
    }

    public void reset() {
        for (int i = 0; i < prevTextures.length; i++) {
            if (!prevTextures[i].second) {
                continue;
            }
            int zero = GL33.GL_TEXTURE0;
            GL33.glActiveTexture(zero + i);
            GL33.glBindTexture(GL33.GL_TEXTURE_2D, prevTextures[i].first);
            prevTextures[i].second = false;
        }
        GL33.glActiveTexture(lastActiveChannel);
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, lastTextureID);
    }
}
