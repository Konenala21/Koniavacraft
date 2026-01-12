package com.github.nalamodikk.particle.network.buffer;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.resources.ResourceLocation;

import java.nio.ByteBuffer;

/**
 * Float 類型數據緩衝區
 */
public class FloatDataBuffer implements ParticleControlerDataBuffer<Float> {
    private static final Id BUFFER_ID = new Id(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "float"));

    private Float value;

    public FloatDataBuffer() {
        this.value = 0.0f;
    }

    public FloatDataBuffer(Float value) {
        this.value = value;
    }

    @Override
    public Float getLoadedValue() {
        return value;
    }

    @Override
    public void setLoadedValue(Float value) {
        this.value = value;
    }

    @Override
    public byte[] encode() {
        return encode(value);
    }

    @Override
    public byte[] encode(Float value) {
        return ByteBuffer.allocate(4).putFloat(value).array();
    }

    @Override
    public Float decode(byte[] buf) {
        if (buf.length < 4) {
            throw new IllegalArgumentException("Buffer too small for float");
        }
        this.value = ByteBuffer.wrap(buf).getFloat();
        return this.value;
    }

    @Override
    public Id getBufferID() {
        return BUFFER_ID;
    }
}
