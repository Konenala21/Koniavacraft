package com.github.nalamodikk.particle.network.packet;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.particle.control.ControlType;
import com.github.nalamodikk.particle.network.buffer.ParticleControlerDataBuffer;
import com.github.nalamodikk.particle.network.buffer.ParticleControlerDataBuffers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record PacketParticleGroupS2C(
    UUID uuid,
    ControlType controlType,
    Map<String, ParticleControlerDataBuffer<?>> args
) implements CustomPacketPayload {

    public static final ResourceLocation ID = KoniavacraftMod.rl("particle_group");
    public static final CustomPacketPayload.Type<PacketParticleGroupS2C> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, PacketParticleGroupS2C> CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeUUID(packet.uuid);
            buf.writeInt(packet.controlType.getId());
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
            Map<String, ParticleControlerDataBuffer<?>> args = new HashMap<>();
            
            while (buf.readableBytes() > 0) {
                int len = buf.readInt();
                String key = buf.readUtf();
                byte[] bytes = new byte[len];
                buf.readBytes(bytes);
                ParticleControlerDataBuffer<?> decode = ParticleControlerDataBuffers.decodeToBuffer(bytes);
                args.put(key, decode);
            }
            
            return new PacketParticleGroupS2C(uuid, type, args);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public enum PacketArgsType {
        POS("pos"),
        CURRENT_TICK("current_tick"),
        MAX_TICK("max_tick"),
        ROTATE_TO("rotate_to"),
        ROTATE_AXIS("rotate_axis"),
        INVOKE("invoke"),
        AXIS("axis"),
        SCALE("scale"),
        GROUP_TYPE("groupType");

        public final String ofArgs;

        PacketArgsType(String ofArgs) {
            this.ofArgs = ofArgs;
        }

        public static PacketArgsType fromArgsName(String value) {
            for (PacketArgsType t : values()) {
                if (t.ofArgs.equals(value)) return t;
            }
            return INVOKE;
        }
    }
}
