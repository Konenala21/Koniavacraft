package com.github.nalamodikk.network.packet;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * 顯示實體同步封包 (Server to Client)
 */
public record PacketDisplayEntityS2C(UUID uuid, String entityType, byte[] data) implements CustomPacketPayload {
    public static final Type<PacketDisplayEntityS2C> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "display_entity_s2c"));

    public static final StreamCodec<FriendlyByteBuf, PacketDisplayEntityS2C> CODEC = CustomPacketPayload.codec(
            PacketDisplayEntityS2C::write,
            PacketDisplayEntityS2C::new);

    public PacketDisplayEntityS2C(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readUtf(), buf.readByteArray());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUtf(entityType);
        buf.writeByteArray(data);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
