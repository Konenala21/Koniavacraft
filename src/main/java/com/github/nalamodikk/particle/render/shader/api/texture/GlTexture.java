package com.github.nalamodikk.particle.render.shader.api.texture;

public interface GlTexture {
    int textureID();

    void init();

    /**
     * ??踐??????? Texture Unit ????堊垮餈?Texture
     */
    void useOnCurrent();

    void reset();
}
