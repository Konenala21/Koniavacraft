package com.github.nalamodikk.particle.network.buffer;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.resources.ResourceLocation;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * UUID 類型數據緩衝區
 */
public class UUIDDataBuffer implements ParticleControlerDataBuffer<UUID> {
    private static final Id BUFFER_ID = new Id(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "uuid"));

    private UUID value;

    public UUIDDataBuffer() {
        this.value = new UUID(0, 0);
    }

    public UUIDDataBuffer(UUID value) {
        this.value = value;
    }

    @Override
    public UUID getLoadedValue() {
        return value;
    }

    @Override
    public void setLoadedValue(UUID value) {
        this.value = value;
    }

    @Override
    public byte[] encode() {
        return encode(value);
    }

    @Override
    public byte[] encode(UUID value) {
        ByteBuffer buffer = ByteBuffer.allocate(16); // 2 * 8 bytes for 2 longs
        buffer.putLong(value.getMostSignificantBits());
        buffer.putLong(value.getLeastSignificantBits());
        return buffer.array();
    }

    @Override
    public UUID decode(byte[] buf) {
        if (buf.length < 16) {
            throw new IllegalArgumentException("Buffer too small for UUID");
        }
        ByteBuffer buffer = ByteBuffer.wrap(buf);
        long mostSigBits = buffer.getLong();
        long leastSigBits = buffer.getLong();
        this.value = new UUID(mostSigBits, leastSigBits);
        return this.value;
    }

    @Override
    public Id getBufferID() {
        return BUFFER_ID;
    }
}
