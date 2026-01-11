package com.github.nalamodikk.network.codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

public class Vec3StreamCodec implements StreamCodec<RegistryFriendlyByteBuf, Vec3> {
    public static final Vec3StreamCodec INSTANCE = new Vec3StreamCodec();

    @Override
    public Vec3 decode(RegistryFriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf, Vec3 vec) {
        buf.writeDouble(vec.x);
        buf.writeDouble(vec.y);
        buf.writeDouble(vec.z);
    }
}
