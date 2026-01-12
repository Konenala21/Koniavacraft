package com.github.nalamodikk.particle.network.buffer;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.resources.ResourceLocation;

/**
 * Boolean 類型數據緩衝區
 */
public class BooleanDataBuffer implements ParticleControlerDataBuffer<Boolean> {
    private static final Id BUFFER_ID = new Id(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "boolean"));

    private Boolean value;

    public BooleanDataBuffer() {
        this.value = false;
    }

    public BooleanDataBuffer(Boolean value) {
        this.value = value;
    }

    @Override
    public Boolean getLoadedValue() {
        return value;
    }

    @Override
    public void setLoadedValue(Boolean value) {
        this.value = value;
    }

    @Override
    public byte[] encode() {
        return encode(value);
    }

    @Override
    public byte[] encode(Boolean value) {
        return new byte[]{(byte) (value ? 1 : 0)};
    }

    @Override
    public Boolean decode(byte[] buf) {
        if (buf.length < 1) {
            throw new IllegalArgumentException("Buffer too small for boolean");
        }
        this.value = buf[0] != 0;
        return this.value;
    }

    @Override
    public Id getBufferID() {
        return BUFFER_ID;
    }
}
