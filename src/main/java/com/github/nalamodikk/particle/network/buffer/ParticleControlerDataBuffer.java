package com.github.nalamodikk.particle.network.buffer;

import net.minecraft.resources.ResourceLocation;

public interface ParticleControlerDataBuffer<T> {

    record Id(ResourceLocation value) {
        public static Id toID(String string) {
            String[] split = string.split(":");
            if (split.length != 2) {
                throw new IllegalArgumentException("Invalid ID format: " + string);
            }
            return new Id(ResourceLocation.fromNamespaceAndPath(split[0], split[1]));
        }
    }

    T getLoadedValue();
    void setLoadedValue(T value);

    byte[] encode();
    byte[] encode(T value);

    T decode(byte[] buf);

    Id getBufferID();
}
