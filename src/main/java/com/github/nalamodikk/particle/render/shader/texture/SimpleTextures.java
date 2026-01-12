package com.github.nalamodikk.particle.render.shader.texture;

import com.github.nalamodikk.particle.render.shader.api.texture.GlTexture;
import com.github.nalamodikk.particle.render.shader.api.texture.GlTextures;
import org.lwjgl.opengl.GL33;
import java.util.ArrayList;
import java.util.List;

public class SimpleTextures implements GlTextures {
    public static class VariablePair<K, V> {
        public K first;
        public V second;

        public VariablePair(K first, V second) {
            this.first = first;
            this.second = second;
        }
    }

    private final List<GlTexture> textureWithChannel = new ArrayList<>();
    private int lastTextureID = 0;
    private int lastActiveChannel = 0;
    private final VariablePair<Integer, Boolean>[] prevTextures;

    @SuppressWarnings("unchecked")
    public SimpleTextures() {
        this.prevTextures = new VariablePair[32];
        for (int i = 0; i < 32; i++) {
            this.prevTextures[i] = new VariablePair<>(0, false);
        }
    }

    @Override
    public void addTexture(GlTexture texture) {
        if (textureWithChannel.size() >= 32) {
            throw new IllegalArgumentException("????鞈????!");
        }
        textureWithChannel.add(texture);
    }

    @Override
    public int getTextureCounts() {
        return textureWithChannel.size();
    }

    @Override
    public void init() {
        for (GlTexture texture : textureWithChannel) {
            texture.init();
        }
    }

    @Override
    public void use() {
        lastActiveChannel = GL33.glGetInteger(GL33.GL_ACTIVE_TEXTURE);
        lastTextureID = GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
        
        for (int i = 0; i < textureWithChannel.size(); i++) {
            int zero = GL33.GL_TEXTURE0;
            int channel = zero + i;
            GL33.glActiveTexture(channel);
            int prev = GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
            
            prevTextures[i].first = prev;
            prevTextures[i].second = true;
            
            GL33.glBindTexture(GL33.GL_TEXTURE_2D, textureWithChannel.get(i).textureID());
        }
    }

    @Override
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

    @Override
    public void drawWith(Runnable renderContext) {
        use();
        renderContext.run();
        reset();
    }
}
