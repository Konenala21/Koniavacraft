package com.github.nalamodikk.particle.network.packet;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.particle.control.ControlType;
import com.github.nalamodikk.particle.network.buffer.ParticleControlerDataBuffer;
import com.github.nalamodikk.particle.network.buffer.ParticleControlerDataBuffers;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record PacketParticleStyleS2C(
    UUID uuid,
    ControlType controlType,
    Map<String, ParticleControlerDataBuffer<?>> args
) implements CustomPacketPayload {

    public static final ResourceLocation ID = KoniavacraftMod.rl("particle_style");
    public static final CustomPacketPayload.Type<PacketParticleStyleS2C> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, PacketParticleStyleS2C> CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeUUID(packet.uuid);
            buf.writeInt(packet.controlType.getId());
            buf.writeInt(packet.args.size());
            packet.args.forEach((key, value) -> {
                byte[] encode = ParticleControlerDataBuffers.encode(value);
                buf.writeInt(encode.length);
                buf.writeUtf(key);
                buf.writeBytes(encode);
            });
        },
        buf -> {
            UUID uuid = buf.readUUID();
            ControlType type = ControlType.getTypeById(buf.readInt());
            int argsCount = buf.readInt();
            Map<String, ParticleControlerDataBuffer<?>> args = new HashMap<>();
            
            for (int i = 0; i < argsCount; i++) {
                int len = buf.readInt();
                String key = buf.readUtf();
                byte[] bytes = new byte[len];
                buf.readBytes(bytes);
                ParticleControlerDataBuffer<?> decode = ParticleControlerDataBuffers.decodeToBuffer(bytes);
                args.put(key, decode);
            }
            
            return new PacketParticleStyleS2C(uuid, type, args);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
