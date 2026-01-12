package com.github.nalamodikk.particle.render.shader.api.texture;

public interface GlTextures {

    void addTexture(GlTexture texture);

    int getTextureCounts();

    void init();

    void use();

    void reset();

    /**
     * ?秋撒?????Texture ??貔?潮??陪????
     */
    void drawWith(Runnable renderContext);
}
