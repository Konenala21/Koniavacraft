package com.github.nalamodikk.particle.network.buffer;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.resources.ResourceLocation;

import java.nio.ByteBuffer;

/**
 * Double 類型數據緩衝區
 */
public class DoubleDataBuffer implements ParticleControlerDataBuffer<Double> {
    private static final Id BUFFER_ID = new Id(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "double"));

    private Double value;

    public DoubleDataBuffer() {
        this.value = 0.0;
    }

    public DoubleDataBuffer(Double value) {
        this.value = value;
    }

    @Override
    public Double getLoadedValue() {
        return value;
    }

    @Override
    public void setLoadedValue(Double value) {
        this.value = value;
    }

    @Override
    public byte[] encode() {
        return encode(value);
    }

    @Override
    public byte[] encode(Double value) {
        return ByteBuffer.allocate(8).putDouble(value).array();
    }

    @Override
    public Double decode(byte[] buf) {
        if (buf.length < 8) {
            throw new IllegalArgumentException("Buffer too small for double");
        }
        this.value = ByteBuffer.wrap(buf).getDouble();
        return this.value;
    }

    @Override
    public Id getBufferID() {
        return BUFFER_ID;
    }
}
