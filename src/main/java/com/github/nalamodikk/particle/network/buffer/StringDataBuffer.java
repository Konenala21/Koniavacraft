package com.github.nalamodikk.particle.network.buffer;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.resources.ResourceLocation;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * String 類型數據緩衝區
 */
public class StringDataBuffer implements ParticleControlerDataBuffer<String> {
    private static final Id BUFFER_ID = new Id(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "string"));

    private String value;

    public StringDataBuffer() {
        this.value = "";
    }

    public StringDataBuffer(String value) {
        this.value = value;
    }

    @Override
    public String getLoadedValue() {
        return value;
    }

    @Override
    public void setLoadedValue(String value) {
        this.value = value;
    }

    @Override
    public byte[] encode() {
        return encode(value);
    }

    @Override
    public byte[] encode(String value) {
        byte[] stringBytes = value.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(4 + stringBytes.length);
        buffer.putInt(stringBytes.length);
        buffer.put(stringBytes);
        return buffer.array();
    }

    @Override
    public String decode(byte[] buf) {
        if (buf.length < 4) {
            throw new IllegalArgumentException("Buffer too small for String length");
        }
        ByteBuffer buffer = ByteBuffer.wrap(buf);
        int length = buffer.getInt();

        if (buffer.remaining() < length) {
            throw new IllegalArgumentException("Buffer too small for String data");
        }

        byte[] stringBytes = new byte[length];
        buffer.get(stringBytes);
        this.value = new String(stringBytes, StandardCharsets.UTF_8);
        return this.value;
    }

    @Override
    public Id getBufferID() {
        return BUFFER_ID;
    }
}
