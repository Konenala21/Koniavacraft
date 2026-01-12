package com.github.nalamodikk.particle.network.buffer;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.nio.ByteBuffer;

/**
 * Vec3 類型數據緩衝區
 */
public class Vec3DataBuffer implements ParticleControlerDataBuffer<Vec3> {
    private static final Id BUFFER_ID = new Id(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "vec3"));

    private Vec3 value;

    public Vec3DataBuffer() {
        this.value = Vec3.ZERO;
    }

    public Vec3DataBuffer(Vec3 value) {
        this.value = value;
    }

    @Override
    public Vec3 getLoadedValue() {
        return value;
    }

    @Override
    public void setLoadedValue(Vec3 value) {
        this.value = value;
    }

    @Override
    public byte[] encode() {
        return encode(value);
    }

    @Override
    public byte[] encode(Vec3 value) {
        ByteBuffer buffer = ByteBuffer.allocate(24); // 3 * 8 bytes for 3 doubles
        buffer.putDouble(value.x);
        buffer.putDouble(value.y);
        buffer.putDouble(value.z);
        return buffer.array();
    }

    @Override
    public Vec3 decode(byte[] buf) {
        if (buf.length < 24) {
            throw new IllegalArgumentException("Buffer too small for Vec3");
        }
        ByteBuffer buffer = ByteBuffer.wrap(buf);
        double x = buffer.getDouble();
        double y = buffer.getDouble();
        double z = buffer.getDouble();
        this.value = new Vec3(x, y, z);
        return this.value;
    }

    @Override
    public Id getBufferID() {
        return BUFFER_ID;
    }
}
