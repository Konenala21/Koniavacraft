package com.github.nalamodikk.particle.render.shader.data;

public enum CooVertexFormat {
    POINT_FORMAT(3),
    POINT_COLOR_FORMAT(7),
    POINT_TEXTURE_UV_FORMAT(5),
    POINT_COLOR_TEXTURE_UV_FORMAT(9);

    private final int elementsPerVertex;

    CooVertexFormat(int elementsPerVertex) {
        this.elementsPerVertex = elementsPerVertex;
    }

    public int getElementsPerVertex() {
        return elementsPerVertex;
    }
}
