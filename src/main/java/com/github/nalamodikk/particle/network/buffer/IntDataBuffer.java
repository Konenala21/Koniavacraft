package com.github.nalamodikk.particle.network.buffer;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.resources.ResourceLocation;

import java.nio.ByteBuffer;

/**
 * Integer 類型數據緩衝區
 */
public class IntDataBuffer implements ParticleControlerDataBuffer<Integer> {
    private static final Id BUFFER_ID = new Id(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "int"));

    private Integer value;

    public IntDataBuffer() {
        this.value = 0;
    }

    public IntDataBuffer(Integer value) {
        this.value = value;
    }

    @Override
    public Integer getLoadedValue() {
        return value;
    }

    @Override
    public void setLoadedValue(Integer value) {
        this.value = value;
    }

    @Override
    public byte[] encode() {
        return encode(value);
    }

    @Override
    public byte[] encode(Integer value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    @Override
    public Integer decode(byte[] buf) {
        if (buf.length < 4) {
            throw new IllegalArgumentException("Buffer too small for int");
        }
        this.value = ByteBuffer.wrap(buf).getInt();
        return this.value;
    }

    @Override
    public Id getBufferID() {
        return BUFFER_ID;
    }
}
